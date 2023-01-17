package com.reco1l.data;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.UI;
import com.reco1l.utils.Animation;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 00:04

public final class GameNotification {

    private Runnable
            mOnClick,
            mOnDismiss;

    private String mMessage;

    private BassSoundProvider mSound;

    private boolean
            mSilent = true,
            mShowProgress = false,
            mShowDismissButton = true;

    private int
            mProgress = -1,
            mIconResource = 0,
            mProgressMax = 100;

    private final String mHeader;

    //--------------------------------------------------------------------------------------------//

    private GameNotification(String pHeader) {
        mHeader = pHeader;
    }

    public static GameNotification of(String pHeader) {
        GameNotification current = UI.notificationCenter.get(pHeader);

        if (current != null) {
            return current;
        }
        return new GameNotification(pHeader);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean hasPriority() {
        if (!mShowDismissButton) {
            return true;
        }
        return mShowProgress;
    }

    public boolean isSilent() {
        return mSilent;
    }

    //--------------------------------------------------------------------------------------------//

    public Holder build(View pRoot) {
        return new Holder(pRoot, this);
    }

    public void commit() {
        UI.notificationCenter.add(this);
    }

    public void remove() {
        UI.notificationCenter.remove(this);
    }

    //--------------------------------------------------------------------------------------------//

    public void onNotify() {
        if (UI.notificationCenter.contains(this)) {
            if (mSound != null) {
                mSound.play();
            }
        }
    }

    public void onDismiss() {
        if (!UI.notificationCenter.contains(this)) {
            if (mOnDismiss != null) {
                mOnDismiss.run();
            }
        }
    }

    public void onClick() {
        if (mOnClick != null) {
            mOnClick.run();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public GameNotification setMessage(String pMessage) {
        mMessage = pMessage;
        return this;
    }

    public GameNotification setIcon(@DrawableRes int pResourceId) {
        mIconResource = pResourceId;
        return this;
    }

    public GameNotification runOnClick(Runnable pOnClick) {
        mOnClick = pOnClick;
        return this;
    }

    public GameNotification runOnDismiss(Runnable pOnDismiss) {
        mOnDismiss = pOnDismiss;
        return this;
    }

    public GameNotification showCloseButton(boolean pBool) {
        mShowDismissButton = pBool;
        return this;
    }

    public GameNotification showProgress(boolean pBool) {
        mShowProgress = pBool;
        return this;
    }

    // Notify without sound and without popup
    public GameNotification setSilent(boolean bool) {
        mSilent = bool;
        return this;
    }

    public GameNotification setProgress(int pProgress) {
        mShowProgress = true;
        mProgress = pProgress;
        return this;
    }

    public GameNotification setProgressMax(int pMax) {
        mProgressMax = pMax;
        return this;
    }

    // Set null to notify without sound
    public GameNotification setSound(BassSoundProvider pSound) {
        mSound = pSound;
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public String getKey() {
        return mHeader;
    }

    //--------------------------------------------------------------------------------------------//

    public static class Holder {

        public final View
                body,
                closeButton;

        public final TextView
                headerText,
                messageText;

        public final ImageView iconView;
        public final CircularProgressIndicator progressView;

        private final GameNotification mNotification;

        //----------------------------------------------------------------------------------------//

        public Holder(View root, GameNotification pNotification) {
            body = root.findViewById(R.id.n_body);
            iconView = root.findViewById(R.id.n_icon);
            headerText = root.findViewById(R.id.n_header);
            closeButton = root.findViewById(R.id.n_close);
            messageText = root.findViewById(R.id.n_message);
            progressView = root.findViewById(R.id.n_progress);

            mNotification = pNotification;
            bind(pNotification);
        }

        //----------------------------------------------------------------------------------------//

        private void bind(GameNotification n) {
            headerText.setText(n.mHeader);
            messageText.setText(n.mMessage);

            if (n.mIconResource != 0) {
                iconView.setImageResource(n.mIconResource);
            }

            if (n.mShowProgress) {
                progressView.setVisibility(View.VISIBLE);
                progressView.setMax(n.mProgressMax);

                if (n.mProgress < 0) {
                    progressView.setIndeterminate(true);
                } else {
                    progressView.setIndeterminate(false);
                    progressView.setProgress(n.mProgress);
                }
            } else {
                progressView.setVisibility(View.GONE);
            }
        }

        public void handleUpdate() {
            if (messageText.getText().equals(mNotification.mMessage)) {
                Animation.of(messageText)
                        .toAlpha(0)
                        .runOnEnd(() -> messageText.setText(mNotification.mMessage))
                        .play(160);
            }


        }
    }
}