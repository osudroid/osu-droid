package com.reco1l.data.adapters;
// Created by Reco1l on 20/12/2022, 05:41

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.data.mods.ModWrapper;
import com.reco1l.utils.Animation;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ModListAdapter extends BaseAdapter<ModListAdapter.ModViewHolder, ModWrapper> {

    //--------------------------------------------------------------------------------------------//

    public ModListAdapter(ArrayList<ModWrapper> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.mod_menu_item;
    }

    @Override
    protected ModViewHolder getViewHolder(View root) {
        return new ModViewHolder(root);
    }

    //--------------------------------------------------------------------------------------------//

    public static class ModViewHolder extends BaseViewHolder<ModWrapper> {

        private final CardView body;

        private final ImageView icon;
        private final TextView name;

        //----------------------------------------------------------------------------------------//

        public ModViewHolder(@NonNull View root) {
            super(root);
            body = (CardView) root;
            icon = root.findViewById(R.id.mm_modIcon);
            name = root.findViewById(R.id.mm_modName);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(ModWrapper modWrapper, int position) {
            modWrapper.holder = this;

            icon.setImageBitmap(Game.bitmapManager.get(modWrapper.getIcon()));
            name.setText(modWrapper.getName());

            UI.modMenu.bindTouch(root, () -> UI.modMenu.onModSelect(modWrapper, false));

            if (isEnabled()) {
                body.setCardBackgroundColor(0xFF222F3D);
            }
        }

        private boolean isEnabled() {
            return UI.modMenu.enabled.contains(item);
        }

        public void onModSelect(boolean isEnabled) {
            int color = body.getCardBackgroundColor().getDefaultColor();

            Animation.ofColor(color, isEnabled ? 0xFF222F3D : 0xFF242424)
                    .runOnUpdate(value -> body.setCardBackgroundColor((int) value))
                    .play(100);
        }
    }
}
