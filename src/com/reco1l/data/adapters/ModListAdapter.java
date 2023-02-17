package com.reco1l.data.adapters;
// Created by Reco1l on 20/12/2022, 05:41

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.utils.Animation;
import com.reco1l.data.mods.ModWrapper;
import com.reco1l.view.RoundLayout;
import com.reco1l.view.custom.ModBadge;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ModListAdapter extends BaseAdapter<ModListAdapter.ModViewHolder, ModWrapper> {

    //--------------------------------------------------------------------------------------------//

    public ModListAdapter(ArrayList<ModWrapper> pList) {
        super(pList);
        setMarginAtBounds(sdp(12));
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_mod;
    }

    @Override
    protected ModViewHolder getViewHolder(View rootView) {
        return new ModViewHolder(rootView);
    }

    //--------------------------------------------------------------------------------------------//

    public static class ModViewHolder extends BaseViewHolder<ModWrapper> {

        private final RoundLayout mBody;
        private final TextView mName;
        private final ModBadge mIcon;

        private final ColorDrawable mBackground;

        //----------------------------------------------------------------------------------------//

        public ModViewHolder(@NonNull View root) {
            super(root);
            mBody = root.findViewById(R.id.mm_modBody);
            mIcon = root.findViewById(R.id.mm_modIcon);
            mName = root.findViewById(R.id.mm_modName);

            mBackground = new ColorDrawable(0xFF242424);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(ModWrapper wrapper, int pPosition) {
            wrapper.holder = this;

            mName.setText(wrapper.getName());
            mIcon.setText(wrapper.getAcronym());

            UI.modMenu.bindTouch(mBody, () -> UI.modMenu.onModSelect(wrapper, false));

            if (isEnabled()) {
                mBackground.setColor(0xFF222F3D);
            } else {
                mBackground.setColor(0xFF242424);
            }
            mBody.setBackground(mBackground);
        }

        private boolean isEnabled() {
            return Game.modManager.contains(item);
        }

        @Override
        public void onSelect() {
            int color = mBackground.getColor();

            Animation.ofColor(color, 0xFF222F3D)
                    .runOnUpdate(value -> {
                        mBackground.setColor((int) value);
                        mBody.invalidate();
                    })
                    .play(100);
        }

        @Override
        public void onDeselect() {
            int color = mBackground.getColor();

            Animation.ofColor(color, 0xFF242424)
                    .runOnUpdate(value -> {
                        mBackground.setColor((int) value);
                        mBody.invalidate();
                    })
                    .play(100);
        }
    }
}
