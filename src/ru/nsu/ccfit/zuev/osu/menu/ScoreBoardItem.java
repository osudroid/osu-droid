package ru.nsu.ccfit.zuev.osu.menu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.reco1l.ibancho.data.WinCondition;
import com.reco1l.osu.multiplayer.Multiplayer;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;

public class ScoreBoardItem implements Cloneable {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);


    private static final StringBuilder accSb = new StringBuilder();
    private static final Formatter DECIMAL_FORMAT = new Formatter(accSb, Locale.ENGLISH);


    public String userName;
    public int playScore;
    public int scoreId;

    /**
     * The rank position in the leaderboard.
     */
    public int rank = -1;

    /**
     * In single player this is always 'maxCombo', in multiplayer if the room win condition isn't Max Combo it'll be
     * current combo.
     */
    public int maxCombo;

    /**
     * Only shown in multiplayer when room win condition is Accuracy
     */
    public float accuracy = -1;

    /**
     * Indicates that the player is alive (HP hasn't reached 0, or it recovered)
     */
    public boolean isAlive = true;


    public ScoreBoardItem() {}

    public ScoreBoardItem(String userName, int playScore, int maxCombo, float accuracy, boolean isAlive) {
        this.userName = userName;
        this.playScore = playScore;
        this.maxCombo = maxCombo;
        this.accuracy = accuracy;
        this.isAlive = isAlive;
    }


    public void set(int rankPos, String name, int combo, int score, int id) {
        rank = rankPos;
        userName = name;
        maxCombo = combo;
        playScore = score;
        scoreId = id;
    }


    public String get() {
        var text = userName + "\n" + NUMBER_FORMAT.format(playScore) + "\n";

        //noinspection DataFlowIssue
        if (Multiplayer.isConnected() && Multiplayer.room.getWinCondition() == WinCondition.HighestAccuracy) {
            accSb.setLength(0);
            text += DECIMAL_FORMAT.format("%2.2f%%", accuracy * 100f);
        } else
            text += NUMBER_FORMAT.format(maxCombo) + "x";

        return text;
    }

    /**
     * Converts the item into a JSONObject, it is used specifically for Multiplayer.
     */
    public JSONObject toJson() {
        return new JSONObject() {{
            try {
                put("accuracy", accuracy);
                put("score", playScore);
                put("combo", maxCombo);
                put("isAlive", isAlive);
            } catch (Exception e) {
                Multiplayer.log(e);
            }
        }};
    }


    @Override
    public boolean equals(@Nullable Object o)
    {
        if (o == this)
            return true;

        if (!(o instanceof ScoreBoardItem))
            return false;

        var other = (ScoreBoardItem) o;

        return Objects.equals(other.userName, userName)
                && other.playScore == playScore
                && other.maxCombo == maxCombo
                && other.accuracy == accuracy
                && other.isAlive == isAlive;
    }

    @NonNull
    @Override
    public ScoreBoardItem clone() throws CloneNotSupportedException {
        return (ScoreBoardItem) super.clone();
    }
}
