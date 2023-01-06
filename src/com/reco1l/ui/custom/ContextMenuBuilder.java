package com.reco1l.ui.custom;

import java.util.ArrayList;

public class ContextMenuBuilder {

    final ArrayList<ContextMenu.Item> items;

    public ContextMenuBuilder() {
        this.items = new ArrayList<>();
    }

    public ContextMenuBuilder addItem(ContextMenu.Item item) {
        items.add(item);
        return this;
    }
}
