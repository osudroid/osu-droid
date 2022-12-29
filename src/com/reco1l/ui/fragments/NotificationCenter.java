package com.reco1l.ui.fragments;

import static android.widget.RelativeLayout.ALIGN_PARENT_END;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.view.View;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.data.GameNotification;
import com.reco1l.UI;
import com.reco1l.data.adapters.NotificationListAdapter;
import com.reco1l.data.adapters.NotificationListAdapter.ViewHolder;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AnimationOld;
import com.reco1l.data.tables.ResourceTable;
import com.reco1l.utils.ViewUtils;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 27/6/22 17:17

public final class NotificationCenter extends BaseFragment {

    public static NotificationCenter instance;

    private final NotificationListAdapter adapter;
    private final ArrayList<GameNotification> notifications;

    private final PopupFragment popupFragment;

    private View body, layer;
    private RecyclerView container;
    private TextView counter, emptyText;

    private boolean isAllowedPopups = true;

    private float bodyWidth;

    //--------------------------------------------------------------------------------------------//

    public NotificationCenter() {
        super();
        notifications = new ArrayList<>();
        adapter = new NotificationListAdapter(notifications);
        popupFragment = new PopupFragment();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "nc";
    }

    @Override
    protected int getLayout() {
        return R.layout.notification_center;
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(true, true);
        bodyWidth = ResourceTable.dimen(R.dimen.notificationCenterWidth);

        body = find("body");
        layer = find("layer");
        counter = find("counter");
        emptyText = find("emptyText");
        container = find("container");

        Animation.of(rootBackground)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        /*Game.platform.animateScreen()
                .toX(-60)
                .play(300);*/

        Animation.of(layer)
                .fromX(bodyWidth)
                .toX(0)
                .interpolator(Easing.OutExpo)
                .play(350);

        Animation.of(body)
                .fromX(bodyWidth)
                .toX(0)
                .interpolator(Easing.OutExpo)
                .delay(50)
                .play(400);

        container.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, false));
        container.setAdapter(adapter);

        bindTouchListener(find("clear"), this::clear);
    }

    @Override
    protected void onUpdate(float sec) {
        if (emptyText != null) {
            ViewUtils.visibility(notifications.isEmpty(), emptyText);
        }

        if (counter != null) {
            counter.setText("" + notifications.size());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void allowPopupNotifications(boolean bool) {
        isAllowedPopups = bool;
    }

    public void createPopup(GameNotification notification) {
        if (isAdded() || !isAllowedPopups) {
            return;
        }
        popupFragment.load(notification);
    }

    //--------------------------------------------------------------------------------------------//

    public void update(GameNotification notification) {
        if (isAdded()) {
            ViewHolder holder = getViewHolder(notification);

            if (holder != null) {
                holder.notifyUpdate();
            }
        } else if (popupFragment.isAdded()) {
            popupFragment.notifyUpdate();
        }
    }

    public void updateProgress(GameNotification notification) {
        if (isAdded()) {
            ViewHolder holder = getViewHolder(notification);

            if (holder != null) {
                holder.notifyProgressUpdate();
            }
        } else if (popupFragment.isAdded()) {
            popupFragment.notifyProgressUpdate();
        }
    }

    private ViewHolder getViewHolder(GameNotification notification) {
        int i = 0;
        while (i < notifications.size()) {
            ViewHolder holder = (ViewHolder) container.findViewHolderForAdapterPosition(i);

            if (holder != null && holder.notification == notification) {
                return holder;
            }
            ++i;
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------//

    public void clear() {
        ArrayList<GameNotification> toRemove = new ArrayList<>();

        for (int i = 0; i < notifications.size(); ++i) {
            GameNotification notification = notifications.get(i);

            if (notification != null && !notification.hasPriority()) {
                toRemove.add(notification);
            }
        }
        remove(toRemove.toArray(new GameNotification[0]));
    }

    public void add(GameNotification notification) {
        if (notifications.contains(notification)) {
            return;
        }

        if (notification.hasPriority()) {
            notifications.add(0, notification);
        } else {
            notifications.add(notification);
        }

        if (notification.showPopupOnNotify) {
            createPopup(notification);
        }

        Game.activity.runOnUiThread(adapter::notifyDataSetChanged);
    }

    public void remove(GameNotification... toRemove) {
        for (GameNotification notification : toRemove) {
            if (!notifications.remove(notification)) {
                return;
            }

            if (popupFragment.notification == notification) {
                popupFragment.close();
            }
            Game.activity.runOnUiThread(adapter::notifyDataSetChanged);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void show() {
        Game.platform.close(UI.getExtras());
        popupFragment.close();
        super.show();
    }

    @Override
    public void close() {
        if (!isAdded())
            return;

        Game.activity.runOnUiThread(() -> {

            new AnimationOld(Game.platform.renderView).moveX(-50, 0)
                    .play(400);
            new AnimationOld(body).moveX(0, ResourceTable.dimen(R.dimen.notificationCenterWidth))
                    .interpolator(Easing.InExpo)
                    .runOnStart(() -> new AnimationOld(rootBackground).fade(1, 0).play(300))
                    .play(350);
            new AnimationOld(layer).moveX(0, bodyWidth).interpolator(Easing.InExpo)
                    .runOnEnd(super::close)
                    .delay(50)
                    .play(400);
        });
    }

    //--------------------------------------------------------------------------------------------/

    public static class PopupFragment extends BaseFragment {

        private GameNotification notification;
        private GameNotification.Holder holder;

        //----------------------------------------------------------------------------------------//

        @Override
        protected String getPrefix() {
            return "n";
        }

        @Override
        protected int getLayout() {
            return R.layout.notification;
        }

        @Override
        protected long getCloseTime() {
            return 8000;
        }

        //----------------------------------------------------------------------------------------//

        private void load(GameNotification notification) {
            this.notification = notification;

            if (isAdded()) {
                resetDismissTimer();

                Animation.of(rootView)
                        .runOnEnd(this::bind)
                        .toY(-50)
                        .toAlpha(0)
                        .play(150);
                return;
            }
            show();
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            int xs = ResourceTable.dimen(R.dimen.XS);

            rootView.setPadding(xs, xs, xs, xs);
            rootView.setElevation(ResourceTable.dimen(R.dimen.XXL));
            bind();
        }

        private void bind() {
            holder = notification.build(this.rootView);

            LayoutParams params = (LayoutParams) holder.body.getLayoutParams();

            params.width = ResourceTable.dimen(R.dimen.popupNotificationWidth);
            params.addRule(ALIGN_PARENT_END);
            holder.body.setLayoutParams(params);

            if (holder != null) {
                unbindTouchListeners();

                bindTouchListener(holder.close, this::close);
                bindTouchListener(holder.body, UI.notificationCenter::show);

                Animation.of(rootView)
                        .fromY(-50)
                        .toY(0)
                        .fromAlpha(0)
                        .toAlpha(1)
                        .play(150);
            }
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void close() {
            if (isAdded()) {
                Animation.of(rootView)
                        .runOnEnd(super::close)
                        .toY(-50)
                        .toAlpha(0)
                        .play(150);
            }
        }

        //----------------------------------------------------------------------------------------//

        public void notifyUpdate() {
            Animation.of(holder.innerBody)
                    .toX(-50)
                    .toAlpha(0)
                    .runOnEnd(() -> {
                        bind();

                        Animation.of(holder.innerBody)
                                .fromX(50)
                                .toX(0)
                                .toAlpha(1)
                                .play(120);
                    })
                    .play(120);
        }

        public void notifyProgressUpdate() {
            if (holder.progressIndicator != null) {
                holder.progressIndicator.setProgress(notification.progress);
            }
        }
    }
}
