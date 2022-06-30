package com.reco1l.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.edlplan.framework.easing.Easing;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.Animator;
import com.reco1l.utils.ClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 27/6/22 17:17

public class Inbox extends BaseLayout {

    public static List<Notification> notifications;
    public static Inbox instance;

    protected LinearLayout container;
    protected PopupNotification currentPopup;

    private boolean isAllowedPopups = true;
    private TextView emptyText;
    private View body, layer;
    private float bodyWidth;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "nc";
    }

    @Override
    protected int getLayout() {
        return R.layout.notification_center;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(true, true);
        bodyWidth = res().getDimension(R.dimen.notificationCenterWidth);

        body = find("body");
        layer = find("layer");
        emptyText = find("emptyText");
        container = find("container");
        ImageView close = find("close");
        ImageView clear = find("clear");

        if (notifications.size() != 0)
            setVisible(false, emptyText);

        new ClickListener(close).simple(this::close);
        new ClickListener(clear).simple(() -> clear(false));

        new Animator(rootBackground).fade(0, 1).play(300);

        new Animator(platform.renderView).moveX(0, -60)
                .play(300);
        new Animator(layer).moveX(bodyWidth, 0).interpolator(Easing.OutExpo)
                .play(350);
        new Animator(body).moveX(bodyWidth, 0).interpolator(Easing.OutExpo)
                .delay(50)
                .play(400);

        body.postDelayed(this::loadNotifications, 200);
    }

    private void loadNotifications() {
        if (notifications.isEmpty())
            return;

        final List<Notification> list = new ArrayList<>();

        // Finding notifications that are priority
        for (int i = 0; i < notifications.size(); i++) {
            if (!notifications.get(i).isPriority())
                list.add(notifications.get(i));
        }

        // Then adding not priority notifications after the priority ones
        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).isPriority())
                list.add(notifications.get(i));
        }

        // Showing notifications with the new order
        for (int i = 0; i < list.size(); i++) {
            int finalI = i;
            container.postDelayed(() -> {
                container.addView(list.get(finalI).build(getContext()));
                list.get(finalI).load();
                list.get(finalI).show();
            }, i * 100L);
        }
    }

    /**
     * Clears all the notifications that can be dismissed in the Notification Center.
     */
    public void clear(boolean onlyVisually) {
        if (notifications.isEmpty())
            return;

        if (onlyVisually) {
            new Animator(container).moveX(0, 80).fade(1, 0)
                    .runOnEnd(container::removeAllViews)
                    .play(200);
            return;
        }

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);

            if (!notification.canDismiss)
                continue;

            notification.dismiss(i * 100L, () -> {
                container.removeView(notification.root);
                notifications.remove(notification);
                if (notifications.size() == 0) {
                    setVisible(true, emptyText);
                    new Animator(emptyText).fade(0, 1).moveX(50, 0).play(300);
                }
            });
            notification.isAdded = false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void allowPopupNotificator(boolean bool) {
        this.isAllowedPopups = bool;

        if (currentPopup != null) {

            if (!bool) {
                currentPopup.dismiss();
                currentPopup = null;
                return;
            }
            // If there's a pending Popup Notification, it will be shown instantly.
            mActivity.runOnUiThread(() -> {
                if (!currentPopup.isAdded())
                    platform.manager.beginTransaction()
                            .add(platform.container.getId(), currentPopup, currentPopup.tag)
                            .commit();
            });
        }
    }

    /**
     * Adds a new Notification, if the notification center is not showing a Popup Notification
     * will be shown instead if they are Allowed.
     *
     * @param notification The notification to be added to the Notification Center.
     */
    public void add(Notification notification) {
        if (notifications.contains(notification))
            return;

        notifications.add(notification);

        mActivity.runOnUiThread(() -> {
            if (isShowing) {
                setVisible(false, emptyText);
                
                container.addView(notification.build(getContext()));
                notification.load();
                notification.show();
            } else {
                if (currentPopup != null) {
                    currentPopup.dismiss();
                    currentPopup = null;
                }
                final String tag = "popup" + "@" + notifications.indexOf(notification);

                currentPopup = new PopupNotification(tag, notification.title, notification.runOnClick);

                if (!isAllowedPopups)
                    return;
                platform.manager.beginTransaction()
                        .add(platform.container.getId(), currentPopup, currentPopup.tag)
                        .commit();
            }
        });
    }

    /**
     * Removes a notification from the Notification Center.
     *
     * @param notification The notification to be removed from the Notification Center.
     */
    public void remove(Notification notification) {
        if (!notifications.remove(notification))
            return;

        if (currentPopup != null && currentPopup.text.equals(notification.title)) {
            currentPopup.dismiss();
            currentPopup = null;
        }

        if (!isShowing)
            return;

        mActivity.runOnUiThread(() -> {
            notification.dismiss(0, () -> {
                container.removeView(notification.root);
                if (notifications.size() == 0) {
                    setVisible(true, emptyText);
                    new Animator(emptyText).fade(0, 1).moveX(50, 0).play(300);
                }
            });
            notification.isAdded = false;
        });
    }

    /**
     * @return <code>true</code> if the notification is already added to the notifications list.
     */
    public boolean hasAdded(Notification notification) {
        return notifications.contains(notification);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void show() {
        if (currentPopup != null)
            currentPopup.dismiss();
        super.show();
    }

    @Override
    public void close() {
        mActivity.runOnUiThread(() -> {
            clear(true);

            new Animator(platform.renderView).moveX(-50, 0)
                    .play(400);
            new Animator(body).moveX(0, res().getDimension(R.dimen.notificationCenterWidth)).interpolator(Easing.InExpo)
                    .runOnStart(() -> new Animator(rootBackground).fade(1, 0).play(300))
                    .play(350);
            new Animator(layer).moveX(0, bodyWidth).interpolator(Easing.InExpo)
                    .runOnEnd(super::close)
                    .delay(50)
                    .play(400);
        });
    }

    //--------------------------------------------------------------------------------------------/

    public static class PopupNotification extends Fragment {

        protected final String tag;

        private LinearLayout body;
        private final String text;
        private final Runnable onClick;

        public PopupNotification(String tag, String text, Runnable onClick) {
            this.tag = tag;
            this.text = text;
            this.onClick = onClick;
        }

        public void dismiss() {
            new Animator(body).moveY(0, -50).fade(1, 0)
                    .runOnEnd(() -> platform.manager.beginTransaction().remove(this).commit())
                    .play(200);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

            View root = inflater.inflate(R.layout.popup_notification, container, false);

            body = root.findViewById(R.id.npop_body);
            if (onClick != null) {
                new ClickListener(body).simple(onClick);
            }

            TextView text = root.findViewById(R.id.npop_text);
            text.setText(this.text);

            body.postDelayed(this::dismiss, 3000);
            new Animator(body).moveY(-50, 0).fade(0, 1).play(200);

            return root;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Notification {

        protected View root;
        protected boolean isAdded = false;

        public Drawable icon;
        public String title, message;
        public Runnable runOnClick, onDismiss;
        public int progressBarMax = 100, progress = 0;
        public boolean
                canDismiss = true,
                hasProgressBar = false,
                isProgressBarIndeterminate = true;

        private View body;
        private ImageView close;
        private ShapeableImageView iconIv;
        private TextView titleTv, messageTv;
        private LinearProgressIndicator progressIndicator;

        //----------------------------------------------------------------------------------------//

        protected void show() {
            new Animator(body).fade(0, 1).moveX(80, 0).play(100);
        }

        protected void dismiss(long delay, Runnable runOnEnd) {
            int duration = 240;

            ValueAnimator anim = ValueAnimator.ofInt(body.getHeight(), 0);
            anim.setDuration(duration - 1);
            anim.addUpdateListener(animation -> {
                ViewGroup.LayoutParams params = body.getLayoutParams();
                params.height = (int) animation.getAnimatedValue();
                body.setLayoutParams(params);
                body.requestLayout();
            });

            new Animator(body).fade(1, 0).moveX(0, body.getWidth())
                    .runOnStart(anim::start)
                    .runOnEnd(runOnEnd)
                    .delay(delay).play(duration);
        }

        /**
         * You can change later the content of the notification by modifying the variables.
         * <p>
         * If you change something after the creation of the notification, don't forget to call {@link #update()}.
         */
        public Notification(String title, String message) {
            this.title = title;
            this.message = message;
            if (icon == null) {
                try {
                    InputStream asset = mActivity.getAssets().open("music_list.png");
                    icon = Drawable.createFromStream(asset, null);
                } catch (IOException ignored) {}
            }
        }

        protected View build(Context context) {
            root = LayoutInflater.from(context).inflate(R.layout.notification, null);
            body = root.findViewById(R.id.n_body);
            iconIv = root.findViewById(R.id.n_icon);
            close = root.findViewById(R.id.n_close);
            titleTv = root.findViewById(R.id.n_title);
            messageTv = root.findViewById(R.id.n_message);
            progressIndicator = root.findViewById(R.id.n_progress);
            isAdded = true;
            return root;
        }

        protected boolean isPriority() {
            return !canDismiss || (hasProgressBar && isProgressBarIndeterminate);
        }

        protected void load() {
            if (!isAdded)
                return;

            titleTv.setText(title);
            messageTv.setText(message);

            if (isPriority()) {
                close.setVisibility(View.INVISIBLE);
            } else {
                close.setVisibility(View.VISIBLE);
                new ClickListener(close).simple(() -> {
                    if (onDismiss != null)
                        onDismiss.run();
                    inbox.remove(this);
                });
            }

            if (hasProgressBar) {
                progressIndicator.setVisibility(View.VISIBLE);
                progressIndicator.setMax(progressBarMax);
                progressIndicator.setProgress(progress);
                progressIndicator.setIndeterminate(isProgressBarIndeterminate);
            } else {
                progressIndicator.setVisibility(View.GONE);
            }
            if (icon != null)
                iconIv.setImageDrawable(icon);

            if (runOnClick != null)
                new ClickListener(body).simple(runOnClick);
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        public Notification showProgressBar(int max, boolean indeterminate) {
            hasProgressBar = true;
            progressBarMax = max;
            isProgressBarIndeterminate = indeterminate;
            return this;
        }

        /**
         * Set if the notification can be dismissed by the user, you can change this later modifying
         * the variable onDismiss.
         * <p>
         * By default this is <code>true</code>, call this method only if you want to disable it.
         * <p>
         * Note: If you set this to <code>false</code>, the notification will automatically set
         * as priority.
         */
        public Notification showDismissButton(boolean bool) {
            canDismiss = bool;
            return this;
        }

        /**
         * Set runnable to be executed when the user clicks on the close button.
         */
        public Notification runOnDismiss(Runnable runnable) {
            onDismiss = runnable;
            return this;
        }

        /**
         * Update the notification with the new values.
         */
        public void update() {
            if (root == null || !inbox.isShowing)
                return;
            load();
        }
    }

}
