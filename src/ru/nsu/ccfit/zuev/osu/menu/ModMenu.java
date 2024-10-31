package ru.nsu.ccfit.zuev.osu.menu;

import com.edlplan.ui.fragment.InGameSettingMenu;
import com.reco1l.ibancho.RoomAPI;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.Execution;
import com.reco1l.osu.multiplayer.Multiplayer;
import com.reco1l.ibancho.data.RoomMods;
import com.reco1l.osu.multiplayer.MultiplayerConverter;
import com.reco1l.osu.multiplayer.RoomScene;

import com.rian.osu.GameMode;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.difficulty.calculator.DifficultyCalculationParameters;
import com.rian.osu.utils.ModUtils;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

import org.anddev.andengine.util.MathUtils;
import org.jetbrains.annotations.Nullable;

import kotlinx.coroutines.Job;
import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.game.mods.IModSwitcher;
import ru.nsu.ccfit.zuev.osu.game.mods.ModButton;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.TextButton;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;


public class ModMenu implements IModSwitcher {

    public static final float DEFAULT_FL_FOLLOW_DELAY = 0.12f;

    private static final ModMenu instance = new ModMenu();
    private Scene scene = null, parent;
    private EnumSet<GameMod> mod;
    private ChangeableText multiplierText;
    private BeatmapInfo selectedBeatmap;
    private final Map<GameMod, ModButton> modButtons = new TreeMap<>();
    private float changeSpeed = 1.0f;
    private boolean enableNCWhenSpeedChange = false;
    private boolean modsRemoved = false;
    private float FLfollowDelay = DEFAULT_FL_FOLLOW_DELAY;
    private Job calculationJob;


    private Float customAR = null;
    private Float customOD = null;
    private Float customHP = null;
    private Float customCS = null;
    private InGameSettingMenu menu;

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

    public void show(Scene scene, BeatmapInfo selectedBeatmap) {
        parent = scene;
        setSelectedTrack(selectedBeatmap);
        scene.setChildScene(getScene(), false, true, true);
        if (menu == null) {
            menu = new InGameSettingMenu();
        }

        Execution.mainThread(menu::show);
        update();
    }

    public void update()
    {
        // Ensure selected mods are visually selected
        synchronized (modButtons) {
            for (GameMod key : modButtons.keySet()) {
                var button = modButtons.get(key);

                if (button != null)
                    button.setModEnabled(mod.contains(key));
            }

            // Updating multiplier text just in case
            changeMultiplierText();
        }
    }

    public void setMods(RoomMods mods, boolean isFreeMods, boolean allowForceDifficultyStatistics)
    {
        var modSet = mods.getSet();

        if (!isFreeMods)
        {
            mod = modSet;

            FLfollowDelay = mods.getFlFollowDelay();
        }

        if (!isFreeMods || !allowForceDifficultyStatistics) {
            customAR = mods.getCustomAR();
            customOD = mods.getCustomOD();
            customCS = mods.getCustomCS();
            customHP = mods.getCustomHP();
        }

        changeSpeed = mods.getSpeedMultiplier();

        if (!Multiplayer.isRoomHost())
        {
            if (modSet.contains(GameMod.MOD_DOUBLETIME) || modSet.contains(GameMod.MOD_NIGHTCORE))
            {
                mod.remove(Config.isUseNightcoreOnMultiplayer() ? GameMod.MOD_DOUBLETIME : GameMod.MOD_NIGHTCORE);
                mod.add(Config.isUseNightcoreOnMultiplayer() ? GameMod.MOD_NIGHTCORE : GameMod.MOD_DOUBLETIME);
            }
            else {
                mod.remove(GameMod.MOD_NIGHTCORE);
                mod.remove(GameMod.MOD_DOUBLETIME);
            }
        }

        if (modSet.contains(GameMod.MOD_SCOREV2))
            mod.add(GameMod.MOD_SCOREV2);
        else
            mod.remove(GameMod.MOD_SCOREV2);

        if (modSet.contains(GameMod.MOD_HALFTIME))
            mod.add(GameMod.MOD_HALFTIME);
        else
            mod.remove(GameMod.MOD_HALFTIME);

        update();
    }

    public void hide() {
        hide(true);
    }

    public void hide(boolean updatePlayerMods) {
        if (parent != null) {
            parent.clearChildScene();
            parent = null;
        }
//        InGameSettingMenu.Companion.getInstance().dismiss();
        if (menu != null) {
            menu.dismiss();
        }

        if (Multiplayer.isConnected())
        {
            RoomScene.isWaitingForModsChange = true;

            var string = MultiplayerConverter.modsToString(mod);

            // The room mods are the same as the host mods
            if (Multiplayer.isRoomHost()) {
                RoomAPI.setRoomMods(
                        string,
                        changeSpeed,
                        FLfollowDelay,
                        customAR,
                        customOD,
                        customCS,
                        customHP
                );
            } else if (updatePlayerMods) {
                RoomAPI.setPlayerMods(
                        string,
                        changeSpeed,
                        FLfollowDelay,
                        customAR,
                        customOD,
                        customCS,
                        customHP
                );
            } else {
                RoomScene.isWaitingForModsChange = false;
            }
        }
    }

    public void hideByFrag() {
        if (parent != null) {
            parent.clearChildScene();
            parent = null;
        }

        menu = null;
    }

    private void addButton(int x, int y, GameMod mod) {
        ModButton mButton;

        mButton = new ModButton(x, y, mod);
        mButton.setModEnabled(this.mod.contains(mod));
        mButton.setSwitcher(this);
        scene.attachChild(mButton);
        scene.registerTouchArea(mButton);
        modButtons.put(mod, mButton);
    }

    public void init() {
        cancelCalculationJob();
        calculationJob = null;

        modButtons.clear();
        scene = new Scene();
        scene.setBackgroundEnabled(false);
        final Rectangle bg = new Rectangle(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 0.7f);
        scene.attachChild(bg);

        multiplierText = new ChangeableText(0, Utils.toRes(50),
                ResourceManager.getInstance().getFont("CaptionFont"),
                StringTable.format(com.osudroid.resources.R.string.menu_mod_multiplier, 1f));
        multiplierText.setScale(1.2f);
        scene.attachChild(multiplierText);

        menu = new InGameSettingMenu();

        changeMultiplierText();

        final int offset = 100;
        final int offsetGrowth = 130;
        final TextureRegion button = ResourceManager.getInstance().getTexture("selection-mod-easy");

        var clickShortSound = ResourceManager.getInstance().getSound("click-short");
        var clickShortConfirmSound = ResourceManager.getInstance().getSound("click-short-confirm");

        //line 1
        addButton(offset, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, GameMod.MOD_EASY);

        // Used to define the X offset of each button according to its visibility
        int factor = 1;

        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, GameMod.MOD_NOFAIL);

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost())
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, GameMod.MOD_HALFTIME);

        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, GameMod.MOD_REALLYEASY);

        factor = 1;

        //line 2
        addButton(offset, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_HARDROCK);

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost())
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_DOUBLETIME);

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost())
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_NIGHTCORE);

        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_HIDDEN);
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_FLASHLIGHT);
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_SUDDENDEATH);
        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, GameMod.MOD_PERFECT);

        factor = 1;

        //line 3
        addButton(offset, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, GameMod.MOD_RELAX);
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, GameMod.MOD_AUTOPILOT);

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, GameMod.MOD_AUTO);

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, GameMod.MOD_SCOREV2);

        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, GameMod.MOD_PRECISE);

        final TextButton resetText = new TextButton(ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(com.osudroid.resources.R.string.menu_mod_reset)) {

            boolean moved = false;
            private float dx = 0, dy = 0;

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    moved = false;
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    return true;
                }

                if (pSceneTouchEvent.isActionUp()) {
                    if (!moved) {
                        if (clickShortConfirmSound != null) {
                            clickShortConfirmSound.play();
                        }

                        mod.clear();
                        reloadMusicEffects();
                        changeMultiplierText();
                        for (ModButton btn : modButtons.values()) {
                            btn.setModEnabled(false);
                        }
                    }

                    return true;
                }

                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    if (!moved && clickShortSound != null) {
                        clickShortSound.play();
                    }

                    moved = true;
                }

                return false;
            }
        };

        if (!Multiplayer.isMultiplayer) {
            scene.attachChild(resetText);
            scene.registerTouchArea(resetText);
        }
        resetText.setScale(1.2f);

        final TextButton back = new TextButton(ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(com.osudroid.resources.R.string.menu_mod_back)) {

            boolean moved = false;
            private float dx = 0, dy = 0;

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    moved = false;
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    return true;
                }

                if (pSceneTouchEvent.isActionUp() && !moved) {
                    if (clickShortConfirmSound != null) {
                        clickShortConfirmSound.play();
                    }

                    cancelCalculationJob();

                    calculationJob = Execution.async(scope -> {
                        if (selectedBeatmap == null) {
                            return;
                        }

                        try (var parser = new BeatmapParser(selectedBeatmap.getPath(), scope)) {
                            var beatmap = parser.parse(
                                true,
                                Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid ? GameMode.Droid : GameMode.Standard
                            );
                            if (beatmap == null) {
                                GlobalManager.getInstance().getSongMenu().setStarsDisplay(0);
                                return;
                            }

                            var parameters = new DifficultyCalculationParameters(
                                ModUtils.convertLegacyMods(
                                    mod,
                                    isCustomCS() ? customCS : null,
                                    isCustomAR() ? customAR : null,
                                    isCustomOD() ? customOD : null
                                ),
                                changeSpeed
                            );

                            switch (Config.getDifficultyAlgorithm()) {
                                case droid -> {
                                    var attributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(
                                        beatmap, parameters, scope
                                    );

                                    GlobalManager.getInstance().getSongMenu().setStarsDisplay(
                                        GameHelper.Round(attributes.starRating, 2)
                                    );
                                }

                                case standard -> {
                                    var attributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(
                                        beatmap, parameters, scope
                                    );

                                    GlobalManager.getInstance().getSongMenu().setStarsDisplay(
                                        GameHelper.Round(attributes.starRating, 2)
                                    );
                                }
                            }
                        }
                    });
                    hide();
                    return true;
                }

                if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    if (!moved && clickShortSound != null) {
                        clickShortSound.play();
                    }
                    moved = true;
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

        update();
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
        GlobalManager.getInstance().getSongMenu().changeDimensionInfo(selectedBeatmap);
        //calculateAble = true;
        float mult = 1;
        for (GameMod m : mod) {
            mult *= m.scoreMultiplier;
        }
        if (changeSpeed != 1.0f){
            mult *= StatisticV2.getSpeedChangeScoreMultiplier(getSpeed(), mod);
        }
        if (selectedBeatmap != null) {
            if (isCustomCS()) {
                mult *= StatisticV2.getCustomCSScoreMultiplier(selectedBeatmap.getCircleSize(), customCS);
            }

            if (isCustomOD()) {
                mult *= StatisticV2.getCustomODScoreMultiplier(selectedBeatmap.getOverallDifficulty(), customOD);
            }
        }

        multiplierText.setText(StringTable.format(com.osudroid.resources.R.string.menu_mod_multiplier,
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

    public boolean handleCustomDifficultyStatisticsFlags() {
        if (!isCustomCS() || !isCustomAR() || !isCustomOD() || !isCustomHP()) {
            return false;
        }

        var modsToRemove = new GameMod[] { GameMod.MOD_HARDROCK, GameMod.MOD_EASY, GameMod.MOD_REALLYEASY };
        var modsRemoved = false;

        for (var gameMod : modsToRemove) {
            if (mod.contains(gameMod)) {
                mod.remove(gameMod);
                modButtons.get(gameMod).setModEnabled(false);

                modsRemoved = true;
            }
        }

        if (modsRemoved) {
            ToastLogger.showTextId(com.osudroid.resources.R.string.force_diffstat_mod_unpickable, false);
        }

        return modsRemoved;
    }

    public boolean switchMod(GameMod flag) {
        boolean returnValue = true;

        var checkOffSound = ResourceManager.getInstance().getSound("check-off");
        var checkOnSound = ResourceManager.getInstance().getSound("check-on");

        if (mod.contains(flag)) {
            mod.remove(flag);

            if (flag == GameMod.MOD_FLASHLIGHT)
                resetFLFollowDelay();

            returnValue = false;
        } else {
            mod.add(flag);

            if (handleCustomDifficultyStatisticsFlags()) {
                if (checkOffSound != null) {
                    checkOffSound.play();
                }
                return false;
            }

            handleModFlags(flag, GameMod.MOD_HARDROCK, new GameMod[]{GameMod.MOD_EASY});
            handleModFlags(flag, GameMod.MOD_EASY, new GameMod[]{GameMod.MOD_HARDROCK});
            handleModFlags(flag, GameMod.MOD_AUTOPILOT, new GameMod[]{GameMod.MOD_RELAX, GameMod.MOD_AUTO, GameMod.MOD_NOFAIL});
            handleModFlags(flag, GameMod.MOD_AUTO, new GameMod[]{GameMod.MOD_RELAX, GameMod.MOD_AUTOPILOT, GameMod.MOD_PERFECT, GameMod.MOD_SUDDENDEATH});
            handleModFlags(flag, GameMod.MOD_RELAX, new GameMod[]{GameMod.MOD_AUTO, GameMod.MOD_NOFAIL, GameMod.MOD_AUTOPILOT});
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

        if (flag == GameMod.MOD_DOUBLETIME || flag == GameMod.MOD_NIGHTCORE || flag == GameMod.MOD_HALFTIME) {
            reloadMusicEffects();
        }

        if (returnValue) {
            if (checkOnSound != null) {
                checkOnSound.play();
            }
        } else {
            if (checkOffSound != null) {
                checkOffSound.play();
            }
        }

        changeMultiplierText();

        return returnValue;
    }

    public void setSelectedTrack(BeatmapInfo selectedBeatmap) {
        this.selectedBeatmap = selectedBeatmap;
        if (selectedBeatmap != null) {
            changeMultiplierText();
        }
    }

    private void reloadMusicEffects() {
        GlobalManager.getInstance().getSongMenu().updateMusicEffects();
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

        GlobalManager.getInstance().getSongMenu().updateMusicEffects();
    }

    public boolean isDefaultFLFollowDelay() {
        return FLfollowDelay == DEFAULT_FL_FOLLOW_DELAY;
    }

    public void resetFLFollowDelay() {
        FLfollowDelay = DEFAULT_FL_FOLLOW_DELAY;
    }

    public boolean isEnableNCWhenSpeedChange(){
        return enableNCWhenSpeedChange;
    }

    public void setEnableNCWhenSpeedChange(boolean t){
        enableNCWhenSpeedChange = t;

        GlobalManager.getInstance().getSongMenu().updateMusicEffects();
    }

    public void updateMultiplierText(){
        changeMultiplierText();
    }

    public void cancelCalculationJob() {
        if (calculationJob != null) {
            calculationJob.cancel(new CancellationException("Difficulty calculation has been cancelled."));
        }
    }


    public boolean isCustomAR() {
        return customAR != null;
    }

    public Float getCustomAR() {
        return customAR;
    }

    public void setCustomAR(@Nullable Float customAR) {
        this.customAR = customAR;

        handleCustomDifficultyStatisticsFlags();
    }


    public boolean isCustomOD() {
        return customOD != null;
    }

    public Float getCustomOD() {
        return customOD;
    }

    public void setCustomOD(@Nullable Float customOD) {
        this.customOD = customOD;

        handleCustomDifficultyStatisticsFlags();
    }


    public boolean isCustomHP() {
        return customHP != null;
    }

    public Float getCustomHP() {
        return customHP;
    }

    public void setCustomHP(@Nullable Float customHP) {
        this.customHP = customHP;

        handleCustomDifficultyStatisticsFlags();
    }


    public boolean isCustomCS() {
        return customCS != null;
    }

    public Float getCustomCS() {
        return customCS;
    }

    public void setCustomCS(@Nullable Float customCS) {
        this.customCS = customCS;

        handleCustomDifficultyStatisticsFlags();
    }

    public InGameSettingMenu getMenu() {
        return menu;
    }
}
