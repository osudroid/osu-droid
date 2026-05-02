#include <jni.h>
#include <stdint.h>
#include <GLES2/gl2.h>

// ===========================================================
// org.andengine.opengl.GLES20Fix
// ===========================================================

JNIEXPORT void JNICALL Java_org_andengine_opengl_GLES20Fix_glVertexAttribPointer (JNIEnv *env, jclass c, jint index, jint size, jint type, jboolean normalized, jint stride, jint offset) {
	glVertexAttribPointer(index, size, type, normalized, stride, (void*)(intptr_t)offset);
}

JNIEXPORT void JNICALL Java_org_andengine_opengl_GLES20Fix_glDrawElements (JNIEnv *env, jclass c, jint mode, jint count, jint type, jint offset) {
	glDrawElements(mode, count, type, (void*)(intptr_t)offset);
}