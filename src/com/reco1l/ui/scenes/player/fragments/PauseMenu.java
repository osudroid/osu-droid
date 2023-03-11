package com.reco1l.ui.scenes.player.fragments;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.reco1l.ui.base.Layers;
import com.reco1l.ui.custom.Notification;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;
import com.reco1l.tools.Logging;
import com.reco1l.framework.input.TouchListener;
import com.reco1l.view.ButtonView;
import com.reco1l.ui.elements.MenuButtonVIew;

import org.anddev.andengine.entity.scene.Scene;

import com.rimu.R;

public class PauseMenu extends BaseFragment {

    public static final PauseMenu instance = new PauseMenu();

    private final Scene mChild;

    private MenuButtonVIew
            mResume,
            mRetry,
            mExit;

    private LinearLayout mLayout;
    private ButtonView mSave;

    private boolean mWasFailed = false;

    //--------------------------------------------------------------------------------------------//

    public PauseMenu() {
        super();

        mChild = new Scene();
        mChild.setBackgroundEnabled(false);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.overlay_pause_menu;
    }

    @Override
    protected String getPrefix() {
        return "pm";
    }

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Overlay;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mLayout = find("buttons");
        mResume = find("resume");
        mRetry = find("retry");
        mExit = find("exit");
        mSave = find("save");

        if (Scenes.player.isReplaying()) {
            mSave.setVisibility(View.VISIBLE);
        }

        if (mWasFailed) {
            mResume.setVisibility(View.GONE);
        } else {
            mResume.setVisibility(View.VISIBLE);
            mSave.setVisibility(View.GONE);
        }

        Animation.of(mLayout)
                .fromHeight(0)
                .toHeight(sdp(90))
                .fromAlpha(0)
                .toAlpha(1)
                .play(250);

        Animation.of(mRetry, mExit, mWasFailed ? null : mResume)
                .fromWidth(0)
                .toWidth(sdp(120))
                .fromAlpha(0)
                .toAlpha(1)
                .delay(50)
                .runOnEnd(this::bindTouchListeners)
                .play(250);

        if (mWasFailed) {
            Animation.of(mSave)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .fromY(40)
                    .toY(0)
                    .play(200);
        }
    }

    private void bindTouchListeners() {
        bindTouch(mResume, new TouchListener() {

            public String getPressUpSound() {
                return "menuback";
            }

            public void onPressUp() {
                close(Scenes.player::resume);
            }
        });

        bindTouch(mRetry, new TouchListener() {

            public String getPressUpSound() {
                return "menuhit";
            }

            public void onPressUp() {
                close(Scenes.player::retry);
            }
        });

        bindTouch(mExit, new TouchListener() {

            public String getPressUpSound() {
                return "menuback";
            }

            public void onPressUp() {
                close(Scenes.player::quit);
            }
        });

        bindTouch(mSave, () -> {
            Notification n = new Notification("Replay");

            if (Scenes.player.saveReplay()) {
                n.setMessage("Replay successfully saved!");
                unbindTouch(mSave);

                Animation.of(mSave)
                        .toAlpha(0)
                        .toY(40)
                        .play(200);
            } else {
                n.setMessage("Failed to save replay!");
            }
            n.commit();
        });
    }

    //--------------------------------------------------------------------------------------------//

    public boolean show(boolean isFail) {
        mWasFailed = isFail;

        Scenes.player.setChildScene(mChild, false, true, true);
        return super.show();
    }

    @Override
    public boolean show() {
        return show(false);
    }

    public void close(Runnable task) {
        if (!isAdded()) {
            task.run();
            return;
        }

        unbindTouchHandlers();

        Animation.of(mRetry, mExit, mWasFailed ? null : mResume)
                .toWidth(0)
                .toAlpha(0)
                .play(250);

        Animation.of(mLayout)
                .toHeight(0)
                .toAlpha(0)
                .delay(50)
                .runOnEnd(() -> {
                    if (task != null) {
                        task.run();
                    }
                    super.close();
                })
                .play(250);
    }

    @Override
    public void close() {
        Logging.e(this, "Cannot call method close(), call close(Runnable) instead.");
    }
}
