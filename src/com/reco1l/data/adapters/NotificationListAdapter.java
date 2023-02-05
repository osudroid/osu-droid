package com.reco1l.data.adapters;
// Created by Reco1l on 05/12/2022, 06:27

import static com.reco1l.data.Notification.*;

import android.view.View;

import androidx.annotation.NonNull;

import com.reco1l.global.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.data.Notification;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class NotificationListAdapter extends BaseAdapter<NotificationListAdapter.ViewHolder, Notification> {

    //--------------------------------------------------------------------------------------------/

    public NotificationListAdapter(ArrayList<Notification> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_notification;
    }

    @Override
    protected ViewHolder getViewHolder(View pRootView) {
        return new ViewHolder(pRootView);
    }

    //--------------------------------------------------------------------------------------------/

    public static class ViewHolder extends BaseViewHolder<Notification> {

        private Holder mHolder;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View root) {
            super(root);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(Notification n, int position) {
            mHolder = n.build(root);

            UI.notificationCenter.bindTouch(mHolder.closeButton, item::remove);
            UI.notificationCenter.bindTouch(mHolder.body, n::onClick);

            if (n.hasPriority()) {
                mHolder.closeButton.setVisibility(View.GONE);
            } else {
                mHolder.closeButton.setVisibility(View.VISIBLE);
            }
        }

        //----------------------------------------------------------------------------------------//

        public void notifyUpdate() {
            mHolder.handleUpdate();
        }
    }
}
