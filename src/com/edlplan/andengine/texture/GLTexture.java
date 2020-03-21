package com.edlplan.andengine.texture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.edlplan.framework.math.Color4;
import com.edlplan.framework.math.IQuad;
import com.edlplan.framework.math.Quad;
import com.edlplan.framework.math.RectF;
import com.edlplan.framework.math.Vec2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class GLTexture extends AbstractTexture {
    public static final int[] glTexIndex = new int[]{
            GLES20.GL_TEXTURE0,
            GLES20.GL_TEXTURE1,
            GLES20.GL_TEXTURE2,
            GLES20.GL_TEXTURE3,
            GLES20.GL_TEXTURE4,
            GLES20.GL_TEXTURE5,
            GLES20.GL_TEXTURE6,
            GLES20.GL_TEXTURE7,
            GLES20.GL_TEXTURE8,
            GLES20.GL_TEXTURE9,
            GLES20.GL_TEXTURE10,
            GLES20.GL_TEXTURE11,
            GLES20.GL_TEXTURE12,
            GLES20.GL_TEXTURE13,
            GLES20.GL_TEXTURE14,
            GLES20.GL_TEXTURE15,
            GLES20.GL_TEXTURE16,
            GLES20.GL_TEXTURE17,
            GLES20.GL_TEXTURE18
    };
    public static BitmapFactory.Options DEF_CREATE_OPTIONS;
    public static GLTexture White;
    public static GLTexture Alpha;
    public static GLTexture Black;
    public static GLTexture Red;
    public static GLTexture Blue;
    public static GLTexture Yellow;
    public static GLTexture ErrorTexture;
    public static boolean poor_font_mode = false;
    public static boolean SCALE_22 = true;

    static {
        initial();
    }

    private int width, height;

    private int realWidth, realHeight;

    private float glWidth, glHeight;

    private int textureId;

    private boolean recycled = false;

    private Quad rawQuad;

    public GLTexture() {

    }

    public static GLTexture createGPUTexture(int w, int h) {
		/*if(w>GLWrapped.GL_MAX_TEXTURE_SIZE||h>GLWrapped.GL_MAX_TEXTURE_SIZE){
			GLTexture t=createErrTexture();
			return t;
		}*/
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLTexture tex = new GLTexture();
        tex.width = w;
        tex.height = h;
        tex.textureId = t[0];
        tex.glHeight = 1;
        tex.glWidth = 1;
        tex.endCreate();
        //Log.v("fbo","gen tx: "+tex.textureId);
        return tex;
    }

    public static GLTexture createGPUAlphaTexture(int w, int h) {
		/*if(w>GLWrapped.GL_MAX_TEXTURE_SIZE||h>GLWrapped.GL_MAX_TEXTURE_SIZE){
			GLTexture t=createErrTexture();
			return t;
		}*/
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, w, h, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLTexture tex = new GLTexture();
        tex.width = w;
        tex.height = h;
        tex.textureId = t[0];
        tex.glHeight = 1;
        tex.glWidth = 1;
        tex.endCreate();
        //Log.v("fbo","gen tx: "+tex.textureId);
        return tex;
    }

    public static GLTexture createNotChecked(Bitmap bmp, int w, int h) {

        GLTexture tex = new GLTexture();
        tex.textureId = createTexture();
        //Log.v("texture","create Texture: "+tex.textureId);
        //GLWrapped.checkGlError("create Texture: " + tex.textureId);
        tex.width = w;
        tex.height = h;
        tex.glHeight = tex.height / (float) bmp.getHeight();
        tex.glWidth = tex.width / (float) bmp.getWidth();
        tex.endCreate();
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        //GLWrapped.checkGlError("load Texture");
        return tex;
    }

    public static GLTexture decodeStream(InputStream in) {
        //double time=Framework.relativePreciseTimeMillion();
        Bitmap bmp = BitmapFactory.decodeStream(in, null, DEF_CREATE_OPTIONS);
        //System.out.println("decode bitmap cost "+(int)(Framework.relativePreciseTimeMillion()-time)+"ms");
        return create(bmp, true);
    }

    public static GLTexture decodeStream(InputStream in, boolean ifClose) throws IOException {
        GLTexture t = decodeStream(in);
        if (ifClose) in.close();
        return t;
    }

    public static GLTexture decodeFile(File f) throws FileNotFoundException, IOException {
        return decodeStream(new FileInputStream(f), true);
    }

    public static GLTexture create1pxTexture(Color4 color) {
        Bitmap bmp = Bitmap.createBitmap(new int[]{color.toIntBit()}, 1, 1, Bitmap.Config.ARGB_8888);
        return create(bmp, true);
    }

    public static GLTexture create(Bitmap bmp, boolean ifDispos) {
        GLTexture t = create(bmp);
        if (ifDispos) bmp.recycle();
        return t;
    }

    public static GLTexture create(Bitmap bmp) {

        int w = 1;
        int h = 1;
        while (w < bmp.getWidth()) w *= 2;
        while (h < bmp.getHeight()) h *= 2;

        w = bmp.getWidth();
        h = bmp.getHeight();
        Bitmap nb = Bitmap.createBitmap(w, h, poor_font_mode ? Bitmap.Config.ALPHA_8 : Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(nb);
        GLTexture tex;
        c.drawColor(0x00000000);
        Paint p = new Paint();
        p.setAntiAlias(false);
        c.drawBitmap(bmp, 0, 0, p);
        tex = createNotChecked(nb, bmp.getWidth(), bmp.getHeight());
        nb.recycle();
		/*
		if(SCALE_22){
			if(w!=bmp.getWidth()||h!=bmp.getHeight()){
				Bitmap nb=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
				Canvas c=new Canvas(nb);
				c.clearColor(0x00000000);
				Paint p=new Paint();
				p.setAntiAlias(false);
				c.drawBitmap(bmp,0,0,p);
				tex=createNotChecked(nb,bmp.getWidth(),bmp.getHeight());
				nb.recycle();
			}else{
				tex=createNotChecked(bmp,bmp.getWidth(),bmp.getHeight());
			}
		}else{
			tex=createNotChecked(bmp,bmp.getWidth(),bmp.getHeight());
		}*/
        return tex;
    }

    private static int createTexture() {
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        return t[0];
    }

    public static GLTexture createErrTexture() {
        Bitmap bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < bmp.getWidth(); x++) {
            for (int y = 0; y < bmp.getHeight(); y++) {
                if ((x / 32 + y / 32) % 2 == 0) {
                    bmp.setPixel(x, y, Color.argb(255, 10, 5, 5));
                } else {
                    bmp.setPixel(x, y, Color.argb(255, 110, 40, 50));
                }
            }
        }
        return create(bmp, true);
    }

    public static void initial() {
        DEF_CREATE_OPTIONS = new BitmapFactory.Options();
        DEF_CREATE_OPTIONS.inPremultiplied = true;
        White = create1pxTexture(Color4.White);
        Alpha = create1pxTexture(Color4.Alpha);
        Black = create1pxTexture(Color4.Black);
        Red = create1pxTexture(Color4.Red);
        Blue = create1pxTexture(Color4.Blue);
        Yellow = create1pxTexture(Color4.Yellow);
        ErrorTexture = createErrTexture();
    }

    @Override
    public IQuad getRawQuad() {
        return rawQuad;
    }

    protected void endCreate() {
        rawQuad = RectF.xywh(0, 0, glWidth, glHeight).toQuad();
    }

    @Override
    public GLTexture getTexture() {

        return this;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public int getRealWidth() {
        return realWidth;
    }

    public int getRealHeight() {
        return realHeight;
    }

    public float getGlWidth() {
        return glWidth;
    }

    public float getGlHeight() {
        return glHeight;
    }

    @Override
    public int getTextureId() {
        return textureId;
    }

    public void bind(int loc) {
        bindGl(glTexIndex[loc]);
    }

    private void bindGl(int i) {
        GLES20.glActiveTexture(i);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
    }

    @Override
    public Vec2 toTexturePosition(float x, float y) {
        return new Vec2(glWidth * x / width, glHeight * y / height);
    }

    public void delete() {
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        recycled = true;
    }

    @Override
    protected void finalize() throws Throwable {

        if (!recycled) delete();
        super.finalize();
    }

    @Override
    public int hashCode() {

        return textureId;
    }

    @Override
    public boolean equals(Object obj) {

        return super.equals(obj);
    }
}