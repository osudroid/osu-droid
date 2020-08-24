package ru.nsu.ccfit.zuev.osu.helper;

import android.graphics.PointF;
import android.util.Log;

import org.anddev.andengine.util.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.BeatmapData;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.OSUParser;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameObjectData;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;
import test.tpdifficulty.TimingPoint;
import test.tpdifficulty.hitobject.HitCircle;
import test.tpdifficulty.hitobject.HitObject;
import test.tpdifficulty.hitobject.HitObjectType;
import test.tpdifficulty.hitobject.Slider;
import test.tpdifficulty.hitobject.SliderType;
import test.tpdifficulty.hitobject.Spinner;
import test.tpdifficulty.tp.AiModtpDifficulty;

public class DifficultyReCalculator {
    private ArrayList<TimingPoint> timingPoints;
    private ArrayList<HitObject> hitObjects;
    private TimingPoint currentTimingPoint;
    private int tpIndex = 0;
    private double total, aim, speed, acc;
    private AiModtpDifficulty tpDifficulty;
    //copy from OSUParser.java
    private boolean init(final TrackInfo track, float speedmulti){
        tpIndex = 0;
        OSUParser parser = new OSUParser(track.getFilename());
        final BeatmapData data;
        if (parser.openFile()) {
            data = parser.readData();
        } else {
            Debug.e("startGame: cannot open file");
            ToastLogger.showText(
                    StringTable.format(R.string.message_error_open,
                            track.getFilename()), true);
            return false;
        }
        //float sliderTick = parser.tryParse(data.getData("Difficulty", "SliderTickRate"), 1.0f);
        float sliderSpeed = parser.tryParse(data.getData("Difficulty", "SliderMultiplier"), 1.0f);
        parser = null;
        //get first no inherited timingpoint
        for (final String tempString : data.getData("TimingPoints")) {
            if (timingPoints == null) {
                timingPoints = new ArrayList<TimingPoint>();
            }
            String[] tmpdata = tempString.split("[,]");
            if(Float.parseFloat(tmpdata[1]) > 0){
                float offset = Float.parseFloat(tmpdata[0]);
                float bpm = 60000.0f / Float.parseFloat(tmpdata[1]);
                float speed = 1.0f;
                TimingPoint timing = new TimingPoint(bpm, offset, speed);
                currentTimingPoint = timing;
                break;
            }
        }
        //load all timingpoint
        for (final String tempString : data.getData("TimingPoints")) {
            String[] tmpdata = tempString.split("[,]");
            float offset = Float.parseFloat(tmpdata[0]);
            float bpm = Float.parseFloat(tmpdata[1]);
            float speed = 1.0f;
            boolean inherited = false;
            if (bpm < 0) {
                inherited = true;
                speed = -100.0f / bpm;
                bpm = currentTimingPoint.getBpm();
            } else {
                bpm = 60000.0f / bpm;
            }
            TimingPoint timing = new TimingPoint(bpm, offset, speed);
            if (!inherited) currentTimingPoint = timing;
            try {
                bpm = GameHelper.Round(bpm, 2);
            } catch (NumberFormatException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
                Log.e("Beatmap Error", "" + track.getMode());
                ToastLogger.showText(StringTable.get(R.string.osu_parser_error) + " " + track.getMode(), true);
                return false;
            }
            if (timingPoints == null) return false;
            timingPoints.add(timing);
        }
        if (GlobalManager.getInstance().getSongMenu().getSelectedTrack() != track){
            return false;
        }
        final ArrayList<String> hitObjectss = data.getData("HitObjects");
        if (hitObjectss.size() <= 0) {
            return false;
        }
        for (final String tempString : hitObjectss) {
            if (hitObjects == null) {
                hitObjects = new ArrayList<HitObject>();
                tpIndex = 0;
                currentTimingPoint = timingPoints.get(tpIndex);
            }
            String[] data1 = tempString.split("[,]");
            String[] rawdata = null;
            //Ignoring v10 features
            int dataSize = data1.length;
            while (dataSize > 0 && data1[dataSize - 1].matches("([0-9][:][0-9][|]?)+")) {
                dataSize--;
            }
            if (dataSize < data1.length) {
                rawdata = new String[dataSize];
                for (int i = 0; i < rawdata.length; i++) {
                    rawdata[i] = data1[i];
                }
            } else
                rawdata = data1;

            int time = Integer.parseInt(rawdata[2]);
            while (tpIndex < timingPoints.size() - 1 && timingPoints.get(tpIndex + 1).getOffset() <= time) {
                tpIndex += 1;
            }
            currentTimingPoint = timingPoints.get(tpIndex);
            HitObjectType hitObjectType = HitObjectType.valueOf(Integer.parseInt(rawdata[3]) % 16);
            PointF pos = new PointF(Float.parseFloat(rawdata[0]), Float.parseFloat(rawdata[1]));
            HitObject object = null;
            if (hitObjectType == null) {
                System.out.println(tempString);
                continue;
            }
            if (hitObjectType == HitObjectType.Normal || hitObjectType == HitObjectType.NormalNewCombo) { // hitcircle
                object = new HitCircle((int)(time / speedmulti), pos, currentTimingPoint);
            } else if (hitObjectType == HitObjectType.Spinner) { // spinner
                int endTime = Integer.parseInt(rawdata[5]);
                object = new Spinner((int)(time / speedmulti), (int)(endTime / speedmulti), pos, currentTimingPoint);
            } else if (hitObjectType == HitObjectType.Slider || hitObjectType == HitObjectType.SliderNewCombo) { // slider
                String data2[] = rawdata[5].split("[|]");
                SliderType sliderType = SliderType.parse(data2[0].charAt(0));
                ArrayList<PointF> poss = new ArrayList<PointF>();
                for (int i = 1; i < data2.length; i++) {
                    String temp[] = data2[i].split("[:]");
                    poss.add(new PointF(Float.parseFloat(temp[0]), Float.parseFloat(temp[1])));
                }
                int repeat = Integer.parseInt(rawdata[6]);
                float rawLength = Float.parseFloat(rawdata[7]);
                int endTime = time + (int) (rawLength * (600 / currentTimingPoint.getBpm()) / sliderSpeed) * repeat;
                object = new Slider((int)(time / speedmulti), (int)(endTime / speedmulti), pos, currentTimingPoint, sliderType, repeat, poss, rawLength);
            }
            if (hitObjects == null) return false;
            hitObjects.add(object);
        }
        return true;
    }
    public float reCalculateStar(final TrackInfo track, float speedmulti, float cs){
        if (init(track, speedmulti) == false) {
            return 0f;
        }
        if (GlobalManager.getInstance().getSongMenu().getSelectedTrack() != track){
            return 0f;
        }
        try {
            tpDifficulty = new AiModtpDifficulty();
            tpDifficulty.CalculateAll(hitObjects, cs);
            double star = tpDifficulty.getStarRating();
            if (!timingPoints.isEmpty()){
                timingPoints.clear();
                timingPoints = null;
            }
            if (!hitObjects.isEmpty()){
                hitObjects.clear();
                hitObjects = null;
            }
            if (GlobalManager.getInstance().getSongMenu().getSelectedTrack() != track){
                return 0f;
            }
            return GameHelper.Round(star, 2);
        } catch (Exception e) {
            return 0f;
        }
    }
    public void calculaterPP(final StatisticV2 stat, final TrackInfo track){
        pp(tpDifficulty.getAimStars(),tpDifficulty.getSpeedStars(),track.getMaxCombo(),track.getHitCircleCount(),
            track.getTotalHitObjectCount(),getAR(stat,track),getOD(stat,track),stat.getMod(),
            stat.getMaxCombo(),stat.getAccuracy(),stat.getMisses());
    }
    public void calculaterMaxPP(final StatisticV2 stat, final TrackInfo track){
        pp(tpDifficulty.getAimStars(),tpDifficulty.getSpeedStars(),track.getMaxCombo(),track.getHitCircleCount(),
            track.getTotalHitObjectCount(),getAR(stat,track),getOD(stat,track),stat.getMod(),
            track.getMaxCombo(),1f,0);
    }
    //copy from koohii.java
    private double pp_base(double stars)
    {
        return Math.pow(5.0 * Math.max(1.0, stars / 0.0675) - 4.0, 3.0)
            / 100000.0;
    }
    //copy from koohii.java
    private void pp(double aim_stars, double speed_stars,
                            int max_combo, int ncircles, int nobjects,
                            float base_ar, float base_od, EnumSet<GameMod> mods,
                            int combo, float accuracy, int nmiss){
        /* global values --------------------------------------- */
        double nobjects_over_2k = nobjects / 2000.0;

        double length_bonus = 0.95 + 0.4 *
            Math.min(1.0, nobjects_over_2k);

        if (nobjects > 2000) {
            length_bonus += Math.log10(nobjects_over_2k) * 0.5;
        }

        double miss_penality = Math.pow(0.97, nmiss);
        double combo_break = Math.pow(combo, 0.8) /
            Math.pow(max_combo, 0.8);
        if (combo > max_combo){
            combo_break = 1.0;
        }
        /* ar bonus -------------------------------------------- */
        double ar_bonus = 1.0;

        if (base_ar > 10.33) {
            ar_bonus += 0.3 * (base_ar - 10.33);
        }

        else if (base_ar < 8.0) {
            ar_bonus +=  0.01 * (8.0 - base_ar);
        }
        
        /* aim pp ---------------------------------------------- */
        aim = pp_base(aim_stars);
        aim *= length_bonus;
        aim *= miss_penality;
        aim *= combo_break;
        aim *= ar_bonus;

        double hd_bonus = 1.0;
        if (mods.contains(GameMod.MOD_HIDDEN)) {
            hd_bonus *= 1.0 + 0.04 * (12.0 - base_ar);
        }
        aim *= hd_bonus;

        if (mods.contains(GameMod.MOD_FLASHLIGHT)) {
            double fl_bonus = 1.0 + 0.35 * Math.min(1.0, nobjects / 200.0);
            if (nobjects > 200) {
                fl_bonus += 0.3 * Math.min(1.0, (nobjects - 200) / 300.0);
            }
            if (nobjects > 500) {
                fl_bonus += (nobjects - 500) / 1200.0;
            }
            aim *= fl_bonus;
        }

        double acc_bonus = 0.5 + accuracy / 2.0;
        double od_squared = base_od * base_od;
        double od_bonus = 0.98 + od_squared / 2500.0;

        aim *= acc_bonus;
        aim *= od_bonus;
        if (mods.contains(GameMod.MOD_AUTOPILOT)) {
            aim *= 0;
        }
        /* speed pp -------------------------------------------- */
        speed = pp_base(speed_stars);
        speed *= length_bonus;
        speed *= miss_penality;
        speed *= combo_break;
        if (base_ar > 10.33) {
            speed *= ar_bonus;
        }
        speed *= hd_bonus;

        /* similar to aim acc and od bonus */
        speed *= 0.02 + accuracy;
        speed *= 0.96 + od_squared / 1600.0;
        if (mods.contains(GameMod.MOD_RELAX)) {
            speed *= 0;
        }
        /* acc pp ---------------------------------------------- */
        acc = Math.pow(1.52163, base_od) *
            Math.pow(accuracy, 24.0) * 2.83;

        acc *= Math.min(1.15, Math.pow(ncircles / 1000.0, 0.3));

        if (mods.contains(GameMod.MOD_HIDDEN)) {
            acc *= 1.08;
        }

        if (mods.contains(GameMod.MOD_FLASHLIGHT)) {
            acc *= 1.02;
        }

        if (mods.contains(GameMod.MOD_RELAX)) {
            acc *= 0.1;
        }
        /* total pp -------------------------------------------- */
        double final_multiplier = 1.12;

        if (mods.contains(GameMod.MOD_NOFAIL)){
            final_multiplier *= 0.90;
        }

        //if ((mods & MODS_SO) != 0) {
        //    final_multiplier *= 0.95;
        //}

        total = Math.pow(
            Math.pow(aim, 1.1) + Math.pow(speed, 1.1) +
            Math.pow(acc, 1.1),
            1.0 / 1.1
        ) * final_multiplier;
    }
    public double getTotalPP(){
        return total;
    }
    public double getAimPP(){
        return aim;
    }
    public double getSpdPP(){
        return speed;
    }
    public double getAccPP(){
        return acc;
    }
    private float getAR(final StatisticV2 stat, final TrackInfo track){
        float ar = track.getApproachRate();
        float od = track.getOverallDifficulty();
        float cs = track.getCircleSize();
        float hp = track.getHpDrain();
        EnumSet<GameMod> mod = stat.getMod();
        float bpm_max = track.getBpmMax();
        float bpm_min = track.getBpmMin();
        long length = track.getMusicLength();
        if (mod.contains(GameMod.MOD_EASY)) {
            ar *= 0.5f;
            od *= 0.5f;
            cs -= 1f;
            hp *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            ar *= 1.4f;
            if(ar > 10) {
                ar = 10;
            }
            od *= 1.4f;
            cs += 1f;
            hp *= 1.4f;
        }
        float speed = stat.getSpeed();
        bpm_max *= speed;
        bpm_min *= speed;
        length /= speed;
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            if (mod.contains(GameMod.MOD_EASY)){
                ar *= 2f;
                ar -= 0.5f;
            }
            ar -= 0.5f;
            ar -= speed - 1.0f;
            od *= 0.5f;
            cs -= 1f;
            hp *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_SMALLCIRCLE)) {
            cs += 4f;
        }
        ar = Math.min(13.f, ar);
        od = Math.min(10.f, od);
        cs = Math.min(15.f, cs);
        hp = Math.min(10.f, hp);
        ar = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(ar) / speed), 2);
        od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) / speed), 2);
        if (stat.isEnableForceAR()){
            ar = stat.getForceAR();
        }
        return ar;
    }
    private float getOD(final StatisticV2 stat, final TrackInfo track){
        float od = track.getOverallDifficulty();
        EnumSet<GameMod> mod = stat.getMod();
        if (mod.contains(GameMod.MOD_EASY)) {
            od *= 0.5f;
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            od *= 1.4f;
        }
        float speed = stat.getSpeed();
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            od *= 0.5f;
        }
        od = Math.min(10.f, od);
        od = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(od) / speed), 2);
        return od;
    }
    public float getCS(EnumSet<GameMod> mod, final TrackInfo track){
        float cs = track.getCircleSize();
        if (mod.contains(GameMod.MOD_EASY)) {
            cs -= 1f;
        }
        if (mod.contains(GameMod.MOD_HARDROCK)) {
            cs += 1f;
        }
        if (mod.contains(GameMod.MOD_REALLYEASY)) {
            cs -= 1f;
        }
        if (mod.contains(GameMod.MOD_SMALLCIRCLE)) {
            cs += 4f;
        }
        return cs;
    }
    public float getCS(final StatisticV2 stat, final TrackInfo track){
        return getCS(stat.getMod(), track);
    }
    public float getCS(final TrackInfo track){
        return getCS(ModMenu.getInstance().getMod(), track);
    }
}