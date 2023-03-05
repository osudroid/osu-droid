package com.reco1l.ui.fragments;

import static android.widget.RelativeLayout.ALIGN_PARENT_END;
import static com.reco1l.data.adapters.NotificationListAdapter.ViewHolder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.edlplan.framework.easing.Easing;
import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.ui.custom.Notification;
import com.reco1l.data.adapters.NotificationListAdapter;
import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 27/6/22 17:17

public final class NotificationCenter extends BaseFragment {

    public static final NotificationCenter instance = new NotificationCenter();

    private final PopupFragment mPopupFragment;
    private final NotificationListAdapter mAdapter;

    private final ArrayList<Notification> mNotifications;
    private final Map<String, Notification> mNotificationMap;

    private View
            mBody,
            mLayer;

    private TextView
            mEmptyText,
            mCounterText;

    private BouncyRecyclerView mListView;

    //--------------------------------------------------------------------------------------------//

    public NotificationCenter() {
        super();
        mNotifications = new ArrayList<>();
        mNotificationMap = new HashMap<>();
        mPopupFragment = new PopupFragment();
        mAdapter = new NotificationListAdapter(mNotifications);

        closeOnBackgroundClick(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "nc";
    }

    @Override
    protected int getLayout() {
        return R.layout.extra_notification_center;
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    @Override
    public int getWidth() {
        return dimen(R.dimen.notificationCenterWidth);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {

        mBody = find("body");
        mLayer = find("layer");
        mListView = find("container");
        mCounterText = find("counter");
        mEmptyText = find("emptyText");

        Game.platform.animate(true, true)
                .toX(-50)
                .play(400);

        Animation.of(rootBackground)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        Animation.of(mLayer)
                .fromX(getWidth())
                .toX(0)
                .interpolate(Easing.OutExpo)
                .play(350);

        Animation.of(mBody)
                .fromX(getWidth())
                .toX(0)
                .interpolate(Easing.OutExpo)
                .delay(50)
                .play(400);

        mListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListView.setAdapter(mAdapter);

        bindTouch(find("clear"), this::clear);
    }

    @Override
    protected void onEngineUpdate(float pSecElapsed) {
        if (mEmptyText != null) {
            Views.visibility(mNotifications.isEmpty(), mEmptyText);
        }

        if (mCounterText != null) {
            mCounterText.setText("" + mNotifications.size());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void onNotificationChange(Notification n) {
        if (!mNotifications.contains(n)) {
            return;
        }

        if (mPopupFragment != null) {
            if (mPopupFragment.mNotification == n) {
                Game.activity.runOnUiThread(() ->
                        mPopupFragment.bind(false)
                );
            }
        }

        if (mAdapter != null) {
            ViewHolder holder = mAdapter.getHolderOf(n);

            if (holder != null) {
                Game.activity.runOnUiThread(holder::rebind);
            }
        }
    }

    public void clear() {
        for (int i = 0; i < mNotifications.size(); ++i) {
            Notification n = mNotifications.get(i);

            if (n != null && !n.hasPriority()) {
                remove(n);
            }
        }
    }

    public void add(Notification n) {
        if (mNotifications.contains(n)) {
            return;
        }

        if (n.hasPriority()) {
            mNotifications.add(0, n);
        } else {
            mNotifications.add(n);
        }
        mNotificationMap.put(n.getKey(), n);

        if (!n.isSilent() && !isAdded()) {
            mPopupFragment.show(n);
        }

        Game.activity.runOnUiThread(mAdapter::notifyDataSetChanged);
        n.onNotify();
    }

    public void remove(Notification n) {
        if (!mNotifications.remove(n)) {
            return;
        }
        mNotificationMap.remove(n.getKey());

        if (mPopupFragment.mNotification == n) {
            mPopupFragment.close();
        }
        Game.activity.runOnUiThread(mAdapter::notifyDataSetChanged);
        n.onDismiss();
    }

    public boolean contains(Notification n) {
        return mNotifications.contains(n);
    }

    public Notification get(String pKey) {
        for (String key : mNotificationMap.keySet()) {
            if (key.equals(pKey)) {
                return mNotificationMap.get(key);
            }
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean show() {
        mPopupFragment.close();
        return super.show();
    }

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }
        unbindTouchHandlers();

        Game.platform.animate(true, true)
                .toX(0)
                .play(400);

        Animation.of(mBody)
                .toX(dimen(R.dimen.notificationCenterWidth))
                .interpolate(Easing.InExpo)
                .runOnStart(() ->
                        Animation.of(rootBackground)
                                .toAlpha(0)
                                .play(300)
                )
                .play(350);

        Animation.of(mLayer)
                .toX(dimen(R.dimen.notificationCenterWidth))
                .runOnEnd(super::close)
                .interpolate(Easing.InExpo)
                .delay(50)
                .play(400);
    }

    //--------------------------------------------------------------------------------------------/

    public static class PopupFragment extends BaseFragment {

        private Notification mNotification;

        //----------------------------------------------------------------------------------------//

        @Override
        protected String getPrefix() {
            return "n";
        }

        @Override
        protected int getLayout() {
            return R.layout.item_notification;
        }

        @Override
        protected long getCloseTime() {
            return 8000;
        }

        @Override
        protected boolean getConditionToShow() {
            return Game.engine.getScene() != Scenes.player;
        }

        @Override
        protected boolean isOverlay() {
            return true;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            View body = find("body");
            View close = find("close");

            Views.width(body, sdp(240));
            Views.rule(body, ALIGN_PARENT_END);
            Views.padding(rootView).all(sdp(4));
            Views.margins(rootView).top(UI.topBar.getHeight());

            bindTouch(close, this::close);
            bindTouch(body, UI.notificationCenter::show);
            bind(true);
        }

        private void bind(boolean animated) {
            mNotification.bind(rootView);

            if (animated) {
                Animation.of(rootView)
                        .toY(0)
                        .toAlpha(1)
                        .play(150);
            }
        }

        //----------------------------------------------------------------------------------------//

        public boolean show(Notification notification) {
            mNotification = notification;
            return super.show();
        }

        @Override
        protected void onShowAttempt() {
            resetCloseTimer();

            Animation.of(rootView)
                    .runOnEnd(() -> bind(true))
                    .toY(-50)
                    .toAlpha(0)
                    .play(150);
        }

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
    }
}
