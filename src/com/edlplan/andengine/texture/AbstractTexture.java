package com.edlplan.andengine.texture;


import com.edlplan.framework.math.IQuad;
import com.edlplan.framework.math.Quad;
import com.edlplan.framework.math.RectF;
import com.edlplan.framework.math.Vec2;

public abstract class AbstractTexture {
    public abstract int getTextureId();

    public abstract GLTexture getTexture();

    public abstract int getHeight();

    public abstract int getWidth();

    public abstract Vec2 toTexturePosition(float x, float y);

    public abstract IQuad getRawQuad();

    public Vec2 toTexturePosition(Vec2 v) {
        return toTexturePosition(v.x, v.y);
    }

    //将一个width x height坐标的rect换成1x1坐标
    public RectF toTextureRect(RectF raw) {
        return toTextureRect(raw.getLeft(), raw.getTop(), raw.getRight(), raw.getBottom());
    }

    public RectF toTextureRect(float l, float t, float r, float b) {
        Vec2 lt = toTexturePosition(l, t);
        Vec2 rb = toTexturePosition(r, b);
        return RectF.ltrb(lt.x, lt.y, rb.x, rb.y);
    }

    public Quad toTextureQuad(IQuad q) {
        Quad r = new Quad();
        r.set(
                toTexturePosition(q.getTopLeft()),
                toTexturePosition(q.getTopRight()),
                toTexturePosition(q.getBottomLeft()),
                toTexturePosition(q.getBottomRight()));
        return r;
    }

}
