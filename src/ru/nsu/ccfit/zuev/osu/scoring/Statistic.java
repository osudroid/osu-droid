package ru.nsu.ccfit.zuev.osu.scoring;

import java.io.Serializable;
import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public class Statistic implements Serializable {
    private static final long serialVersionUID = 8339570462000129479L;

    int notes = 0;
    int hit300 = 0, hit100 = 0, hit50 = 0;
    int hit300k = 0, hit100k = 0;
    int misses = 0;
    int maxCombo = 0;
    int currentCombo = 0;
    int totalScore;
    int possibleScore = 0;
    int realScore = 0;
    float hp = 1;
    float diffModifier = 1;
    EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);

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
        float mult = 1;
        if (mod.contains(GameMod.MOD_AUTO)) {
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
        if (mod.contains(GameMod.MOD_FLASHLIGHT)) {
            mult *= 1.12f;
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
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            mult *= 0.4f;
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
        if (mod.contains(GameMod.MOD_FLASHLIGHT)) {
            mult *= 1.12f;
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
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            mult *= 0.4f;
        }
        return (int) (totalScore * mult);
    }

    public void registerSpinnerHit() {
        totalScore += 100;
    }

    public void registerHit(final int score, final boolean k, final boolean g) {
        if (score == 1000) {
            totalScore += score;
            return;
        }
        if (score < 50 && score > 0) {
            changeHp(0.05f);
            totalScore += score;
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
                addScore(300);
                realScore += 300;
                currentCombo++;
                break;
            case 100:
                changeHp(k ? 0.15f : 0.05f);
                if (k) {
                    hit100k++;
                }

                hit100++;
                addScore(100);
                realScore += 100;
                currentCombo++;
                break;
            case 50:
                changeHp(0.05f);
                hit50++;
                addScore(50);
                realScore += 50;
                currentCombo++;
                break;
            default:
                changeHp(-(5 + GameHelper.getDrain()) / 100f);
                misses++;
                if (currentCombo > maxCombo) {
                    maxCombo = currentCombo;
                }
                currentCombo = 0;
                break;
        }
    }

    public float getAccuracy() {
        if (possibleScore == 0) {
            return 0;
        }
        return realScore / (float) possibleScore;
    }

    public void addScore(final int amount) {
        totalScore += amount + (amount * currentCombo * diffModifier) / 25;
    }

    public String getMark() {
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

        if (hit100 == 0 && hit100k == 0 && hit50 == 0 && misses == 0) {
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

    public int getMaxCombo() {
        if (currentCombo > maxCombo) {
            maxCombo = currentCombo;
        }
        return maxCombo;
    }

    public int getNotes() {
        return notes;
    }

    public int getHit300() {
        return hit300;
    }

    public int getHit100() {
        return hit100;
    }

    public int getHit50() {
        return hit50;
    }

    public int getHit300k() {
        return hit300k;
    }

    public int getHit100k() {
        return hit100k;
    }

    public int getMisses() {
        return misses;
    }

    public int getCombo() {
        return currentCombo;
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
}
