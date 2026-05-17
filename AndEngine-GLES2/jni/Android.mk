# The andengine JNI module (GLES20Fix + BufferUtils workarounds) has been
# removed. minSdkVersion 24 means both workaround code paths were always
# skipped; all three JNI functions are no longer declared in Java.
# The pre-built libandengine.so files in libs/ are kept for reference but
# are no longer packaged by the Gradle build (jniLibs are only picked up
# from the configured jniLibs.srcDirs, and this file is not compiled).
