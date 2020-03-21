package ru.nsu.ccfit.zuev.osu.scoring;

import com.dgsrz.bancho.security.SecurityUtils;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Random;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
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
        if (mod.contains(GameMod.MOD_AUTO)) {
            mult *= 0;
        }
        if (mod.contains(GameMod.MOD_RELAX)) {
            mult *= 0;
        }
        if (mod.contains(GameMod.MOD_AUTOPILOT)) {
            mult *= 0;
        }
        if (mod.contains(GameMod.MOD_EASY)) {
            mult *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_NOFAIL)) {
            mult *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            mult *= 1.06f;
        }
        if (mod.contains(GameMod.MOD_HIDDEN)) {
            mult *= 1.06f;
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            mult *= 1.12f;
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            mult *= 1.12f;
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            mult *= 0.3f;
        }
        return (int) (totalScore * mult);
    }

    public int getAutoTotalScore() {
        float mult = 1;
        if (mod.contains(GameMod.MOD_EASY)) {
            mult *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_NOFAIL)) {
            mult *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            mult *= 1.06f;
        }
        if (mod.contains(GameMod.MOD_HIDDEN)) {
            mult *= 1.06f;
        }
        if (mod.contains(GameMod.MOD_DOUBLETIME)) {
            mult *= 1.12f;
        }
        if (mod.contains(GameMod.MOD_NIGHTCORE)) {
            mult *= 1.12f;
        }
        if (mod.contains(GameMod.MOD_HALFTIME)) {
            mult *= 0.3f;
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
        totalScore += amount;
        if (combo) {
            totalScore += (amount * currentCombo * diffModifier) / 25;
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
        return s;
    }

    public void setModFromString(String s) {
        mod = EnumSet.noneOf(GameMod.class);
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
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
            }
        }
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
}
