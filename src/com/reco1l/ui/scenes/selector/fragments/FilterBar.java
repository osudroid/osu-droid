package com.reco1l.ui.scenes.selector.fragments;
/*
 * Written by Reco1l on 18/6/22 03:03
 */

import android.graphics.PointF;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.reco1l.Game;
import com.reco1l.management.BeatmapCollection.SortOrder;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.custom.ContextMenu;
import com.reco1l.ui.custom.ContextMenuBuilder;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.execution.ScheduledTask;
import com.reco1l.view.IconButtonView;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class FilterBar extends BaseFragment {

    public static final FilterBar instance = new FilterBar();

    private IconButtonView mSortButton;
    private EditText mSearchField;

    private final TextWatcher mTextWatcher;
    private final ScheduledTask mUpdateTask;

    private Editable mEditable;

    //--------------------------------------------------------------------------------------------//

    public FilterBar() {
        super();

        mUpdateTask = new ScheduledTask() {
            protected void run() {
                if (mEditable == null) {
                    return;
                }
                Game.beatmapCollection.setFilter(mEditable.toString());
            }
        };

        mTextWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mUpdateTask.cancel();
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            public void afterTextChanged(Editable editable) {
                mEditable = editable;
                mUpdateTask.execute(250);
            }
        };
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "fm";
    }

    @Override
    protected int getLayout() {
        return R.layout.selector_filter_bar;
    }

    @Override
    protected boolean isExtra() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mSortButton = find("sort");
        mSearchField = find("search");

        mSearchField.addTextChangedListener(mTextWatcher);

        bindTouch(mSortButton, new TouchListener() {
            public void onPressUp() {
                showSortingMenu(getTouchPosition());
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void showSortingMenu(PointF position) {
        ContextMenuBuilder builder = new ContextMenuBuilder(position)
                .setFixedWidth(sdp(80));

        for (SortOrder entry : SortOrder.values()) {
            builder.addItem(new ContextMenu.Item() {
                public String getText() {
                    return entry.name();
                }

                public void onClick(TextView view) {
                    Game.beatmapCollection.sort(entry);
                }
            });
        }

        new ContextMenu(builder).show();
    }
}
