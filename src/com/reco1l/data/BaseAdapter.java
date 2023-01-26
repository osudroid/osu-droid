package com.reco1l.data;
// Created by Reco1l on 05/12/2022, 06:23

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.reco1l.tables.ResourceTable;

import java.util.ArrayList;
import java.util.Arrays;

// TODO [BaseAdapter] make compatible with multiple selection
public abstract class BaseAdapter<VH extends BaseViewHolder<T>, T> extends Adapter<VH>
        implements ResourceTable {

    private ArrayList<T> mItems;
    private SelectionListener mListener;

    private int mSelectedPosition = -1;

    private final ArrayList<VH> mHolders = new ArrayList<>();

    //--------------------------------------------------------------------------------------------//

    public BaseAdapter(ArrayList<T> pItems) {
        mItems = pItems;
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
        return mItems != null ? mItems.size() : 0;
    }

    //--------------------------------------------------------------------------------------------//

    protected int getItemLayout() {
        return 0;
    }

    protected abstract VH getViewHolder(View pRootView);

    //--------------------------------------------------------------------------------------------//

    public final void clear() {
        mItems.clear();
        mSelectedPosition = -1;
        notifyDataSetChanged();
    }

    public final void setData(ArrayList<T> pItems) {
        mItems = pItems;
        if (pItems == null) {
            mSelectedPosition = -1;
        }
        notifyDataSetChanged();
    }

    public final ArrayList<T> getData() {
        return mItems;
    }

    //--------------------------------------------------------------------------------------------//

    public final void select(T pItem) {
        for (T item : mItems) {
            if (item.equals(pItem)) {
                select(mItems.indexOf(item));
            }
        }
    }

    public final boolean select(int pPosition) {
        if (pPosition < 0 || pPosition == mSelectedPosition) {
            return false;
        }

        notifyItemChanged(mSelectedPosition);
        mSelectedPosition = pPosition;
        notifyItemChanged(mSelectedPosition);

        if (mListener != null) {
            mListener.onPositionChange(mSelectedPosition);
        }
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    public void setSelectionListener(SelectionListener pListener) {
        mListener = pListener;
    }

    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    public VH getHolderOf(T pItem) {
        for (VH holder : mHolders) {
            if (holder.item.equals(pItem)) {
                return holder;
            }
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface SelectionListener {
        void onPositionChange(int pPosition);
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public VH onCreateViewHolder(@NonNull ViewGroup pParent, int pType) {
        Context context = pParent.getContext();

        View root = null;
        if (getItemLayout() != 0) {
            root = LayoutInflater.from(context).inflate(getItemLayout(), pParent, false);
        }

        VH holder = getViewHolder(root);
        holder.context = context;
        holder.adapter = (BaseAdapter<BaseViewHolder<T>, T>) this;
        onHolderCreated(holder);
        mHolders.add(holder);
        return holder;
    }

    protected void onHolderCreated(VH pHolder) {}

    @Override
    public void onBindViewHolder(@NonNull VH pHolder, int i) {
        T item = mItems.get(i);

        pHolder.bind(item, i);

        if (mSelectedPosition == i) {
            pHolder.handleSelection();
        } else {
            pHolder.handleDeselection();
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
