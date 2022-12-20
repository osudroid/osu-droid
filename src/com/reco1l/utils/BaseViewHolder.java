package com.reco1l.utils;
// Created by Reco1l on 20/12/2022, 05:07

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

    protected View root;
    protected Context context;

    //--------------------------------------------------------------------------------------------//

    public BaseViewHolder(@NonNull View root) {
        super(root);
        this.root = root;
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract void bind(T object);

    protected void onAttachmentChange(boolean isAttached) {

    }
}
