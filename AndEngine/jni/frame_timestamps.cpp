// osu!droid ONLY
#include <jni.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/log.h>

#include <cstddef>
#include <cstring>

namespace {

constexpr char LOG_TAG[] = "FrameTimestamps";
constexpr char EXTENSION_NAME[] = "EGL_ANDROID_get_frame_timestamps";

// Constants from FrameTimestampSource
constexpr jlong RESULT_DROPPED = -1;
constexpr jlong RESULT_PENDING = -2;
constexpr jlong RESULT_UNKNOWN = -3;
constexpr jlong NO_FRAME_ID = -1;

// Constants from NativeFrameTimestampSource
constexpr jint START_OK = 0;
constexpr jint START_UNSUPPORTED = 1;
constexpr jint START_FAILED = 2;

// The number of frames handled per pair of array region copies.
constexpr jsize CHUNK_SIZE = 32;

// Resolved once by nativeStart, on the GL thread. Either all of them are set or none.
PFNEGLGETNEXTFRAMEIDANDROIDPROC sGetNextFrameId = nullptr;
PFNEGLGETFRAMETIMESTAMPSANDROIDPROC sGetFrameTimestamps = nullptr;
PFNEGLGETFRAMETIMESTAMPSUPPORTEDANDROIDPROC sGetFrameTimestampSupported = nullptr;

// Whether pExtensions, a space separated list, contains pName as a whole entry.
bool hasExtension(const char* pExtensions, const char* pName) {
    const size_t nameLength = std::strlen(pName);

    for (const char* match = std::strstr(pExtensions, pName); match != nullptr;
         match = std::strstr(match + 1, pName)) {
        const bool startsEntry = match == pExtensions || match[-1] == ' ';

        if (const bool endsEntry = match[nameLength] == '\0' || match[nameLength] == ' '; startsEntry && endsEntry) {
            return true;
        }
    }

    return false;
}

bool resolveFunctions() {
    if (sGetNextFrameId != nullptr) {
        return true;
    }

    const auto getNextFrameId = reinterpret_cast<PFNEGLGETNEXTFRAMEIDANDROIDPROC>(
            eglGetProcAddress("eglGetNextFrameIdANDROID"));

    const auto getFrameTimestamps = reinterpret_cast<PFNEGLGETFRAMETIMESTAMPSANDROIDPROC>(
            eglGetProcAddress("eglGetFrameTimestampsANDROID"));

    const auto getFrameTimestampSupported = reinterpret_cast<PFNEGLGETFRAMETIMESTAMPSUPPORTEDANDROIDPROC>(
            eglGetProcAddress("eglGetFrameTimestampSupportedANDROID"));

    if (getNextFrameId == nullptr || getFrameTimestamps == nullptr || getFrameTimestampSupported == nullptr) {
        return false;
    }

    sGetNextFrameId = getNextFrameId;
    sGetFrameTimestamps = getFrameTimestamps;
    sGetFrameTimestampSupported = getFrameTimestampSupported;

    return true;
}

// Maps the outcome of a timestamp query to a RESULT_ constant or a present time.
jlong mapResult(const EGLBoolean pSucceeded, const EGLnsecsANDROID pValue) {
    if (pSucceeded != EGL_TRUE) {
        return RESULT_UNKNOWN;
    }

    if (pValue == EGL_TIMESTAMP_PENDING_ANDROID) {
        return RESULT_PENDING;
    }

    if (pValue == EGL_TIMESTAMP_INVALID_ANDROID) {
        return RESULT_DROPPED;
    }

    return pValue;
}

// Fills the first pCount results with RESULT_UNKNOWN.
void fillUnknown(JNIEnv* pEnv, const jlongArray pResults, const jsize pCount) {
    jlong unknown[CHUNK_SIZE];

    for (jsize i = 0; i < CHUNK_SIZE; ++i) {
        unknown[i] = RESULT_UNKNOWN;
    }

    for (jsize offset = 0; offset < pCount; offset += CHUNK_SIZE) {
        const jsize length = pCount - offset < CHUNK_SIZE ? pCount - offset : CHUNK_SIZE;

        pEnv->SetLongArrayRegion(pResults, offset, length, unknown);

        if (pEnv->ExceptionCheck()) {
            return;
        }
    }
}

}  // namespace

extern "C" {

JNIEXPORT jint JNICALL
Java_org_anddev_andengine_opengl_view_NativeFrameTimestampSource_nativeStart(JNIEnv*, jclass) {
    const EGLDisplay display = eglGetCurrentDisplay();
    const EGLSurface surface = eglGetCurrentSurface(EGL_DRAW);

    if (display == EGL_NO_DISPLAY || surface == EGL_NO_SURFACE) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "No current EGL surface");
        return START_FAILED;
    }

    const char* extensions = eglQueryString(display, EGL_EXTENSIONS);

    if (extensions == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "Cannot query the EGL extensions");
        return START_FAILED;
    }

    if (!hasExtension(extensions, EXTENSION_NAME) || !resolveFunctions()) {
        return START_UNSUPPORTED;
    }

    if (sGetFrameTimestampSupported(display, surface, EGL_DISPLAY_PRESENT_TIME_ANDROID) != EGL_TRUE) {
        return START_UNSUPPORTED;
    }

    if (eglSurfaceAttrib(display, surface, EGL_TIMESTAMPS_ANDROID, EGL_TRUE) != EGL_TRUE) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "Cannot enable timestamps (EGL error 0x%x)", eglGetError());
        return START_FAILED;
    }

    return START_OK;
}

JNIEXPORT void JNICALL
Java_org_anddev_andengine_opengl_view_NativeFrameTimestampSource_nativeStop(JNIEnv*, jclass) {
    if (sGetNextFrameId == nullptr) {
        return;
    }

    const EGLDisplay display = eglGetCurrentDisplay();
    const EGLSurface surface = eglGetCurrentSurface(EGL_DRAW);

    if (display == EGL_NO_DISPLAY || surface == EGL_NO_SURFACE) {
        return;
    }

    eglSurfaceAttrib(display, surface, EGL_TIMESTAMPS_ANDROID, EGL_FALSE);
}

JNIEXPORT jlong JNICALL
Java_org_anddev_andengine_opengl_view_NativeFrameTimestampSource_nativeQueryAndGetNextFrameId(
        JNIEnv* pEnv, jclass, const jlongArray pFrameIds, const jint pCount, const jlongArray pResults) {
    // Keep count below both arrays' capacity.
    jsize count = pCount;
    const jsize frameIdsLength = pEnv->GetArrayLength(pFrameIds);
    const jsize resultsLength = pEnv->GetArrayLength(pResults);

    if (count > frameIdsLength) {
        count = frameIdsLength;
    }

    if (count > resultsLength) {
        count = resultsLength;
    }

    if (count < 0) {
        count = 0;
    }

    const EGLDisplay display = eglGetCurrentDisplay();
    const EGLSurface surface = eglGetCurrentSurface(EGL_DRAW);

    if (sGetNextFrameId == nullptr || display == EGL_NO_DISPLAY || surface == EGL_NO_SURFACE) {
        fillUnknown(pEnv, pResults, count);
        return NO_FRAME_ID;
    }

    static constexpr EGLint timestampNames[] = {EGL_DISPLAY_PRESENT_TIME_ANDROID};

    jlong frameIds[CHUNK_SIZE];
    jlong results[CHUNK_SIZE];

    for (jsize offset = 0; offset < count; offset += CHUNK_SIZE) {
        const jsize length = count - offset < CHUNK_SIZE ? count - offset : CHUNK_SIZE;

        pEnv->GetLongArrayRegion(pFrameIds, offset, length, frameIds);

        if (pEnv->ExceptionCheck()) {
            return NO_FRAME_ID;
        }

        for (jsize i = 0; i < length; ++i) {
            EGLnsecsANDROID presentTime = EGL_TIMESTAMP_INVALID_ANDROID;
            const EGLBoolean succeeded = sGetFrameTimestamps(
                    display, surface, static_cast<EGLuint64KHR>(frameIds[i]), 1, timestampNames, &presentTime);

            results[i] = mapResult(succeeded, presentTime);
        }

        pEnv->SetLongArrayRegion(pResults, offset, length, results);

        if (pEnv->ExceptionCheck()) {
            return NO_FRAME_ID;
        }
    }

    EGLuint64KHR nextFrameId = 0;

    if (sGetNextFrameId(display, surface, &nextFrameId) != EGL_TRUE) {
        return NO_FRAME_ID;
    }

    return static_cast<jlong>(nextFrameId);
}

}  // extern "C"
