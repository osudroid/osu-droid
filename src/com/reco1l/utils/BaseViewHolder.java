package com.reco1l.utils;
// Created by Reco1l on 20/12/2022, 05:07

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

    protected BaseAdapter<BaseViewHolder<T>, T> adapter;
    protected T item;

    protected View root;
    protected Context context;

    boolean isAttached = false;

    private boolean isSelected = false;

    //--------------------------------------------------------------------------------------------//

    public BaseViewHolder(@NonNull View root) {
        super(root);
        this.root = root;
    }

    //--------------------------------------------------------------------------------------------//

    protected final void bind(T item, int position) {
        this.item = item;
        onBind(item, position);
    }

    protected final void rebind() {
        onBind(item, getAdapterPosition());
    }

    protected abstract void onBind(T item, int position);

    //--------------------------------------------------------------------------------------------//

    protected final boolean isSelected() {
        return item == adapter.getSelectedItem();
    }

    protected final boolean isAttached() {
        return isAttached;
    }

    //--------------------------------------------------------------------------------------------//

    final void invalidateSelection() {
        if (isSelected) {
            onDeselectVisually();
        }
        isSelected = false;
    }

    protected final void deselect() {
        if (isSelected) {
            isSelected = false;
            onDeselectVisually();
        }
    }

    protected final boolean select() {
        if (!isSelected) {
            isSelected = true;
            onSelectVisually();
            adapter.handleSelection(this);
            return true;
        }
        Log.e(getClass().getSimpleName(), "Already selected!");
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onSelectVisually() {}

    protected void onDeselectVisually() {}

    protected void onAttachmentChange(boolean isAttached) {}
}
