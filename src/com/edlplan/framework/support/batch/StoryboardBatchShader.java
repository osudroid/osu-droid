package com.edlplan.framework.support.batch;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Minimal GLES20 shader program for {@link TextureQuadBatch}.
 *
 * Vertex layout per vertex (8 floats / 32 bytes):
 *   [x, y,  u, v,  r, g, b, a]
 *
 * Fixed attribute locations:
 *   0 = a_position  (vec2)
 *   1 = a_texCoord  (vec2)
 *   2 = a_color     (vec4)
 */
public class StoryboardBatchShader {

    private static final String TAG = "StoryboardBatchShader";

    private static final String VERTEX_SHADER =
            "uniform mat4 u_mvpMatrix;\n" +
            "attribute vec2 a_position;\n" +
            "attribute vec2 a_texCoord;\n" +
            "attribute vec4 a_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "varying vec4 v_color;\n" +
            "void main() {\n" +
            "    gl_Position = u_mvpMatrix * vec4(a_position, 0.0, 1.0);\n" +
            "    v_texCoord  = a_texCoord;\n" +
            "    v_color     = a_color;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D s_texture;\n" +
            "varying vec2 v_texCoord;\n" +
            "varying vec4 v_color;\n" +
            "void main() {\n" +
            "    gl_FragColor = v_color * texture2D(s_texture, v_texCoord);\n" +
            "}\n";

    // Singleton ------------------------------------------------------------------

    private static StoryboardBatchShader sInstance;

    public static StoryboardBatchShader getInstance() {
        if (sInstance == null) sInstance = new StoryboardBatchShader();
        return sInstance;
    }

    // Attribute locations (bound before link) ------------------------------------
    public static final int ATTRIB_POSITION = 0;
    public static final int ATTRIB_TEXCOORD = 1;
    public static final int ATTRIB_COLOR    = 2;

    // Uniform locations (resolved after link) ------------------------------------
    public int programID   = -1;
    public int uMVPLoc     = -1;
    public int uTextureLoc = -1;

    private StoryboardBatchShader() {}

    // Compile / link -------------------------------------------------------------

    /** Ensures the program is compiled. Returns {@code true} on success. */
    public boolean ensureCompiled() {
        if (programID != -1) return true;

        int vs = compileShader(GLES20.GL_VERTEX_SHADER,   VERTEX_SHADER);
        if (vs == 0) return false;
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (fs == 0) { GLES20.glDeleteShader(vs); return false; }

        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glBindAttribLocation(prog, ATTRIB_POSITION, "a_position");
        GLES20.glBindAttribLocation(prog, ATTRIB_TEXCOORD, "a_texCoord");
        GLES20.glBindAttribLocation(prog, ATTRIB_COLOR,    "a_color");
        GLES20.glLinkProgram(prog);

        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);

        int[] status = new int[1];
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Storyboard shader link error: " + GLES20.glGetProgramInfoLog(prog));
            GLES20.glDeleteProgram(prog);
            return false;
        }

        programID   = prog;
        uMVPLoc     = GLES20.glGetUniformLocation(prog, "u_mvpMatrix");
        uTextureLoc = GLES20.glGetUniformLocation(prog, "s_texture");
        return true;
    }

    /** Call when the EGL context is lost so the shader gets recompiled next use. */
    public void resetForContextLoss() {
        programID = uMVPLoc = uTextureLoc = -1;
    }

    // Helpers --------------------------------------------------------------------

    private static int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) return 0;
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Storyboard shader compile error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}

