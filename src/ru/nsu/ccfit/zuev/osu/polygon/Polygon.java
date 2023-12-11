package ru.nsu.ccfit.zuev.osu.polygon;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.shape.IShape;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.opengl.util.GLHelper;
import org.anddev.andengine.opengl.vertex.VertexBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class Polygon extends Shape {

    // ===========================================================
    // Fields
    // ===========================================================

    private final float[] mVertices;

    private final PolygonVertexBuffer mPolygonVertexBuffer;

    // ===========================================================
    // Constructors
    // ===========================================================

    public Polygon(final float pX, final float pY, final float[] pVertices) {
        this(pX, pY, pVertices, new PolygonVertexBuffer(pVertices.length, GL11.GL_STATIC_DRAW, true));
    }

    public Polygon(final float pX, final float pY, final float[] pVertices, final PolygonVertexBuffer pPolygonVertexBuffer) {
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


    @Override
    protected void onInitDraw(final GL10 pGL) {
        super.onInitDraw(pGL);
        GLHelper.disableTextures(pGL);
        GLHelper.disableTexCoordArray(pGL);
    }


    @Override
    protected VertexBuffer getVertexBuffer() {
        return this.mPolygonVertexBuffer;
    }


    @Override
    protected void onUpdateVertexBuffer() {
        this.mPolygonVertexBuffer.update(mVertices);
    }

    public void updateShape() {
        onUpdateVertexBuffer();
    }

    public float[] getVertices() {
        return mVertices;
    }


    @Override
    protected void drawVertices(final GL10 pGL, final Camera pCamera) {
        pGL.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVertices.length / 2);
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
