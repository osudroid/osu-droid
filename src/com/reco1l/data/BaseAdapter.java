package com.reco1l.data;
// Created by Reco1l on 05/12/2022, 06:23

import static androidx.recyclerview.widget.RecyclerView.*;

import static com.reco1l.utils.Views.*;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.reco1l.tables.ResourceTable;
import com.reco1l.utils.Views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

// TODO [BaseAdapter] make compatible with multiple selection
public abstract class BaseAdapter<VH extends BaseViewHolder<T>, T> extends Adapter<VH>
        implements ResourceTable {

    private final ArrayList<VH> mHolders = new ArrayList<>();

    private ArrayList<T> mItems;
    private RecyclerView mRecyclerView;
    private SelectionListener mListener;
    private LayoutManager mLayoutManager;

    private int
            mOrientation = -1,
            mMarginAtBounds = 0,
            mSelectedPosition = -1;

    private T mLastSelected;

    //--------------------------------------------------------------------------------------------//

    public BaseAdapter(ArrayList<T> items) {
        mItems = items;
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

    // Define layout XML resource to be inflated
    protected @LayoutRes int getItemLayout() {
        return 0;
    }

    // Return a new instance of your custom view holder class, this can be a replacement for
    // getItemLayout() if you want to programmatically create a view instead of inflating a layout
    protected abstract VH getViewHolder(View pRootView);

    protected void onHolderCreated(VH pHolder) {}

    // Called after the item was assigned to the holder and before of calling onBind()
    protected void onHolderAssignment(VH holder, int position) {}

    //--------------------------------------------------------------------------------------------//

    public final void clear() {
        mItems.clear();
        mSelectedPosition = -1;
        notifyDataSetChanged();
    }

    public final void setData(ArrayList<T> pItems) {
        mSelectedPosition = -1;

        if (pItems != null) {
            deselectAll();

            pItems.forEach(item -> {
                if (Objects.equals(item, mLastSelected)) {
                    mSelectedPosition = pItems.indexOf(item);
                }
            });
        }
        mItems = pItems;
        notifyDataSetChanged();
    }

    public final ArrayList<T> getData() {
        return mItems;
    }

    //--------------------------------------------------------------------------------------------//

    public final void select(T pItem) {
        mLastSelected = pItem;
        select(mItems.indexOf(pItem));
    }

    public final boolean select(int pPosition) {
        if (pPosition < 0 || pPosition == mSelectedPosition) {
            return false;
        }
        mLastSelected = mItems.get(pPosition);

        notifyItemChanged(mSelectedPosition);
        mSelectedPosition = pPosition;
        notifyItemChanged(mSelectedPosition);

        if (mListener != null) {
            mListener.onPositionChange(mSelectedPosition);
        }
        return true;
    }

    public final void deselectAll() {
        mSelectedPosition = -1;
        mHolders.forEach(BaseViewHolder::handleDeselection);
    }

    //--------------------------------------------------------------------------------------------//

    public final void setMarginAtBounds(int margin) {
        mMarginAtBounds = margin;
    }

    public final void setSelectionListener(SelectionListener pListener) {
        mListener = pListener;
    }

    public final void forEachHolder(Consumer<VH> action) {
        mHolders.forEach(action);
    }

    //--------------------------------------------------------------------------------------------//

    public final int getSelectedPosition() {
        return mSelectedPosition;
    }

    public final ArrayList<VH> getHolders() {
        return mHolders;
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
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int pType) {
        Context context = parent.getContext();

        View root = null;
        if (getItemLayout() != 0) {
            root = LayoutInflater.from(context).inflate(getItemLayout(), parent, false);
        }

        VH holder = getViewHolder(root);
        holder.context = context;
        holder.adapter = (BaseAdapter<BaseViewHolder<T>, T>) this;

        mHolders.add(holder);
        onHolderCreated(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int i) {
        T item = mItems.get(i);

        holder.bind(item, i);

        if (mSelectedPosition == i) {
            holder.handleSelection();
        } else {
            holder.handleDeselection();
        }

        if (mMarginAtBounds > 0 && mOrientation != -1) {
            MarginUtils m = margins(holder.root);

            if (mOrientation == VERTICAL) {
                m.top(i == 0 ? mMarginAtBounds : 0);
                m.bottom(i == getItemCount() - 1 ? mMarginAtBounds : 0);
            } else {
                m.left(i == 0 ? mMarginAtBounds : 0);
                m.right(i == getItemCount() - 1 ? mMarginAtBounds : 0);
            }
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

    //--------------------------------------------------------------------------------------------//


    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mLayoutManager = mRecyclerView.getLayoutManager();

        if (mLayoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linear = (LinearLayoutManager) mLayoutManager;
            mOrientation = linear.getOrientation();
        } else {
            mOrientation = -1;
        }
    }
}
