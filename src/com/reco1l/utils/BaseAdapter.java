package com.reco1l.utils;
// Created by Reco1l on 05/12/2022, 06:23

import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public abstract class BaseAdapter<VH extends ViewHolder> extends Adapter<VH> {

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
