package ru.nsu.ccfit.zuev.osu.scoring;

import ru.nsu.ccfit.zuev.osu.SecurityUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Random;

import com.reco1l.ibancho.data.RoomTeam;
import com.reco1l.ibancho.data.WinCondition;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.ScoreInfo;
import com.reco1l.osu.multiplayer.Multiplayer;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem;

public class StatisticV2 implements Serializable {
    @Serial
    private static final long serialVersionUID = 8339570462000129479L;
    private static final Random random = new Random();
    private static final int scoreV2MaxScore = 1000000;
    private static final float scoreV2AccPortion = 0.3f;
    private static final float scoreV2ComboPortion = 0.7f;

    int hit300 = 0, hit100 = 0, hit50 = 0;
    int hit300k = 0, hit100k = 0;
    int misses = 0;
    int scoreMaxCombo = 0;
    long time = 0;
    private int currentCombo = 0;
    private int scoreHash = 0;
    private int totalScore;
    private float hp = 1;
    private float diffModifier = 1;
    private EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);
    private String playerName = Config.getOnlineUsername();
    private String replayFilename = "";
    private int forcedScore = -1;
    private String mark = null;
    private float changeSpeed = 1.0f;
    private int beatmapNoteCount = 0;
    private int beatmapMaxCombo = 0;
    private int bonusScore = 0;
    private float flFollowDelay = FlashLightEntity.defaultMoveDelayS;
    private int positiveTotalOffsetSum;
    private double positiveHitOffsetSum;
    private int negativeTotalOffsetSum;
    private double negativeHitOffsetSum;
    private double unstableRate;

    private Float beatmapCS;
    private Float beatmapOD;

    private Float customAR;
    private Float customOD;
    private Float customCS;
    private Float customHP;
    private int life = 1;

    // Used to indicate that the score was done before version 1.6.8. Used in difficulty calculation.
    private boolean isOldScore;

    private boolean isLegacySC = false;

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


    public StatisticV2() {}

    public StatisticV2(final String[] params) {
        playerName = "";
        if (params.length < 6) return;

        setModFromString(params[0]);
        isOldScore = !params[0].contains("|");
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
        computeModScoreMultiplier();
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
            if (currentCombo == currentMaxCombo) {
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
        boolean isH = false;

        forcycle:
        for (final GameMod m : mod) {
            switch (m) {
                case MOD_HIDDEN:
                case MOD_FLASHLIGHT:
                    isH = true;
                    break forcycle;
                default:
                    break;
            }
        }

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

    public EnumSet<GameMod> getMod() {
        return mod;
    }

    public void setMod(final EnumSet<GameMod> mod) {
        this.mod = mod.clone();

        computeModScoreMultiplier();
    }

    public boolean isOldScore() {
        return isOldScore;
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

    public String getModString() {
        String s = "";

        if (mod.contains(GameMod.MOD_AUTO)) {
            s += "a";
        }
        if (mod.contains(GameMod.MOD_RELAX)) {
            s += "x";
        }
        if (mod.contains(GameMod.MOD_AUTOPILOT)) {
            s += "p";
        }
        if (mod.contains(GameMod.MOD_EASY)) {
            s += "e";
        }
        if (mod.contains(GameMod.MOD_NOFAIL)) {
            s += "n";
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            s += "r";
        }
        if (mod.contains(GameMod.MOD_HIDDEN)) {
            s += "h";
        }
        if (mod.contains(GameMod.MOD_FLASHLIGHT)) {
            s += "i";
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            s += "d";
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            s += "c";
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            s += "t";
        }
        if (mod.contains(GameMod.MOD_PRECISE)) {
            s += "s";
        }
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            s += "l";
        }
        if (mod.contains(GameMod.MOD_PERFECT)) {
            s += "f";
        }
        if (mod.contains(GameMod.MOD_SUDDENDEATH)) {
            s += "u";
        }
        if (mod.contains(GameMod.MOD_SCOREV2)) {
            s += "v";
        }
        s += "|";
        s += getExtraModString();
        return s;
    }

    public void setModFromString(String s) {
        String[] strMod = s.split("\\|", 2);
        mod = EnumSet.noneOf(GameMod.class);
        for (int i = 0; i < strMod[0].length(); i++) {
            switch (strMod[0].charAt(i)) {
                case 'a':
                    mod.add(GameMod.MOD_AUTO);
                    break;
                case 'x':
                    mod.add(GameMod.MOD_RELAX);
                    break;
                case 'p':
                    mod.add(GameMod.MOD_AUTOPILOT);
                    break;
                case 'e':
                    mod.add(GameMod.MOD_EASY);
                    life = 3;
                    break;
                case 'n':
                    mod.add(GameMod.MOD_NOFAIL);
                    break;
                case 'r':
                    mod.add(GameMod.MOD_HARDROCK);
                    break;
                case 'h':
                    mod.add(GameMod.MOD_HIDDEN);
                    break;
                case 'i':
                    mod.add(GameMod.MOD_FLASHLIGHT);
                    break;
                case 'd':
                    mod.add(GameMod.MOD_DOUBLETIME);
                    break;
                case 'c':
                    mod.add(GameMod.MOD_NIGHTCORE);
                    break;
                case 't':
                    mod.add(GameMod.MOD_HALFTIME);
                    break;
                case 's':
                    mod.add(GameMod.MOD_PRECISE);
                    break;
                // Special handle for old removed SmallCircles mod.
                case 'm':
                    isLegacySC = true;
                    break;
                case 'l':
                    mod.add(GameMod.MOD_REALLYEASY);
                    break;    
                case 'u':
                    mod.add(GameMod.MOD_SUDDENDEATH);
                    break;    
                case 'f':
                    mod.add(GameMod.MOD_PERFECT);
                    break;
                case 'v':
                    mod.add(GameMod.MOD_SCOREV2);
                    break;   
            }
        }
        if (strMod.length > 1)
            setExtraModFromString(strMod[1]);

        computeModScoreMultiplier();
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
        String mstring = getModString();
        if (mstring.isEmpty())
            mstring = "-";
        builder.append(mstring);
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

    public float getChangeSpeed(){
        return changeSpeed;
    }

    public void setChangeSpeed(float speed){
        changeSpeed = speed;

        computeModScoreMultiplier();
    }


    public boolean isCustomAR() {
        return customAR != null;
    }

    public Float getCustomAR() {
        return customAR;
    }

    public void setCustomAR(@Nullable Float ar) {
        customAR = ar;
    }

    public boolean isCustomOD() {
        return customOD != null;
    }

    public Float getCustomOD() {
        return customOD;
    }

    public void setCustomOD(@Nullable Float customOD) {
        this.customOD = customOD;
    }


    public boolean isCustomHP() {
        return customHP != null;
    }

    public Float getCustomHP() {
        return customHP;
    }

    public void setCustomHP(@Nullable Float customHP) {
        this.customHP = customHP;
    }


    public boolean isCustomCS() {
        return customCS != null;
    }

    public Float getCustomCS() {
        return customCS;
    }

    public void setCustomCS(@Nullable Float customCS) {
        this.customCS = customCS;
    }

    public void setBeatmapCS(float beatmapCS) {
        this.beatmapCS = beatmapCS;
    }

    public void setBeatmapOD(float beatmapOD) {
        this.beatmapOD = beatmapOD;
    }


    public void setFLFollowDelay(float delay) {
        flFollowDelay = delay;
    }

    public float getFLFollowDelay() {
        return flFollowDelay;
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

    public float getSpeed(){
        float speed = changeSpeed;
        if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)){
            speed *= 1.5f;
        }
        if (mod.contains(GameMod.MOD_HALFTIME)){
            speed *= 0.75f;
        }
        return speed;
    }

    public static float getSpeedChangeScoreMultiplier(float speed, EnumSet<GameMod> mod) {
        float multi = speed;
        if (multi > 1){
            multi = 1.0f + (multi - 1.0f) * 0.24f;
        } else if (multi < 1){
            multi = (float) Math.pow(0.3, (1.0 - multi) * 4);
        } else {
            return 1f;
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME) || mod.contains(GameMod.MOD_NIGHTCORE)){
            multi /= 1.12f;
        }
        if (mod.contains(GameMod.MOD_HALFTIME)){
            multi /= 0.3f;
        }
        return multi;
    }

    private float getSpeedChangeScoreMultiplier(){
        return getSpeedChangeScoreMultiplier(getSpeed(), mod);
    }

    public String getExtraModString() {
        StringBuilder builder = new StringBuilder();
        if (changeSpeed != 1){
            builder.append(String.format(Locale.ENGLISH, "x%.2f|", changeSpeed));
        }
        if (isCustomAR()){
            builder.append(String.format(Locale.ENGLISH, "AR%.1f|", customAR));
        }
        if (isCustomOD()){
            builder.append(String.format(Locale.ENGLISH, "OD%.1f|", customOD));
        }
        if (isCustomCS()){
            builder.append(String.format(Locale.ENGLISH, "CS%.1f|", customCS));
        }
        if (isCustomHP()){
            builder.append(String.format(Locale.ENGLISH, "HP%.1f|", customHP));
        }
        if (flFollowDelay != FlashLightEntity.defaultMoveDelayS) {
            builder.append(String.format(Locale.ENGLISH, "FLD%.2f|", flFollowDelay));
        }
        if (builder.length() > 0){
            builder.delete(builder.length() - 1, builder.length());
        }

        return builder.toString();
    }

    public void setExtraModFromString(String s) {
        for (String str: s.split("\\|")){
            if (str.startsWith("x") && str.length() == 5){
                changeSpeed = Float.parseFloat(str.substring(1));
                continue;
            }
            if (str.startsWith("AR")){
                customAR = Float.parseFloat(str.substring(2));
            }
            if (str.startsWith("OD")){
                customOD = Float.parseFloat(str.substring(2));
            }
            if (str.startsWith("CS")){
                customCS = Float.parseFloat(str.substring(2));
            }
            if (str.startsWith("HP")){
                customHP = Float.parseFloat(str.substring(2));
            }
            if (str.startsWith("FLD")) {
                flFollowDelay = Float.parseFloat(str.substring(3));
            }
        }

        computeModScoreMultiplier();
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
                put("modstring", getModString());
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
            getModString(),
            getTotalScoreWithMultiplier(),
            scoreMaxCombo,
            getMark(),
            hit300k,
            hit300,
            hit100k,
            hit100,
            hit50,
            misses,
            time
        );
    }


    private void computeModScoreMultiplier() {
        modScoreMultiplier = 1;

        for (GameMod m : mod) {
            modScoreMultiplier *= m.scoreMultiplier;
        }

        if (isCustomCS() && beatmapCS != null) {
            modScoreMultiplier *= getCustomCSScoreMultiplier(beatmapCS, customCS);
        }

        if (isCustomOD() && beatmapOD != null) {
            modScoreMultiplier *= getCustomODScoreMultiplier(beatmapOD, customOD);
        }

        if (changeSpeed != 1f) {
            modScoreMultiplier *= getSpeedChangeScoreMultiplier();
        }
    }

    public static float getCustomCSScoreMultiplier(float beatmapCS, float customCS) {
        float diff = customCS - beatmapCS;

        return diff >= 0
            ? 1 + 0.0075f * (float) Math.pow(diff, 1.5)
            : 2 / (1 + (float) Math.exp(-0.5 * diff));
    }

    public static float getCustomODScoreMultiplier(float beatmapOD, float customOD) {
        float diff = customOD - beatmapOD;

        return diff >= 0
            ? 1 + 0.005f * (float) Math.pow(diff, 1.3)
            : 2 / (1 + (float) Math.exp(-0.25 * diff));
    }

    /**
     * Determines if the score has the old SC mod enabled, this will be replaced with a custom CS when replaying.
     */
    public boolean isLegacySC() {
        return isLegacySC;
    }

    /**
     * Whether the statistic corresponds to a team.
     */
    public boolean isTeamStatistic() {
        return Multiplayer.isConnected() && (playerName.equals(RoomTeam.Red.toString()) || playerName.equals(RoomTeam.Blue.toString()));
    }

    /**
     * Applies the equivalent of the old SC mod with custom CS according to the track passed.
     */
    public void processLegacySC(BeatmapInfo track) {

        var cs = track.getCircleSize();

        for (GameMod m : mod) switch (m) {

            case MOD_HARDROCK:
                ++cs;
                continue;

            case MOD_EASY:
            case MOD_REALLYEASY:
                --cs;
        }

        customCS = cs + 4;
    }
}
