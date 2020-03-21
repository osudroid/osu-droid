package com.edlplan.framework.support.osb;

import com.edlplan.framework.support.batch.object.TextureQuadBatch;
import com.edlplan.framework.support.graphics.BaseCanvas;
import com.edlplan.framework.support.graphics.BlendType;
import com.edlplan.framework.utils.advance.LinkedNode;

public class DepthOrderRenderEngine {

    public LinkedNode<EGFStoryboardSprite> first, end;

    public DepthOrderRenderEngine() {
        first = new LinkedNode<>();
        end = new LinkedNode<>();
        first.insertToNext(end);
    }

    public void add(EGFStoryboardSprite sprite) {
        for (LinkedNode<EGFStoryboardSprite> s = end.pre; s != first; s = s.pre) {
            if (s.value.sprite.depth < sprite.sprite.depth) {
                s.insertToNext(new LinkedNode<>(sprite));
                return;
            }
        }
        first.insertToNext(new LinkedNode<>(sprite));
    }

    public void remove(EGFStoryboardSprite sprite) {
        for (LinkedNode<EGFStoryboardSprite> s = first.next; s != end; s = s.next) {
            if (s.value == sprite) {
                s.removeFromList();
                break;
            }
        }
    }

    public void draw(BaseCanvas canvas) {
        TextureQuadBatch batch = TextureQuadBatch.getDefaultBatch();
        for (LinkedNode<EGFStoryboardSprite> s = first.next; s != end; s = s.next) {
            if (s.value.textureQuad.alpha.value < 0.001) {
                continue;
            }
            canvas.getBlendSetting().setBlendType(s.value.blendMode.value ? BlendType.Additive : BlendType.Normal);
            batch.add(s.value.textureQuad);
        }
    }


}
