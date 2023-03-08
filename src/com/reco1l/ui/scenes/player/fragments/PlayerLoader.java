package com.reco1l.ui.scenes.player.fragments;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.base.Layers;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.framework.Animation;
import com.reco1l.tools.helpers.BeatmapHelper;
import com.reco1l.view.BadgeTextView;
import com.rimu.R;

import main.osu.TrackInfo;

public class PlayerLoader extends BaseFragment {

    public static final PlayerLoader instance = new PlayerLoader();

    private LinearLayout mLayout;

    private TextView
            mTitleText,
            mArtistText,
            mDifficultyText;

    private BadgeTextView
            mMapperText,
            mStarsText;

    //----------------------------------------------------------------------------------------//

    public PlayerLoader() {
        super(Scenes.player);
    }

    //----------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "pl";
    }

    @Override
    protected int getLayout() {
        return R.layout.player_loader;
    }

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Screen;
    }

    //----------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mLayout = find("body");
        mTitleText = find("title");
        mStarsText = find("stars");
        mArtistText = find("artist");
        mMapperText = find("mapper");
        mDifficultyText = find("difficulty");

        TrackInfo track = Game.musicManager.getTrack();

        if (track != null) {
            mTitleText.setText(BeatmapHelper.getTitle(track));
            mArtistText.setText(BeatmapHelper.getArtist(track));

            mDifficultyText.setText(track.getMode());
            mMapperText.setText(track.getCreator());
            mStarsText.setText("" + track.getDifficulty());
        }

        Animation.of(mLayout)
                 .fromAlpha(0)
                 .toAlpha(1)
                 .fromScale(0.8f)
                 .toScale(1)
                 .play(500);
    }

    //--------------------------------------------------------------------------------------------//

    public void close(Runnable onEnd) {

        Animation.of(mLayout)
                 .toAlpha(0)
                 .toScale(0.8f)
                 .cancelCurrentAnimations(false)
                 .runOnStart(() -> {
                     Game.musicManager.stop();
                     Scenes.player.setFixedScreenDimension(true);
                 })
                 .runOnEnd(() -> {
                     super.close();
                     onEnd.run();
                 })
                 .delay(3000)
                 .play(500);
    }
}
