package com.edlplan.osu.support.slider;

import com.edlplan.andengine.SpriteCache;
import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.andengine.TrianglePack;
import com.edlplan.framework.math.Color4;
import com.edlplan.framework.math.line.LinePath;
import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.Modifiers;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.Slider;
import ru.nsu.ccfit.zuev.osu.helper.ModifierListener;

public class SliderBody2D extends AbstractSliderBody {

    private static final ThreadLocal<BuildCache> localCache = new ThreadLocal<>() {
        @Override
        public BuildCache get() {
            BuildCache cache = super.get();
            if (cache == null) {
                cache = new BuildCache();
                set(cache);
            }
            return cache;
        }
    };
    private float sliderBodyBaseAlpha = 0.7f;
    private float hintAlpha = 0.3f;
    private TrianglePack body = null, border = null, hint = null;
    private RGBColor bodyColor = new RGBColor(), borderColor = new RGBColor(), hintColor = new RGBColor();
    private float bodyWidth, borderWidth, hintWidth;
    private float startLength = 0, endLength = 0;
    private boolean enableHint = false;

    public SliderBody2D(LinePath path) {
        super(path);
    }

    public boolean isEnableHint() {
        return enableHint;
    }

    public void setEnableHint(boolean enableHint) {
        this.enableHint = enableHint;
    }

    public float getSliderBodyBaseAlpha() {
        return sliderBodyBaseAlpha;
    }

    @Override
    public void setSliderBodyBaseAlpha(float sliderBodyBaseAlpha) {
        this.sliderBodyBaseAlpha = sliderBodyBaseAlpha;
    }

    public void setHintAlpha(float hintAlpha) {
        this.hintAlpha = hintAlpha;
    }

    public void setHintColor(float r, float g, float b) {
        this.hintColor.set(r, g, b);
        if (hint != null) {
            hint.setColor(r, g, b);
        }
    }

    public void setHintWidth(float hintWidth) {
        this.hintWidth = hintWidth;
    }

    public void applyFadeAdjustments(float fadeInDuration) {
        if (body != null) {
            body.registerEntityModifier(Modifiers.alpha(fadeInDuration, 0, sliderBodyBaseAlpha));
        }

        if (border != null) {
            border.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }

        if (hint != null) {
            hint.registerEntityModifier(Modifiers.alpha(fadeInDuration, 0, hintAlpha));
        }
    }

    public void applyFadeAdjustments(float fadeInDuration, float fadeOutDuration) {
        final EaseQuadOut easing = EaseQuadOut.getInstance();

        if (body != null) {
            body.registerEntityModifier(Modifiers.sequence(
                    Modifiers.alpha(fadeInDuration, 0, sliderBodyBaseAlpha),
                    Modifiers.alpha(fadeOutDuration, sliderBodyBaseAlpha, 0, null, easing)
            ));
        }

        if (border != null) {
            border.registerEntityModifier(Modifiers.sequence(
                    Modifiers.fadeIn(fadeInDuration),
                    Modifiers.fadeOut(fadeOutDuration, null, easing)
            ));
        }

        if (hint != null) {
            hint.registerEntityModifier(Modifiers.sequence(
                    Modifiers.alpha(fadeInDuration, 0, hintAlpha),
                    Modifiers.alpha(fadeOutDuration, hintAlpha, 0, null, easing)
            ));
        }
    }

    public void applyDimAnimations(float dimDelaySec) {

        var colorDim = 195f / 255f;

         body.setColor(bodyColor.r() * colorDim, bodyColor.g() * colorDim, bodyColor.b() * colorDim);
         body.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(dimDelaySec),
            Modifiers.color(0.1f / GameHelper.getSpeedMultiplier(),
                 body.getRed(), bodyColor.r(),
                 body.getGreen(), bodyColor.g(),
                 body.getBlue(), bodyColor.b()
            )
        ));

        border.setColor(borderColor.r() * colorDim, borderColor.g() * colorDim, borderColor.b() * colorDim);
        border.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(dimDelaySec),
            Modifiers.color(0.1f / GameHelper.getSpeedMultiplier(),
                border.getRed(), borderColor.r(),
                border.getGreen(), borderColor.g(),
                border.getBlue(), borderColor.b()
            )
        ));

        if (hint != null) {
            hint.setColor(hintColor.r() * colorDim, hintColor.g() * colorDim, hintColor.b() * colorDim);
            hint.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f / GameHelper.getSpeedMultiplier(),
                    hint.getRed(), hintColor.r(),
                    hint.getGreen(), hintColor.g(),
                    hint.getBlue(), hintColor.b()
                )
            ));

        }
    }

    public void applyShakeAnimations() {
        body.registerEntityModifier(Modifiers.shakeHorizontal(0.32f / GameHelper.getSpeedMultiplier(), 8f));
        border.registerEntityModifier(Modifiers.shakeHorizontal(0.32f / GameHelper.getSpeedMultiplier(), 8f));
        if (hint != null) {
            hint.registerEntityModifier(Modifiers.shakeHorizontal(0.32f / GameHelper.getSpeedMultiplier(), 8f));
        }
    }


    @Override
    public void onUpdate() {
        if (body == null || border == null) {
            return;
        }

        BuildCache cache = localCache.get();
        LinePath sub = path.cutPath(startLength, endLength).fitToLinePath(cache.path);

        if (hint != null) {
            cache.drawLinePath
                    .reset(sub, hintWidth)
                    .getTriangles(cache.triangleBuilder)
                    .getVertex(hint.getVertices());
        }

        cache.drawLinePath
                .reset(sub, bodyWidth)
                .getTriangles(cache.triangleBuilder)
                .getVertex(body.getVertices());
        cache.drawLinePath
                .reset(sub, borderWidth)
                .getTriangles(cache.triangleBuilder)
                .getVertex(border.getVertices());
    }

    @Override
    public void setBodyWidth(float width) {
        bodyWidth = width;
    }

    @Override
    public void setBorderWidth(float width) {
        borderWidth = width;
    }

    @Override
    public void setBodyColor(float r, float g, float b) {
        bodyColor.set(r, g, b);
        if (body != null) {
            body.setColor(r, g, b);
        }
    }

    @Override
    public void setBorderColor(float r, float g, float b) {
        borderColor.set(r, g, b);
        if (border != null) {
            border.setColor(r, g, b);
        }
    }

    @Override
    public void setStartLength(float length) {
        startLength = length;
    }

    @Override
    public void setEndLength(float length) {
        endLength = length;
    }

    @Override
    public void applyToScene(Scene scene, boolean emptyOnStart) {
        BuildCache cache = localCache.get();
        body = SpriteCache.trianglePackCache.get();
        border = SpriteCache.trianglePackCache.get();

        if (enableHint) {
            hint = SpriteCache.trianglePackCache.get();
            hint.setAlpha(0);
            hint.setDepthTest(true);
            hint.setClearDepthOnStart(true);
            hint.setColor(hintColor.r(), hintColor.g(), hintColor.b());
        }

        body.setAlpha(0);
        body.setDepthTest(true);
        body.setClearDepthOnStart(!enableHint);
        body.setColor(bodyColor.r(), bodyColor.g(), bodyColor.b());

        border.setAlpha(0);
        border.setDepthTest(true);
        border.setClearDepthOnStart(false);
        border.setColor(borderColor.r(), borderColor.g(), borderColor.b());

        if (emptyOnStart) {
            if (hint != null) {
                hint.getVertices().length = 0;
            }
            body.getVertices().length = 0;
            border.getVertices().length = 0;
        } else {
            if (hint != null) {
                cache.drawLinePath
                        .reset(path, hintWidth)
                        .getTriangles(cache.triangleBuilder)
                        .getVertex(hint.getVertices());
            }
            cache.drawLinePath
                    .reset(path, bodyWidth)
                    .getTriangles(cache.triangleBuilder)
                    .getVertex(body.getVertices());
            cache.drawLinePath
                    .reset(path, borderWidth)
                    .getTriangles(cache.triangleBuilder)
                    .getVertex(border.getVertices());
        }


        if (!emptyOnStart) {
            startLength = 0;
            endLength = path.getMeasurer().maxLength();
        }

        scene.attachChild(border, 0);
        scene.attachChild(body, 0);
        if (hint != null) {
            scene.attachChild(hint, 0);
        }
    }

    public void removeFromScene(Scene scene, float duration, Slider slider)
    {
        if (hint != null) {
            hint.registerEntityModifier(Modifiers.alpha(duration, hintAlpha, 0));
        }

        if (body != null) {
            body.registerEntityModifier(Modifiers.alpha(duration, sliderBodyBaseAlpha, 0));
        }

        if (border != null) {
            border.registerEntityModifier(Modifiers.fadeOut(duration, new ModifierListener() {

                @Override
                public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                    Execution.updateThread(() -> {
                        removeFromScene(scene);

                        // We can pool the hit object once all animations are finished.
                        // The follow circle animation is the last one to finish but if it's disabled this will be, so should
                        // pool the object here in that case.
                        if (!Config.isAnimateFollowCircle()) {
                            slider.poolObject();
                        }
                    });
                }
            }));
        }
    }

    @Override
    public void removeFromScene(Scene scene) {
        if (hint != null) {
            hint.clearEntityModifiers();
            hint.detachSelf();
            SpriteCache.trianglePackCache.save(hint);
            hint = null;
        }
        if (body != null) {
            body.clearEntityModifiers();
            body.detachSelf();
            SpriteCache.trianglePackCache.save(body);
            body = null;
        }
        if (border != null) {
            border.clearEntityModifiers();
            border.detachSelf();
            SpriteCache.trianglePackCache.save(border);
            border = null;
        }
    }

    private static class BuildCache {
        public LinePath path = new LinePath();
        public TriangleBuilder triangleBuilder = new TriangleBuilder();
        public DrawLinePath drawLinePath = new DrawLinePath();
    }

    public static class SliderProperty {

        public Color4 color = Color4.White.copyNew();

        public float width;

        public TrianglePack pack;

    }
}
