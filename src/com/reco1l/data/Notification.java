package com.reco1l.data;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.reco1l.global.UI;
import com.reco1l.tables.ResourceTable;
import com.reco1l.ui.fragments.NotificationCenter;
import com.reco1l.view.ProgressIndicator;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 00:04

public final class Notification implements ResourceTable {

    private Runnable
            mOnClick,
            mOnDismiss;

    private String mMessage;
    private Drawable mDrawable;
    private BassSoundProvider mSound;

    private boolean
            mSilent = false,
            mShowProgress = false,
            mShowDismissButton = true;

    private int
            mProgress = -1,
            mProgressMax = 100;

    private final String mHeader;

    //--------------------------------------------------------------------------------------------//

    public Notification(String pHeader) {
        mHeader = pHeader;
        mDrawable = drw(R.drawable.v18_notifications);
    }

    public static Notification of(String pHeader) {
        Notification current = UI.notificationCenter.get(pHeader);

        if (current != null) {
            return current;
        }
        return new Notification(pHeader);
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

    public String getKey() {
        return mHeader;
    }

    //--------------------------------------------------------------------------------------------//

    public void bind(View root) {

        TextView header = root.findViewById(R.id.n_header);
        header.setText(mHeader);

        TextView message = root.findViewById(R.id.n_message);
        message.setText(mMessage);

        ImageView icon = root.findViewById(R.id.n_icon);
        ProgressIndicator indicator = root.findViewById(R.id.n_progress);

        if (mShowProgress) {
            icon.setVisibility(View.GONE);
            indicator.setVisibility(View.VISIBLE);
            indicator.setMax(mProgressMax);

            if (mProgress < 0) {
                indicator.setIndeterminate(true);
            } else {
                indicator.setIndeterminate(false);
                indicator.setProgress(mProgress);
            }
        } else {
            indicator.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);

            if (icon.getDrawable() != mDrawable) {
                icon.setImageDrawable(mDrawable);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public Notification commit() {
        NotificationCenter.instance.add(this);
        return this;
    }

    public void remove() {
        NotificationCenter.instance.remove(this);
    }

    private void notifyChange() {
        NotificationCenter.instance.onNotificationChange(this);
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

    public Notification setMessage(String pMessage) {
        mMessage = pMessage;
        notifyChange();
        return this;
    }

    public Notification setIcon(Drawable drawable) {
        mDrawable = drawable;
        notifyChange();
        return this;
    }

    public Notification runOnClick(Runnable pOnClick) {
        mOnClick = pOnClick;
        notifyChange();
        return this;
    }

    public Notification runOnDismiss(Runnable pOnDismiss) {
        mOnDismiss = pOnDismiss;
        notifyChange();
        return this;
    }

    public Notification showCloseButton(boolean pBool) {
        mShowDismissButton = pBool;
        notifyChange();
        return this;
    }

    public Notification showProgress(boolean pBool) {
        mShowProgress = pBool;
        notifyChange();
        return this;
    }

    // -1 to indeterminate
    public Notification setProgress(int pProgress) {
        mShowProgress = true;
        mProgress = pProgress;
        notifyChange();
        return this;
    }

    public Notification setProgressMax(int pMax) {
        mProgressMax = pMax;
        notifyChange();
        return this;
    }

    // Notify without sound and without popup
    public Notification setSilent(boolean bool) {
        mSilent = bool;
        return this;
    }

    // Set null to notify without sound
    public Notification setSound(BassSoundProvider pSound) {
        mSound = pSound;
        return this;
    }
}