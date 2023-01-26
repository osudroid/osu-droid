package com.reco1l.ui.custom;

import android.graphics.PointF;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.Game;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Views;

import com.reco1l.data.adapters.ContextMenuAdapter;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ContextMenu extends BaseFragment {

    private final ContextMenuBuilder mBuilder;

    private PointF mPosition;

    private CardView card;
    private ContextMenuAdapter adapter;
    private BouncyRecyclerView recyclerView;

    //--------------------------------------------------------------------------------------------//

    public ContextMenu(ContextMenuBuilder builder) {
        super();
        mBuilder = builder;
        mPosition = builder.position;
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
        layout.setElevation(Res.dimen(R.dimen.top_layer));
        layout.setLayoutParams(Views.match_parent);
        layout.setId(R.id.background);

        card = new CardView(Game.activity);

        card.setCardBackgroundColor(Res.color(R.color.backgroundSecondary));
        card.setRadius(Res.dimen(R.dimen.app_corners));
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
            adapter = new ContextMenuAdapter(mBuilder.items);
        }
        recyclerView.setAdapter(adapter);

        card.post(() -> {
            final int height = card.getHeight();

            Animation.of(card)
                    .fromHeight(0)
                    .toHeight(height)
                    .toAlpha(1)
                    .play(200);

            fixLocation();
        });
    }

    private void fixLocation() {
        card.setX(mPosition.x);
        card.setY(mPosition.y);

        if (card.getX() + card.getWidth() > getWidth()) {
            card.setX(getWidth() - card.getWidth());
        }

        if (card.getY() + card.getHeight() > getHeight()) {
            card.setY(getHeight() - card.getHeight());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static abstract class Item {

        public abstract String getText();

        public abstract void onClick(TextView view);

        public boolean isSelectable() {
            return false;
        }
    }

}
