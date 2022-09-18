package com.reco1l.ui.fragments.extras;

import static android.view.ViewGroup.*;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.edlplan.framework.easing.Easing;
import com.reco1l.ui.data.GameNotification;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.listeners.TouchListener;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 27/6/22 17:17

//TODO replace with RecyclerView due to performance issues when are too many notifications
public class NotificationCenter extends UIFragment {

    public static List<GameNotification> notifications;

    protected BadgeNotification currentPopup;
    public LinearLayout container;

    private boolean isAllowedPopups = true;
    private TextView emptyText;
    private View body, layer;
    private float bodyWidth;

    //--------------------------------------------------------------------------------------------//

    public NotificationCenter() {
        notifications = new ArrayList<>();
    }

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
        bodyWidth = Resources.dimen(R.dimen.notificationCenterWidth);

        body = find("body");
        layer = find("layer");
        emptyText = find("emptyText");
        container = find("container");

        ViewUtils.visibility(notifications.isEmpty(), emptyText);

        bindTouchListener(find("close"), new TouchListener() {
            public boolean isOnlyOnce() { return true; }

            public void onPressUp() {
                close();
            }
        });
        bindTouchListener(find("clear"), new TouchListener() {
            public void onPressUp() {
                clear(false);
            }
        });

        new Animation(rootBackground).fade(0, 1)
                .play(300);
        new Animation(platform.renderView).moveX(0, -60)
                .play(300);
        new Animation(layer).moveX(bodyWidth, 0).interpolator(Easing.OutExpo)
                .play(350);
        new Animation(body).moveX(bodyWidth, 0).interpolator(Easing.OutExpo)
                .delay(50)
                .play(400);

        body.postDelayed(this::loadNotifications, 250);
    }

    private void loadNotifications() {
        if (notifications.isEmpty())
            return;

        for (int i = 0; i < notifications.size(); i++) {
            display(i * 120L, notifications.get(i));
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void allowBadgeNotificator(boolean bool) {
        isAllowedPopups = bool;

        if (currentPopup != null) {
            if (!bool) {
                currentPopup.dismiss();
                return;
            }
            // If there's a pending BadgeNotification while they were not allowed, it will be shown instantly.
            mActivity.runOnUiThread(() -> {
                if (!currentPopup.isAdded())
                    currentPopup.show();
            });
        }
    }

    public void createBadgeNotification(GameNotification notification) {
        if (currentPopup != null)
            currentPopup.dismiss();

        currentPopup = new BadgeNotification(notification.header, notification.message);

        if (isAllowedPopups && !isShowing)
            currentPopup.show();
    }

    //--------------------------------------------------------------------------------------------//

    /**
     * Clears all the notifications that can be dismissed in the Notification Center.
     *
     * @param onlyVisually if true, the notifications will be removed from the screen, but not from the list.
     */
    public void clear(boolean onlyVisually) {

        if (onlyVisually) {
            new Animation(container).moveX(0, 80).fade(1, 0)
                    .runOnEnd(container::removeAllViews)
                    .play(200);
            return;
        }

        for (int i = 0; i < notifications.size(); i++) {
            if (notifications.get(i).hasPriority())
                continue;
            dismiss(i * 100L, notifications.get(i));
        }
    }

    /**
     * Adds a new GameNotification, if the notification center is not showing a Popup Notification
     * will be shown instead if they are allowed.
     */
    public void add(GameNotification notification) {
        if (notifications.contains(notification))
            return;

        notifications.add(notification.hasPriority() ? 0 : notifications.size(), notification);

        mActivity.runOnUiThread(() -> {
            if (isShowing) {
                emptyText.setVisibility(View.GONE);
                display(0, notification);
                return;
            }
            if (!notification.isSilent)
                createBadgeNotification(notification);
        });
    }

    public void remove(GameNotification notification) {
        if (!notifications.remove(notification))
            return;

        if (currentPopup != null && currentPopup.header.equals(notification.header))
            currentPopup.dismiss();

        if (isShowing)
            mActivity.runOnUiThread(() -> dismiss(0, notification));
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void show() {
        platform.close(UIManager.getExtras());
        if (currentPopup != null)
            currentPopup.dismiss();
        super.show();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        mActivity.runOnUiThread(() -> {
            clear(true);

            new Animation(platform.renderView).moveX(-50, 0)
                    .play(400);
            new Animation(body).moveX(0, Resources.dimen(R.dimen.notificationCenterWidth))
                    .interpolator(Easing.InExpo)
                    .runOnStart(() -> new Animation(rootBackground).fade(1, 0).play(300))
                    .play(350);
            new Animation(layer).moveX(0, bodyWidth).interpolator(Easing.InExpo)
                    .runOnEnd(super::close)
                    .delay(50)
                    .play(400);
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void display(long delay, GameNotification notification) {
        container.postDelayed(() -> {
            container.addView(notification.build());
            new Animation(notification.layout).fade(0, 1).moveX(80, 0)
                    .runOnStart(notification::load)
                    .play(100);
        }, delay);
    }

    private void dismiss(long delay, GameNotification notification) {
        if (notification.layout == null) {
            notifications.remove(notification);
            ViewUtils.visibility(notifications.isEmpty(), emptyText);
            return;
        }

        ValueAnimator anim = ValueAnimator.ofInt(notification.layout.getHeight(), 0);
        anim.setDuration(240);
        anim.addUpdateListener(animation -> {
            LayoutParams params = notification.layout.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            notification.layout.setLayoutParams(params);
        });

        new Animation(notification.layout).fade(1, 0).moveX(0, notification.layout.getWidth())
                .runOnStart(anim::start)
                .runOnEnd(() -> {
                    container.removeView(notification.layout);
                    notifications.remove(notification);
                    ViewUtils.visibility(notifications.isEmpty(), emptyText);
                })
                .delay(delay)
                .play(240);
    }

    //--------------------------------------------------------------------------------------------/

    public static class BadgeNotification extends Fragment {

        private LinearLayout body;
        private final String header;
        private final String message;

        //----------------------------------------------------------------------------------------//

        public BadgeNotification(String header, String message) {
            this.header = header;
            this.message = message;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

            View root = inflater.inflate(R.layout.popup_notification, container, false);

            body = root.findViewById(R.id.npop_body);
            new ViewTouchHandler(new TouchListener() {
                public void onPressUp() {
                    notificationCenter.show();
                }
            }).apply(body);

            TextView text = root.findViewById(R.id.npop_text);
            text.setText(header + " - " + message.replace("\n", " "));

            body.postDelayed(() -> { dismiss(); notificationCenter.currentPopup = null; }, 3000);
            new Animation(body).moveY(-50, 0).fade(0, 1).play(150);

            return root;
        }

        //----------------------------------------------------------------------------------------//

        public void show() {
            notificationCenter.currentPopup = this;
            if (isAdded())
                return;
            mActivity.runOnUiThread(() -> platform.manager.beginTransaction()
                    .add(platform.container.getId(), this, null)
                    .commit());
        }

        public void dismiss() {
            notificationCenter.currentPopup = null;
            new Animation(body).moveY(0, -50).fade(1, 0)
                    .runOnEnd(() -> platform.manager.beginTransaction().remove(this).commit())
                    .play(150);
        }
    }
}
