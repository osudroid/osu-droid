package com.reco1l.ui.custom;

import android.graphics.PointF;
import android.widget.TextView;

import java.util.ArrayList;

public class ContextMenuBuilder {

    final PointF position;
    final ArrayList<ContextMenu.Item> items;

    //--------------------------------------------------------------------------------------------//

    public ContextMenuBuilder(PointF pPosition) {
        items = new ArrayList<>();
        position = pPosition;
    }

    //--------------------------------------------------------------------------------------------//

    public ContextMenuBuilder addItem(ContextMenu.Item item) {
        items.add(item);
        return this;
    }

    public ContextMenuBuilder addItem(String text, Runnable onClick) {
        items.add(new ContextMenu.Item() {
            @Override
            public String getText() {
                return text;
            }

            @Override
            public void onClick(TextView view) {
                onClick.run();
            }
        });
        return this;
    }
}
