package com.reco1l.ui.custom;

import android.graphics.PointF;
import android.view.View;
import android.widget.TextView;

import com.reco1l.ui.custom.ContextMenu.Item;

import java.util.ArrayList;

public class ContextMenuBuilder {

    final PointF position;
    final ArrayList<Item> items;

    int fixedWidth = -1;

    //--------------------------------------------------------------------------------------------//

    public ContextMenuBuilder(PointF pPosition) {
        items = new ArrayList<>();
        position = pPosition;
    }

    public ContextMenuBuilder(View view) {
        items = new ArrayList<>();

        int[] location = new int[2];
        view.getLocationInWindow(location);

        position = new PointF(location[0], location[1]);
    }

    //--------------------------------------------------------------------------------------------//

    public ContextMenuBuilder addItem(Item item) {
        items.add(item);
        return this;
    }

    public ContextMenuBuilder addItem(String text, Runnable onClick) {
        items.add(new Item() {

            public String getText() {
                return text;
            }

            public void onClick(TextView view) {
                onClick.run();
            }

        });
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public ContextMenuBuilder setFixedWidth(int width) {
        if (width > 0) {
            fixedWidth = width;
        }
        return this;
    }
}
