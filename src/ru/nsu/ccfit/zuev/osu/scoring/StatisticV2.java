package ru.nsu.ccfit.zuev.osu.scoring;

import com.dgsrz.bancho.security.SecurityUtils;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Random;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

public class StatisticV2 implements Serializable {
    private static final long serialVersionUID = 8339570462000129479L;
    int hit300 = 0, hit100 = 0, hit50 = 0;
    int hit300k = 0, hit100k = 0;
    int misses = 0;
    int maxCombo = 0;
    float accuracy = -1;
    long time = 0;
    private Random random;
    private int notes = 0;
    private boolean perfect = false;
    private int currentCombo = 0;
    private int scoreHash = 0;
    private int totalScore;
    private int possibleScore = 0;
    private int realScore = 0;
    private float hp = 1;
    private float diffModifier = 1;
    private EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);
    private String playerName = "";
    private String fileName = "";
    private String replayName = "";
    private int forcedScore = -1;
    private String mark = null;
    private float changeSpeed = 1.0f;
    private float forceAR = 9.0f;
    private boolean enableForceAR = false;
    private final int MAX_SCORE = 1000000;
    private final float ACC_PORTION = 0.3f;
    private final float COMBO_PORTION = 0.7f;
    private int maxObjectsCount = 0;
    private int maxHighestCombo = 0;
    private int bonusScore = 0;
    private float flFollowDelay = FlashLightEntity.defaultMoveDelayS;
    private int positiveTotalOffsetSum;
    private float positiveHitOffsetSum;
    private int negativeTotalOffsetSum;
    private float negativeHitOffsetSum;
    private float unstableRate;

    public StatisticV2() {
        random = new Random();
        playerName = null;
        if (Config.isStayOnline()) {
            playerName = OnlineManager.getInstance().getUsername();
            if (playerName == null || playerName.length() == 0)
                playerName = Config.getOnlineUsername();
        }

        if (playerName == null || playerName.length() == 0)
            playerName = Config.getLocalUsername();
    }

    public StatisticV2(final Statistic stat) {
        notes = stat.notes;
        hit300 = stat.hit300;
        hit100 = stat.hit100;
        hit50 = stat.hit50;
        hit300k = stat.hit300k;
        hit100k = stat.hit100k;
        misses = stat.misses;
        maxCombo = stat.maxCombo;
        currentCombo = stat.currentCombo;
        totalScore = stat.totalScore;
        possibleScore = stat.possibleScore;
        realScore = stat.realScore;
        hp = stat.hp;
        diffModifier = stat.diffModifier;
        mod = stat.mod.clone();
        setPlayerName(Config.getLocalUsername());
    }

    public StatisticV2(final String[] params) {
        playerName = "";
        if (params.length < 6) return;

        setModFromString(params[0]);
        setForcedScore(Integer.parseInt(params[1]));
        maxCombo = Integer.parseInt(params[2]);
        mark = params[3];
        hit300k = Integer.parseInt(params[4]);
        hit300 = Integer.parseInt(params[5]);
        hit100k = Integer.parseInt(params[6]);
        hit100 = Integer.parseInt(params[7]);
        hit50 = Integer.parseInt(params[8]);
        misses = Integer.parseInt(params[9]);
        accuracy = Integer.parseInt(params[10]) / 100000f;
        if (params.length >= 12) {
            time = Long.parseLong(params[11]);
        }
        if (params.length >= 13) {
            perfect = Integer.parseInt(params[12]) != 0;
        }
        if (params.length >= 14) {
            playerName = params[13];
        }
    }

    public float getHp() {
        return hp;
    }

    public void changeHp(final float amount) {
        hp += amount;
        if (hp < 0) {
            hp = 0;
        }
        if (hp > 1) {
            hp = 1;
        }
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getModifiedTotalScore() {
        if (forcedScore > 0)
            return forcedScore;
        float mult = 1;
        for (GameMod m : mod) {
            mult *= m.scoreMultiplier;
        }
        if (changeSpeed != 1.0f){
            mult *= getSpeedChangeScoreMultiplier();
        }
        return (int) (totalScore * mult);
    }

    public int getAutoTotalScore() {
        float mult = 1;
        for (GameMod m : mod) {
            if (m.unranked) {
                continue;
            }
            mult *= m.scoreMultiplier;
        }
        if (changeSpeed != 1.0f){
            mult *= getSpeedChangeScoreMultiplier();
        }
        return (int) (totalScore * mult);
    }

    public void registerSpinnerHit() {
        addScore(100, false);
    }

    public void registerHit(final int score, final boolean k, final boolean g) {
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
            changeHp(-(5 + GameHelper.getDrain()) / 100f);
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
                currentCombo++;
                break;
            case 100:
                changeHp(k ? 0.15f : 0.05f);
                if (k) {
                    hit100k++;
                }
                hit100++;
                addScore(100, true);
                realScore += 100;
                currentCombo++;
                break;
            case 50:
                changeHp(0.05f);
                hit50++;
                addScore(50, true);
                realScore += 50;
                currentCombo++;
                break;
            default:
                changeHp(-(5 + GameHelper.getDrain()) / 100f);
                misses++;
                perfect = false;
                if (currentCombo > maxCombo) {
                    maxCombo = currentCombo;
                }
                currentCombo = 0;
                break;
        }
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
        if (mod.contains(GameMod.MOD_SCOREV2)){
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
        boolean isH = false;
        forcycle:
        for (final GameMod m : mod) {
            switch (m) {
                case MOD_HIDDEN:
                    isH = true;
                    break forcycle;
                case MOD_FLASHLIGHT:
                    isH = true;
                    break forcycle;
                default:
                    break;
            }
        }

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
        if (mod.contains(GameMod.MOD_SMALLCIRCLE)) {
            s += "m";
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
                case 'm':
                    mod.add(GameMod.MOD_SMALLCIRCLE);
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
    }

    public String getReplayName() {
        return replayName;
    }

    public void setReplayName(String replayName) {
        this.replayName = replayName;
    }

    public void setForcedScore(int forcedScore) {
        this.forcedScore = forcedScore;
        totalScore = forcedScore;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public final boolean isScoreValid() {
        return SecurityUtils.getHigh16Bits(totalScore) == scoreHash;
    }

    public String compile() {
        StringBuilder builder = new StringBuilder();
        String mstring = getModString();
        if (mstring.length() == 0)
            mstring = "-";
        builder.append(mstring);
        builder.append(' ');
        builder.append(getModifiedTotalScore());
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
        builder.append((int) (getAccuracy() * 100000f));
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

    public float getChangeSpeed(){
        return changeSpeed;
    }

    public void setChangeSpeed(float speed){
        changeSpeed = speed;
    }

    public float getForceAR(){
        return forceAR;
    }

    public void setForceAR(float ar){
        forceAR = ar;
    }

    public boolean isEnableForceAR(){
        return enableForceAR;
    }

    public void setFLFollowDelay(float delay) {
        flFollowDelay = delay;
    }

    public float getFLFollowDelay() {
        return flFollowDelay;
    }

    public void setEnableForceAR(boolean t){
        enableForceAR = t;
    }

    public float getUnstableRate() {
        return unstableRate;
    }

    public void addHitOffset(float accuracy) {
        float msAccuracy = accuracy * 1000;

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
        float hitOffsetSum = positiveHitOffsetSum + negativeHitOffsetSum;

        if (totalOffsetSum > 1) {
            float avgOffset = hitOffsetSum / totalOffsetSum;

            unstableRate = 10 * (float) Math.sqrt(
                ((totalOffsetSum - 1) * Math.pow(unstableRate / 10, 2) +
                    (msAccuracy - avgOffset / totalOffsetSum) * (msAccuracy - (avgOffset - msAccuracy) / (totalOffsetSum - 1))) / totalOffsetSum
            );
        }
    }
    
    public float getNegativeHitError() {
        return negativeTotalOffsetSum == 0 ? 0 : negativeHitOffsetSum / negativeTotalOffsetSum;
    }
    
    public float getPositiveHitError() {
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
        } else if (multi == 1){
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
        if (enableForceAR){
            builder.append(String.format(Locale.ENGLISH, "AR%.1f|", forceAR));
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
                enableForceAR = true;
                forceAR = Float.parseFloat(str.substring(2));
            }
            if (str.startsWith("FLD")) {
                flFollowDelay = Float.parseFloat(str.substring(3));
            }
        }
    }
}
