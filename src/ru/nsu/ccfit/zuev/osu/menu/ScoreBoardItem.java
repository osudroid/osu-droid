package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.api.ibancho.data.WinCondition;
import com.reco1l.legacy.ui.multiplayer.Multiplayer;
import com.reco1l.legacy.ui.multiplayer.RoomScene;

import java.text.NumberFormat;
import java.util.Locale;

public class ScoreBoardItem
{
    public String userName;
    public int playScore;
    public int scoreId;
    public int maxCombo;

    /**
     * Only shown in multiplayer when room win condition is Accuracy
     */
    public float accuracy = -1;

    public void set(String name, int com, int scr, int id)
    {
        userName = name;
        maxCombo = com;
        playScore = scr;
        scoreId = id;
    }

    public String get()
    {
        var text = userName + "\n" + NumberFormat.getNumberInstance(Locale.US).format(playScore) + "\n";

        //noinspection DataFlowIssue
        if (Multiplayer.isConnected && RoomScene.getRoom().getWinCondition() == WinCondition.ACCURACY)
        {
            text += String.format(Locale.ENGLISH, "%2.2f%%", accuracy * 100f);
        }
        else
        {
            text += NumberFormat.getNumberInstance(Locale.US).format(maxCombo) + "x";
        }

        return text;
    }
}
