package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * Created by Fuuko on 15/9/7.
 */
public class ScorePropsMenu {
    private static ScorePropsMenu instance = new ScorePropsMenu();
    private Scene scene = null;
    private boolean favorite = false;
    private ChangeableText delText;
    private boolean requestDelete = false;
    private int scoreId = -1;
    private SongMenu menu;

    private ScorePropsMenu() {

    }

    public static ScorePropsMenu getInstance() {
        return instance;
    }

    public void setSongMenu(final SongMenu menu) {
        this.menu = menu;
    }

    public void setScoreId(int scoreId) {
        this.scoreId = scoreId;
        if (scene == null) {
            init();
        }
    }

    public void init() {
        scene = new Scene();
        scene.setBackgroundEnabled(false);
        final Rectangle bg = new Rectangle(Config.getRES_WIDTH() / 4, 0,
                Config.getRES_WIDTH() / 2, Config.getRES_HEIGHT());
        bg.setColor(0, 0, 0, 0.7f);
        scene.attachChild(bg);

        final Text caption = new Text(0, Utils.toRes(60), ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_deletescore_title));
        caption.setPosition(Config.getRES_WIDTH() / 2 - caption.getWidth() / 2,
                caption.getY());
        scene.attachChild(caption);

        final Text back = new Text(0, Utils.toRes(520), ResourceManager
                .getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_mod_back)) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (menu != null) {
                        menu.getScene().clearChildScene();
                        scene = null;
                    }
                    return true;
                }
                return false;
            }
        };
        back.setPosition(Config.getRES_WIDTH() / 2 - back.getWidth() / 2,
                back.getY());
        back.setScale(1.5f);
        scene.attachChild(back);
        scene.registerTouchArea(back);

        delText = new ChangeableText(Utils.toRes(10), Utils.toRes(400),
                ResourceManager.getInstance().getFont("CaptionFont"),
                StringTable.get(R.string.menu_deletescore_delete), 100) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    if (requestDelete) {
                        setText("");
                        setPosition(-200, -200);
                        if (scoreId != -1) {
                            if (ScoreLibrary.getInstance().deleteScore(scoreId)) {
                                ToastLogger.showTextId(R.string.menu_deletescore_delete_success, false);
                            }
                            if (menu != null) {
                                menu.getScene().clearChildScene();
                                scene = null;
                                menu.reloadScoreBroad();
                            }
                        }
                    } else {
                        setText(StringTable
                                .get(R.string.menu_deletescore_delete_confirm));
                        setPosition(Config.getRES_WIDTH() / 2 - getWidth() / 2,
                                getY());
                        setColor(1, 0, 0);
                    }
                    requestDelete = !requestDelete;
                }
                return false;
            }
        };
        delText.setColor(1, 1, 1);
        delText.setText(StringTable.get(R.string.menu_deletescore_delete));
        delText.setPosition(Config.getRES_WIDTH() / 2 - delText.getWidth() / 2,
                delText.getY());
        scene.attachChild(delText);
        scene.registerTouchArea(delText);

        scene.setTouchAreaBindingEnabled(true);
    }

    public Scene getScene() {
        if (scene == null) {
            init();
        }
        return scene;
    }

    public void release() {
        // scene = null;
    }
}
