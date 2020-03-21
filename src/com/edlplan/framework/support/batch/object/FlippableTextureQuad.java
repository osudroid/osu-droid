package com.edlplan.framework.support.batch.object;

import com.edlplan.framework.math.Anchor;
import com.edlplan.framework.math.Color4;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.utils.BooleanRef;
import com.edlplan.framework.utils.FloatRef;
import com.edlplan.framework.utils.Vec2Ref;

import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class FlippableTextureQuad extends ATextureQuad {

    public Vec2 size = new Vec2();

    public Vec2Ref position = new Vec2Ref();

    public Anchor anchor = Anchor.Center;

    public Vec2Ref scale;

    public FloatRef rotation;

    public FloatRef alpha = new FloatRef(1);

    public Color4 accentColor;

    public float u1, v1, u2, v2;

    public BooleanRef flipH = new BooleanRef(false);

    public BooleanRef flipV = new BooleanRef(false);

    public FlippableTextureQuad enableScale() {
        if (scale == null) {
            scale = new Vec2Ref();
            scale.x.value = scale.y.value = 1;
        }
        return this;
    }

    public FlippableTextureQuad enableColor() {
        if (accentColor == null) {
            accentColor = Color4.White.copyNew();
        }
        return this;
    }

    public FlippableTextureQuad syncColor(Color4 color) {
        accentColor = color;
        return this;
    }

    public FlippableTextureQuad syncAlpha(FloatRef ref) {
        this.alpha = ref;
        return this;
    }

    public FlippableTextureQuad enableRotation() {
        if (rotation == null) {
            rotation = new FloatRef();
        }
        return this;
    }

    public FlippableTextureQuad syncRotation(FloatRef ref) {
        this.rotation = ref;
        return this;
    }

    public void setTextureAndSize(TextureRegion texture) {
        this.texture = texture;
        size.set(texture.getWidth(), texture.getHeight());
        u1 = texture.getTextureCoordinateX1();
        u2 = texture.getTextureCoordinateX2();
        v1 = texture.getTextureCoordinateY1();
        v2 = texture.getTextureCoordinateY2();
    }

    public void setBaseWidth(float width) {
        size.set(width, width * (size.y / size.x));
    }

    public void setBaseHeight(float height) {
        size.set(height * (size.x / size.y), height);
    }

    @Override
    public void write(float[] ary, int offset) {

        float u1;
        float v1;
        float u2;
        float v2;

        if (flipH.value) {
            u1 = this.u2;
            u2 = this.u1;
        } else {
            u1 = this.u1;
            u2 = this.u2;
        }

        if (flipV.value) {
            v1 = this.v2;
            v2 = this.v1;
        } else {
            v1 = this.v1;
            v2 = this.v2;
        }

        float l = -size.x * anchor.x();
        float r = size.x + l;
        float t = -size.y * anchor.y();
        float b = size.y + t;
        float cr, cg, cb, ca;
        if (accentColor == null) {
            cr = cg = cb = ca = alpha.value;
        } else {
            float a = alpha.value;
            cr = a * accentColor.r;
            cg = a * accentColor.g;
            cb = a * accentColor.b;
            ca = a * accentColor.a;
        }

        if (scale != null) {
            l *= scale.x.value;
            r *= scale.x.value;
            t *= scale.y.value;
            b *= scale.y.value;
        }

        if (rotation == null) {
            l += position.x.value;
            r += position.x.value;
            t += position.y.value;
            b += position.y.value;
            ary[offset++] = l;
            ary[offset++] = t;
            ary[offset++] = u1;
            ary[offset++] = v1;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;

            ary[offset++] = r;
            ary[offset++] = t;
            ary[offset++] = u2;
            ary[offset++] = v1;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;

            ary[offset++] = l;
            ary[offset++] = b;
            ary[offset++] = u1;
            ary[offset++] = v2;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;


            ary[offset++] = r;
            ary[offset++] = b;
            ary[offset++] = u2;
            ary[offset++] = v2;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;
        } else {
            final float s = (float) Math.sin(rotation.value);
            final float c = (float) Math.cos(rotation.value);
            final float x = position.x.value, y = position.y.value;
            ary[offset++] = l * c - t * s + x;
            ary[offset++] = l * s + t * c + y;
            ary[offset++] = u1;
            ary[offset++] = v1;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;

            ary[offset++] = r * c - t * s + x;
            ary[offset++] = r * s + t * c + y;
            ary[offset++] = u2;
            ary[offset++] = v1;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;

            ary[offset++] = l * c - b * s + x;
            ary[offset++] = l * s + b * c + y;
            ary[offset++] = u1;
            ary[offset++] = v2;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;

            ary[offset++] = r * c - b * s + x;
            ary[offset++] = r * s + b * c + y;
            ary[offset++] = u2;
            ary[offset++] = v2;
            ary[offset++] = cr;
            ary[offset++] = cg;
            ary[offset++] = cb;
            ary[offset++] = ca;
        }
    }


}
