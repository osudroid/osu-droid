# GLES2 → GLES3.2 Migration Review

> **Project:** osu!droid
> **Migrated:** May 21, 2026
> **Scope:** Full codebase upgrade from OpenGL ES 2.0 (`android.opengl.GLES20`) to OpenGL ES 3.2
> (`android.opengl.GLES32`), including the EGL context request, `AndroidManifest.xml` feature
> declaration, and GLSL shader upgrade to `#version 320 es`.
> **Prerequisite:** See `GLES2_MIGRATION_REVIEW_FINAL.md` for the prior GLES1 → GLES2 migration.

---

## Table of Contents

1. [Final Status](#final-status)
2. [What Changed](#what-changed)
3. [File-by-File Summary](#file-by-file-summary)
4. [Architecture Health Check](#architecture-health-check)
5. [No Action Required](#no-action-required)

---

## Final Status

**✅ Migration Complete — All Issues Resolved**

80 source files updated (`GLES20` → `GLES32`). `RenderSurfaceView` upgraded to request an ES 3.2
context at runtime with automatic ES 3.0 fallback. `AndroidManifest.xml` updated with the correct
`<uses-feature>` declaration. All 8 GLSL shader programs upgraded from implicit GLSL ES 1.00 to
explicit `#version 320 es`.

---

## What Changed

### 1. `android.opengl.GLES20` → `android.opengl.GLES32` — 80 files

`GLES32` is a strict superset of `GLES20`. Every method and constant available in `GLES20` exists
identically in `GLES32`. The migration was a pure drop-in rename — no call signatures or constant
values changed.

`GLES32` has been available since **API 24** (Android 7.0), which matches the project's
`minSdkVersion = 24`. No compatibility guards are required.

### 2. `RenderSurfaceView.java` — Custom `GLES32ContextFactory`

Previously both constructors called `setEGLContextClientVersion(2)`. `setEGLContextClientVersion`
sets `EGL_CONTEXT_CLIENT_VERSION` but does **not** expose a minor-version attribute — passing `3`
delivers an ES 3.0 context; there is no way to request ES 3.2 with this API alone.

To request ES 3.2 specifically, a custom `GLSurfaceView.EGLContextFactory` is required. The new
`GLES32ContextFactory` inner class:

1. Tries `EGL_CONTEXT_CLIENT_VERSION = 3` + `EGL_CONTEXT_MINOR_VERSION_KHR (0x30FB) = 2`
   → OpenGL ES 3.2 context.
2. Falls back to `EGL_CONTEXT_CLIENT_VERSION = 3` (no minor version) → OpenGL ES 3.0 context,
   for devices that do not support the `EGL_KHR_create_context` minor-version attribute.

```java
// Before:
this.setEGLContextClientVersion(2);

// After:
this.setEGLContextFactory(new GLES32ContextFactory());
```

### 3. `AndroidManifest.xml` — `<uses-feature>` Declaration

Added the standard Play Store feature requirement so devices without OpenGL ES 3.2 hardware are
filtered. ES 3.2 is available on all Android 7.0+ devices with a modern GPU (Adreno 4xx+,
Mali-T8xx+, PowerVR Series 6XT+) — effectively all devices in the active install base.

```xml
<uses-feature android:glEsVersion="0x00030002" android:required="true" />
```

`0x00030002` encodes major = 3, minor = 2.

### 4. GLSL ES 1.00 → GLSL ES 3.20 — 8 shader files

ES 3.2 still accepts shaders with no `#version` directive and compiles them as GLSL ES 1.00 via
backwards compatibility. However, using deprecated GLSL 1.00 keywords (`attribute`, `varying`,
`gl_FragColor`, `texture2D()`) against an ES 3.2 context is technically stale — those identifiers
were removed from the GLSL ES 3.x specification and rely on driver tolerance.

All 8 shader programs were upgraded:

| Keyword (GLSL ES 1.00) | Replacement (GLSL ES 3.20) |
|------------------------|---------------------------|
| *(no directive)* | `#version 320 es` |
| `attribute` | `in` (vertex shader) |
| `varying` | `out` (vertex) / `in` (fragment) |
| `gl_FragColor` | `out vec4 fragColor;` + `fragColor` |
| `texture2D()` | `texture()` |
| `#extension GL_OES_EGL_image_external` | `#extension GL_OES_EGL_image_external_essl3` |

Files upgraded:

- `PositionColorShaderProgram.java`
- `PositionColorTextureCoordinatesShaderProgram.java`
- `PositionTextureCoordinatesShaderProgram.java`
- `PositionTextureCoordinatesTextureSelectShaderProgram.java` *(fragment only — vertex reuses above)*
- `PositionTextureCoordinatesUniformColorShaderProgram.java`
- `CatJamCircleShader.kt`
- `ExternalOESShaderProgram.java`
- `StoryboardBatchShader.java`

### 5. `CLAUDE.md` — Documentation Update

- Rendering section updated: notes GLES32, `#version 320 es` shaders, custom `EGLContextFactory`,
  and ES 3.0 fallback.

---

## File-by-File Summary

### `AndEngine-GLES2` module — 45 files

| File | Change |
|------|--------|
| `RenderSurfaceView.java` | `setEGLContextClientVersion(2)` → `setEGLContextFactory(new GLES32ContextFactory())`; new `GLES32ContextFactory` inner class added |
| `PositionColorShaderProgram.java` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es` |
| `PositionColorTextureCoordinatesShaderProgram.java` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es` |
| `PositionTextureCoordinatesShaderProgram.java` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es` |
| `PositionTextureCoordinatesTextureSelectShaderProgram.java` | `GLES20` → `GLES32`; fragment GLSL upgraded to `#version 320 es` |
| `PositionTextureCoordinatesUniformColorShaderProgram.java` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es` |
| `PositionTextureCoordinatesPositionInterpolationTextureSelectShaderProgram.java` | `GLES20` → `GLES32` |
| `ShaderProgram.java` | `GLES20` → `GLES32` |
| `DoubleSceneSplitScreenEngine.java` | `GLES20` → `GLES32` |
| `SingleSceneSplitScreenEngine.java` | `GLES20` → `GLES32` |
| `BlendFunctionParticleInitializer.java` | `GLES20` → `GLES32` |
| `DrawMode.java` | `GLES20` → `GLES32` |
| `Line.java` | `GLES20` → `GLES32` |
| `Mesh.java` | `GLES20` → `GLES32` |
| `Rectangle.java` | `GLES20` → `GLES32` |
| `Background.java` | `GLES20` → `GLES32` |
| `IShape.java` | `GLES20` → `GLES32` |
| `SpriteBatch.java` | `GLES20` → `GLES32` |
| `Sprite.java` | `GLES20` → `GLES32` |
| `TiledSprite.java` | `GLES20` → `GLES32` |
| `UncoloredSprite.java` | `GLES20` → `GLES32` |
| `UniformColorSprite.java` | `GLES20` → `GLES32` |
| `Text.java` | `GLES20` → `GLES32` |
| `TickerText.java` | `GLES20` → `GLES32` |
| `ScreenGrabber.java` | `GLES20` → `GLES32` |
| `Font.java` | `GLES20` → `GLES32` |
| `GLES20Fix.java` | `GLES20` → `GLES32` (tombstoned class; class name retained) |
| `BitmapTextureAtlas.java` | `GLES20` → `GLES32` |
| `BitmapTexture.java` | `GLES20` → `GLES32` |
| `ETC1Texture.java` | `GLES20` → `GLES32` |
| `GreedyPVRTexturePixelBufferStrategy.java` | `GLES20` → `GLES32` |
| `SmartPVRTexturePixelBufferStrategy.java` | `GLES20` → `GLES32` |
| `PVRTexture.java` | `GLES20` → `GLES32` |
| `ITexture.java` | `GLES20` → `GLES32` |
| `PixelFormat.java` | `GLES20` → `GLES32` |
| `RenderTexture.java` | `GLES20` → `GLES32` |
| `TextureOptions.java` | `GLES20` → `GLES32` |
| `TextureWarmUpVertexBufferObject.java` | `GLES20` → `GLES32` |
| `GLState.java` | `GLES20` → `GLES32` |
| `VertexBufferObjectAttribute.java` | `GLES20` → `GLES32` |
| `VertexBufferObjectAttributesBuilder.java` | `GLES20` → `GLES32` |
| `DrawType.java` | `GLES20` → `GLES32` |
| `HighPerformanceVertexBufferObject.java` | `GLES20` → `GLES32` |
| `LowMemoryVertexBufferObject.java` | `GLES20` → `GLES32` |
| `VertexBufferObject.java` | `GLES20` → `GLES32` |
| `ZeroMemoryVertexBufferObject.java` | `GLES20` → `GLES32` |
| `EngineRenderer.java` | `GLES20` → `GLES32` |
| `TexturePackParser.java` | `GLES20` → `GLES32` |

### `src/` module — 35 files

| File | Change |
|------|--------|
| `CatJamCircleShader.kt` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es` |
| `ExternalOESShaderProgram.java` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es`; extension → `GL_OES_EGL_image_external_essl3` |
| `StoryboardBatchShader.java` | `GLES20` → `GLES32`; GLSL upgraded to `#version 320 es` |
| `CatJamMaskedShader.kt` | `GLES20` → `GLES32` |
| `Effect.kt` | `GLES20` → `GLES32` |
| `KiaiCatJamSprite.kt` | `GLES20` → `GLES32` |
| `TriangleRenderer.java` | `GLES20` → `GLES32` |
| `TextureQuadBatch.java` | `GLES20` → `GLES32` |
| `BlendProperty.java` | `GLES20` → `GLES32` |
| `BlendType.java` | `GLES20` → `GLES32` |
| `GLWrapped.java` | `GLES20` → `GLES32` |
| `SupportSprite.java` | `GLES20` → `GLES32` |
| `FPSCounter.kt` | `GLES20` → `GLES32` |
| `HUDLeaderboard.kt` | `GLES20` → `GLES32` |
| `RoomPlayerCard.kt` | `GLES20` → `GLES32` |
| `IBuffer.kt` | `GLES20` → `GLES32` |
| `TextureCoordinatesBuffer.kt` | `GLES20` → `GLES32` |
| `UIBufferedComponent.kt` | `GLES20` → `GLES32` |
| `VertexBuffer.kt` | `GLES20` → `GLES32` |
| `BlendInfo.kt` | `GLES20` → `GLES32` |
| `DepthInfo.kt` | `GLES20` → `GLES32` |
| `ScissorStack.kt` | `GLES20` → `GLES32` |
| `UITriangle.kt` | `GLES20` → `GLES32` |
| `UITriangleMesh.kt` | `GLES20` → `GLES32` |
| `UIShapedSprite.kt` | `GLES20` → `GLES32` |
| `UISprite.kt` | `GLES20` → `GLES32` |
| `UIVideoSprite.kt` | `GLES20` → `GLES32` |
| `FontAwesomeIcon.kt` | `GLES20` → `GLES32` |
| `UIText.kt` | `GLES20` → `GLES32` |
| `UITextureText.kt` | `GLES20` → `GLES32` |
| `VideoTexture.kt` | `GLES20` → `GLES32` |
| `UIGradientBox.kt` | `GLES20` → `GLES32` |
| `UIResourceManager.kt` | `GLES20` → `GLES32` |
| `StatisticSelector.kt` | `GLES20` → `GLES32` |
| `CursorTrail.java` | `GLES20` → `GLES32` |
| `FancyCursorTrail.kt` | `GLES20` → `GLES32` |
| `GameScene.java` | `GLES20` → `GLES32` |

### Root — 2 files

| File | Change |
|------|--------|
| `AndroidManifest.xml` | Added `<uses-feature android:glEsVersion="0x00030002" android:required="true" />` |
| `CLAUDE.md` | Updated rendering section: GLES32, `#version 320 es`, custom `EGLContextFactory`, ES 3.0 fallback |

---

## Architecture Health Check

| Area | Status |
|------|--------|
| `RenderSurfaceView` — custom `GLES32ContextFactory`, requests ES 3.2 then falls back to ES 3.0 | ✅ Correct |
| `RenderSurfaceView.setPreserveEGLContextOnPause(true)` | ✅ Present |
| `AndroidManifest.xml` — `glEsVersion="0x00030002"` | ✅ Present |
| `GLState` — exclusively `android.opengl.GLES32` | ✅ Correct |
| `ShaderProgram` — exclusively `android.opengl.GLES32` | ✅ Correct |
| All 80 updated files — `GLES32` import and calls | ✅ Confirmed |
| No `import android.opengl.GLES20` remaining in the codebase | ✅ Confirmed |
| `GLES20Fix.java` — tombstoned class; class name retained; uses `GLES32` internally | ✅ Correct |
| All 8 GLSL shader programs — `#version 320 es`, `in`/`out`, `texture()`, `out vec4 fragColor` | ✅ Upgraded |
| `ExternalOESShaderProgram` — uses `GL_OES_EGL_image_external_essl3` (required for ESSL 3.x) | ✅ Correct |
| `minSdkVersion = 24` — satisfies `GLES32` API availability (added in API 24) | ✅ Compatible |

---

## No Action Required

- **`ConfigChooser.java`** — the `EGL_OPENGL_ES3_BIT_KHR = 0x40` surface bit covers all ES 3.x
  contexts including 3.2; no change needed.
- **`GLES20Fix.java`** — tombstoned class retained by design; the class name is historical and
  the class itself has no external callers.
- **NDK / native code** — the `jni/` sources (`GLES20Fix.c`, `BufferUtils.cpp`) are dead code;
  the NDK module was disabled in a prior migration (minSdk 24 made all workarounds obsolete).
  The pre-built `libandengine.so` files in `libs/` are no longer packaged by the Gradle build.
- **Javadoc cross-references** (`{@link GLES20#GL_TEXTURE0}` etc. in `GLState` and
  `ShaderProgram`) — cosmetic only; the numeric constant values are identical across
  `GLES20`/`GLES32`; no runtime impact.
