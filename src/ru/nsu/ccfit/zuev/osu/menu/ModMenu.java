package ru.nsu.ccfit.zuev.osu.menu;

import com.edlplan.ui.fragment.ModSettingsMenu;
import com.reco1l.ibancho.RoomAPI;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.Execution;
import com.reco1l.osu.multiplayer.Multiplayer;
import com.reco1l.ibancho.data.RoomMods;
import com.reco1l.osu.multiplayer.RoomScene;

import com.rian.osu.GameMode;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.mods.*;
import com.rian.osu.utils.ModHashMap;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.anddev.andengine.util.MathUtils;

import kotlinx.coroutines.Job;
import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.IModSwitcher;
import ru.nsu.ccfit.zuev.osu.game.mods.ModButton;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.TextButton;

public class ModMenu implements IModSwitcher {
    private static final ModMenu instance = new ModMenu();
    private Scene scene = null, parent;
    private ModHashMap enabledMods = new ModHashMap();
    private ChangeableText multiplierText;
    private BeatmapInfo selectedBeatmap;
    private final Map<Mod, ModButton> modButtons = new HashMap<>();
    private boolean enableNCWhenSpeedChange = false;
    private Job calculationJob;
    private ModSettingsMenu menu;

    private ModMenu() {}
    
    public static ModMenu getInstance() {
        return instance;
    }

    public void show(Scene scene, BeatmapInfo selectedBeatmap) {
        parent = scene;
        setSelectedTrack(selectedBeatmap);
        scene.setChildScene(getScene(), false, true, true);
        if (menu == null) {
            menu = new ModSettingsMenu();
        }

        Execution.mainThread(menu::show);
        update();
    }

    public void update()
    {
        // Ensure selected mods are visually selected
        synchronized (modButtons) {
            for (var key : modButtons.keySet()) {
                var button = modButtons.get(key);

                if (button != null)
                    button.setEnabled(enabledMods.contains(key));
            }

            // Updating multiplier text just in case
            changeMultiplierText();
        }
    }

    public void setMods(RoomMods mods, boolean isFreeMods, boolean allowForceDifficultyStatistics)
    {
        var modMap = mods.map;

        if (isFreeMods) {
            for (var mod : modMap.values()) {
                if (!mod.isValidForMultiplayerAsFreeMod()) {
                    enabledMods.put(mod);
                }
            }
        } else {
            enabledMods = modMap;
        }

        if (!isFreeMods || !allowForceDifficultyStatistics) {
            var difficultyAdjust = modMap.ofType(ModDifficultyAdjust.class);

            if (difficultyAdjust != null) {
                enabledMods.put(difficultyAdjust);
            }
        }

        if (!Multiplayer.isRoomHost() && (modMap.contains(ModDoubleTime.class) || modMap.contains(ModNightCore.class))) {
            var doubleTime = new ModDoubleTime();
            var nightCore = new ModNightCore();

            enabledMods.remove(Config.isUseNightcoreOnMultiplayer() ? doubleTime : nightCore);
            enabledMods.put(Config.isUseNightcoreOnMultiplayer() ? nightCore : doubleTime);
        }

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

            var string = enabledMods.toString();

            // The room mods are the same as the host mods
            if (Multiplayer.isRoomHost()) {
                RoomAPI.setRoomMods(string);
            } else if (updatePlayerMods) {
                RoomAPI.setPlayerMods(string);
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

    private void addButton(int x, int y, Mod mod) {
        if (!(mod instanceof IModUserSelectable selectableMod)) {
            throw new IllegalArgumentException("Mod must implement IModUserSelectable");
        }

        var button = new ModButton(x, y, selectableMod, this);

        button.setEnabled(this.enabledMods.contains(mod));
        scene.attachChild(button);
        scene.registerTouchArea(button);

        modButtons.put(mod, button);
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

        menu = new ModSettingsMenu();

        changeMultiplierText();

        final int offset = 100;
        final int offsetGrowth = 130;
        final TextureRegion button = ResourceManager.getInstance().getTexture("selection-mod-easy");

        var clickShortSound = ResourceManager.getInstance().getSound("click-short");
        var clickShortConfirmSound = ResourceManager.getInstance().getSound("click-short-confirm");

        //line 1
        addButton(offset, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, new ModEasy());

        // Used to define the X offset of each button according to its visibility
        int factor = 1;

        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, new ModNoFail());

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost())
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, new ModHalfTime());

        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, new ModReallyEasy());

        factor = 1;

        //line 2
        addButton(offset, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModHardRock());

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost())
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModDoubleTime());

        if (!Multiplayer.isMultiplayer || Multiplayer.isRoomHost())
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModNightCore());

        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModHidden());
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModTraceable());
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModFlashlight());
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModSuddenDeath());
        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, new ModPerfect());

        factor = 1;

        //line 3
        addButton(offset, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, new ModRelax());
        addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, new ModAutopilot());

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, new ModAuto());

        if (!Multiplayer.isMultiplayer)
            addButton(offset + offsetGrowth * factor++, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, new ModScoreV2());

        addButton(offset + offsetGrowth * factor, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, new ModPrecise());

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

                        enabledMods.clear();
                        reloadMusicEffects();
                        changeMultiplierText();
                        for (ModButton btn : modButtons.values()) {
                            btn.setEnabled(false);
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

                            // Copy the mods to avoid concurrent modification
                            var mods = enabledMods.deepCopy().values();

                            switch (Config.getDifficultyAlgorithm()) {
                                case droid -> {
                                    var attributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(
                                        beatmap, mods, scope
                                    );

                                    GlobalManager.getInstance().getSongMenu().setStarsDisplay(
                                        GameHelper.Round(attributes.starRating, 2)
                                    );
                                }

                                case standard -> {
                                    var attributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(
                                        beatmap, mods, scope
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

    public ModHashMap getEnabledMods() {
        return enabledMods;
    }

    public void changeMultiplierText() {
        GlobalManager.getInstance().getSongMenu().changeDimensionInfo(selectedBeatmap);

        float multiplier = 1;

        if (selectedBeatmap != null) {
            var difficulty = selectedBeatmap.getBeatmapDifficulty();

            for (var mod : enabledMods.values()) {
                multiplier *= mod.calculateScoreMultiplier(difficulty);
            }
        }

        multiplierText.setText(StringTable.format(com.osudroid.resources.R.string.menu_mod_multiplier, multiplier));
        multiplierText.setPosition(
                Config.getRES_WIDTH() / 2f - multiplierText.getWidth() / 2,
                multiplierText.getY());
        if (multiplier == 1) {
            multiplierText.setColor(1, 1, 1);
        } else if (multiplier < 1) {
            multiplierText.setColor(1, 150f / 255f, 0);
        } else {
            multiplierText.setColor(5 / 255f, 240 / 255f, 5 / 255f);
        }
    }

    @Override
    public boolean switchMod(IModUserSelectable selectableMod) {
        var mod = (Mod) selectableMod;
        boolean returnValue = true;

        var checkOffSound = ResourceManager.getInstance().getSound("check-off");
        var checkOnSound = ResourceManager.getInstance().getSound("check-on");

        if (enabledMods.contains(mod)) {
            enabledMods.remove(mod);
            returnValue = false;
        } else {
            enabledMods.put(mod);
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

        if (mod instanceof ModRateAdjust) {
            reloadMusicEffects();
        }

        update();

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

    public boolean isEnableNCWhenSpeedChange(){
        return enableNCWhenSpeedChange;
    }

    public void setEnableNCWhenSpeedChange(boolean t){
        enableNCWhenSpeedChange = t;

        GlobalManager.getInstance().getSongMenu().updateMusicEffects();
    }

    public void cancelCalculationJob() {
        if (calculationJob != null) {
            calculationJob.cancel(new CancellationException("Difficulty calculation has been cancelled."));
        }
    }

    public ModSettingsMenu getMenu() {
        return menu;
    }
}
