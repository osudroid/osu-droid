package com.reco1l.data;
// Created by Reco1l on 20/12/2022, 05:07

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.management.resources.ResourceTable;

public abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder implements ResourceTable {

    boolean isAttached = false;

    protected BaseAdapter<BaseViewHolder<T>, T> adapter;
    protected T item;

    protected View root;
    protected Context context;

    private boolean wasSelected = false;

    //--------------------------------------------------------------------------------------------//

    public BaseViewHolder(@NonNull View root) {
        super(root);
        this.root = root;
    }

    //--------------------------------------------------------------------------------------------//

    public final void rebind() {
        onBind(item, getBindingAdapterPosition());
    }

    protected final void bind(T item, int position) {
        this.item = item;
        adapter.onHolderAssignment(this, position);
        onBind(item, position);
    }

    protected abstract void onBind(T item, int position);

    //--------------------------------------------------------------------------------------------//

    protected final boolean isSelected() {
        return adapter.getSelectedPosition() == getBindingAdapterPosition();
    }

    protected final boolean isAttached() {
        return isAttached;
    }

    protected int getItemCount() {
        return adapter.getItemCount();
    }

    //--------------------------------------------------------------------------------------------//

    protected final boolean select() {
        return !wasSelected && adapter.select(getBindingAdapterPosition());
    }

    final void handleSelection() {
        if (isSelected() && !wasSelected) {
            wasSelected = true;
            onSelect();
        }
    }

    final void handleDeselection() {
        if (!isSelected() && wasSelected) {
            wasSelected = false;
            onDeselect();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void onSelect() {}

    public void onDeselect() {}

    protected void onAttachmentChange(boolean isAttached) {}

    //--------------------------------------------------------------------------------------------//

    public View getView() {
        return root;
    }

    public T getItem() {
        return item;
    }

    public int getHeight() {
        return root.getHeight();
    }

    public int getWidth() {
        return root.getWidth();
    }
}
