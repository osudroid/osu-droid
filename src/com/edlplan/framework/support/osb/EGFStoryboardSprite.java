package com.edlplan.framework.support.osb;

import com.edlplan.edlosbsupport.command.Target;
import com.edlplan.edlosbsupport.player.CommandBooleanHandleTimeline;
import com.edlplan.edlosbsupport.player.CommandColor4HandleTimeline;
import com.edlplan.edlosbsupport.player.CommandFloatHandleTimeline;
import com.edlplan.edlosbsupport.player.CommandHandleTimeline;
import com.edlplan.edlosbsupport.player.PlayingSprite;
import com.edlplan.framework.support.batch.object.FlippableTextureQuad;
import com.edlplan.framework.utils.BooleanRef;

public class EGFStoryboardSprite extends PlayingSprite {

    public FlippableTextureQuad textureQuad;
    public BooleanRef blendMode = new BooleanRef(false);
    protected OsbContext context;

    public EGFStoryboardSprite(OsbContext context) {
        this.context = context;
    }

    @Override
    protected void onLoad() {
        textureQuad = new FlippableTextureQuad();
        textureQuad.setTextureAndSize(context.texturePool.get(sprite.spriteFilename));
        textureQuad.position.x.value = sprite.startX;
        textureQuad.position.y.value = sprite.startY;
        textureQuad.anchor = sprite.origin.value;
    }

    @Override
    public void onAddedToScene() {
        context.engines[sprite.layer.ordinal()].add(this);
    }

    @Override
    public void onRemoveFromScene() {
        context.engines[sprite.layer.ordinal()].remove(this);
    }

    @Override
    public CommandHandleTimeline createByTarget(Target target) {
        switch (target) {
            case X:
                return new CommandFloatHandleTimeline(textureQuad.position.x);
            case Y:
                return new CommandFloatHandleTimeline(textureQuad.position.y);
            case ScaleX:
                return new CommandFloatHandleTimeline(textureQuad.enableScale().scale.x);
            case ScaleY:
                return new CommandFloatHandleTimeline(textureQuad.enableScale().scale.y);
            case Alpha:
                return new CommandFloatHandleTimeline(textureQuad.alpha);
            case Rotation:
                return new CommandFloatHandleTimeline(textureQuad.enableRotation().rotation);
            case Color:
                return new CommandColor4HandleTimeline() {{
                    value = textureQuad.enableColor().accentColor;
                }};
            case FlipH:
                return new CommandBooleanHandleTimeline() {{
                    value = textureQuad.flipH;
                }};
            case FlipV:
                return new CommandBooleanHandleTimeline() {{
                    value = textureQuad.flipV;
                }};
            case BlendingMode:
                return new CommandBooleanHandleTimeline() {{
                    value = blendMode;
                }};
        }
        return null;
    }
}
