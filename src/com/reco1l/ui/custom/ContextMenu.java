package com.reco1l.ui.custom;

import android.view.View;
import android.widget.RelativeLayout;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.Game;
import com.reco1l.data.adapters.ContextMenuAdapter;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Views;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ContextMenu extends BaseFragment {

    private final ContextMenuBuilder builder;

    private View parent;
    private CardView card;
    private ContextMenuAdapter adapter;
    private BouncyRecyclerView recyclerView;

    private boolean isChildView = false;

    //--------------------------------------------------------------------------------------------//

    public ContextMenu(ContextMenuBuilder builder) {
        super();
        this.builder = builder;
        closeOnBackgroundClick(true);
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    protected boolean isOverlay() {
        return true;
    }

    @Override
    protected boolean isExtra() {
        return true;
    }

    @Override
    protected View getRootView() {
        RelativeLayout layout = new RelativeLayout(getContext());
        layout.setElevation(Res.dimen(R.dimen.imposedLayer));
        layout.setLayoutParams(Views.match_parent);
        layout.setId(R.id.background);

        card = new CardView(Game.activity);

        card.setCardBackgroundColor(Res.color(R.color.backgroundSecondary));
        card.setRadius(Res.dimen(R.dimen.globalCorners));
        card.setAlpha(0);
        layout.addView(card);

        recyclerView = new BouncyRecyclerView(Game.activity, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(Game.activity));
        card.addView(recyclerView);

        return layout;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        fixLocation();
        if (adapter == null) {
            adapter = new ContextMenuAdapter(builder.items);
        }
        recyclerView.setAdapter(adapter);

        card.post(() -> {
            final int height = card.getHeight();

            Animation.of(card)
                    .fromHeight(0)
                    .toHeight(height)
                    .toAlpha(1)
                    .play(200);
        });
    }

    @Override
    protected void onUpdate(float sec) {
        fixLocation();
    }

    private void fixLocation() {
        if (parent != null) {
            int[] location = new int[2];

            parent.getLocationInWindow(location);

            card.setX(location[0]);
            card.setY(location[1]);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void show(View view) {
        if (builder == null) {
            throw new NullPointerException("Dropdown builder can't be null!");
        }
        this.parent = view;
        super.show();


    }

    @Override
    public void show() {
        throw new RuntimeException("You have to set parent view by calling show(View) instead");
    }

    //--------------------------------------------------------------------------------------------//

    public static class Item {

        private String text;
        private Runnable onClick;

        public Item(String text, Runnable onClick) {
            this.text = text;
            this.onClick = onClick;
        }

        public String getText() {
            return text;
        }

        public Runnable getOnClick() {
            return onClick;
        }
    }

}
