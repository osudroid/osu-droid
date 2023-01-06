package com.reco1l.data.adapters;
// Created by Reco1l on 20/12/2022, 05:41

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.ui.fragments.ModMenu;
import com.reco1l.tables.Res;
import com.reco1l.utils.Views;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ModSectionAdapter extends BaseAdapter<ModSectionAdapter.SectionHolder, ModMenu.Section> {

    //--------------------------------------------------------------------------------------------//

    public ModSectionAdapter(ModMenu.Section[] array) {
        super(array);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getItemLayout() {
        return R.layout.mod_menu_section_item;
    }

    @Override
    protected SectionHolder getViewHolder(View root) {
        return new SectionHolder(root);
    }

    //--------------------------------------------------------------------------------------------//

    public static class SectionHolder extends BaseViewHolder<ModMenu.Section> {

        private final TextView header;
        private final RecyclerView recyclerView;

        //----------------------------------------------------------------------------------------//

        public SectionHolder(@NonNull View root) {
            super(root);
            header = root.findViewById(R.id.mm_sectionName);
            recyclerView = root.findViewById(R.id.mm_modContainer);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(ModMenu.Section section, int position) {
            if (position == 0) {
                Views.margins(root).left(Res.sdp(12));
            }
            header.setText(section.title);
            recyclerView.setAdapter(new ModListAdapter(section.modWrappers));
        }
    }
}
