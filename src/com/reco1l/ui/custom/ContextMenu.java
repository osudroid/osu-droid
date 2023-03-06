package com.reco1l.ui.custom;

import static com.reco1l.data.adapters.ContextMenuAdapter.*;

import android.graphics.PointF;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.Game;
import com.reco1l.tables.Res;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.base.Layers;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Views;

import com.reco1l.data.adapters.ContextMenuAdapter;

import com.rimu.R;

public class ContextMenu extends BaseFragment {

    private final ContextMenuBuilder mBuilder;

    private final PointF mPosition;

    private CardView mBody;
    private ContextMenuAdapter mAdapter;
    private BouncyRecyclerView mRecyclerView;

    //--------------------------------------------------------------------------------------------//

    public ContextMenu(ContextMenuBuilder builder) {
        super();
        mBuilder = builder;
        mPosition = builder.position;
        closeOnBackgroundClick(true);
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Overlay;
    }

    /*@Override
    protected boolean isExtra() {
        return true;
    }*/

    @Override
    protected View getRootView() {
        RelativeLayout layout = new RelativeLayout(getContext());
        layout.setElevation(sdp(3));
        layout.setLayoutParams(Views.match_parent);

        View view = new View(getContext());
        view.setLayoutParams(Views.match_parent);
        view.setId(R.id.background);
        layout.addView(view);

        mBody = new CardView(Game.activity);

        mBody.setCardBackgroundColor(Res.color(R.color.backgroundPrimary));
        mBody.setRadius(Res.dimen(R.dimen.app_corners));
        mBody.setAlpha(0);
        layout.addView(mBody);

        mRecyclerView = new BouncyRecyclerView(Game.activity, null);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(Game.activity));
        mBody.addView(mRecyclerView);

        return layout;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        fixLocation();
        if (mAdapter == null) {
            mAdapter = new ContextMenuAdapter(mBuilder.items);
        }
        mRecyclerView.setAdapter(mAdapter);

        mBody.post(() -> {
            final int height = mBody.getHeight();

            Animation.of(mBody)
                    .fromHeight(0)
                    .toHeight(height)
                    .toAlpha(1)
                    .play(200);

            fixLocation();
        });

        mRecyclerView.post(this::fixWidth);
    }

    private void fixWidth() {
        if (mBuilder.fixedWidth != -1) {
            mAdapter.forEachHolder(h ->
                    Views.width(h.getView(), mBuilder.fixedWidth)
            );
            return;
        }

        int w = 0;
        for (ItemHolder h : mAdapter.getHolders()) {
            if (h.getWidth() > w) {
                w = h.getWidth();
            }
        }

        if (w > 0) {
            mBuilder.fixedWidth = w;
            fixWidth();
        }
    }

    private void fixLocation() {
        mBody.setX(mPosition.x);
        mBody.setY(mPosition.y);

        if (mBody.getX() + mBody.getWidth() > getWidth()) {
            mBody.setX(getWidth() - mBody.getWidth());
        }

        if (mBody.getY() + mBody.getHeight() > getHeight()) {
            mBody.setY(getHeight() - mBody.getHeight());
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        unbindTouchHandlers();
        super.close();
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
