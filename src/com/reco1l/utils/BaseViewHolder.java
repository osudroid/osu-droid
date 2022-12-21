package com.reco1l.utils;
// Created by Reco1l on 20/12/2022, 05:07

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

    protected BaseAdapter<BaseViewHolder<T>, T> adapter;
    protected T object;

    protected View root;
    protected Context context;

    //--------------------------------------------------------------------------------------------//

    public BaseViewHolder(@NonNull View root) {
        super(root);
        this.root = root;
    }

    //--------------------------------------------------------------------------------------------//

    protected final void rebind() {
        onBind(object);
    }

    protected final void onBind(T object) {
        onBind(object, adapter.list.indexOf(object));
    }

    protected abstract void onBind(T object, int position);

    protected void onAttachmentChange(boolean isAttached) {

    }
}
