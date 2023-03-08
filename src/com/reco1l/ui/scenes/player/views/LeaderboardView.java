package com.reco1l.ui.scenes.player.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.management.game.GameWrapper;
import com.reco1l.management.scoreboard.ScoreInfo;
import com.reco1l.Game;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.view.RoundLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import main.osu.scoring.StatisticV2;
import com.rimu.R;

// Written by Reco1l

public class LeaderboardView extends RoundLayout implements IPassiveObject {

    private RecyclerView mRecycler;
    private Adapter mAdapter;

    private ArrayList<ScoreInfo> mScores;

    private ScoreInfo mCurrentScore;
    private StatisticV2 mStatistics;

    private boolean mSwapping;

    private int mCurrentPosition;

    //--------------------------------------------------------------------------------------------//

    public LeaderboardView(@NonNull Context context) {
        super(context);
    }

    public LeaderboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LeaderboardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setGravity(RelativeLayout.CENTER_VERTICAL);
        setRadius(0);

        mRecycler = new RecyclerView(getContext());
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.setVerticalScrollBarEnabled(false);
        addView(mRecycler);

        mAdapter = new Adapter(null);
        mRecycler.setAdapter(mAdapter);
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressWarnings("unchecked")
    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        if (wrapper == null) {
            clear();
            return;
        }
        mStatistics = wrapper.statistics;
        mScores = new ArrayList<>(Game.scoreboardManager.getList());

        int replay = Scenes.summary.getReplayID();
        if (replay != -1) {
            ScoreInfo toRemove = null;

            for (ScoreInfo score : mScores) {
                if (score.getId() == replay) {
                    toRemove = score;
                    break;
                }
            }
            mScores.remove(toRemove);
        }

        Collections.reverse(mScores);
        mCurrentScore = new ScoreInfo().setName(wrapper.playerName);
        mScores.add(mCurrentScore);
        mCurrentPosition = mScores.size() - 1;

        mAdapter.setData(mScores);
    }

    @Override
    public void onObjectUpdate(float dt, float sec) {
        if (mStatistics == null || mScores == null) {
            return;
        }

        mCurrentScore.setScore(mStatistics.getAutoTotalScore());
        mCurrentScore.setCombo(mStatistics.getCombo());

        Game.activity.runOnUiThread(() ->
                mAdapter.notifyDataSetChanged()
        );

        if (mCurrentPosition == 0) {
            return;
        }

        int behindPosition = mCurrentPosition - 1;
        ScoreInfo behindScore = mScores.get(behindPosition);

        if (mCurrentScore.getScore() > behindScore.getScore()) {

            mScores.remove(mCurrentScore);
            mScores.add(behindPosition, mCurrentScore);

            mCurrentPosition = behindPosition;

            Game.activity.runOnUiThread(() ->
                    mRecycler.scrollToPosition(mCurrentPosition)
            );
        }
    }

    @Override
    public void clear() {
        Game.activity.runOnUiThread(() ->
                mAdapter.setData(null)
        );

        mStatistics = null;
        mCurrentScore = null;
        mScores = null;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    private static class Adapter extends BaseAdapter<ViewHolder, ScoreInfo> {

        public Adapter(ArrayList<ScoreInfo> items) {
            super(items);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected ViewHolder getViewHolder(View rootView) {
            return new ViewHolder(rootView);
        }

        @Override
        protected int getItemLayout() {
            return R.layout.item_game_board;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private static class ViewHolder extends BaseViewHolder<ScoreInfo> {

        private final DecimalFormat mDF;

        private final TextView
                mRank,
                mName,
                mCombo,
                mScore;

        //----------------------------------------------------------------------------------------//

        public ViewHolder(@NonNull View root) {
            super(root);

            mRank = root.findViewById(R.id.id_rank);
            mName = root.findViewById(R.id.id_name);
            mCombo = root.findViewById(R.id.id_max_combo);
            mScore = root.findViewById(R.id.id_score);

            mDF = new DecimalFormat("###,###,###,###");
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onBind(ScoreInfo item, int position) {
            mName.setText(item.getName());
            mRank.setText("" + (position + 1));
            mScore.setText(mDF.format(item.getScore()));
            mCombo.setText(mDF.format(item.getCombo()) + "x");
        }

        @Override
        public void onSelect() {
            super.onSelect();
        }

        @Override
        public void onDeselect() {
            super.onDeselect();
        }
    }
}
