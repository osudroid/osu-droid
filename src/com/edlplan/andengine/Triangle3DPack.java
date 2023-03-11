package com.edlplan.andengine;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.shape.IShape;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.vertex.VertexBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import main.osu.polygon.PolygonVertexBuffer;

public class Triangle3DPack extends Shape {

    // ===========================================================
    // Fields
    // ===========================================================

    private final PolygonVertexBuffer mPolygonVertexBuffer;
    private float[] mVertices;
    private boolean clearDepthOnStart = false;

    // ===========================================================
    // Constructors
    // ===========================================================

    public Triangle3DPack(final float pX, final float pY, final float[] pVertices) {
        this(pX, pY, pVertices, new PolygonVertexBuffer(pVertices.length,
                GL11.GL_STATIC_DRAW, true));
    }

    public Triangle3DPack(final float pX, final float pY, final float[] pVertices,
                          final PolygonVertexBuffer pPolygonVertexBuffer) {
        super(pX, pY);

        this.mVertices = pVertices;

        this.mPolygonVertexBuffer = pPolygonVertexBuffer;
        this.updateVertexBuffer();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================


    public void setClearDepthOnStart(boolean clearDepthOnStart) {
        this.clearDepthOnStart = clearDepthOnStart;
    }

    @Override
    protected void onInitDraw(final GL10 pGL) {
        super.onInitDraw(pGL);
        GLHelper.disableCulling(pGL);
        GLHelper.disableTextures(pGL);
        GLHelper.disableTexCoordArray(pGL);
        if (clearDepthOnStart) pGL.glClear(GL10.GL_DEPTH_BUFFER_BIT);
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
        if (mVertices.length == 0) {
            return;
        }
        boolean isEnable = GLHelper.isEnableDepthTest();
        GLHelper.enableDepthTest(pGL);
        pGL.glColor4f(getRed(), getGreen(), getBlue(), getAlpha());
        Triangle3DRenderer.get().renderTriangles(mVertices, pGL);
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
