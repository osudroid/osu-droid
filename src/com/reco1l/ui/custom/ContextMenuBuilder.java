package com.reco1l.ui.custom;

import android.graphics.PointF;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class ContextMenuBuilder {

    final PointF position;
    final ArrayList<ContextMenu.Item> items;

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

    //--------------------------------------------------------------------------------------------//

    public ContextMenuBuilder setFixedWidth(int width) {
        if (width > 0) {
            fixedWidth = width;
        }
        return this;
    }
}
