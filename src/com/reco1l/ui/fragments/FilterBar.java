package com.reco1l.ui.fragments;
/*
 * Written by Reco1l on 18/6/22 03:03
 */

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.BaseFragment;
import com.reco1l.view.IconButton;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class FilterBar extends BaseFragment {

    public static FilterBar instance;

    private IconButton sort;
    private EditText field;

    //--------------------------------------------------------------------------------------------//

    public FilterBar() {
        super(Screens.Selector);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "fm";
    }

    @Override
    protected int getLayout() {
        return R.layout.filter_bar;
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

        bindTouch(sort, Game.beatmapCollection::nextOrder);
    }

    @Override
    protected void onUpdate(float sec) {
        if (!isLoaded()) {
            return;
        }
    }

    //--------------------------------------------------------------------------------------------//

}
