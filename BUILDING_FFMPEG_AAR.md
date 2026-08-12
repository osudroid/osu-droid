# Building media3-decoder-ffmpeg.aar

This guide documents how to rebuild `libs/media3-decoder-ffmpeg.aar` from source.
The AAR available on Maven Central ships only Java/Kotlin wrappers, so the compiled
FFmpeg `.so` files must be built manually via the Android NDK.

All shell commands run on **Linux**, **macOS**, or **WSL** on Windows.

---

## Prerequisites

- **Android NDK r26b** (recommended by Media3) — download from the [Android NDK archive](https://github.com/android/ndk/wiki/Unsupported-Downloads). Other recent versions (r25b confirmed working) should also work when targeting API 24.
- **Android SDK** with at least one platform installed
- **Java 17**
- **Git**

---

## Step 0 — Set shell variables

Define these variables once. All subsequent commands reference them.

```bash
# Root of the cloned androidx/media repository
MEDIA3_ROOT=~/media3

# Android NDK location
NDK_PATH=~/android-ndk-r26b

# Android SDK location
ANDROID_HOME=~/android-sdk

# Java 17 home (adjust to match your installation)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Shorthand used by build_ffmpeg.sh
FFMPEG_JNI="$MEDIA3_ROOT/libraries/decoder_ffmpeg/src/main/jni"
```

---

## Step 1 — Clone Media3

```bash
git clone https://github.com/androidx/media.git "$MEDIA3_ROOT"
```

---

## Step 2 — Fetch the FFmpeg source

The Media3 decoder module expects FFmpeg source at `$FFMPEG_JNI/ffmpeg/`.
It is not included in the Media3 repository, so clone it separately:

```bash
cd "$FFMPEG_JNI"
git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg
cd ffmpeg && git checkout release/6.0
```

---

## Step 3 — Compile FFmpeg static libraries

Run the bundled build script from the `jni/` directory:

```bash
cd "$FFMPEG_JNI"

./build_ffmpeg.sh \
  "$NDK_PATH" \
  linux-x86_64 \
  "armeabi-v7a arm64-v8a x86 x86_64" \
  24
```

**Arguments:**
| Argument | Description |
|---|---|
| `"$NDK_PATH"` | Path to the NDK root directory |
| `linux-x86_64` | Host platform (use `darwin-arm64` on Apple Silicon Macs, `darwin-x86_64` on Intel Macs) |
| `"armeabi-v7a arm64-v8a x86 x86_64"` | Target ABIs, space-separated and quoted |
| `24` | Android API level. Must match or exceed `minSdkVersion` in [`build.gradle`](build.gradle) |

> **Important:** The 4th argument controls both FFmpeg's `./configure` step and
> the CMake linker sysroot. Passing an API level below 23 causes a linker error
> because `stderr` is not exported by Android's `libc.so` stubs until API 23.
> Always pass at least `24` here.

On success, static libraries appear at:

```
$FFMPEG_JNI/ffmpeg/android-libs/arm64-v8a/libavcodec.a
$FFMPEG_JNI/ffmpeg/android-libs/arm64-v8a/libavutil.a
$FFMPEG_JNI/ffmpeg/android-libs/arm64-v8a/libswresample.a
# (same for armeabi-v7a, x86, x86_64)
```

---

## Step 4 — Build the AAR with Gradle

```bash
cd "$MEDIA3_ROOT"

ANDROID_HOME="$ANDROID_HOME" \
JAVA_HOME="$JAVA_HOME" \
./gradlew :lib-decoder-ffmpeg:assembleRelease
```

The AAR is written to:

```
$MEDIA3_ROOT/libraries/decoder_ffmpeg/buildout/outputs/aar/lib-decoder-ffmpeg-release.aar
```

> Note: the output directory is `buildout/`, not the standard `build/`.

---

## Step 5 — Copy the AAR into the project

```bash
# Set this to your project root
OSU_DROID_ROOT=~/osu-droid

cp "$MEDIA3_ROOT/libraries/decoder_ffmpeg/buildout/outputs/aar/lib-decoder-ffmpeg-release.aar" \
   "$OSU_DROID_ROOT/libs/media3-decoder-ffmpeg.aar"
```
