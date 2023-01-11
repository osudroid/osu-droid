package com.reco1l.data;
// Created by Reco1l on 05/12/2022, 06:23

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import java.util.ArrayList;
import java.util.Arrays;

// TODO [BaseAdapter] make compatible with multiple selection
public abstract class BaseAdapter<VH extends BaseViewHolder<T>, T> extends Adapter<VH> {

    public ArrayList<T> items;

    private SelectionListener listener;

    int selectedPosition = -1;

    //--------------------------------------------------------------------------------------------//

    public BaseAdapter(ArrayList<T> items) {
        this.items = items;
        setHasStableIds(true);
    }

    public BaseAdapter(T[] array) {
        this(new ArrayList<>(Arrays.asList(array)));
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    //--------------------------------------------------------------------------------------------//

    protected int getItemLayout() {
        return 0;
    }

    protected abstract VH getViewHolder(View root);

    //--------------------------------------------------------------------------------------------//

    public final void clear() {
        items.clear();
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    public final void setData(ArrayList<T> newList) {
        items = newList;
        notifyDataSetChanged();
    }

    public final ArrayList<T> getData() {
        return items;
    }

    public final void select(T item) {
        for (T i : items) {
            if (i.equals(item)) {
                select(items.indexOf(i));
            }
        }
    }

    public final boolean select(int position) {
        if (position < 0 || position == selectedPosition) {
            return false;
        }

        notifyItemChanged(selectedPosition);
        selectedPosition = position;
        notifyItemChanged(selectedPosition);

        if (listener != null) {
            listener.onPositionChange(selectedPosition);
        }
        return true;
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface SelectionListener {
        void onPositionChange(int position);
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        View root = null;
        if (getItemLayout() != 0) {
            root = LayoutInflater.from(context).inflate(getItemLayout(), parent, false);
        }

        VH holder = getViewHolder(root);
        holder.context = context;
        holder.adapter = (BaseAdapter<BaseViewHolder<T>, T>) this;
        onHolderCreated(holder);
        return holder;
    }

    protected void onHolderCreated(VH holder) {}

    @Override
    public void onBindViewHolder(@NonNull VH holder, int i) {
        T item = items.get(i);

        holder.bind(item, i);

        if (selectedPosition == i) {
            holder.handleSelection();
        } else {
            holder.handleDeselection();
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onViewAttachedToWindow(@NonNull VH holder) {
        holder.isAttached = true;
        holder.onAttachmentChange(true);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VH holder) {
        holder.isAttached = false;
        holder.onAttachmentChange(false);
    }
}
