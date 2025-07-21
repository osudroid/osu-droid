package ru.nsu.ccfit.zuev.osu.scoring;

import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Random;

import com.osudroid.multiplayer.api.data.RoomTeam;
import com.osudroid.multiplayer.api.data.WinCondition;
import com.osudroid.data.ScoreInfo;
import com.osudroid.multiplayer.Multiplayer;
import com.rian.osu.beatmap.Beatmap;
import com.rian.osu.beatmap.sections.BeatmapDifficulty;
import com.rian.osu.mods.IMigratableMod;
import com.rian.osu.mods.IModRequiresOriginalBeatmap;
import com.rian.osu.mods.ModFlashlight;
import com.rian.osu.mods.ModHidden;
import com.rian.osu.utils.ModHashMap;
import com.rian.osu.utils.ModUtils;

import org.json.JSONException;
import org.json.JSONObject;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;

public class StatisticV2 implements Serializable {
    @Serial
    private static final long serialVersionUID = 8339570462000129479L;
    private static final Random random = new Random();
    private static final int scoreV2MaxScore = 1000000;
    private static final float scoreV2AccPortion = 0.3f;
    private static final float scoreV2ComboPortion = 0.7f;

    private int hit300 = 0, hit100 = 0, hit50 = 0;
    private int hit300k = 0, hit100k = 0;
    private int misses = 0;
    private int scoreMaxCombo = 0;
    private int sliderTickHits = 0;
    private int sliderEndHits = 0;
    private long time = 0;
    private int currentCombo = 0;
    private int scoreHash = 0;
    private int totalScore;
    private float hp = 1;
    private float diffModifier = 1;
    private ModHashMap mod = new ModHashMap();
    private String playerName = Config.getOnlineUsername();
    private String replayFilename = "";
    private int forcedScore = -1;
    private String mark = null;
    private int beatmapNoteCount = 0;
    private int beatmapMaxCombo = 0;
    private int bonusScore = 0;
    private int positiveHitOffsetCount;
    private double positiveHitOffsetSum;
    private int negativeHitOffsetCount;
    private double negativeHitOffsetSum;
    private double unstableRate;

    private int life = 1;

    /**
     * Indicates that the player is alive (HP hasn't reached 0, or it recovered), this is exclusively used for
     * multiplayer.
     */
    public boolean isAlive = true;

    /**
     * Whether the player can fail.
     */
    public boolean canFail = true;

    /**
     * The score multiplier from mods.
     */
    private float modScoreMultiplier = 1;

    /**
     * The MD5 hash of the beatmap.
     */
    private String beatmapMD5 = "";

    /**
     * The current performance points.
     */
    private double pp = 0f;


    public StatisticV2() {}

    public StatisticV2(final String[] params) throws JSONException {
        this(params, null);
    }

    public StatisticV2(final String[] params, final BeatmapDifficulty originalDifficulty) throws JSONException {
        playerName = "";
        if (params.length < 6) return;

        mod = ModUtils.deserializeMods(params[0]);
        setForcedScore(Integer.parseInt(params[1]));
        scoreMaxCombo = Integer.parseInt(params[2]);
        mark = params[3];
        hit300k = Integer.parseInt(params[4]);
        hit300 = Integer.parseInt(params[5]);
        hit100k = Integer.parseInt(params[6]);
        hit100 = Integer.parseInt(params[7]);
        hit50 = Integer.parseInt(params[8]);
        misses = Integer.parseInt(params[9]);
        if (params.length >= 11) {
            time = Long.parseLong(params[10]);
        }
        if (params.length >= 13) {
            playerName = params[12];
        }

        sliderTickHits = params.length >= 14 ? Integer.parseInt(params[13]) : -1;
        sliderEndHits = params.length >= 15 ? Integer.parseInt(params[14]) : -1;

        if (originalDifficulty != null) {
            migrateLegacyMods(originalDifficulty);
        }
    }

    public float getHp() {
        return hp;
    }

    public void changeHp(final float amount) {
        hp += amount;
        if (hp < 0) {
            hp = 0;
            life = Math.max(0, life - 1);

            if (canFail && life == 0) {
                isAlive = false;
            }
        }
        if (hp > 1) {
            hp = 1;
            isAlive = true;
        }
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getTotalScoreWithMultiplier() {
        if (forcedScore > 0)
            return forcedScore;

        return (int) (totalScore * modScoreMultiplier);
    }

    public void registerSpinnerHit() {
        addScore(100, false);
    }

    public void registerHit(final int score, final boolean k, final boolean g) {
        registerHit(score, k, g, true);
    }

    public void registerHit(final int score, final boolean k, final boolean g, final boolean incrementCombo) {
        if (score == 1000) {
            addScore(score, false);
            return;
        }
        if (score < 50 && score > 0) {
            changeHp(0.05f);
            addScore(score, false);
            currentCombo++;
            return;
        }
        if (score == 0 && k) {
            changeHp(-(5 + GameHelper.getHealthDrain()) / 100f);
            if (currentCombo > scoreMaxCombo) {
                scoreMaxCombo = currentCombo;
            }
            currentCombo = 0;
            return;
        }

        switch (score) {
            case 300:
                changeHp(k ? 0.10f : 0.05f);
                if (g) {
                    hit300k++;
                }
                hit300++;
                addScore(300, true);
                if (incrementCombo) {
                    currentCombo++;
                }
                break;
            case 100:
                changeHp(k ? 0.15f : 0.05f);
                if (k) {
                    hit100k++;
                }
                hit100++;
                addScore(100, true);
                if (incrementCombo) {
                    currentCombo++;
                }
                break;
            case 50:
                changeHp(0.05f);
                hit50++;
                addScore(50, true);
                if (incrementCombo) {
                    currentCombo++;
                }
                break;
            default:
                changeHp(-(5 + GameHelper.getHealthDrain()) / 100f);
                misses++;
                if (currentCombo > scoreMaxCombo) {
                    scoreMaxCombo = currentCombo;
                }
                currentCombo = 0;
                // Still add score to update ScoreV2 value.
                addScore(0, false);
                break;
        }
    }

    public float getAccuracy() {
        int notesHit = getNotesHit();

        if (notesHit == 0) {
            return 1;
        }

        return (hit300 * 6f + hit100 * 2 + hit50) / (6 * notesHit);
    }

    private void addScore(final int amount, final boolean combo) {
        if (!isScoreValid()) {
            scoreHash = random.nextInt(1313) | 3455;
            return;
        }
        //如果使用scorev2
        if (GameHelper.isScoreV2()) {
            if (amount == 1000) {
                bonusScore += amount;
            }

            int currentMaxCombo = getScoreMaxCombo();
            // At this point, the combo increment in registerHit has not happened, but it is necessary for ScoreV2
            // calculation, so we do it locally here.
            if (combo && currentCombo == currentMaxCombo) {
                currentMaxCombo++;
            }

            double comboPortion = scoreV2ComboPortion * currentMaxCombo / beatmapMaxCombo;
            double accuracyPortion = scoreV2AccPortion * Math.pow(getAccuracy(), 10) * getNotesHit() / beatmapNoteCount;

            totalScore = (int) (scoreV2MaxScore * (comboPortion + accuracyPortion)) + bonusScore;
        } else if (amount + amount * currentCombo * diffModifier / 25 > 0) {
            // It is possible for score addition to be a negative number due to
            // difficulty modifier, hence the prior check.
            //
            // In that case, just skip score addition to ensure score is always positive.

            //如果分数溢出或分数满了
            if (totalScore + (amount * currentCombo * diffModifier) / 25 + amount < 0 || totalScore == Integer.MAX_VALUE){
                totalScore = Integer.MAX_VALUE;
            }
            else{
                totalScore += amount;
                if (combo) {
                    totalScore += (int) ((amount * currentCombo * diffModifier) / 25);
                }
            }
        }
        scoreHash = SecurityUtils.getHigh16Bits(totalScore);
    }

    public String getMark() {
        if (mark != null)
            return mark;

        boolean isH = mod.contains(ModHidden.class) || mod.contains(ModFlashlight.class);

        int notesHit = getNotesHit();

        if (hit100 == 0 && hit50 == 0 && misses == 0) {
            if (isH) {
                return "XH";
            }
            return "X";
        }
        if (hit300 / (float) notesHit > 0.9f && misses == 0
                && hit50 / (float) notesHit < 0.01f) {
            if (isH) {
                return "SH";
            }
            return "S";
        }
        if (hit300 / (float) notesHit > 0.8f && misses == 0
                || hit300 / (float) notesHit > 0.9f) {
            return "A";
        }
        if (hit300 / (float) notesHit > 0.7f && misses == 0
                || hit300 / (float) notesHit > 0.8f) {
            return "B";
        }
        if (hit300 / (float) notesHit > 0.6f) {
            return "C";
        }
        return "D";
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getScoreMaxCombo() {
        if (currentCombo > scoreMaxCombo) {
            scoreMaxCombo = currentCombo;
        }

        return scoreMaxCombo;
    }

    public void setScoreMaxCombo(int maxCombo) {
        scoreMaxCombo = maxCombo;
    }

    public int getNotesHit() {
        return hit300 + hit100 + hit50 + misses;
    }

    public int getHit300() {
        return hit300;
    }

    public void setHit300(int hit300) {
        this.hit300 = hit300;
    }

    public int getHit100() {
        return hit100;
    }

    public void setHit100(int hit100) {
        this.hit100 = hit100;
    }

    public int getHit50() {
        return hit50;
    }

    public void setHit50(int hit50) {
        this.hit50 = hit50;
    }

    public int getHit300k() {
        return hit300k;
    }

    public void setHit300k(int hit300k) {
        this.hit300k = hit300k;
    }

    public int getHit100k() {
        return hit100k;
    }

    public void setHit100k(int hit100k) {
        this.hit100k = hit100k;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public int getSliderTickHits() {
        return sliderTickHits;
    }

    public void setSliderTickHits(int sliderTickHits) {
        this.sliderTickHits = sliderTickHits;
    }

    public void addSliderTickHit() {
        sliderTickHits++;
    }

    public int getSliderEndHits() {
        return sliderEndHits;
    }

    public void setSliderEndHits(int sliderEndHits) {
        this.sliderEndHits = sliderEndHits;
    }

    public void addSliderEndHit() {
        sliderEndHits++;
    }

    public boolean isPerfect() {
        return getAccuracy() == 1f;
    }

    public int getCombo() {
        return currentCombo;
    }

    public void setCombo(int combo) {
        currentCombo = combo;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public ModHashMap getMod() {
        return mod;
    }

    public void setMod(final ModHashMap mod) {
        this.mod = mod;
    }

    public float getDiffModifier() {
        return diffModifier;
    }

    public void setDiffModifier(final float diffModifier) {
        this.diffModifier = diffModifier;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(final String playerName) {
        this.playerName = playerName;
    }

    public String getReplayFilename() {
        return replayFilename;
    }

    public void setReplayFilename(String replayName) {
        this.replayFilename = replayName;
    }

    public void setForcedScore(int forcedScore) {
        this.forcedScore = forcedScore;
        totalScore = forcedScore;
    }

    public void setBeatmapMD5(String beatmapMD5) {
        this.beatmapMD5 = beatmapMD5;
    }

    public final boolean isScoreValid() {
        return SecurityUtils.getHigh16Bits(totalScore) == scoreHash;
    }

    public String compile() {
        StringBuilder builder = new StringBuilder();
        builder.append(mod.serializeMods(false));
        builder.append(' ');
        builder.append(getTotalScoreWithMultiplier());
        builder.append(' ');
        builder.append(getScoreMaxCombo());
        builder.append(' ');
        builder.append(getMark());
        builder.append(' ');
        builder.append(getHit300k());
        builder.append(' ');
        builder.append(getHit300());
        builder.append(' ');
        builder.append(getHit100k());
        builder.append(' ');
        builder.append(getHit100());
        builder.append(' ');
        builder.append(getHit50());
        builder.append(' ');
        builder.append(getMisses());
        builder.append(' ');
        builder.append(getSliderTickHits());
        builder.append(' ');
        builder.append(getSliderEndHits());
        builder.append(' ');
        builder.append(getAccuracy());
        builder.append(' ');
        builder.append(getTime());
        builder.append(' ');
        builder.append(isPerfect() ? 1 : 0);
        builder.append(' ');
        builder.append(getPlayerName());
        return builder.toString();
    }

    public void setBeatmapNoteCount(int count){
        beatmapNoteCount = count;
    }
    public void setBeatmapMaxCombo(int count){
        beatmapMaxCombo = count;
    }

    public double getUnstableRate() {
        return unstableRate;
    }

    public void addHitOffset(double accuracy) {
        double msAccuracy = accuracy * 1000;

        // Update hit offset
        if (accuracy >= 0) {
            positiveHitOffsetSum += msAccuracy;
            positiveHitOffsetCount++;
        } else {
            negativeHitOffsetSum += msAccuracy;
            negativeHitOffsetCount++;
        }

        // Update unstable rate
        // Reference: https://math.stackexchange.com/questions/775391/can-i-calculate-the-new-standard-deviation-when-adding-a-value-without-knowing-t
        int hitOffsetCount = positiveHitOffsetCount + negativeHitOffsetCount;
        double hitOffsetSum = positiveHitOffsetSum + negativeHitOffsetSum;

        if (hitOffsetCount > 1) {
            unstableRate = 10 * Math.sqrt(
                ((hitOffsetCount - 1) * Math.pow(unstableRate / 10, 2) +
                    (msAccuracy - hitOffsetSum / hitOffsetCount) * (msAccuracy - (hitOffsetSum - msAccuracy) / (hitOffsetCount - 1))) / hitOffsetCount
            );
        }
    }

    public double getNegativeHitError() {
        return negativeHitOffsetCount == 0 ? 0 : negativeHitOffsetSum / negativeHitOffsetCount;
    }

    public double getPositiveHitError() {
        return positiveHitOffsetCount == 0 ? 0 : positiveHitOffsetSum / positiveHitOffsetCount;
    }

    /**
     * Converts the statistic into a JSONObject readable by the multiplayer server.
     */
    public JSONObject toJson(){
        return new JSONObject() {{
            try {
                put("accuracy", getAccuracy());
                put("score", getTotalScoreWithMultiplier());
                put("username", playerName);
                put("mods", mod.serializeMods());
                put("maxCombo", scoreMaxCombo);
                put("geki", hit300k);
                put("perfect", hit300);
                put("katu", hit100k);
                put("good", hit100);
                put("bad", hit50);
                put("miss", misses);
                put("isAlive", isAlive);
            } catch (Exception e) {
                Multiplayer.log(e);
            }
        }};
    }

    /**
     * Converts the statistic to a ScoreBoardItem, used specifically for Multiplayer.
     */
    public ScoreBoardItem toBoardItem() {

        //noinspection DataFlowIssue
        var combo = !Multiplayer.isConnected() || Multiplayer.room.getWinCondition() != WinCondition.MaximumCombo ? currentCombo : scoreMaxCombo;

        return new ScoreBoardItem(playerName, getTotalScoreWithMultiplier(), combo, getAccuracy(), isAlive);
    }

    /**
     * Converts the statistic to a ScoreInfo.
     */
    public ScoreInfo toScoreInfo() {
        return new ScoreInfo(
            beatmapMD5,
            playerName,
            replayFilename,
            mod.serializeMods(false).toString(),
            getTotalScoreWithMultiplier(),
            scoreMaxCombo,
            getMark(),
            hit300k,
            hit300,
            hit100k,
            hit100,
            hit50,
            misses,
            time,
            sliderTickHits >= 0 ? sliderTickHits : null,
            sliderEndHits >= 0 ? sliderEndHits : null
        );
    }

    public void calculateModScoreMultiplier(final Beatmap beatmap) {
        for (var m : mod.values()) {
            if (m instanceof IModRequiresOriginalBeatmap requiresOriginalBeatmap) {
                requiresOriginalBeatmap.applyFromBeatmap(beatmap);
            }
        }

        modScoreMultiplier = ModUtils.calculateScoreMultiplier(mod);
    }

    public void migrateLegacyMods(final BeatmapDifficulty originalDifficulty) {
        for (var m : mod.values()) {
            if (m instanceof IMigratableMod migratableMod) {
                mod.remove(m);
                mod.put(migratableMod.migrate(originalDifficulty));
            }
        }
    }

    /**
     * Whether the statistic corresponds to a team.
     */
    public boolean isTeamStatistic() {
        return Multiplayer.isConnected() && (playerName.equals(RoomTeam.Red.toString()) || playerName.equals(RoomTeam.Blue.toString()));
    }

    public void setPP(double value) {
        pp = value;
    }

    public double getPP() {
        return pp;
    }

    /**
     * Resets the statistics to their initial values.
     */
    public void reset() {
        hit300 = 0;
        hit100 = 0;
        hit50 = 0;
        hit300k = 0;
        hit100k = 0;
        misses = 0;
        sliderTickHits = 0;
        sliderEndHits = 0;
        scoreMaxCombo = 0;
        currentCombo = 0;
        totalScore = 0;
        hp = 1;
        mark = null;
        bonusScore = 0;
        positiveHitOffsetCount = 0;
        positiveHitOffsetSum = 0;
        negativeHitOffsetCount = 0;
        negativeHitOffsetSum = 0;
        unstableRate = 0;
        pp = 0;
    }
}
