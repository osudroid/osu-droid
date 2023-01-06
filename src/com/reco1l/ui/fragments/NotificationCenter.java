package com.reco1l.ui.fragments;

import static android.widget.RelativeLayout.ALIGN_PARENT_END;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.view.View;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.edlplan.framework.easing.Easing;
import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.Game;
import com.reco1l.data.GameNotification;
import com.reco1l.UI;
import com.reco1l.data.adapters.NotificationListAdapter;
import com.reco1l.data.adapters.NotificationListAdapter.ViewHolder;
import com.reco1l.enums.Screens;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.tables.Res;
import com.reco1l.utils.Views;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 27/6/22 17:17

public final class NotificationCenter extends BaseFragment {

    public static NotificationCenter instance;

    private final NotificationListAdapter adapter;
    private final ArrayList<GameNotification> notifications;

    private final PopupFragment popupFragment;

    private View body, layer;
    private BouncyRecyclerView container;
    private TextView counter, emptyText;

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
        closeOnBackgroundClick(true);
        bodyWidth = Res.dimen(R.dimen.notificationCenterWidth);

        body = find("body");
        layer = find("layer");
        counter = find("counter");
        emptyText = find("emptyText");
        container = find("container");

        Animation.of(rootBackground)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        Game.platform.animate(true, true)
                .toX(-50)
                .play(400);

        Animation.of(layer)
                .fromX(bodyWidth)
                .toX(0)
                .interpolate(Easing.OutExpo)
                .play(350);

        Animation.of(body)
                .fromX(bodyWidth)
                .toX(0)
                .interpolate(Easing.OutExpo)
                .delay(50)
                .play(400);

        container.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, false));
        container.setAdapter(adapter);

        bindTouch(find("clear"), this::clear);
    }

    @Override
    protected void onUpdate(float sec) {
        if (emptyText != null) {
            Views.visibility(notifications.isEmpty(), emptyText);
        }

        if (counter != null) {
            counter.setText("" + notifications.size());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void createPopup(GameNotification notification) {
        if (isAdded()) {
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
        popupFragment.close();
        super.show();
    }

    @Override
    public void close() {
        if (!isAdded())
            return;

        Game.activity.runOnUiThread(() -> {

            Game.platform.animate(true, true)
                    .toX(0)
                    .play(400);

            Animation.of(body)
                    .toX(Res.dimen(R.dimen.notificationCenterWidth))
                    .interpolate(Easing.InExpo)
                    .runOnStart(() ->
                            Animation.of(rootBackground)
                                    .toAlpha(0)
                                    .play(300)
                    )
                    .play(350);

            Animation.of(layer)
                    .toX(bodyWidth)
                    .runOnEnd(super::close)
                    .interpolate(Easing.InExpo)
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

        @Override
        protected boolean getConditionToShow() {
            return Game.engine.getScreen() != Screens.Game;
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
            int xs = Res.dimen(R.dimen.XS);

            rootView.setPadding(xs, xs, xs, xs);
            rootView.setElevation(Res.dimen(R.dimen.XXL));
            bind();
        }

        private void bind() {
            holder = notification.build(this.rootView);

            LayoutParams params = (LayoutParams) holder.body.getLayoutParams();

            params.width = Res.dimen(R.dimen.popupNotificationWidth);
            params.addRule(ALIGN_PARENT_END);
            holder.body.setLayoutParams(params);

            if (holder != null) {
                unbindTouchHandlers();

                bindTouch(holder.close, this::close);
                bindTouch(holder.body, UI.notificationCenter::show);

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
