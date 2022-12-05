package com.reco1l.ui.data;
// Created by Reco1l on 05/12/2022, 06:27

import static com.reco1l.ui.data.NotificationListAdapter.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.UI;
import com.reco1l.utils.Animation;
import com.reco1l.utils.BaseAdapter;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class NotificationListAdapter extends BaseAdapter<ViewHolder> {

    private final ArrayList<GameNotification> notifications;

    //--------------------------------------------------------------------------------------------/

    public NotificationListAdapter(ArrayList<GameNotification> notifications) {
        this.notifications = notifications;
    }

    //--------------------------------------------------------------------------------------------/

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    //--------------------------------------------------------------------------------------------/

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public View rootView;
        public GameNotification notification;

        private GameNotification.Holder holder;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View rootView) {
            super(rootView);
            this.rootView = rootView;
        }

        //----------------------------------------------------------------------------------------//

        private void bind(GameNotification notification) {
            this.notification = notification;
            this.holder = notification.build(rootView);

            UI.notificationCenter.bindTouchListener(holder.close, this::onDismiss);
            UI.notificationCenter.bindTouchListener(holder.body, notification.runOnClick);

            if (notification.hasPriority()) {
                holder.close.setVisibility(View.GONE);
            } else {
                holder.close.setVisibility(View.VISIBLE);
            }
        }

        //----------------------------------------------------------------------------------------//

        private void onDismiss() {
            if (notification.onDismiss != null) {
                notification.onDismiss.run();
            }
            UI.notificationCenter.remove(notification);
        }

        public void notifyUpdate() {
            Animation.of(holder.innerBody)
                    .toX(-50)
                    .toAlpha(0)
                    .runOnEnd(() -> {
                        bind(notification);

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
