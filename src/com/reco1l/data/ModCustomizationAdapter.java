package com.reco1l.data;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.reco1l.Game;
import com.reco1l.data.mods.ModWrapper;
import com.reco1l.ui.Identifiers;
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
        return R.layout.mod_menu_custom_item;
    }

    @Override
    protected VH getViewHolder(View root) {
        return new VH(root);
    }

    @Override
    protected void onHolderCreated(VH holder) {
        holderCount++;

        holder.frame = new FrameLayout(Game.activity);
        holder.frame.setId(Identifiers.ModMenu_CustomizationFrames + holderCount);
        holder.body.addView(holder.frame);
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
