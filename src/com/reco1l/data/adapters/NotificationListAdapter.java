package com.reco1l.data.adapters;
// Created by Reco1l on 05/12/2022, 06:27

import android.view.View;

import androidx.annotation.NonNull;

import com.reco1l.ui.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.ui.custom.Notification;

import java.util.ArrayList;

import com.rimu.R;

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
    protected ViewHolder getViewHolder(View rootView) {
        return new ViewHolder(rootView);
    }

    //--------------------------------------------------------------------------------------------/

    public static class ViewHolder extends BaseViewHolder<Notification> {

        private final View
                mBody,
                mCloseButton;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View root) {
            super(root);

            mBody = root.findViewById(R.id.n_body);
            mCloseButton = root.findViewById(R.id.n_close);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(Notification n, int position) {
            UI.notificationCenter.bindTouch(mCloseButton, item::remove);
            UI.notificationCenter.bindTouch(mBody, n::onClick);

            mCloseButton.setVisibility(n.hasPriority() ? View.GONE : View.VISIBLE);

            n.bind(root);
        }
    }
}
