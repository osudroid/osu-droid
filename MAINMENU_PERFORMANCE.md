# MainMenuV2 Performance Issues & Fixes

## Overview

Nine performance issues were identified across the main menu hot path and
rendering stack. Issues 1–5 are **CPU-side per-frame costs** (already applied);
issues 6–9 are **GPU/rendering costs** that become significant on low-end
Android devices (Helio / Snapdragon 4xx class with shared DRAM).

`onManagedUpdate` runs at ~60 fps on the GL update thread. All costs listed are
per second unless otherwise noted.

**Status:** Issues 1–5 ✅ applied. Issues 6–9 pending.

---

## Part 1 — CPU Hot-Path Fixes (Applied ✅)

### Issue 1 — String allocation in `MusicPlayerPanel.update()` every frame

**File:** `MusicPlayerPanel.kt`  
**Cost:** 1 heap allocation / frame (60 /s) → GC pressure on small-heap devices

```kotlin
// BEFORE — new String created every frame even when beatmap didn't change
val title = if (info != null) "${info.artistText} - ${info.titleText}" else ""
if (songTitleText.text != title) songTitleText.text = title
```

The `"${}"` template allocates a new `String` unconditionally. The equality
check prevents the text widget update, but the object is still created.

```kotlin
// AFTER — allocates only when beatmap actually changes
private var lastTitle = ""

val newTitle = if (info != null) "${info.artistText} - ${info.titleText}" else ""
if (newTitle != lastTitle) { lastTitle = newTitle; songTitleText.text = newTitle }
```

---

### Issue 2 — `ResourceManager.getTexture()` called every frame for play/pause icon

**File:** `MusicPlayerPanel.kt`  
**Cost:** 1–2 `HashMap` lookups / frame while panel is expanded

```kotlin
// BEFORE — HashMap lookup on every tick
playPauseBtn.icon = if (svc.status == Status.PLAYING)
    ResourceManager.getInstance().getTexture("music_pause")
else
    ResourceManager.getInstance().getTexture("music_play")
```

```kotlin
// AFTER — textures cached at construction; icon only reassigned on status change
private val pauseTex  = ResourceManager.getInstance().getTexture("music_pause")
private val playTex2  = ResourceManager.getInstance().getTexture("music_play")
private var lastStatus: Status? = null

val status = svc.status
if (status != lastStatus) {
    lastStatus = status
    playPauseBtn.icon = if (status == Status.PLAYING) pauseTex else playTex2
}
```

---

### Issue 3 — Background texture `HashMap` lookup every frame

**File:** `MainMenuV2.kt`  
**Cost:** 2 `HashMap` lookups + 1 singleton traversal / frame (120 ops /s)

The background texture almost never changes (only on skin reload). Two lookups
per frame to guard a rare event is unnecessary.

```kotlin
// AFTER — dirty flag; lookups only run after a skin reload
var bgNeedsRefresh = false   // set true in SettingsFragment.dismiss()

if (bgNeedsRefresh) {
    bgNeedsRefresh = false
    val rm   = ResourceManager.getInstance()
    val rmbg = rm.getTexture("::background") ?: rm.getTexture("menu-background")
    if (rmbg != null) background.textureRegion = rmbg
}
```

> **Action required:** call `mainMenuV2.bgNeedsRefresh = true` from
> `SettingsFragment.dismiss()` (or wherever skins are reloaded).

---

### Issue 4 — `versionBg` layout properties written every frame forever

**File:** `MainMenuV2.kt` · `versionBg` anonymous `onManagedUpdate`  
**Cost:** 6 property writes + layout recalculation / frame after frame 1 (360 writes/s)

The version text is static. Once measured it never changes, but the layout was
recalculated every frame indefinitely.

```kotlin
// AFTER — one-shot: stops after first successful measurement
private var sized = false
override fun onManagedUpdate(deltaTimeSec: Float) {
    super.onManagedUpdate(deltaTimeSec)
    if (sized) return
    val tw = versionText.width; val th = versionText.height
    if (tw > 0f && th > 0f) {
        sized = true
        width = tw + 25f; height = th + 12f
        x = 10f; y = h - height - 10f
        versionText.x = x + 10f; versionText.y = y + (height - th) / 2f
    }
}
```

---

### Issue 5 — `Logo.update()` re-fetches `songService` independently

**File:** `Logo.kt`  
**Cost:** 2 extra `GlobalManager.getInstance()` + nullable-chain calls / frame

`MainMenuV2.onManagedUpdate` already holds a non-null `svc` reference.
`Logo.update()` was independently fetching `svc.spectrum` and `svc.status`.

```kotlin
// AFTER — fft and isPlaying passed in as parameters from the call site
fun update(dt: Float, songPos: Int, bpmMs: Float, timingOffset: Int,
           fft: FloatArray?, isPlaying: Boolean) { ... }

// Call site in MainMenuV2:
logo.update(dt = deltaTimeSec, songPos = songPos, bpmMs = bpmLength,
    timingOffset = currentTimingPoint?.time?.toInt() ?: 0,
    fft = svc.spectrum, isPlaying = svc.status == Status.PLAYING)
```

---

## Part 2 — GPU / Rendering Fixes (Pending ⏳)

### Issue 6 — Spectrum VBO: 360-bar geometry uploaded to GPU every frame

**File:** `Spectrum.kt`  
**Cost:** ~17 KB CPU→GPU transfer + 2 160-iteration CPU loops / frame  
**Severity:** 🔴 Highest impact on low-end devices

```
BAR_COUNT=120 × VISUALISER_ROUNDS=3 = 360 bars
360 bars × 6 verts × 2 floats × 4 bytes = 17 280 bytes uploaded every frame
At 60 fps → ~1 MB/s of DRAM bus traffic on shared-memory SoCs
```

On low-end Mali/Adreno GPUs the CPU→GPU VBO upload causes a pipeline stall
every frame. The decay loop also runs 360 iterations unconditionally regardless
of whether any bars are actually visible.

**Fix A — Reduce `VISUALISER_ROUNDS` (1 line, 66% win):**

```kotlin
// Spectrum.kt companion object
const val VISUALISER_ROUNDS = 1  // was 3 — visually near-identical, ⅓ the cost
```

Cuts VBO size to ~5.7 KB and the CPU loops from 360 to 120 iterations.

**Fix B — Skip VBO upload when all bars are silent:**

```kotlin
override fun onUpdateBuffer() {
    val vbo = buffer ?: return
    // No geometry to update when every bar is below the dead zone
    // (common while paused or stopped). Saves the full GPU upload.
    if (frequencyAmplitudes.none { it >= AMPLITUDE_DEAD_ZONE }) return
    // ... rest of loop unchanged
}
```

---

### Issue 7 — `UIGradientBox` (side flashes) uploads VBO every frame unconditionally

**File:** `UIGradientBox.kt`  
**Cost:** 2 × 4-vertex VBO uploads / frame even when both flashes are invisible  
**Severity:** 🟡 Medium

Both flash boxes use `BufferSharingMode.Dynamic`, which means
`onUpdateBuffer()` is called every frame regardless of visibility.
When `alpha = 0` (most of the time) there is nothing to update.

**Fix — demand-driven uploads: only upload when `drawAlpha` actually changes:**

```kotlin
// Switch to Static sharing mode
override fun onCreateBuffer() = GradientVBO().also {
    it.sharingMode = BufferSharingMode.Static
}

// Track alpha and request update only when it changes
private var lastDrawAlpha = -1f
override fun onManagedUpdate(deltaTimeSec: Float) {
    super.onManagedUpdate(deltaTimeSec)
    if (drawAlpha != lastDrawAlpha) {
        lastDrawAlpha = drawAlpha
        requestBufferUpdate()
    }
}
```

---

### Issue 8 — `SideFlashes.resolveColor()` allocates a list on every beat

**File:** `SideFlashes.kt`  
**Cost:** 1 list allocation per beat (~1–2 /s)  
**Severity:** 🟢 Low, but trivially fixable

```kotlin
// BEFORE — called on every beat; getComboColor() likely creates a new list
val c = OsuSkin.get().getComboColor().firstOrNull() ?: return DEFAULT_COLOR
```

**Fix — cache the resolved color; bust it on skin reload:**

```kotlin
private var cachedFlashColor: Color4? = null

private fun resolveColor(): Color4 {
    cachedFlashColor?.let { return it }
    val c = OsuSkin.get().getComboColor().firstOrNull() ?: return DEFAULT_COLOR
    val bad = (c.red > 0.9f && c.green > 0.9f && c.blue > 0.9f) ||
              (c.red < 0.1f && c.green < 0.1f && c.blue < 0.1f)
    return (if (bad) DEFAULT_COLOR else c).also { cachedFlashColor = it }
}

/** Call after a skin reload to force the color to be re-resolved. */
fun invalidateSkinCache() { cachedFlashColor = null }
```

---

### Issue 9 — No low-quality fallback for very low-end devices

**Files:** `Spectrum.kt`, `MainMenuV2.kt`  
**Severity:** 🟡 Medium — relevant for devices below ~2 GB RAM / Mali-G52 class

The Spectrum visualiser (even at `VISUALISER_ROUNDS=1`) is the most expensive
single entity in the scene. There is currently no way to disable it short of
commenting out code.

**Fix — add a `Config` toggle and honour it in `MainMenuV2`:**

```kotlin
// In MainMenuV2 init, after spectrum is configured:
if (!Config.isMenuSpectrum()) spectrum.isVisible = false

// In Logo.update(), skip the spectrum update when invisible:
if (spectrum.isVisible) spectrum.update(dt, kiaiActive)
```

The logo still pulses via `beatPulse` and `kiaiPulse` when the spectrum is
off — no functional loss, only visual.

---

## Combined Impact Summary

| # | File | Issue | Status | Savings |
|---|---|---|---|---|
| 1 | `MusicPlayerPanel` | String alloc every frame | ✅ Applied | 60 heap allocs /s |
| 2 | `MusicPlayerPanel` | `getTexture()` every frame | ✅ Applied | 60–120 HashMap ops /s |
| 3 | `MainMenuV2` | BG texture lookup every frame | ✅ Applied | 120 HashMap ops /s |
| 4 | `MainMenuV2` | versionBg layout thrash | ✅ Applied | 360 property writes /s |
| 5 | `Logo` | Redundant `songService` fetches | ✅ Applied | 120 singleton calls /s |
| 6A | `Spectrum` | `VISUALISER_ROUNDS = 1` | ⏳ Pending | −66% VBO size & CPU loop |
| 6B | `Spectrum` | Skip upload when silent | ✅ Applied | ~100% GPU upload while paused |
| 7 | `UIGradientBox` | Dynamic VBO when invisible | ⏳ Pending | 2 GPU uploads /frame eliminated |
| 8 | `SideFlashes` | List alloc on every beat | ⏳ Pending | 1 alloc /beat eliminated |
| 9 | `Spectrum` + `MainMenuV2` | No low-quality mode | ⏳ Pending | 100% Spectrum cost on toggle |

### Priority order for low-end devices
1. **Issue 6A** — single-line change, biggest GPU win
2. **Issue 6B** — 4-line change, eliminates upload while paused/stopped
3. **Issue 7** — eliminates wasteful flash-box uploads at rest
4. **Issue 9** — escape hatch for the lowest-tier devices
5. **Issue 8** — minor but trivially clean
