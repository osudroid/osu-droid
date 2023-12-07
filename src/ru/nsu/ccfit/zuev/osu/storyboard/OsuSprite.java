package ru.nsu.ccfit.zuev.osu.storyboard;

import android.opengl.GLES10;
import android.util.Log;
import com.dgsrz.bancho.ui.StoryBoardTestActivity;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.sprite.BaseSprite;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureBuilder;
import org.anddev.andengine.opengl.texture.atlas.buildable.builder.ITextureBuilder;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadIn;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by dgsrz on 16/9/16.
 */
public class OsuSprite {

    public static final int LAYER_BACKGROUND = 0;

    public static final int LAYER_FAIL = 1;

    public static final int LAYER_PASS = 2;

    public static final int LAYER_FOREGROUND = 3;

    public static float TO_RADIANS = (1 / 180.0f) * (float) Math.PI;

    public static float TO_DEGREES = (1 / (float) Math.PI) * 180;

    public long spriteStartTime, spriteEndTime;

    private String fileName;

    private String debugLine;

    private int layer, ZIndex;

    private Origin origin;

    private BaseSprite sprite;

    private ArrayList<OsuEvent> eventList;

    private TextureRegion textureRegion;

    private StoryBoardTestActivity activity = StoryBoardTestActivity.activity;

    private boolean isValid;

    private ParallelEntityModifier parallelEntityModifier;

    private boolean isAnimation;

    private float anchorCenterX = 0f;

    private float anchorCenterY = 0f;

    public OsuSprite(float x, float y, int layer, Origin origin, String filePath, ArrayList<OsuEvent> eventList, int ZIndex) {//normal sprite
        this.fileName = filePath.replaceAll("\"", "").replaceAll("\\\\", "/");
        textureRegion = ResourceManager.getInstance().getTexture(new File(StoryBoardTestActivity.FOLDER, fileName).getPath());
        if (null == textureRegion) {
            isValid = false;
        } else {
            isValid = true;
            sprite = new Sprite(x, y, textureRegion);
            this.layer = layer;
            this.origin = origin;
            this.eventList = eventList;
            if (filePath.equals(activity.mBackground)) {
                activity.mBackground = null;
            }
            if (eventList.isEmpty()) {
                isValid = false;
                return;
            }
            this.ZIndex = ZIndex;
            setUpSprite();
        }
    }

    public OsuSprite(float x, float y, int layer, Origin origin, String filePath, ArrayList<OsuEvent> eventList, int ZIndex, int count, int delay, String loopType) {//Animation
        isAnimation = true;
        filePath = filePath.replaceAll("\"", "").replaceAll("\\\\", "/");
        this.fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
        String fileExt = filePath.substring(filePath.lastIndexOf("."));
        FileBitmapTextureAtlasSource cSource = new FileBitmapTextureAtlasSource(new File(StoryBoardTestActivity.FOLDER, filePath.substring(0, filePath.lastIndexOf(".")) + "0" + fileExt));
        int tw = 16, th = 16;
        int width = cSource.getWidth() * count;
        int height = cSource.getHeight();
        while (tw < width) {
            tw *= 2;
        }
        while (th < height) {
            th *= 2;
        }
        BuildableBitmapTextureAtlas mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(tw, th, TextureOptions.BILINEAR);
        ArrayList<TextureRegion> textureRegions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            File temp = new File(StoryBoardTestActivity.FOLDER, filePath.substring(0, filePath.lastIndexOf(".")) + i + fileExt);
            if (temp.exists()) {
                FileBitmapTextureAtlasSource cSource2 = new FileBitmapTextureAtlasSource(temp);
                TextureRegion iTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromSource(mBitmapTextureAtlas, cSource2);
                textureRegions.add(iTextureRegion);
            } else {
                break;
            }
        }
        if (!textureRegions.isEmpty()) {
            isValid = true;
        } else {
            isValid = false;
            return;
        }
        try {
            mBitmapTextureAtlas.build(new BlackPawnTextureBuilder<>(0));
        } catch (ITextureBuilder.TextureAtlasSourcePackingException e) {
            e.printStackTrace();
        }
        ResourceManager.getInstance().getEngine().getTextureManager().loadTexture(mBitmapTextureAtlas);

        isValid = true;
        TiledTextureRegion tiledTextureRegion = new TiledTextureRegion(mBitmapTextureAtlas,
                0, 0, width, height, count, 1);
        AnimatedSprite sprite = new AnimatedSprite(x, y, tiledTextureRegion);
        sprite.animate(delay, loopType.equals("LoopForever"));
        this.sprite = sprite;
        this.layer = layer;
        this.origin = origin;
        this.eventList = eventList;
        if (eventList.isEmpty()) {
            isValid = false;
            return;
        }
        this.ZIndex = ZIndex;
        setUpSprite();
    }

    private void setUpSprite() {
//        spriteStartTime = eventList.get(eventList.size() - 1).startTime;
        for (OsuEvent osuEvent : eventList) {
            if (osuEvent.startTime + 1 >= osuEvent.endTime && osuEvent.command != Command.F) {
                continue;
            }
            spriteStartTime = osuEvent.startTime;
            break;
        }

        for (OsuEvent osuEvent : eventList) {
            if (osuEvent.startTime + 1 >= osuEvent.endTime && osuEvent.command != Command.F) {
                continue;
            }
            if (spriteStartTime > osuEvent.startTime) {
                spriteStartTime = osuEvent.startTime;
            }
        }
        for (OsuEvent firstEvent : eventList) {
            if (firstEvent.startTime + 1 == firstEvent.endTime && firstEvent.command != Command.F) {
                firstEvent.startTime = spriteStartTime;
                break;
            }
        }
        spriteEndTime = eventList.get(eventList.size() - 1).endTime;
        sprite.setVisible(false);
        sprite.setZIndex(ZIndex);
        // TODO: TextureMeta
        float x = sprite.getX();
        float y = sprite.getY();

        switch (origin) {
            case TopLeft:
                // sprite.setAnchorCenter(0f, 1f);
                sprite.setScaleCenter(0f, 0f);
                sprite.setRotationCenter(0f, 0f);
                sprite.setPosition(x, y);
                anchorCenterX = 0f;
                anchorCenterY = 0f;
                break;
            case TopCentre:
                // sprite.setAnchorCenter(0.5f, 1f);
                sprite.setScaleCenter(sprite.getWidth() / 2f, 0);
                sprite.setRotationCenter(sprite.getWidth() / 2f, 0);
                sprite.setPosition(x - sprite.getWidth() / 2f, y);
                anchorCenterX = sprite.getWidth() / 2f;
                anchorCenterY = 0f;
                break;
            case TopRight:
                // sprite.setAnchorCenter(1f, 1f);
                sprite.setScaleCenter(sprite.getWidth(), 0);
                sprite.setRotationCenter(sprite.getWidth(), 0);
                sprite.setPosition(x - sprite.getWidth(), y);
                anchorCenterX = sprite.getWidth();
                anchorCenterY = 0f;
                break;
            case CentreLeft:
                // sprite.setAnchorCenter(0f, 0.5f);
                sprite.setScaleCenter(0f, sprite.getHeight() / 2f);
                sprite.setRotationCenter(0f, sprite.getHeight() / 2f);
                sprite.setPosition(x, y - sprite.getHeight() / 2f);
                anchorCenterX = 0f;
                anchorCenterY = sprite.getHeight() / 2f;
                break;
            case Centre:
                // sprite.setAnchorCenter(0.5f, 0.5f);
                sprite.setScaleCenter(sprite.getWidth() / 2f, sprite.getHeight() / 2f);
                sprite.setRotationCenter(sprite.getWidth() / 2f, sprite.getHeight() / 2f);
                sprite.setPosition(x - sprite.getWidth() / 2f, y - sprite.getHeight() / 2f);
                anchorCenterX = sprite.getWidth() / 2f;
                anchorCenterY = sprite.getHeight() / 2f;
                break;
            case CentreRight:
                // sprite.setAnchorCenter(1f, 0.5f);
                sprite.setScaleCenter(sprite.getWidth(), sprite.getHeight() / 2f);
                sprite.setRotationCenter(sprite.getWidth(), sprite.getHeight() / 2f);
                sprite.setPosition(x - sprite.getWidth(), y - sprite.getHeight() / 2f);
                anchorCenterX = sprite.getWidth();
                anchorCenterY = sprite.getHeight() / 2f;
                break;
            case BottomLeft:
                // sprite.setAnchorCenter(0f, 0f);
                sprite.setScaleCenter(0f, sprite.getHeight());
                sprite.setRotationCenter(0f, sprite.getHeight());
                sprite.setPosition(x, y - sprite.getHeight());
                anchorCenterX = 0;
                anchorCenterY = sprite.getHeight();
                break;
            case BottomCentre:
                // sprite.setAnchorCenter(0.5f, 0f);
                sprite.setScaleCenter(sprite.getWidth() / 2f, sprite.getHeight());
                sprite.setRotationCenter(sprite.getWidth() / 2f, sprite.getHeight());
                sprite.setPosition(x - sprite.getWidth() / 2f, y - sprite.getHeight());
                anchorCenterX = sprite.getWidth() / 2f;
                anchorCenterY = sprite.getHeight();
                break;
            case BottomRight:
                // sprite.setAnchorCenter(1f, 0f);
                sprite.setScaleCenter(sprite.getWidth(), sprite.getHeight());
                sprite.setRotationCenter(sprite.getWidth(), sprite.getHeight());
                sprite.setPosition(x - sprite.getWidth(), y - sprite.getHeight());
                anchorCenterX = sprite.getWidth();
                anchorCenterY = sprite.getHeight();
                break;
        }
        IEntityModifier[] entityModifiers = new IEntityModifier[eventList.size()];
        for (int i = 0; i < eventList.size(); i++) {
            OsuEvent osuEvent = eventList.get(i);
            if (osuEvent.startTime == spriteStartTime) {
                entityModifiers[i] = parseModifier(osuEvent);
            } else {
                entityModifiers[i] = new SequenceEntityModifier(
                        new DelayModifier((osuEvent.startTime - spriteStartTime) / 1000f),
                        parseModifier(osuEvent)
                );
            }
        }
        parallelEntityModifier = new ParallelEntityModifier(entityModifiers);
        parallelEntityModifier.addModifierListener(new IModifier.IModifierListener<>() {

            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                sprite.setVisible(true);
            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                sprite.setVisible(false);
                sprite.setIgnoreUpdate(true);

                int total = StoryBoardTestActivity.activity.onScreenDrawCalls.decrementAndGet();
                Log.i("draw calls", "(detach) total draw calls: " + total);
            }
        });
    }

    public void play() {
        if (isValid) {
            sprite.registerEntityModifier(parallelEntityModifier);
            if (!sprite.hasParent()) {
                switch (layer) {
                    case LAYER_BACKGROUND:
                        StoryBoardTestActivity.activity.attachBackground(sprite);
                        break;
                    case LAYER_FOREGROUND:
                        StoryBoardTestActivity.activity.attachForeground(sprite);
                        break;
                    case LAYER_PASS:
                        StoryBoardTestActivity.activity.attachPass(sprite);
                        break;
                    case LAYER_FAIL:
                        //ignoring fail
//                      StoryBoardTestActivity.activity.attachFail(sprite);
                        break;
                }

            }
            int total = StoryBoardTestActivity.activity.onScreenDrawCalls.incrementAndGet();
            Log.i("draw calls", "total draw calls: " + total);
        }
    }

    public String getDebugLine() {
        return debugLine;
    }

    public void setDebugLine(String debugLine) {
        this.debugLine = debugLine;
    }

    private IEntityModifier parseModifier(OsuEvent osuEvent) {
        // TODO: ease变换的时候注意anchor
        IEntityModifier iEntityModifier = null;
        int ease = osuEvent.ease;
        IEaseFunction iEaseFunction = null;
        switch (ease) {
            case 0://no easing
                break;
            case 1://the changes happen fast at first, but then slow down toward the end (Easing Out)
                iEaseFunction = EaseQuadOut.getInstance();
                break;
            case 2://the changes happen slowly at first, but then speed up toward the end (Easing In)
                iEaseFunction = EaseQuadIn.getInstance();
                break;
        }
        float duration = (osuEvent.endTime - osuEvent.startTime) / 1000f;
        label:
        switch (osuEvent.command) {
            case F://Fade
                if (iEaseFunction != null) {
                    iEntityModifier = new AlphaModifier(duration, osuEvent.params[0], osuEvent.params[1], iEaseFunction);
                } else {
                    iEntityModifier = new AlphaModifier(duration, osuEvent.params[0], osuEvent.params[1]);
                }
                break;
            case M://Move
                if (iEaseFunction != null) {
                    iEntityModifier = new MoveModifier(duration, osuEvent.params[0] - anchorCenterX, osuEvent.params[2] - anchorCenterX, osuEvent.params[1] - anchorCenterY, osuEvent.params[3] - anchorCenterY, iEaseFunction);
                } else {
                    iEntityModifier = new MoveModifier(duration, osuEvent.params[0] - anchorCenterX, osuEvent.params[2] - anchorCenterX, osuEvent.params[1] - anchorCenterY, osuEvent.params[3] - anchorCenterY);
                }
                break;
            case MX://Move X
                if (iEaseFunction != null) {
                    iEntityModifier = new MoveXModifier(duration, osuEvent.params[0] - anchorCenterX, osuEvent.params[1] - anchorCenterX, iEaseFunction);
                } else {
                    iEntityModifier = new MoveXModifier(duration, osuEvent.params[0] - anchorCenterX, osuEvent.params[1] - anchorCenterX);
                }
                break;
            case MY://Move Y
                if (iEaseFunction != null) {
                    iEntityModifier = new MoveYModifier(duration, osuEvent.params[0] - anchorCenterY, osuEvent.params[1] - anchorCenterY, iEaseFunction);
                } else {
                    iEntityModifier = new MoveYModifier(duration, osuEvent.params[0] - anchorCenterY, osuEvent.params[1] - anchorCenterY);
                }
                break;
            case S://Scale
                if (iEaseFunction != null) {
                    iEntityModifier = new ScaleModifier(duration, osuEvent.params[0], osuEvent.params[1], iEaseFunction);
                } else {
                    iEntityModifier = new ScaleModifier(duration, osuEvent.params[0], osuEvent.params[1]);
                }
                break;
            case V://Vector Scale
                if (iEaseFunction != null) {
                    iEntityModifier = new ScaleModifier(duration, osuEvent.params[0], osuEvent.params[2], osuEvent.params[1], osuEvent.params[3], iEaseFunction);
                } else {
                    iEntityModifier = new ScaleModifier(duration, osuEvent.params[0], osuEvent.params[2], osuEvent.params[1], osuEvent.params[3]);
                }
                break;
            case R://Rotate
                if (iEaseFunction != null) {
                    iEntityModifier = new RotationModifier(duration, osuEvent.params[0] * TO_DEGREES, osuEvent.params[1] * TO_DEGREES, iEaseFunction);
                } else {
                    iEntityModifier = new RotationModifier(duration, osuEvent.params[0] * TO_DEGREES, osuEvent.params[1] * TO_DEGREES);
                }
                break;
            case C://Colour
                if (iEaseFunction != null) {
                    iEntityModifier = new ColorModifier(duration, osuEvent.params[0] / 255f, osuEvent.params[1] / 255f, osuEvent.params[2] / 255f,
                            osuEvent.params[3] / 255, osuEvent.params[4] / 255, osuEvent.params[5] / 255, iEaseFunction);
                } else {
                    iEntityModifier = new ColorModifier(duration, osuEvent.params[0] / 255, osuEvent.params[1] / 255, osuEvent.params[2] / 255,
                            osuEvent.params[3] / 255, osuEvent.params[4] / 255, osuEvent.params[5] / 255);
                }
                break;
            case P://Parameter
                iEntityModifier = new DelayModifier(0f);//fake modifier
                switch (osuEvent.P) {
                    case "H":
                        sprite.setFlippedHorizontal(true);
                        break;
                    case "V":
                        sprite.setFlippedVertical(true);
                        break;
                    case "A":
                        sprite.setBlendFunction(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE);
                        break;
                }
                break;
            case L: {
                ArrayList<OsuEvent> subEventList = osuEvent.subEvents;
                IEntityModifier[] subEntityModifiers = new IEntityModifier[subEventList.size()];
                float firstSubTime = 0f;
                if (!subEventList.isEmpty()) {
                    firstSubTime = subEventList.get(0).startTime;
                }
                for (int i = 0; i < subEventList.size(); i++) {
                    OsuEvent subOsuEvent = subEventList.get(i);
                    if (subOsuEvent.startTime == 0) {
                        subEntityModifiers[i] = parseModifier(subOsuEvent);
                    } else {
                        subEntityModifiers[i] = new SequenceEntityModifier(
                                new DelayModifier((subOsuEvent.startTime - firstSubTime) / 1000f),
                                parseModifier(subOsuEvent)
                        );
                    }
                }
                iEntityModifier = new LoopEntityModifier(new ParallelEntityModifier(subEntityModifiers), osuEvent.loopCount);
            }
            break;
            case T: {
                ArrayList<OsuEvent> subEventList = osuEvent.subEvents;
                ArrayList<HitSound> hitSounds = OsbParser.instance.getHitSounds();
                ArrayList<IEntityModifier> entityModifierList = new ArrayList<>();
                int soundType;
                switch (osuEvent.triggerType) {
                    case "HitSoundWhistle":
                        soundType = 2;
                        break;
                    case "HitSoundFinish":
                        soundType = 4;
                        break;
                    case "HitSoundClap":
                        soundType = 8;
                        break;
                    default:
                        break label;
                }
                long firstSoundTime = -1;
                for (HitSound hitSound : hitSounds) {
                    if (hitSound.time >= osuEvent.startTime && hitSound.time <= osuEvent.endTime && (hitSound.soundType & soundType) == soundType) {
                        if (firstSoundTime < 0) {
                            firstSoundTime = hitSound.time;
                        }
                        IEntityModifier[] subEntityModifiers = new IEntityModifier[subEventList.size()];
                        long firstSubTime = 0;
                        if (!subEventList.isEmpty()) {
                            firstSubTime = subEventList.get(0).startTime;
                        }
                        for (int i = 0; i < subEventList.size(); i++) {
                            OsuEvent subOsuEvent = subEventList.get(i);
                            if (subOsuEvent.startTime == 0) {
                                subEntityModifiers[i] = parseModifier(subOsuEvent);
                            } else {
                                subEntityModifiers[i] = new SequenceEntityModifier(
                                        new DelayModifier((subOsuEvent.startTime - firstSubTime) / 1000f),
                                        parseModifier(subOsuEvent)
                                );
                            }
                        }
                        if (firstSoundTime == hitSound.time) {
                            entityModifierList.add(new ParallelEntityModifier(subEntityModifiers));
                        } else {
                            entityModifierList.add(new SequenceEntityModifier(
                                    new DelayModifier((hitSound.time - firstSoundTime) / 1000f),
                                    new ParallelEntityModifier(subEntityModifiers)));
                        }
                    }
                    if (hitSound.time > osuEvent.endTime) {
                        break;
                    }
                }
                if (!entityModifierList.isEmpty()) {
                    iEntityModifier = new ParallelEntityModifier(entityModifierList.toArray(new IEntityModifier[0]));
                }
            }
            break;
            case NONE://do nothing
                break;
        }
        if (iEntityModifier == null) {
            iEntityModifier = new DelayModifier(0f);
        }
        return iEntityModifier;
    }

    public enum Origin {
        TopLeft, TopCentre, TopRight, CentreLeft, Centre, CentreRight, BottomLeft, BottomCentre, BottomRight, NONE;

        public static Origin getType(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (Exception e) {
                return NONE;
            }
        }
    }

}
