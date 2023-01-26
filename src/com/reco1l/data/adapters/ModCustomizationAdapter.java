package com.reco1l.data.adapters;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.ui.Identifiers;
import com.reco1l.data.mods.ModWrapper;
import ru.nsu.ccfit.zuev.osuplus.R;

import java.util.ArrayList;

public class ModCustomizationAdapter extends BaseAdapter<ModCustomizationAdapter.VH, ModWrapper> {

    private int holderCount;

    //----------------------------------------------------------------------------------------//

    public ModCustomizationAdapter(ArrayList<ModWrapper> list) {
        super(list);
    }

    //----------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.item_mod_customization;
    }

    @Override
    protected VH getViewHolder(View pRootView) {
        return new VH(pRootView);
    }

    @Override
    protected void onHolderCreated(VH pHolder) {
        holderCount++;

        pHolder.frame = new FrameLayout(Game.activity);
        pHolder.frame.setId(Identifiers.ModMenu_CustomizationFrames + holderCount);
        pHolder.body.addView(pHolder.frame);
    }

    //----------------------------------------------------------------------------------------//

    public static class VH extends BaseViewHolder<ModWrapper> {

        private final TextView name;
        private final LinearLayout body;

        private FrameLayout frame;

        //------------------------------------------------------------------------------------//

        public VH(@NonNull View root) {
            super(root);
            name = root.findViewById(R.id.mm_customName);
            body = root.findViewById(R.id.mm_customBody);
        }

        //------------------------------------------------------------------------------------//

        @Override
        protected void onBind(ModWrapper mod, int position) {
            name.setText(mod.getName());

            ModWrapper.Properties fragment = mod.getProperties();
            fragment.replace(frame);
         }
    }
}
