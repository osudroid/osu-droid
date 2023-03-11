package com.edlplan.andengine;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.shape.IShape;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.vertex.VertexBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import main.osu.polygon.PolygonVertexBuffer;

public class TriangleTexture3DPack extends Shape {

    // ===========================================================
    // Fields
    // ===========================================================

    private final PolygonVertexBuffer mPolygonVertexBuffer;
    private float[] mVertices;
    private float[] mTextureCoord;
    private TextureRegion textureRegion;

    // ===========================================================
    // Constructors
    // ===========================================================

    public TriangleTexture3DPack(final float pX, final float pY, final float[] pVertices, float[] pTextureCoord) {
        this(pX, pY, pVertices, pTextureCoord, new PolygonVertexBuffer(pVertices.length,
                GL11.GL_STATIC_DRAW, true));
    }

    public TriangleTexture3DPack(final float pX, final float pY, final float[] pVertices, float[] pTextureCoord,
                                 final PolygonVertexBuffer pPolygonVertexBuffer) {
        super(pX, pY);

        this.mVertices = pVertices;
        this.mTextureCoord = pTextureCoord;

        this.mPolygonVertexBuffer = pPolygonVertexBuffer;
        this.updateVertexBuffer();
    }

    public TextureRegion getTextureRegion() {
        return textureRegion;
    }

    public void setTextureRegion(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected void onInitDraw(final GL10 pGL) {
        super.onInitDraw(pGL);
        GLHelper.disableCulling(pGL);
    }


    @Override
    protected VertexBuffer getVertexBuffer() {
        return this.mPolygonVertexBuffer;
    }


    @Override
    protected void onUpdateVertexBuffer() {
        //this.mPolygonVertexBuffer.update(mVertices);
    }

    public void updateShape() {
        onUpdateVertexBuffer();
    }

    public float[] getVertices() {
        return mVertices;
    }

    public void setVertices(float[] v) {
        mVertices = v;
        //onUpdateVertexBuffer();
    }

    @Override
    protected void drawVertices(final GL10 pGL, final Camera pCamera) {
        if (mVertices.length == 0 || mTextureCoord.length == 0 || textureRegion == null) {
            return;
        }
        boolean isEnable = GLHelper.isEnableDepthTest();
        GLHelper.enableDepthTest(pGL);
        GLHelper.enableTexCoordArray(pGL);
        GLHelper.enableTextures(pGL);
        textureRegion.getTexture().bind(pGL);
        //pGL.glColor4f(getRed(), getGreen(), getBlue(), getAlpha());
        TriangleTexture3DRenderer.get().renderTriangles(mVertices, mTextureCoord, pGL);
        GLHelper.setDepthTest(pGL, isEnable);
    }


    @Override
    protected boolean isCulled(final Camera pCamera) {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean collidesWith(final IShape pOtherShape) {
        // TODO Auto-generated method stub
        return false;
    }


    public float getBaseHeight() {
        // TODO Auto-generated method stub
        return 0;
    }


    public float getBaseWidth() {
        // TODO Auto-generated method stub
        return 0;
    }


    public float getHeight() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public float[] getSceneCenterCoordinates() {
        // TODO Auto-generated method stub
        return null;
    }


    public float getWidth() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Deprecated
    public boolean contains(final float pX, final float pY) {
        return false;
    }


    @Override
    @Deprecated
    public float[] convertLocalToSceneCoordinates(final float pX, final float pY) {
        return null;
    }


    @Override
    @Deprecated
    public float[] convertSceneToLocalCoordinates(final float pX, final float pY) {
        return null;
    }

}
