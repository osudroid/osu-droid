package com.reco1l.data.adapters;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.reco1l.data.adapters.ContextMenuAdapter.*;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchHandler;

import java.util.ArrayList;

import com.rimu.R;

public class ContextMenuAdapter extends BaseAdapter<ItemHolder, ContextMenu.Item> {

    public ContextMenuAdapter(ArrayList<ContextMenu.Item> items) {
        super(items);
    }

    @Override
    protected ItemHolder getViewHolder(View rootView) {
        TextView textView = new TextView(new ContextThemeWrapper(Game.activity, R.style.text));

        Drawable drawable = new ColorDrawable(color(R.color.accent));
        drawable.setAlpha(0);

        textView.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        textView.setBackground(drawable);
        textView.setSingleLine(true);

        int m = dimen(R.dimen.M);
        int s = dimen(R.dimen.S);
        textView.setPadding(m, s, m, s);

        return new ItemHolder(textView);
    }

    public static class ItemHolder extends BaseViewHolder<ContextMenu.Item> {

        public ItemHolder(@NonNull View root) {
            super(root);
        }

        @Override
        protected void onBind(ContextMenu.Item item, int position) {
            ((TextView) root).setText(item.getText());

            new TouchHandler(() ->
                    item.onClick((TextView) root)
            ).apply(root);
        }

        @Override
        public void onSelect() {
            Drawable background = root.getBackground();

            Animation.ofInt(background.getAlpha(), 60)
                    .runOnUpdate(value -> {
                        background.setAlpha((int) value);
                        root.setBackground(background);
                    })
                    .play(200);

            Animation.ofColor(Color.WHITE, color(R.color.accent))
                    .runOnUpdate(value -> ((TextView) root).setTextColor((int) value))
                    .play(200);
        }

        @Override
        public void onDeselect() {
            Drawable background = root.getBackground();

            Animation.ofInt(background.getAlpha(), 0)
                    .runOnUpdate(value -> {
                        background.setAlpha((int) value);
                        root.setBackground(background);
                    })
                    .play(200);

            Animation.ofColor(color(R.color.accent), Color.WHITE)
                    .runOnUpdate(value -> ((TextView) root).setTextColor((int) value))
                    .play(200);
        }
    }
}
