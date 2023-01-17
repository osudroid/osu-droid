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
import com.reco1l.management.ModManager;
import com.reco1l.utils.Animation;
import com.reco1l.data.mods.ModWrapper;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ModListAdapter extends BaseAdapter<ModListAdapter.ModViewHolder, ModWrapper> {

    //--------------------------------------------------------------------------------------------//

    public ModListAdapter(ArrayList<ModWrapper> pList) {
        super(pList);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_mod;
    }

    @Override
    protected ModViewHolder getViewHolder(View pRootView) {
        return new ModViewHolder(pRootView);
    }

    //--------------------------------------------------------------------------------------------//

    public static class ModViewHolder extends BaseViewHolder<ModWrapper> {

        private final CardView mBody;
        private final TextView mName;
        private final ImageView mIcon;

        //----------------------------------------------------------------------------------------//

        public ModViewHolder(@NonNull View root) {
            super(root);
            mBody = (CardView) root;
            mIcon = root.findViewById(R.id.mm_modIcon);
            mName = root.findViewById(R.id.mm_modName);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(ModWrapper pModWrapper, int pPosition) {
            pModWrapper.holder = this;

            mIcon.setImageBitmap(Game.bitmapManager.get(pModWrapper.getIcon()));
            mName.setText(pModWrapper.getName());

            UI.modMenu.bindTouch(root, () -> UI.modMenu.onModSelect(pModWrapper, false));

            if (isEnabled()) {
                mBody.setCardBackgroundColor(0xFF222F3D);
            }
        }

        private boolean isEnabled() {
            return ModManager.modList.contains(item);
        }

        @Override
        public void onSelect() {
            int color = mBody.getCardBackgroundColor().getDefaultColor();

            Animation.ofColor(color, 0xFF222F3D)
                    .runOnUpdate(value -> mBody.setCardBackgroundColor((int) value))
                    .play(100);
        }

        @Override
        public void onDeselect() {
            int color = mBody.getCardBackgroundColor().getDefaultColor();

            Animation.ofColor(color, 0xFF242424)
                    .runOnUpdate(value -> mBody.setCardBackgroundColor((int) value))
                    .play(100);
        }
    }
}
