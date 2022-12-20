package com.reco1l.ui.data;
// Created by Reco1l on 20/12/2022, 05:41

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.ui.fragments.ModMenu;
import com.reco1l.utils.BaseAdapter;
import com.reco1l.utils.BaseViewHolder;
import com.reco1l.view.PanelLayout;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ModSectionAdapter extends BaseAdapter<ModSectionAdapter.SectionHolder, ModMenu.Section> {

    //--------------------------------------------------------------------------------------------//

    public ModSectionAdapter(ArrayList<ModMenu.Section> list) {
        super(list);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.mod_menu_section_item;
    }

    @Override
    protected SectionHolder getViewHolder(View root) {
        return new SectionHolder(root);
    }

    //--------------------------------------------------------------------------------------------//

    public static class SectionHolder extends BaseViewHolder<ModMenu.Section> {

        private final PanelLayout panel;

        public RecyclerView recyclerView;
        public ModListAdapter adapter;

        //----------------------------------------------------------------------------------------//

        public SectionHolder(@NonNull View root) {
            super(root);
            panel = (PanelLayout) root;
            recyclerView = root.findViewById(R.id.mm_modContainer);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void bind(ModMenu.Section section) {
            panel.setTitle(section.title);
            adapter = new ModListAdapter(section.modWrappers);
            recyclerView.setLayoutManager(new LinearLayoutManager(context, VERTICAL, false));
            recyclerView.setAdapter(adapter);
        }
    }
}
