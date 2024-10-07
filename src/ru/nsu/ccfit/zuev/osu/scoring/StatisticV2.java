package ru.nsu.ccfit.zuev.osu.scoring;

import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Random;

import com.reco1l.ibancho.data.RoomTeam;
import com.reco1l.ibancho.data.WinCondition;
import com.reco1l.osu.data.ScoreInfo;
import com.reco1l.osu.multiplayer.Multiplayer;
import com.rian.osu.beatmap.sections.BeatmapDifficulty;
import com.rian.osu.mods.ILegacyMod;
import com.rian.osu.mods.ModFlashlight;
import com.rian.osu.mods.ModHidden;
import com.rian.osu.utils.ModHashSet;
import com.rian.osu.utils.ModUtils;

import org.json.JSONObject;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;

public class StatisticV2 implements Serializable {
    @Serial
    private static final long serialVersionUID = 8339570462000129479L;
    private static final Random random = new Random();

    int hit300 = 0, hit100 = 0, hit50 = 0;
    int hit300k = 0, hit100k = 0;
    int misses = 0;
    int maxCombo = 0;
    float accuracy = -1;
    long time = 0;
    private int notes = 0;
    private boolean perfect = false;
    private int currentCombo = 0;
    private int scoreHash = 0;
    private int totalScore;
    private int possibleScore = 0;
    private int realScore = 0;
    private float hp = 1;
    private float diffModifier = 1;
    private ModHashSet mod = new ModHashSet();
    private String playerName = Config.getOnlineUsername();
    private String replayFilename = "";
    private int forcedScore = -1;
    private String mark = null;
    private final int MAX_SCORE = 1000000;
    private final float ACC_PORTION = 0.3f;
    private final float COMBO_PORTION = 0.7f;
    private int maxObjectsCount = 0;
    private int maxHighestCombo = 0;
    private int bonusScore = 0;
    private int positiveTotalOffsetSum;
    private double positiveHitOffsetSum;
    private int negativeTotalOffsetSum;
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
     * The directory of the beatmap set.
     */
    private String beatmapSetDirectory = "";

    /**
     * The filename of the beatmap.
     */
    private String beatmapFilename = "";


    public StatisticV2() {}

    public StatisticV2(final String[] params) {
        this(params, null);
    }

    public StatisticV2(final String[] params, final BeatmapDifficulty originalDifficulty) {
        playerName = "";
        if (params.length < 6) return;

        mod = ModUtils.convertModString(params[0]);
        setForcedScore(Integer.parseInt(params[1]));
        maxCombo = Integer.parseInt(params[2]);
        mark = params[3];
        hit300k = Integer.parseInt(params[4]);
        hit300 = Integer.parseInt(params[5]);
        hit100k = Integer.parseInt(params[6]);
        hit100 = Integer.parseInt(params[7]);
        hit50 = Integer.parseInt(params[8]);
        misses = Integer.parseInt(params[9]);
        accuracy = Float.parseFloat(params[10]);
        if (params.length >= 12) {
            time = Long.parseLong(params[11]);
        }
        if (params.length >= 13) {
            perfect = Integer.parseInt(params[12]) != 0;
        }
        if (params.length >= 14) {
            playerName = params[13];
        }

        if (originalDifficulty != null) {
            migrateLegacyMods(originalDifficulty);
            calculateModScoreMultiplier(originalDifficulty);
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
        if (score == 0 && k == true) {
            changeHp(-(5 + GameHelper.getHealthDrain()) / 100f);
            if (currentCombo > maxCombo) {
                maxCombo = currentCombo;
            }
            currentCombo = 0;
            return;
        }

        notes++;
        possibleScore += 300;

        switch (score) {
            case 300:
                changeHp(k ? 0.10f : 0.05f);
                if (g) {
                    hit300k++;
                }
                hit300++;
                addScore(300, true);
                realScore += 300;
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
                realScore += 100;
                if (incrementCombo) {
                    currentCombo++;
                }
                break;
            case 50:
                changeHp(0.05f);
                hit50++;
                addScore(50, true);
                realScore += 50;
                if (incrementCombo) {
                    currentCombo++;
                }
                break;
            default:
                changeHp(-(5 + GameHelper.getHealthDrain()) / 100f);
                misses++;
                perfect = false;
                if (currentCombo > maxCombo) {
                    maxCombo = currentCombo;
                }
                currentCombo = 0;
                break;
        }
    }

    public float getAccuracyForServer() {

        var value = (hit300 * 6f + hit100 * 2f + hit50) / ((hit300 + hit100 + hit50 + misses) * 6f);

        if (Double.isNaN(value) || Double.isInfinite(value))
            value = 0;

        return value;
    }

    public float getAccuracy() {
        if (accuracy >= 0)
            return accuracy;
        if (possibleScore == 0) {
            return 0;
        }
        return realScore / (float) possibleScore;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
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
            float percentage = (float)(notes) / maxObjectsCount;
            //get real maxcb
            int maxcb = getMaxCombo();
            if (currentCombo == maxcb)maxcb++;
            //get real acc
            float acc = 0;
            if (possibleScore > 0){
                switch (amount) {
                    case 300:
                        acc = (realScore + 300) / (float) possibleScore;
                        break;
                    case 100:
                        acc = (realScore + 100) / (float) possibleScore;
                        break;
                    case 50:
                        acc = (realScore + 50) / (float) possibleScore;
                        break;
                    default:
                        acc = realScore / (float) possibleScore;
                        break;
                }
            }
            totalScore = (int)(MAX_SCORE * (ACC_PORTION * Math.pow(acc , 10) * percentage
                    + COMBO_PORTION * maxcb / maxHighestCombo) + bonusScore);
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
                    totalScore += (amount * currentCombo * diffModifier) / 25;
                }
            }
        }
        scoreHash = SecurityUtils.getHigh16Bits(totalScore);
    }

    public String getMark() {
        if (mark != null)
            return mark;

        boolean isH = mod.contains(ModHidden.class) || mod.contains(ModFlashlight.class);

        if (hit100 == 0 && hit50 == 0 && misses == 0) {
            if (isH) {
                return "XH";
            }
            return "X";
        }
        if ((hit300) / (float) notes > 0.9f && misses == 0
                && hit50 / (float) notes < 0.01f) {
            if (isH) {
                return "SH";
            }
            return "S";
        }
        if ((hit300) / (float) notes > 0.8f && misses == 0
                || (hit300) / (float) notes > 0.9f) {
            return "A";
        }
        if ((hit300) / (float) notes > 0.7f && misses == 0
                || (hit300) / (float) notes > 0.8f) {
            return "B";
        }
        if ((hit300) / (float) notes > 0.6f) {
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

    public int getMaxCombo() {
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo;
        }
        return maxCombo;
    }

    public void setMaxCombo(int maxCombo) {
        this.maxCombo = maxCombo;
    }

    public int getNotes() {
        return notes;
    }

    public void setNotes(int notes) {
        this.notes = notes;
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

    public boolean isPerfect() {
        return perfect;
    }

    public void setPerfect(boolean perfect) {
        this.perfect = perfect;
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

    public ModHashSet getMod() {
        return mod;
    }

    public void setMod(final ModHashSet mod) {
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

    public void setBeatmap(String beatmapSetDirectory, String beatmapFilename) {
        this.beatmapSetDirectory = beatmapSetDirectory;
        this.beatmapFilename = beatmapFilename;
    }

    public final boolean isScoreValid() {
        return SecurityUtils.getHigh16Bits(totalScore) == scoreHash;
    }

    public String compile() {
        StringBuilder builder = new StringBuilder();
        String mstring = mod.toString();
        if (mstring.length() == 0)
            mstring = "-";
        builder.append(mstring);
        builder.append(' ');
        builder.append(getTotalScoreWithMultiplier());
        builder.append(' ');
        builder.append(getMaxCombo());
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
        builder.append(getAccuracy());
        builder.append(' ');
        builder.append(getTime());
        builder.append(' ');
        builder.append(isPerfect() ? 1 : 0);
        builder.append(' ');
        builder.append(getPlayerName());
        return builder.toString();
    }

    public void setMaxObjectsCount(int count){
        maxObjectsCount = count;
    }
    public void setMaxHighestCombo(int count){
        maxHighestCombo = count;
    }

    public double getUnstableRate() {
        return unstableRate;
    }

    public void addHitOffset(double accuracy) {
        double msAccuracy = accuracy * 1000;

        // Update hit offset
        if (accuracy >= 0) {
            positiveHitOffsetSum += msAccuracy;
            positiveTotalOffsetSum++;
        } else {
            negativeHitOffsetSum += msAccuracy;
            negativeTotalOffsetSum++;
        }

        // Update unstable rate
        // Reference: https://math.stackexchange.com/questions/775391/can-i-calculate-the-new-standard-deviation-when-adding-a-value-without-knowing-t
        int totalOffsetSum = positiveTotalOffsetSum + negativeTotalOffsetSum;
        double hitOffsetSum = positiveHitOffsetSum + negativeHitOffsetSum;

        if (totalOffsetSum > 1) {
            double avgOffset = hitOffsetSum / totalOffsetSum;

            unstableRate = 10 * Math.sqrt(
                ((totalOffsetSum - 1) * Math.pow(unstableRate / 10, 2) +
                    (msAccuracy - avgOffset / totalOffsetSum) * (msAccuracy - (avgOffset - msAccuracy) / (totalOffsetSum - 1))) / totalOffsetSum
            );
        }
    }

    public double getNegativeHitError() {
        return negativeTotalOffsetSum == 0 ? 0 : negativeHitOffsetSum / negativeTotalOffsetSum;
    }

    public double getPositiveHitError() {
        return positiveTotalOffsetSum == 0 ? 0 : positiveHitOffsetSum / positiveTotalOffsetSum;
    }

    /**
     * Converts the statistic into a JSONObject readable by the multiplayer server.
     */
    public JSONObject toJson(){
        return new JSONObject() {{
            try {
                put("accuracy", getAccuracyForServer());
                put("score", getTotalScoreWithMultiplier());
                put("username", playerName);
                put("modstring", mod.toString());
                put("maxCombo", maxCombo);
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
        var combo = !Multiplayer.isConnected() || Multiplayer.room.getWinCondition() != WinCondition.MAX_COMBO ? currentCombo : maxCombo;

        return new ScoreBoardItem(playerName, getTotalScoreWithMultiplier(), combo, getAccuracyForServer(), isAlive);
    }

    /**
     * Converts the statistic to a ScoreInfo.
     */
    public ScoreInfo toScoreInfo() {
        return new ScoreInfo(
            beatmapFilename,
            beatmapSetDirectory,
            playerName,
            replayFilename,
            mod.toString(),
            getTotalScoreWithMultiplier(),
            maxCombo,
            getMark(),
            hit300k,
            hit300,
            hit100k,
            hit100,
            hit50,
            misses,
            getAccuracy(),
            time,
            isPerfect()
        );
    }

    public void calculateModScoreMultiplier(final BeatmapDifficulty originalDifficulty) {
        modScoreMultiplier = 1;

        for (var m : mod) {
            modScoreMultiplier *= m.calculateScoreMultiplier(originalDifficulty);
        }
    }

    public void migrateLegacyMods(final BeatmapDifficulty originalDifficulty) {
        for (var m : mod) {
            if (m instanceof ILegacyMod legacyMod) {
                mod.remove(m);
                mod.add(legacyMod.migrate(originalDifficulty));
            }
        }
    }

    /**
     * Whether the statistic corresponds to a team.
     */
    public boolean isTeamStatistic() {
        return Multiplayer.isConnected() && (playerName.equals(RoomTeam.RED.toString()) || playerName.equals(RoomTeam.BLUE.toString()));
    }
}
