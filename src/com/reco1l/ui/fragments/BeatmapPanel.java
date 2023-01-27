package com.reco1l.ui.fragments;

import static android.view.View.LAYER_TYPE_HARDWARE;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.data.TrackCard;
import com.reco1l.data.ScoreInfo;
import com.reco1l.management.BoardManager;
import com.reco1l.enums.Screens;
import com.reco1l.interfaces.IGameMod;
import com.reco1l.interfaces.MusicObserver;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.view.drawables.SelectorBackground;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 13/9/22 01:22

public final class BeatmapPanel extends BaseFragment implements
        IGameMod,
        MusicObserver,
        BoardManager.Listener {

    public static BeatmapPanel instance;

    public boolean isOnlineBoard = false;

    private int bodyWidth;

    private View body, message, pageContainer, tabIndicator;
    private TextView localTab, globalTab, messageText;

    private boolean
            isTabAnimInProgress = false,
            isBannerExpanded = true;

    private TrackCard mTrackCard;

    //--------------------------------------------------------------------------------------------//

    public BeatmapPanel() {
        super(Screens.Selector);
        Game.musicManager.bindMusicObserver(this);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.selector_beatmap_panel;
    }

    @Override
    protected String getPrefix() {
        return "bp";
    }

    @Override
    protected boolean getConditionToShow() {
        return Game.libraryManager.getSizeOfBeatmaps() != 0;
    }

    //--------------------------------------------------------------------------------------------//
    @Override
    protected void onLoad() {
        rootBackground.setLayerType(LAYER_TYPE_HARDWARE, null);
        rootBackground.setBackground(new SelectorBackground());

        bodyWidth = Res.dimen(R.dimen.beatmapPanelContentWidth);
        isBannerExpanded = true;
        body = find("body");

        Animation.of(body)
                .fromX(-bodyWidth)
                .toX(0)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        message = find("messageLayout");
        messageText = find("messageTv");
        pageContainer = find("pageContainer");

        localTab = find("localTab");
        globalTab = find("globalTab");
        tabIndicator = find("tabIndicator");

        message.setVisibility(View.GONE);

        bindTouch(globalTab, () -> switchTab(globalTab));
        bindTouch(localTab, () -> switchTab(localTab));

        mTrackCard = new TrackCard(find("banner"));
        mTrackCard.onMusicChange(Game.musicManager.getTrack());

        switchTab(localTab);
    }

    @Override
    public void onMusicChange(@Nullable TrackInfo newTrack, boolean isSameAudio) {
        if (isLoaded()) {
            mTrackCard.onMusicChange(newTrack);
            Game.boardManager.load(newTrack);
        }
    }

    @Override
    public void onBoardChange(ArrayList<ScoreInfo> pList) {
        if (!isLoaded()) {
            return;
        }
        handleScoreboard();
    }

    //--------------------------------------------------------------------------------------------//

    public void updateAttributes() {
        mTrackCard.handleModifications();
    }

    private void switchTab(View button) {
        if (isTabAnimInProgress || tabIndicator.getTranslationX() == button.getX())
            return;

        boolean toRight = tabIndicator.getTranslationX() < button.getX();

        Animation.of(tabIndicator)
                .toX(button.getX())
                .play(150);

        Animation.of(pageContainer)
                .toX(toRight ? -60 : 60)
                .toAlpha(0)
                .runOnStart(() -> isTabAnimInProgress = true)
                .play(200);

        Animation.of(pageContainer)
                .fromX(toRight ? 60 : -60)
                .toX(0)
                .toAlpha(1)
                .runOnEnd(() -> isTabAnimInProgress = false)
                .delay(200)
                .play(200);

        pageContainer.postDelayed(() -> {
            isOnlineBoard = button == globalTab;
            Game.boardManager.load(Game.musicManager.getTrack());
        }, 200);
    }

    private void handleScoreboard() {
        if (Game.boardManager.isEmpty()) {
            message.setVisibility(View.VISIBLE);
            messageText.setText(Game.boardManager.getErrorMessage());
        } else {
            message.setVisibility(View.GONE);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }

        Animation.of(body)
                .toX(-bodyWidth)
                .toTopMargin((int) Res.dimen(R.dimen.topBarHeight))
                .toAlpha(0)
                .runOnEnd(super::close)
                .play(300);
    }

    //--------------------------------------------------------------------------------------------//
    // Temporal workaround until DuringGameScoreBoard gets replaced (old UI)

    public ScoreInfo[] getBoard() {
        return Game.boardManager.getListAsArray();
    }

    //--------------------------------------------------------------------------------------------//
}
