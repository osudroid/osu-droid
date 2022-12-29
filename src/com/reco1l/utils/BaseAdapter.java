package com.reco1l.utils;
// Created by Reco1l on 05/12/2022, 06:23

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class BaseAdapter<VH extends BaseViewHolder<T>, T> extends Adapter<VH> {

    public ArrayList<T> items;

    protected final ArrayList<VH> holders;
    protected RecyclerView recyclerView;

    private ItemComparator<T> comparator;
    private T selectedItem;

    //--------------------------------------------------------------------------------------------//

    public BaseAdapter(ArrayList<T> items) {
        this.items = items;
        holders = new ArrayList<>();
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

    protected abstract int getLayout();

    protected abstract VH getViewHolder(View root);

    protected void onItemSelect(VH holder, T item) {}

    //--------------------------------------------------------------------------------------------//

    public final void setData(ArrayList<T> newList) {
        items = newList;
        notifyDataSetChanged();
    }

    public final void setItemComparator(ItemComparator<T> comparator) {
        this.comparator = comparator;
    }

    //--------------------------------------------------------------------------------------------//

    public final void select(T item) {
        selectedItem = item;

        for (VH holder : holders) {
            if (compareItems(holder.item, item)) {
                holder.select();
            }
        }
    }

    protected final void handleSelection(VH holder) {
        for (VH vh : holders) {
            if (vh != holder) {
                vh.deselect();
            }
        }

        onItemSelect(holder, holder.item);
    }

    private boolean compareItems(T i1, T i2) {
        if (i1 == null || i2 == null) {
            return false;
        }
        if (comparator != null) {
            return comparator.compare(i1, i2);
        }
        return i1 == i2;
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface ItemComparator<T> {
        boolean compare(T i1, T i2);
    }

    //--------------------------------------------------------------------------------------------//

    public T getSelectedItem() {
        return selectedItem;
    }

    public VH getSelectedHolder() {
        return getHolderOf(selectedItem);
    }

    public VH getHolderOf(T item) {
        for (VH holder : holders) {
            if (compareItems(holder.item, item)) {
                return holder;
            }
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        View root = LayoutInflater.from(context).inflate(getLayout(), parent, false);

        VH holder = getViewHolder(root);
        holder.context = context;
        holder.adapter = (BaseAdapter<BaseViewHolder<T>, T>) this;

        holders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int i) {
        T item = items.get(i);

        holder.bind(item, i);
        holder.invalidateSelection();

        if (compareItems(item, selectedItem)) {
            holder.select();
        } else {
            holder.deselect();
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

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        select(selectedItem);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = null;
    }
}
