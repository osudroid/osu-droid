package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;


import com.osudroid.utils.Execution;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.UIScene;
import com.reco1l.andengine.component.ComponentsKt;
import com.reco1l.andengine.sprite.UIAnimatedSprite;
import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.framework.Color4;
import com.rian.andengine.modifier.UniversalModifierSequence;

import org.anddev.andengine.entity.shape.Shape;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import kotlin.Unit;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameEffect extends GameObject {
    private static final HashSet<String> animationEffects = new HashSet<>(Arrays.asList(
            "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
    ));

    UISprite hit;
    String texname;

    private float duration;

    public GameEffect(final String texname) {
        this.texname = texname;

        if (isAnimationEffect(texname) && ResourceManager.getInstance().isTextureLoaded(texname + "-0")) {
            var hit = new UIAnimatedSprite(texname, true, OsuSkin.get().getAnimationFramerate());
            hit.setLoop(false);
            this.hit = hit;
        } else {
            hit = new UISprite();
            hit.setTextureRegion(ResourceManager.getInstance().getTexture(texname));
        }
    }

    private static boolean isAnimationEffect(String textureName) {
        return animationEffects.contains(textureName);
    }

    public void setColor(@Nullable Color4 color) {
        if (color == null) {
            hit.setColor(1f, 1f, 1f);
        } else {
            ComponentsKt.setColor4(hit, color);
        }
    }

    @SafeVarargs
    public final void init(final UIScene scene, final PointF pos, final float scale,
                           final Consumer<UniversalModifierSequence>... sequenceConsumers) {
        if (hit instanceof UIAnimatedSprite animatedHit) {
            animatedHit.reset();
        }
        hit.setPosition(pos.x, pos.y);
        hit.setOrigin(Anchor.Center);
        hit.setScale(scale);
        hit.setAlpha(1);
        hit.detachSelf();
        hit.setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT);
        scene.attachChild(hit);

        duration = 0;

        for (int i = 0; i < sequenceConsumers.length; ++i) {
            var consumer = sequenceConsumers[i];

            hit.beginModifierSequence(sequence -> {
                consumer.accept(sequence);
                duration = Math.max(duration, sequence.getDuration());

                return Unit.INSTANCE;
            });
        }

        hit.beginDelayedSequence(duration, sequence -> {
            sequence.after(e -> Execution.updateThread(() -> {
                hit.detachSelf();
                hit.clearEntityModifiers();
                GameObjectPool.getInstance().putEffect(GameEffect.this);
            }));

            return Unit.INSTANCE;
        });
    }

    public void setBlendFunction(int sourceBlend, int destBlend) {
        hit.setBlendFunction(sourceBlend, destBlend);
    }

    public String getTexname() {
        return texname;
    }

    @Override
    public void update(final float dt) {
    }

}
