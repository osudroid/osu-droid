package com.edlplan.andengine;

import com.edlplan.framework.utils.FloatArraySlice;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.shape.IShape;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.vertex.VertexBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import ru.nsu.ccfit.zuev.osu.polygon.PolygonVertexBuffer;

public class TrianglePack extends Shape {

    // ===========================================================
    // Fields
    // ===========================================================

    private FloatArraySlice mVertices;

    private PolygonVertexBuffer mPolygonVertexBuffer;

    private boolean clearDepthOnStart = false;

    private boolean depthTest = false;

    // ===========================================================
    // Constructors
    // ===========================================================

    public TrianglePack() {
        super(0, 0);
        mVertices = new FloatArraySlice();
        mVertices.ary = new float[0];
    }

    public TrianglePack(final float pX, final float pY, final float[] pVertices) {
        this(pX, pY, pVertices, new PolygonVertexBuffer(pVertices.length,
                GL11.GL_STATIC_DRAW, true));
    }

    public TrianglePack(final float pX, final float pY, final float[] pVertices,
                        final PolygonVertexBuffer pPolygonVertexBuffer) {
        super(pX, pY);

        this.mVertices = new FloatArraySlice();
        mVertices.ary = pVertices;
        mVertices.length = pVertices.length;

        this.mPolygonVertexBuffer = pPolygonVertexBuffer;
        this.updateVertexBuffer();
    }

    public void setClearDepthOnStart(boolean clearDepthOnStart) {
        this.clearDepthOnStart = clearDepthOnStart;
    }

    public void setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
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
        GLHelper.disableTextures(pGL);
        GLHelper.disableTexCoordArray(pGL);
        if (clearDepthOnStart) {
            pGL.glClear(GL10.GL_DEPTH_BUFFER_BIT);
        }
    }


    @Override
    protected VertexBuffer getVertexBuffer() {
        return this.mPolygonVertexBuffer;
    }


    @Override
    protected void onUpdateVertexBuffer() {
        //this.mPolygonVertexBuffer.update(mVertices);
    }

    @Override
    protected void onApplyVertices(GL10 pGL) {

    }

    public FloatArraySlice getVertices() {
        return mVertices;
    }

    public void setVertices(FloatArraySlice v) {
        mVertices = v;
    }

    @Override
    protected void drawVertices(final GL10 pGL, final Camera pCamera) {
        if (mVertices.length == 0) return;
        boolean tmp = GLHelper.isEnableDepthTest();
        GLHelper.setDepthTest(pGL, depthTest);
        pGL.glColor4f(getRed(), getGreen(), getBlue(), getAlpha());
        TriangleRenderer.get().renderTriangles(mVertices, pGL);
        GLHelper.setDepthTest(pGL, tmp);
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
