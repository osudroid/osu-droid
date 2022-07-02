package com.reco1l.ui.data;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.reco1l.utils.Animator;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.IMainClasses;
import com.reco1l.utils.UI;

import java.io.IOException;
import java.io.InputStream;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 00:04

public class GameNotification implements IMainClasses, UI {

    public View layout;

    public Drawable icon;
    public String header, message;
    public Runnable runOnClick, onDismiss;
    public int progressMax = 100, progress = 0;
    public boolean
            showProgressBar = false,
            showDismissButton = true,
            isProgressBarIndeterminate = true;

    private ImageView close;
    private View body, innerBody;
    private ShapeableImageView iconIv;
    private TextView titleTv, messageTv;
    private LinearProgressIndicator progressBar;

    //----------------------------------------------------------------------------------------//

    /**
     * If you change something after the creation of the notification, don't forget to call {@link #update()}.
     */
    public GameNotification(String header) {
        this.header = header;
        if (icon == null) {
            try {
                InputStream asset = mActivity.getAssets().open("music_list.png");
                icon = Drawable.createFromStream(asset, null);
            } catch (IOException ignored) { }
        }
    }

    public View build() {
        layout = LayoutInflater.from(platform.context).inflate(R.layout.notification, null);
        body = layout.findViewById(R.id.n_body);
        iconIv = layout.findViewById(R.id.n_icon);
        close = layout.findViewById(R.id.n_close);
        titleTv = layout.findViewById(R.id.n_title);
        messageTv = layout.findViewById(R.id.n_message);
        innerBody = layout.findViewById(R.id.n_innerBody);
        progressBar = layout.findViewById(R.id.n_progress);
        return layout;
    }

    public boolean isAdded() {
        return inbox.container.indexOfChild(layout) != -1;
    }

    public boolean hasPriority() {
        if (!showDismissButton)
            return true;
        return showProgressBar && isProgressBarIndeterminate;
    }

    public void load() {
        if (!isAdded())
            return;

        titleTv.setText(header);
        messageTv.setText(message);

        if (runOnClick != null)
            new ClickListener(body).simple(runOnClick);

        if (hasPriority()) {
            close.setVisibility(View.INVISIBLE);
        } else {
            close.setVisibility(View.VISIBLE);
            new ClickListener(close).simple(() -> {
                if (onDismiss != null)
                    onDismiss.run();
                inbox.remove(this);
            });
        }

        if (icon != null)
            iconIv.setImageDrawable(icon);

        if (!showProgressBar) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(progressMax);
        progressBar.setProgress(progress);
        progressBar.setIndeterminate(isProgressBarIndeterminate);
    }

    /**
     * Update the notification with the new values.
     * <p>
     * Note: To update the progress of the ProgressBar use {@link #updateProgress(int)} instead.
     */
    public void update() {
        mActivity.runOnUiThread(() -> {
            if (!inbox.isShowing) {
                inbox.createBadgeNotification(this);
                return;
            }
            new Animator(innerBody).moveX(0, -50).fade(1, 0)
                    .runOnEnd(() -> {
                        load();
                        new Animator(innerBody).moveX(50, 0).fade(0, 1).play(120);
                    })
                    .play(120);
        });
    }

    public void updateProgress(int progress) {
        this.progress = progress;
        if (inbox.isShowing && progressBar != null)
            mActivity.runOnUiThread(() -> progressBar.setProgress(progress));
    }
}