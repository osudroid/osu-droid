package com.reco1l.utils;
// Created by Reco1l on 05/12/2022, 06:23

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import java.util.ArrayList;

public abstract class BaseAdapter<VH extends BaseViewHolder<T>, T> extends Adapter<VH> {

    public ArrayList<T> list;

    //--------------------------------------------------------------------------------------------//

    public BaseAdapter(ArrayList<T> list) {
        this.list = list;
        setHasStableIds(true);
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
        return list != null ? list.size() : 0;
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract int getLayout();
    
    protected abstract VH getViewHolder(View root);

    //--------------------------------------------------------------------------------------------//

    @Override @NonNull
    @SuppressWarnings("unchecked")
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        
        View root = LayoutInflater.from(context).inflate(getLayout(), parent, false);

        VH holder = getViewHolder(root);
        holder.context = context;
        holder.adapter = (BaseAdapter<BaseViewHolder<T>, T>) this;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.object = list.get(position);
        holder.onBind(list.get(position), position);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onViewAttachedToWindow(@NonNull VH holder) {
        holder.onAttachmentChange(true);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VH holder) {
        holder.onAttachmentChange(false);
    }
}
