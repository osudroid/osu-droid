package ru.nsu.ccfit.zuev.osu.menu;

import com.edlplan.ui.fragment.InGameSettingMenu;
import com.reco1l.api.ibancho.RoomAPI;
import com.reco1l.framework.lang.execution.Async;
import com.reco1l.legacy.data.MultiplayerConverter;
import com.reco1l.legacy.ui.multiplayer.Multiplayer;
import com.reco1l.legacy.ui.multiplayer.RoomScene;
import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.game.mods.IModSwitcher;
import ru.nsu.ccfit.zuev.osu.game.mods.ModButton;
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.TextButton;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

import static com.reco1l.legacy.data.MultiplayerConverter.*;

public class ModMenu implements IModSwitcher {
    private static final ModMenu instance = new ModMenu();
    private Scene scene = null, parent;
    private EnumSet<GameMod> mod;
    private ChangeableText multiplierText;
    private TrackInfo selectedTrack;
    private final Map<GameMod, ModButton> modButtons = new TreeMap<>();
    private float changeSpeed = 1.0f;
    private float forceAR = 9.0f;
    private boolean enableForceAR = false;
    private boolean enableNCWhenSpeedChange = false;
    private boolean modsRemoved = false;
    private float FLfollowDelay = 0.12f;

    private ModMenu() {
        mod = EnumSet.noneOf(GameMod.class);
    }

    public float getFLfollowDelay() {
        return FLfollowDelay;
    }

    public void setFLfollowDelay(float newfLfollowDelay) {
        FLfollowDelay = newfLfollowDelay;
    }
    
    public static ModMenu getInstance() {
        return instance;
    }

    public void reload() {
        mod = EnumSet.noneOf(GameMod.class);
        init();
    }

    public void show(Scene scene, TrackInfo selectedTrack) {
        parent = scene;
        setSelectedTrack(selectedTrack);
        scene.setChildScene(getScene(), false, true, true);

        // TODO Custom mods support for multiplayer.
        if (!Multiplayer.isMultiplayer)
            InGameSettingMenu.getInstance().show();

        update();
    }

    public void update()
    {
        // Ensure selected mods are visually selected
        synchronized (modButtons) {
            if (!modButtons.isEmpty()) {
                for (GameMod key : modButtons.keySet()) {
                    var button = modButtons.get(key);

                    if (button != null)
                        button.setModEnabled(mod.contains(key));
                }
            }

            // Updating multiplier text just in case
            changeMultiplierText();
        }
    }

    public void setMods(EnumSet<GameMod> mods, boolean isFreeMods)
    {
        if (!isFreeMods)
            mod = mods;

        if (!Multiplayer.isRoomHost)
        {
            if (mods.contains(GameMod.MOD_DOUBLETIME) || mods.contains(GameMod.MOD_NIGHTCORE))
            {
                mod.remove(Config.isUseNightcoreOnMultiplayer() ? GameMod.MOD_DOUBLETIME : GameMod.MOD_NIGHTCORE);
                mod.add(Config.isUseNightcoreOnMultiplayer() ? GameMod.MOD_NIGHTCORE : GameMod.MOD_DOUBLETIME);
            }
            else {
                mod.remove(GameMod.MOD_NIGHTCORE);
                mod.remove(GameMod.MOD_DOUBLETIME);
            }
        }

        if (mods.contains(GameMod.MOD_SCOREV2))
            mod.add(GameMod.MOD_SCOREV2);
        else
            mod.remove(GameMod.MOD_SCOREV2);

        if (mods.contains(GameMod.MOD_HALFTIME))
            mod.add(GameMod.MOD_HALFTIME);
        else
            mod.remove(GameMod.MOD_HALFTIME);

        update();
    }

    public void hide() {
        if (parent != null) {
            parent.clearChildScene();
            parent = null;
        }
        InGameSettingMenu.getInstance().dismiss();

        if (!Multiplayer.isConnected)
            return;

        //noinspection DataFlowIssue
        var currentMods = MultiplayerConverter.stringToMods(RoomScene.getRoom().getMods());

        if (!currentMods.equals(mod))
        {
            RoomScene.INSTANCE.setAwaitModsChange(true);

            Async.run(() -> {

                if (Multiplayer.isRoomHost)
                    RoomAPI.setRoomMods(modsToString(mod));
                else
                    RoomAPI.setPlayerMods(modsToString(mod));
            });
        }

    }

    public void hideByFrag() {
        if (parent != null) {
            parent.clearChildScene();
            parent = null;
        }
    }

    private void addButton(int x, int y, String texture, GameMod mod) {
        ModButton mButton;

        mButton = new ModButton(x, y, texture, mod);
        mButton.setModEnabled(this.mod.contains(mod));
        mButton.setSwitcher(this);
        scene.attachChild(mButton);
        scene.registerTouchArea(mButton);
        modButtons.put(mod, mButton);
    }

    public void init() {

        modButtons.clear();
        scene = new Scene();
        scene.setBackgroundEnabled(false);
        final Rectangle bg = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 0.7f);
        scene.attachChild(bg);

        multiplierText = new ChangeableText(0, Utils.toRes(50),
                ResourceManager.getInstance().getFont("CaptionFont"),
                StringTable.format(R.string.menu_mod_multiplier, 1f));
        multiplierText.setScale(1.2f);
        scene.attachChild(multiplierText);

        changeMultiplierText();

        final int offset = 100;
        final int offsetGrowth = 130;
        final TextureRegion button = ResourceManager.getInstance().getTexture("selection-mod-easy");

        //line 1
        addButton(offset, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-easy", GameMod.MOD_EASY);

        // Used to define the X offset of each button according to its visibility
        int factor = 1;

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-nofail", GameMod.MOD_NOFAIL);

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-halftime", GameMod.MOD_HALFTIME);

        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-reallyeasy", GameMod.MOD_REALLYEASY);

        factor = 1;

        //line 2
        addButton(offset, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-hardrock", GameMod.MOD_HARDROCK);

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-doubletime", GameMod.MOD_DOUBLETIME);

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-nightcore", GameMod.MOD_NIGHTCORE);

        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-hidden", GameMod.MOD_HIDDEN);
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-flashlight", GameMod.MOD_FLASHLIGHT);
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-suddendeath", GameMod.MOD_SUDDENDEATH);
        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-perfect", GameMod.MOD_PERFECT);

        factor = 1;

        //line 3
        addButton(offset, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-relax", GameMod.MOD_RELAX);
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-relax2", GameMod.MOD_AUTOPILOT);

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-autoplay", GameMod.MOD_AUTO);

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-scorev2", GameMod.MOD_SCOREV2);

        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-precise", GameMod.MOD_PRECISE);
        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-smallcircle", GameMod.MOD_SMALLCIRCLE);


        final TextButton resetText = new TextButton(ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_mod_reset)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionUp()) {
                    mod.clear();
                    changeMultiplierText();
                    for (ModButton btn : modButtons.values()) {
                        btn.setModEnabled(false);
                    }
                    return true;
                }
                return false;
            }
        };
        scene.attachChild(resetText);
        scene.registerTouchArea(resetText);
        resetText.setScale(1.2f);

        final TextButton back = new TextButton(ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_mod_back)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionUp()) {
                    (new Thread() {
                        public void run() {
                            if (GlobalManager.getInstance().getSongMenu().getSelectedTrack() != null){
                                BeatmapData beatmapData = new BeatmapParser(
                                        GlobalManager.getInstance().getSongMenu().getSelectedTrack().getFilename()
                                ).parse(true);

                                if (beatmapData == null) {
                                    GlobalManager.getInstance().getSongMenu().setStarsDisplay(0);
                                    return;
                                }

                                DifficultyCalculationParameters parameters = new DifficultyCalculationParameters();
                                parameters.mods = getMod();
                                parameters.customSpeedMultiplier = changeSpeed;

                                if (enableForceAR) {
                                    parameters.forcedAR = forceAR;
                                }

                                DifficultyAttributes attributes = BeatmapDifficultyCalculator.calculateDifficulty(
                                        beatmapData,
                                        parameters
                                );


                                GlobalManager.getInstance().getSongMenu().setStarsDisplay(
                                        GameHelper.Round(attributes.starRating, 2)
                                );
                            }
                        }
                    }).start();
                    hide();
                    return true;
                }
                return false;
            }
        };
        back.setScale(1.2f);
        back.setWidth(resetText.getWidth());
        back.setHeight(resetText.getHeight());
        back.setPosition(Config.getRES_WIDTH() - back.getWidth() - 60, Config.getRES_HEIGHT() - back.getHeight() - 30);
        back.setColor(66 / 255f, 76 / 255f, 80 / 255f);
        resetText.setPosition(Config.getRES_WIDTH() - resetText.getWidth() - 60, back.getY() - resetText.getHeight() - 20);
//		multiplierText.setPosition(back.getX() + (back.getWidth() / 2 - multiplierText.getWidth() / 2), resetText.getY() - multiplierText.getHeight() - 40);

        scene.attachChild(back);
        scene.registerTouchArea(back);

        scene.setTouchAreaBindingEnabled(true);
    }

    public Scene getScene() {
        if (scene == null) {
            init();
        }
        return scene;
    }

    public EnumSet<GameMod> getMod() {
        return mod.clone();
    }

    public void setMod(EnumSet<GameMod> mod) {
        this.mod = mod.clone();
    }

    private void changeMultiplierText() {
        GlobalManager.getInstance().getSongMenu().changeDimensionInfo(selectedTrack);
        //calculateAble = true;
        float mult = 1;
        for (GameMod m : mod) {
            mult *= m.scoreMultiplier;
        }
        if (changeSpeed != 1.0f){
            mult *= StatisticV2.getSpeedChangeScoreMultiplier(getSpeed(), mod);
        }

        multiplierText.setText(StringTable.format(R.string.menu_mod_multiplier,
                mult));
        multiplierText.setPosition(
                Config.getRES_WIDTH() / 2f - multiplierText.getWidth() / 2,
                multiplierText.getY());
        if (mult == 1) {
            multiplierText.setColor(1, 1, 1);
        } else if (mult < 1) {
            multiplierText.setColor(1, 150f / 255f, 0);
        } else {
            multiplierText.setColor(5 / 255f, 240 / 255f, 5 / 255f);
        }
    }

    public void handleModFlags(GameMod flag, GameMod modToCheck, GameMod[] modsToRemove) {
        if (flag.equals(modToCheck)) {
            for (GameMod modToRemove: modsToRemove) {
                mod.remove(modToRemove);
                modsRemoved = true;
            }
        }
    }

    public boolean switchMod(GameMod flag) {
        boolean returnValue = true;

        if (mod.contains(flag)) {
            mod.remove(flag);
            returnValue = false;
        } else {
            mod.add(flag);

            handleModFlags(flag, GameMod.MOD_HARDROCK, new GameMod[]{GameMod.MOD_EASY});
            handleModFlags(flag, GameMod.MOD_EASY, new GameMod[]{GameMod.MOD_HARDROCK});
            handleModFlags(flag, GameMod.MOD_AUTOPILOT, new GameMod[]{GameMod.MOD_RELAX, GameMod.MOD_SUDDENDEATH, GameMod.MOD_AUTO, GameMod.MOD_NOFAIL});
            handleModFlags(flag, GameMod.MOD_AUTO, new GameMod[]{GameMod.MOD_RELAX, GameMod.MOD_AUTOPILOT, GameMod.MOD_PERFECT, GameMod.MOD_SUDDENDEATH});
            handleModFlags(flag, GameMod.MOD_RELAX, new GameMod[]{GameMod.MOD_AUTO, GameMod.MOD_SUDDENDEATH, GameMod.MOD_NOFAIL, GameMod.MOD_AUTOPILOT});
            handleModFlags(flag, GameMod.MOD_DOUBLETIME, new GameMod[]{GameMod.MOD_NIGHTCORE, GameMod.MOD_HALFTIME});
            handleModFlags(flag, GameMod.MOD_NIGHTCORE, new GameMod[]{GameMod.MOD_DOUBLETIME, GameMod.MOD_HALFTIME});
            handleModFlags(flag, GameMod.MOD_HALFTIME, new GameMod[]{GameMod.MOD_DOUBLETIME, GameMod.MOD_NIGHTCORE});
            handleModFlags(flag, GameMod.MOD_SUDDENDEATH, new GameMod[]{GameMod.MOD_NOFAIL, GameMod.MOD_PERFECT, GameMod.MOD_AUTO});
            handleModFlags(flag, GameMod.MOD_PERFECT, new GameMod[]{GameMod.MOD_NOFAIL, GameMod.MOD_SUDDENDEATH, GameMod.MOD_AUTO});
            handleModFlags(flag, GameMod.MOD_NOFAIL, new GameMod[]{GameMod.MOD_PERFECT, GameMod.MOD_SUDDENDEATH, GameMod.MOD_AUTOPILOT, GameMod.MOD_RELAX});

            if (modsRemoved) {
                for (GameMod gameMod : modButtons.keySet()) {
                    modButtons.get(gameMod).setModEnabled(mod.contains(gameMod));
                }
            }
        }

        changeMultiplierText();

        return returnValue;
    }

    public void setSelectedTrack(TrackInfo selectedTrack) {
        this.selectedTrack = selectedTrack;
        if (selectedTrack != null) {
            changeMultiplierText();
        }
    }

    public float getSpeed(){
        float speed = changeSpeed;
        if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)){
            speed *= 1.5f;
        } else if (mod.contains(GameMod.MOD_HALFTIME)){
            speed *= 0.75f;
        }

        return speed;
    }

    public boolean isChangeSpeed() {
        return changeSpeed != 1.0;
    }

    public float getChangeSpeed(){
        return changeSpeed;
    }

    public void setChangeSpeed(float speed){
        changeSpeed = speed;
    }

    public float getForceAR(){
        return forceAR;
    }

    public void setForceAR(float ar){
        forceAR = ar;
    }

    public boolean isEnableForceAR(){
        return enableForceAR;
    }

    public boolean isDefaultFLFollowDelay() {
        return FLfollowDelay == 0.12f;
    }

    public void setEnableForceAR(boolean t){
        enableForceAR = t;
    }

    public boolean isEnableNCWhenSpeedChange(){
        return enableNCWhenSpeedChange;
    }

    public void setEnableNCWhenSpeedChange(boolean t){
        enableNCWhenSpeedChange = t;
    }

    public void updateMultiplierText(){
        changeMultiplierText();
    }

}
