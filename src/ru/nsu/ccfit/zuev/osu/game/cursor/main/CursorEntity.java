package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectData;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail;

public class CursorEntity extends Entity {
    private final CursorSprite cursorSprite;
    private ParticleSystem particles = null;
    private PointParticleEmitter emitter = null;
    private boolean isShowing = false;
    private float particleOffsetX, particleOffsetY;
    public boolean isMovingAutoSliderOrSpinner = false;
    private boolean isFirstNote = false;
    private MoveModifier currentModifier;


    public CursorEntity() {
        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -trailTex.getWidth() / 2f;
            particleOffsetY = -trailTex.getHeight() / 2f;

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            particles = new CursorTrail(emitter, trailTex);
        }

        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        cursorSprite = new CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex);

        attachChild(cursorSprite);
    }

    public void setShowing(boolean showing) {
        this.isShowing = showing;
        if (particles != null)
            particles.setParticlesSpawnEnabled(showing);
    }

    public void click() {
        cursorSprite.handleClick();
    }

    private void doEasingAutoMove(float pX, float pY, float durationS) {
        this.unregisterEntityModifier(currentModifier);
        currentModifier = new MoveModifier(durationS, this.getX(), pX, this.getY(), pY, EaseExponentialOut.getInstance());
        this.registerEntityModifier(currentModifier);
    }

    private void doAutoMove(float pX, float pY, float durationS, GameObjectListener listener) {
        if (durationS <= 0) {
            this.setPosition(pX, pY);
            listener.onUpdatedAutoCursor(pX, pY);
        } else if (!this.isMovingAutoSliderOrSpinner) {
            doEasingAutoMove(pX, pY, durationS);
            listener.onUpdatedAutoCursor(pX, pY);
        }
    }

    public void updatePosIfAuto(float pX, float pY, float durationS, GameObjectListener listener) {
        if (!GameHelper.isAuto()) {
            return;
        }

        this.isMovingAutoSliderOrSpinner = true;
        this.doAutoMove(pX, pY, durationS, listener);
    }

    public void updatePosIfAuto(Queue<GameObject> activeObjects, float secPassed, LinkedList<GameObjectData> objects, GameObjectListener listener) {
        if (!GameHelper.isAuto()) {
            return;
        }

        GameObject currentObj = activeObjects.peek();

        if (currentObj == null) {
            return;
        }

        GameObjectData currentObjData = null;
        GameObjectData nextObjData = null;

        if (!isFirstNote) {
            isFirstNote = true;
            try {
                nextObjData = objects.getFirst();
            } catch (NoSuchElementException ignore) {}
        } if (currentObj != null) {
            try {
                currentObjData = objects.get(currentObj.getId());
                nextObjData = objects.get(currentObj.getId() + 1);
            }  catch (IndexOutOfBoundsException ignore) {}
        }

        if (nextObjData == null  || currentObjData != null && secPassed < currentObjData.getTime()) {
            return;
        }

        float movePositionX = nextObjData.getPos().x;
        float movePositionY = nextObjData.getPos().y;
        float moveDelay = nextObjData.getTime() - secPassed;

        this.doAutoMove(movePositionX, movePositionY, moveDelay, listener);
    }


    public void update(float pSecondsElapsed) {
        this.handleLongerTrail();
        cursorSprite.update(pSecondsElapsed, isShowing);

        super.onManagedUpdate(pSecondsElapsed);
    }

    private void handleLongerTrail() {
        //TODO:finish Longer Tail
        /*
        if (isShowing && Config.isUseLongTrail()) {
            if (oldPoint != null){
                float px = cursorSprite.getX() - oldPoint.x;
                float py = cursorSprite.getY() - oldPoint.y;
                float ds = (float)Math.sqrt(px * px + py * py);
                float length = (float)Math.sqrt(particleOffsetX * particleOffsetX + particleOffsetY * particleOffsetY);
                int count = (int)(ds / length);
                for (int i = 1; i < count - 1; i++){
                    final Particle particle = new Particle(oldPoint.x + px * i / count + particleOffsetX,
                            oldPoint.y + py * i / count + particleOffsetY, ResourceManager.getInstance().getTexture("cursortrail"));
                    new ExpireModifier(0.25f).onInitializeParticle(particle);
                    new AlphaModifier(1.0f, 0.0f, 0f, 0.25f).onInitializeParticle(particle);
                    particle.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
                    new ScaleInitializer(csize).onInitializeParticle(particle);
                    if (tails == null){
                        tails = new ArrayList<>();
                    }
                    tails.add(particle);
                    GlobalManager.getInstance().getGameScene().getScene().attachChild(particle);
                }
            }
            oldPoint = new PointF(cursorSprite.getX(), cursorSprite.getY());
            if (tails != null){
                for (Particle p : tails){
                    new ExpireModifier(0.25f).onUpdateParticle(p);
                    new AlphaModifier(1.0f, 0.0f, 0f, 0.25f).onUpdateParticle(p);
                    if (p.getAlpha() == 0f){
                        p.setDead(true);
                    }
                }
                for (int i = tails.size() - 1; i >= 0; i--){
                    Particle p = tails.get(i);
                    if (p.isDead()) {
                        GlobalManager.getInstance().getGameScene().getScene().detachChild(p);
                        tails.remove(p);
                    }
                }
            }
        }*/
    }

    @Override
    public void setPosition(float pX, float pY) {
        if (emitter != null)
            emitter.setCenter(pX + particleOffsetX, pY + particleOffsetY);

        super.setPosition(pX, pY);
    }

    public ParticleSystem getParticles() {
        return particles;
    }

}
