package ru.nsu.ccfit.zuev.osu.menu;

import com.edlplan.ui.fragment.InGameSettingMenu;

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
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.game.mods.IModSwitcher;
import ru.nsu.ccfit.zuev.osu.game.mods.ModButton;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.TextButton;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ModMenu implements IModSwitcher {
    private static ModMenu instance = new ModMenu();
    private Scene scene = null;
    private SongMenu menu;
    private EnumSet<GameMod> mod;
    private ChangeableText multiplierText;
    private TrackInfo selectedTrack;
    private Map<GameMod, ModButton> modButtons = new TreeMap<GameMod, ModButton>();

    private ModMenu() {
        mod = EnumSet.noneOf(GameMod.class);
    }

    public static ModMenu getInstance() {
        return instance;
    }

    public void setSongMenu(final SongMenu menu) {
        this.menu = menu;
    }

    public void reload() {
        mod = EnumSet.noneOf(GameMod.class);
        init();
    }

    public void show(SongMenu songMenu, TrackInfo selectedTrack) {
        setSongMenu(songMenu);
        setSelectedTrack(selectedTrack);
        songMenu.scene.setChildScene(getScene(),
                false, true, true);
        InGameSettingMenu.getInstance().show();
    }

    public void hide() {
        if (menu != null) {
            menu.getScene().clearChildScene();
        }
        InGameSettingMenu.getInstance().dismiss();
    }

    public void hideByFrag() {
        if (menu != null) {
            menu.getScene().clearChildScene();
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
        addButton(offset + offsetGrowth * 0, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-easy", GameMod.MOD_EASY);
        addButton(offset + offsetGrowth * 1, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-nofail", GameMod.MOD_NOFAIL);
        addButton(offset + offsetGrowth * 2, Config.getRES_HEIGHT() / 2 - button.getHeight() * 3, "selection-mod-halftime", GameMod.MOD_HALFTIME);

        //line 2
        addButton(offset + offsetGrowth * 0, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-hardrock", GameMod.MOD_HARDROCK);
        addButton(offset + offsetGrowth * 1, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-doubletime", GameMod.MOD_DOUBLETIME);
        addButton(offset + offsetGrowth * 2, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-nightcore", GameMod.MOD_NIGHTCORE);
        addButton(offset + offsetGrowth * 3, Config.getRES_HEIGHT() / 2 - button.getHeight() / 2, "selection-mod-hidden", GameMod.MOD_HIDDEN);

        //line 3
        addButton(offset + offsetGrowth * 0, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-relax", GameMod.MOD_RELAX);
        addButton(offset + offsetGrowth * 1, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-relax2", GameMod.MOD_AUTOPILOT);
        addButton(offset + offsetGrowth * 2, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-autoplay", GameMod.MOD_AUTO);
        addButton(offset + offsetGrowth * 3, Config.getRES_HEIGHT() / 2 + button.getHeight() * 2, "selection-mod-precise", GameMod.MOD_PRECISE);


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
        float mult = 1;
        if (mod.contains(GameMod.MOD_AUTO)) {
            mult *= 0;
        }
        if (mod.contains(GameMod.MOD_RELAX)) {
            mult *= 0;
        }
        if (mod.contains(GameMod.MOD_AUTOPILOT)) {
            mult *= 0;
        }
        if (mod.contains(GameMod.MOD_EASY)) {
            mult *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_NOFAIL)) {
            mult *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            mult *= 1.06f;
        }
        if (mod.contains(GameMod.MOD_HIDDEN)) {
            mult *= 1.06f;
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            mult *= 1.12f;
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            mult *= 1.12f;
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            mult *= 0.3f;
        }

        multiplierText.setText(StringTable.format(R.string.menu_mod_multiplier,
                mult));
        multiplierText.setPosition(
                Config.getRES_WIDTH() / 2 - multiplierText.getWidth() / 2,
                multiplierText.getY());
        if (mult == 1) {
            multiplierText.setColor(1, 1, 1);
        } else if (mult < 1) {
            multiplierText.setColor(1, 150f / 255f, 0);
        } else {
            multiplierText.setColor(5 / 255f, 240 / 255f, 5 / 255f);
        }
    }


    public boolean switchMod(GameMod flag) {
        if (mod.contains(flag)) {
            mod.remove(flag);
            changeMultiplierText();
            return false;
        } else {
            mod.add(flag);
            boolean modsRemoved = false;
            if (flag.equals(GameMod.MOD_HARDROCK)) {
                mod.remove(GameMod.MOD_EASY);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_EASY)) {
                mod.remove(GameMod.MOD_HARDROCK);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_AUTO)) {
                mod.remove(GameMod.MOD_AUTOPILOT);
                mod.remove(GameMod.MOD_RELAX);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_AUTOPILOT)) {
                mod.remove(GameMod.MOD_AUTO);
                mod.remove(GameMod.MOD_RELAX);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_RELAX)) {
                mod.remove(GameMod.MOD_AUTOPILOT);
                mod.remove(GameMod.MOD_AUTO);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_DOUBLETIME)) {
                mod.remove(GameMod.MOD_NIGHTCORE);
                mod.remove(GameMod.MOD_HALFTIME);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_NIGHTCORE)) {
                mod.remove(GameMod.MOD_DOUBLETIME);
                mod.remove(GameMod.MOD_HALFTIME);
                modsRemoved = true;
            } else if (flag.equals(GameMod.MOD_HALFTIME)) {
                mod.remove(GameMod.MOD_DOUBLETIME);
                mod.remove(GameMod.MOD_NIGHTCORE);
                modsRemoved = true;
            }
            if (modsRemoved) {
                for (GameMod gmod : modButtons.keySet()) {
                    modButtons.get(gmod).setModEnabled(mod.contains(gmod));
                }
            }
            changeMultiplierText();
            return true;
        }
    }

    public TrackInfo getSelectedTrack() {
        return selectedTrack;
    }

    public void setSelectedTrack(TrackInfo selectedTrack) {
        this.selectedTrack = selectedTrack;
        if (selectedTrack != null) {
            changeMultiplierText();
        }
    }
}
