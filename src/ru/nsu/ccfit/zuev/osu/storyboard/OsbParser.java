package ru.nsu.ccfit.zuev.osu.storyboard;

import com.dgsrz.bancho.ui.StoryBoardTestActivity;

import okio.BufferedSource;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

/**
 * Created by dgsrz on 16/9/16.
 */
public class OsbParser {
    public static OsbParser instance = new OsbParser();
    private LinkedList<OsuSprite> sprites = new LinkedList<OsuSprite>();
    private ArrayList<TimingPoint> timingPoints = new ArrayList<TimingPoint>();
    private ArrayList<HitSound> hitSounds = new ArrayList<HitSound>();
    private HashMap<String, String> variablesMap = new HashMap<String, String>();
    private String line;
    private String[] info;
    private float sliderMultiplier;
    private int ZIndex = -900;

    public ArrayList<TimingPoint> getTimingPoints() {
        return timingPoints;
    }

    public HashMap<String, String> getVariablesMap() {
        return variablesMap;
    }

    public LinkedList<OsuSprite> getSprites() {
        return sprites;
    }

    public ArrayList<HitSound> getHitSounds() {
        return hitSounds;
    }

    public void parse(String path) throws IOException {
        File osuFile = new File(path);
        loadBeatmap(osuFile);
        File[] files = FileUtils.listFiles(osuFile.getParentFile(), ".osb");
        if (files.length > 0) {
            BufferedSource source = Okio.buffer(Okio.source(files[0]));

            Pattern pattern;
            Matcher matcher;
            String line;
            while ((line = source.readUtf8Line()) != null) {
                pattern = Pattern.compile("\\[(\\w+)]");
                matcher = pattern.matcher(line.trim());

                if (matcher.find()) {
                    String title = matcher.group(1);
                    if (title.equals("Events")) {
                        parseObjects(source);
                    } else if (title.equals("Variables")) {
                        parseVariables(source);
                    }
                }
            }
            source.close();
        }
        Collections.sort(hitSounds, new Comparator<HitSound>() {
            @Override
            public int compare(HitSound lhs, HitSound rhs) {
                return (int) (lhs.time - rhs.time);
            }
        });
        Collections.sort(sprites, new Comparator<OsuSprite>() {
            @Override
            public int compare(OsuSprite lhs, OsuSprite rhs) {
                return (int) (lhs.spriteStartTime - rhs.spriteStartTime);
            }
        });
    }

    private void parseObjects(BufferedSource source) throws IOException {
        line = source.readUtf8Line();
        while (line != null) {
            if (line.equals("")) {
                break;
            }

            if (line.startsWith("Sprite")) {
                for (String s : variablesMap.keySet()) {
                    if (line.contains(s)) {
                        line = line.replace(s, variablesMap.get(s));
                    }
                }
                info = line.split(",");
                int layer = 0;
                if (info[1].equals("Background")) {
                    layer = 0;
                } else if (info[1].equals("Fail")) {
                    layer = 1;
                } else if (info[1].equals("Pass")) {
                    layer = 2;
                } else if (info[1].equals("Foreground")) {
                    layer = 3;
                }
                OsuSprite.Origin origin = OsuSprite.Origin.valueOf(info[2]);
                String filePath = info[3];
                filePath = filePath.replaceAll("\"", "");
                float x = Float.parseFloat(info[4]);
                float y = Float.parseFloat(info[5]);
                ArrayList<OsuEvent> events = parseEvents(source);
                OsuSprite sprite = new OsuSprite(x, y, layer, origin, filePath, events, ZIndex++);
                sprite.setDebugLine(line);
                sprites.add(sprite);
            } else if (line.startsWith("Animation")) {
                for (String s : variablesMap.keySet()) {
                    if (line.contains(s)) {
                        line = line.replace(s, variablesMap.get(s));
                    }
                }
                info = line.split(",");
                int layer = 0;
                if (info[1].equals("Background")) {
                    layer = 0;
                } else if (info[1].equals("Fail")) {
                    layer = 1;
                } else if (info[1].equals("Pass")) {
                    layer = 2;
                } else if (info[1].equals("Foreground")) {
                    layer = 3;
                }
                OsuSprite.Origin origin = OsuSprite.Origin.valueOf(info[2]);
                String filePath = info[3];
                filePath = filePath.replaceAll("\"", "");
                float x = Float.parseFloat(info[4]);
                float y = Float.parseFloat(info[5]);
                int count = Integer.parseInt(info[6]);
                int delay = Integer.parseInt(info[7]);
                String loopType = "LoopForever";
                if (info.length == 9) {
                    loopType = info[8];
                }
                ArrayList<OsuEvent> events = parseEvents(source);
                sprites.add(new OsuSprite(x, y, layer, origin, filePath, events, ZIndex++, count, delay, loopType));
            } else {
                line = source.readUtf8Line();
            }

        }
    }

    private ArrayList<OsuEvent> parseEvents(BufferedSource source) throws IOException {
        ArrayList<OsuEvent> eventList = new ArrayList<OsuEvent>();
        line = source.readUtf8Line();
        if (line.startsWith("_")) {
            line = line.replaceAll("_", " ");
        }
        while (line != null && line.startsWith(" ")) {
            line = line.trim();
            if (line.length() == 0) break;
            for (String s : variablesMap.keySet()) {
                if (line.contains(s)) {
                    line = line.replace(s, variablesMap.get(s));
                }
            }
            OsuEvent currentOsuEvent = new OsuEvent();
            info = line.split(",");
            Command command = Command.valueOf(info[0]);
            currentOsuEvent.command = command;
            if (command == Command.L) {
                currentOsuEvent.startTime = Long.parseLong(info[1]);
                currentOsuEvent.loopCount = Integer.parseInt(info[2]);
                currentOsuEvent.subEvents = parseSubEvents(source);
                if (currentOsuEvent.subEvents.size() > 0) {//real start time
                    currentOsuEvent.startTime = currentOsuEvent.subEvents.get(0).startTime + currentOsuEvent.startTime;
                }
            } else if (command == Command.T) {
                if (info.length > 2) {
                    currentOsuEvent.startTime = Long.parseLong(info[2]);
                    currentOsuEvent.endTime = Long.parseLong(info[3]);
                } else {
                    currentOsuEvent.startTime = 0;
                    currentOsuEvent.endTime = 999999999;
                }
                currentOsuEvent.triggerType = info[1];
                int soundType = -1;
                if (currentOsuEvent.triggerType.equals("HitSoundWhistle")) {
                    soundType = 2;
                } else if (currentOsuEvent.triggerType.equals("HitSoundFinish")) {
                    soundType = 4;
                } else if (currentOsuEvent.triggerType.equals("HitSoundClap")) {
                    soundType = 8;
                }
                currentOsuEvent.subEvents = parseSubEvents(source);
                for (HitSound hitSound : hitSounds) {//real start time
                    if (hitSound.time >= currentOsuEvent.startTime && (hitSound.soundType & soundType) == soundType) {
                        currentOsuEvent.startTime = hitSound.time;
                        break;
                    }
                }
            } else {
                currentOsuEvent.ease = Integer.parseInt(info[1]);
                currentOsuEvent.startTime = Long.parseLong(info[2]);
                currentOsuEvent.endTime = info[3].equals("") ? currentOsuEvent.startTime + 1 : Long.parseLong(info[3]);
                float[] params = null;
                switch (command) {
                    case F:
                    case MX:
                    case MY:
                    case S:
                    case R:
                        params = new float[2];
                        params[0] = Float.parseFloat(info[4]);
                        if (info.length == 5) {
                            params[1] = Float.parseFloat(info[4]);
                        } else {//TODO more than two params
                            params[1] = Float.parseFloat(info[5]);
                        }
                        break;

                    case M:
                    case V:
                        params = new float[4];
                        params[0] = Float.parseFloat(info[4]);
                        params[1] = Float.parseFloat(info[5]);
                        if (info.length == 6) {
                            params[2] = Float.parseFloat(info[4]);
                            params[3] = Float.parseFloat(info[5]);
                        } else {
                            params[2] = Float.parseFloat(info[6]);
                            params[3] = Float.parseFloat(info[7]);
                        }
                        break;
                    case C:
                        params = new float[6];
                        params[0] = Float.parseFloat(info[4]);
                        params[1] = Float.parseFloat(info[5]);
                        params[2] = Float.parseFloat(info[6]);
                        if (info.length == 7) {
                            params[3] = Float.parseFloat(info[4]);
                            params[4] = Float.parseFloat(info[5]);
                            params[5] = Float.parseFloat(info[6]);
                        } else {
                            params[3] = Float.parseFloat(info[7]);
                            params[4] = Float.parseFloat(info[8]);
                            params[5] = Float.parseFloat(info[9]);
                        }
                        break;
                    case P:
                        currentOsuEvent.P = info[4];
                        break;
                }
                currentOsuEvent.params = params;
                line = source.readUtf8Line();
                if (line.startsWith("_")) {
                    line = line.replaceAll("_", " ");
                }
                for (String s : variablesMap.keySet()) {
                    if (line.contains(s)) {
                        line = line.replace(s, variablesMap.get(s));
                    }
                }
            }
            if (currentOsuEvent.triggerType == null || (!currentOsuEvent.triggerType.equals("Passing") && !currentOsuEvent.triggerType.equals("Failing"))) {
                eventList.add(currentOsuEvent);
            }
        }
        return eventList;
    }

    private ArrayList<OsuEvent> parseSubEvents(BufferedSource source) throws IOException {
        ArrayList<OsuEvent> subOsuEventList = new ArrayList<OsuEvent>();
        while ((line = source.readUtf8Line()) != null && (line.startsWith("  ") || line.startsWith("__"))) {
            line = line.replaceAll("_", " ").trim();
            for (String s : variablesMap.keySet()) {
                if (line.contains(s)) {
                    line = line.replace(s, variablesMap.get(s));
                }
            }
            OsuEvent subEvent = new OsuEvent();
            info = line.split(",");
            Command subCommand = Command.valueOf(info[0]);
            subEvent.command = subCommand;
            subEvent.ease = Integer.parseInt(info[1]);
            subEvent.startTime = Long.parseLong(info[2]);
            subEvent.endTime = info[3].equals("") ? subEvent.startTime + 1 : Long.parseLong(info[3]);
            float[] params = null;
            switch (subCommand) {
                case F:
                case MX:
                case MY:
                case S:
                case R:
                    params = new float[2];
                    params[0] = Float.parseFloat(info[4]);
                    if (info.length == 5) {
                        params[1] = Float.parseFloat(info[4]);
                    } else {//TODO more than two params
                        params[1] = Float.parseFloat(info[5]);
                    }
                    break;

                case M:
                case V:
                    params = new float[4];
                    params[0] = Float.parseFloat(info[4]);
                    params[1] = Float.parseFloat(info[5]);
                    if (info.length == 6) {
                        params[2] = Float.parseFloat(info[4]);
                        params[3] = Float.parseFloat(info[5]);
                    } else {
                        params[2] = Float.parseFloat(info[6]);
                        params[3] = Float.parseFloat(info[7]);
                    }
                    break;
                case C:
                    params = new float[6];
                    params[0] = Float.parseFloat(info[4]);
                    params[1] = Float.parseFloat(info[5]);
                    params[2] = Float.parseFloat(info[6]);
                    if (info.length == 7) {
                        params[3] = Float.parseFloat(info[4]);
                        params[4] = Float.parseFloat(info[5]);
                        params[5] = Float.parseFloat(info[6]);
                    } else {
                        params[3] = Float.parseFloat(info[7]);
                        params[4] = Float.parseFloat(info[8]);
                        params[5] = Float.parseFloat(info[9]);
                    }
                    break;
                case P:
                    subEvent.P = info[4];
                    break;
                case T:
                case L:
                    parseSubEvents(source);
                    break;
            }
            subEvent.params = params;
            subOsuEventList.add(subEvent);
        }
        return subOsuEventList;
    }


    public void loadBeatmap(File file) throws IOException {
        BufferedSource source = Okio.buffer(Okio.source(file));

        String header = source.readUtf8Line().trim();
        Pattern pattern;
        Matcher matcher;
//        Pattern pattern = Pattern.compile("osu file format v(\\d+)");
//        Matcher matcher = pattern.matcher(header);
//        if (!matcher.find())
//        {
//            Log.e("BeatmapParsing", "Incompatible beatmap version.");
//        }
//        mVersion = Integer.parseInt(matcher.group(1));

        String line;
        while ((line = source.readUtf8Line()) != null) {
            pattern = Pattern.compile("\\[(\\w+)]");
            matcher = pattern.matcher(line.trim());

            if (matcher.find()) {
                String title = matcher.group(1);
                if (title.equals("General")) {
                    parseGeneral(source);
                } else if (title.equals("Difficulty")) {
                    parseDifficulty(source);
                } else if (title.equals("Events")) {
                    parseEvent(source);
                } else if (title.equals("TimingPoints")) {
                    parseTimingPoints(source);
                } else if (title.equals("HitObjects")) {
                    parseHitObject(source);
                }
            }
        }
        source.close();
    }

    private void parseGeneral(BufferedSource source) throws IOException {
        String line;
        while ((line = source.readUtf8Line()) != null) {
            line = line.trim();
            if (line.equals("")) return;
            String[] values = line.split(":");
            String key = values[0];
            String value = values[1].trim();

            if (key.equals("AudioFilename")) {
                StoryBoardTestActivity.activity.mAudioFileName = value;
                break;
            }
        }
    }

    private void parseEvent(BufferedSource source) throws IOException {
        String line;
        String info[];
        while ((line = source.readUtf8Line()) != null) {
            line = line.trim();
            if (line.equals("")) return;

            if (line.contains(",")) {
                info = line.split(",");
                Pattern pattern = Pattern.compile("[^\"]+\\.(jpg|png)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(line);
                if (info[0].equals("0") && matcher.find()) {
                    StoryBoardTestActivity.activity.mBackground = matcher.group(0);
                    parseObjects(source);
                    break;
                }
            }
        }
    }

    private void parseVariables(BufferedSource source) throws IOException {
        String line;
        while ((line = source.readUtf8Line()) != null) {
            line = line.trim();
            if (line.equals("")) return;
            String[] values = line.split("=");
            String key = values[0];
            String value = values[1].trim();
            variablesMap.put(key, value);
        }
    }

    private void parseDifficulty(BufferedSource source) throws IOException {
        String line;
        while ((line = source.readUtf8Line()) != null) {
            line = line.trim();
            if (line.equals("")) return;
            String[] values = line.split(":");
            if (values[0].equals("SliderMultiplier")) {
                sliderMultiplier = Float.parseFloat(values[1]);
            }
        }
    }

    private void parseTimingPoints(BufferedSource source) throws IOException {
        String line;
        float lastLengthPerBeat = -100;
        while ((line = source.readUtf8Line()) != null) {
            line = line.trim();
            if (line.equals("")) return;
            String[] values = line.split(",");
            TimingPoint timingPoint = new TimingPoint();
            timingPoint.startTime = (long) Float.parseFloat(values[0]);
            timingPoint.lengthPerBeat = Float.parseFloat(values[1]);
            if (timingPoint.lengthPerBeat < 0) {
                timingPoint.lengthPerBeat = lastLengthPerBeat;
            } else {
                lastLengthPerBeat = timingPoint.lengthPerBeat;
            }
            timingPoints.add(timingPoint);
        }
    }

    private void parseHitObject(BufferedSource source) throws IOException {
        String line;
        while ((line = source.readUtf8Line()) != null) {
            line = line.trim();
            if (line.equals("")) return;
            String[] values = line.split(",");
            int objectType = Integer.parseInt(values[3]);
            if ((objectType & 1) == 1) {//circle
                HitSound hitSound = new HitSound();
                hitSound.time = Long.parseLong(values[2]);
                hitSound.soundType = Integer.parseInt(values[4]);
                hitSounds.add(hitSound);
            } else if ((objectType & 2) == 2) {//slider
                long startTime = Long.parseLong(values[2]);
                int count = Integer.parseInt(values[6]) + 1;
                float sliderLength = Float.parseFloat(values[7]);
                String[] soundTypes = null;
                if (values.length > 8) {
                    soundTypes = values[8].split("\\|");
                }
                TimingPoint currentPoint = timingPoints.get(0);
                for (TimingPoint timingPoint : timingPoints) {
                    if (startTime > timingPoint.startTime) {
                        currentPoint = timingPoint;
                        break;
                    }
                }
                float sliderLengthTime = currentPoint.lengthPerBeat * (sliderMultiplier / sliderLength) / 100;
                for (int i = 0; i < count; i++) {
                    HitSound hitSound = new HitSound();
                    if (values.length > 8) {
                        assert soundTypes != null;
                        hitSound.soundType = Integer.parseInt(soundTypes[i]);
                    } else {
                        hitSound.soundType = Integer.parseInt(values[4]);
                    }

                    hitSound.time = (long) (startTime + sliderLengthTime * i);
                    if (hitSound.soundType > 0)
                        hitSounds.add(hitSound);
                }
            } else if ((objectType & 8) == 8) {//spinner
                HitSound hitSound = new HitSound();
                hitSound.time = Long.parseLong(values[5]);
                hitSound.soundType = Integer.parseInt(values[4]);
                hitSounds.add(hitSound);
            }
        }
    }
}
