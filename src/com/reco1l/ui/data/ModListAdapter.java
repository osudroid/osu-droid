package com.reco1l.ui.data;
// Created by Reco1l on 20/12/2022, 05:41

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.ui.fragments.ModMenu;
import com.reco1l.utils.Animation;
import com.reco1l.utils.BaseAdapter;
import com.reco1l.utils.BaseViewHolder;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ModListAdapter extends BaseAdapter<ModListAdapter.ModHolder, ModMenu.ModWrapper> {

    //--------------------------------------------------------------------------------------------//

    public ModListAdapter(ArrayList<ModMenu.ModWrapper> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.mod_menu_item;
    }

    @Override
    protected ModHolder getViewHolder(View root) {
        return new ModHolder(root);
    }

    //--------------------------------------------------------------------------------------------//

    public static class ModHolder extends BaseViewHolder<ModMenu.ModWrapper> {

        private final CardView body;

        private final ImageView icon;
        private final TextView name;

        private ModMenu.ModWrapper modWrapper;

        //----------------------------------------------------------------------------------------//

        public ModHolder(@NonNull View root) {
            super(root);
            body = (CardView) root;
            icon = root.findViewById(R.id.mm_modIcon);
            name = root.findViewById(R.id.mm_modName);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void bind(ModMenu.ModWrapper modWrapper) {
            this.modWrapper = modWrapper;
            modWrapper.holder = this;

            if (modWrapper.gameMod != null) {
                String texture = "selection-mod-" + modWrapper.gameMod.texture;

                icon.setImageBitmap(Game.bitmapManager.get(texture));
                name.setText(modWrapper.gameMod.name().toLowerCase());
            }

            UI.modMenu.bindTouchListener(root, () -> UI.modMenu.onModSelect(modWrapper));

            if (isEnabled()) {
                body.setCardBackgroundColor(0xFF222F3D);
            }
        }

        private boolean isEnabled() {
            return UI.modMenu.enabled.contains(modWrapper);
        }

        public void setEnabledVisually(boolean bool) {
            if (bool == isEnabled()) {
                return;
            }
            int color = body.getCardBackgroundColor().getDefaultColor();

            Animation.ofColor(color, bool ? 0xFF222F3D : 0xFF242424)
                    .runOnUpdate(value -> body.setCardBackgroundColor((int) value))
                    .play(100);
        }
    }
}
