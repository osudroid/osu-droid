package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import com.osudroid.game.cursor.trail.CursorTrail;
import com.osudroid.game.cursor.trail.FancyCursorTrail;
import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.component.UIComponent;
import com.reco1l.andengine.sprite.UISprite;

import org.andengine.entity.scene.Scene;
import org.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorEntity extends UIComponent {
    protected final CursorSprite cursorSprite;
    private final UISprite cursorMiddleSprite;
    private CursorTrail trail = null;
    private FancyCursorTrail fancyTrail = null;
    private boolean isShowing = false;

    public CursorEntity() {
        cursorSprite = new CursorSprite();

        // "cursormiddle" is drawn statically on top of the cursor, unlike the cursor itself it never
        // rotates or scales on click. its entirely opt in per skin (see OPTIONAL_TEXTURES).
        var middleTexture = ResourceManager.getInstance().getTextureIfLoaded("cursormiddle");
        if (middleTexture != null) {
            cursorMiddleSprite = new UISprite();
            cursorMiddleSprite.setAnchor(Anchor.Center);
            cursorMiddleSprite.setOrigin(OsuSkin.get().isCursorCentre() ? Anchor.Center : Anchor.TopLeft);
            cursorMiddleSprite.setScale(cursorSprite.baseSize);
            cursorMiddleSprite.setTextureRegion(middleTexture);
        } else {
            cursorMiddleSprite = null;
        }

        if (Config.isUseFancyCursorTrail()) {
            fancyTrail = new FancyCursorTrail();
        } else if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");
            trail = new CursorTrail(trailTex, cursorSprite);
        }

        attachChild(cursorSprite);
        if (cursorMiddleSprite != null) {
            attachChild(cursorMiddleSprite);
        }
        setVisible(false);

        // Not necessary to update by itself since it's done by GameScene.
        setIgnoreUpdate(true);
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
        setVisible(showing);
        if (trail != null)
            trail.setSpawningEnabled(showing);
        if (fancyTrail != null) {
            fancyTrail.setVisible(showing);
            if (!showing) fancyTrail.resetTrail();
        }
    }

    public void click() {
        cursorSprite.handleClick();
    }

    public void update(float pSecondsElapsed) {
        if (isShowing) {
            cursorSprite.update(pSecondsElapsed);

            if (fancyTrail != null) {
                fancyTrail.setCursorX(getX());
                fancyTrail.setCursorY(getY());
                fancyTrail.update(pSecondsElapsed);
            }
        }

        // Runs regardless of isShowing so already-spawned trail parts keep fading out in real
        // time instead of freezing while the cursor is hidden.
        if (trail != null) {
            trail.update(pSecondsElapsed);
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        if (trail != null) {
            fgScene.attachChild(trail);
        }
        if (fancyTrail != null) {
            fgScene.attachChild(fancyTrail);
        }
        fgScene.attachChild(this);
    }

    @Override
    public void setPosition(float pX, float pY) {
        if (trail != null) {
            trail.addPosition(pX, pY);
        }

        super.setPosition(pX, pY);
    }
}
