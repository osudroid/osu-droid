package com.reco1l.data;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.reco1l.UI;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 1/7/22 00:04

public class GameNotification {

    public Drawable icon;
    public String header, message;
    public Runnable runOnClick, onDismiss;

    public boolean
            showProgress = false,
            showCloseButton = true,
            showPopupOnNotify = true,
            hasIndeterminateProgress = true;

    public int
            progress = 0,
            progressMax = 100;

    //--------------------------------------------------------------------------------------------//

    public GameNotification(String header) {
        this.header = header;
    }

    //--------------------------------------------------------------------------------------------//

    public boolean hasPriority() {
        if (!showCloseButton) {
            return true;
        }
        return showProgress && hasIndeterminateProgress;
    }

    //--------------------------------------------------------------------------------------------//

    public void update() {
        UI.notificationCenter.update(this);
    }

    public void updateProgress() {
        UI.notificationCenter.updateProgress(this);
    }

    //--------------------------------------------------------------------------------------------//

    public Holder build(View root) {
        return new Holder(root, this);
    }

    //--------------------------------------------------------------------------------------------//

    public static class Holder {

        public final ImageView icon;
        public final TextView header, message;
        public final View body, innerBody, close;
        public final LinearProgressIndicator progressIndicator;

        //----------------------------------------------------------------------------------------//

        public Holder(View root, GameNotification notification) {

            body = root.findViewById(R.id.n_body);
            header = root.findViewById(R.id.n_header);
            icon = root.findViewById(R.id.n_icon);
            close = root.findViewById(R.id.n_close);
            message = root.findViewById(R.id.n_message);
            innerBody = root.findViewById(R.id.n_innerBody);
            progressIndicator = root.findViewById(R.id.n_progress);

            bind(notification);
        }

        //----------------------------------------------------------------------------------------//

        private void bind(GameNotification notification) {
            header.setText(notification.header);
            message.setText(notification.message);

            if (notification.icon != null) {
                icon.setImageDrawable(notification.icon);
            }

            if (!notification.showProgress) {
                progressIndicator.setVisibility(View.GONE);
                return;
            }
            progressIndicator.setVisibility(View.VISIBLE);
            progressIndicator.setMax(notification.progressMax);
            progressIndicator.setProgress(notification.progress);
            progressIndicator.setIndeterminate(notification.hasIndeterminateProgress);
        }
    }
}