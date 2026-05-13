/*
 * GLES20Fix.c — REMOVED (dead code)
 *
 * These JNI stubs backed org.andengine.opengl.GLES20Fix, which originally
 * worked around missing GLES20 entry points on Android 2.2 (Froyo / API 8,
 * issue #8931). GLES20Fix.java was cleaned up in the GLES1→GLES2 migration
 * (Round 1): the System.loadLibrary call and the two 'native' method
 * declarations were removed, so these entry points have no Java callers.
 *
 * The andengine_shared NDK module that compiled this file was disabled
 * alongside BufferUtils.cpp when the last remaining Java-side JNI usage
 * was removed (minSdk 24 — all legacy workaround paths dead).
 *
 * File kept as a historical reference only; it is not compiled.
 */
