package com.reco1l.ui.fragments;
/*
 * Written by Reco1l on 18/6/22 03:03
 */

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.reco1l.Game;
import com.reco1l.ui.platform.BaseFragment;
import com.reco1l.view.TextButton;

import ru.nsu.ccfit.zuev.osuplus.R;

public class FilterMenu extends BaseFragment {

    public static FilterMenu instance;

    private TextButton sort;
    private EditText field;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "fm";
    }

    @Override
    protected int getLayout() {
        return R.layout.filter_menu;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        sort = find("sort");
        field = find("search");

        field.addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            public void afterTextChanged(Editable editable) {
                Game.beatmapCollection.setFilter(editable.toString());
            }
        });

        bindTouchListener(sort, Game.beatmapCollection::nextOrder);
    }

    @Override
    protected void onUpdate(float sec) {
        if (!isLoaded()) {
            return;
        }
        sort.setButtonText(Game.beatmapCollection.getOrder().name());
    }
}
