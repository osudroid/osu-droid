/*
 * BufferUtils.cpp — REMOVED (dead code)
 *
 * These JNI stubs backed three native methods in
 * org.andengine.opengl.util.BufferUtils:
 *
 *   jniPut           — Froyo (API ≤ 8) ByteBuffer.put float-array workaround
 *   jniAllocateDirect — Honeycomb (API 11-12) ByteBuffer.allocateDirect workaround
 *   jniFreeDirect    — companion free for jniAllocateDirect
 *
 * With minSdkVersion = 24 both workaround flags were always false and the
 * native methods were never called. The 'native' declarations and the
 * System.loadLibrary("andengine") call were removed from BufferUtils.java
 * as part of the GLES1→GLES2 migration cleanup (Issue 2), leaving these
 * entry points with no Java callers.
 *
 * The andengine_shared NDK module that compiled this file was disabled at
 * the same time. File kept as a historical reference only; it is not compiled.
 */
