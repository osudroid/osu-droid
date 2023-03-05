package com.reco1l.management.game;

import com.reco1l.annotation.Legacy;

import main.osu.helper.DifficultyHelper;
import main.osu.scoring.StatisticV2;

@Legacy // Wrap necessary data from legacy to use in new UI.
public class GameWrapper {

    public StatisticV2 statistics;
    public DifficultyHelper difficultyHelper;

    public float
            time,
            startTime,
            overallDifficulty;

    public String playerName;

    public boolean
            isUnranked,
            isReplaying;
}
