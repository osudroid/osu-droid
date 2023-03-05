package com.reco1l.management;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.reco1l.Game;
import com.reco1l.utils.helpers.BeatmapHelper;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public class BeatmapCollection {

    public static final BeatmapCollection instance = new BeatmapCollection();

    private final ArrayList<Listener> mListeners;

    private ArrayList<BeatmapInfo> mList;

    private SortOrder mOrder;
    private String mFilter;

    //--------------------------------------------------------------------------------------------//

    public enum SortOrder {
        Title, Artist, Creator, Date, BPM, Stars, Length
    }

    //--------------------------------------------------------------------------------------------//

    public BeatmapCollection() {
        mList = new ArrayList<>();
        mListeners = new ArrayList<>();

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Game.activity);

        mOrder = SortOrder.values()[p.getInt("sortOrder", 0)];
    }

    //--------------------------------------------------------------------------------------------//

    public void setFilter(String filter) {
        if (Objects.equals(filter, this.mFilter)) {
            return;
        }
        this.mFilter = filter;

        handleFiltering();
        notifyChange();
    }

    private void handleFiltering() {
        if (mFilter == null) {
            return;
        }

        ArrayList<BeatmapInfo> newList = new ArrayList<>();

        for (BeatmapInfo beatmap : Game.libraryManager.getLibrary()) {

            String[] sequence = mFilter.split(" ");
            String data = BeatmapHelper.getDataConcat(beatmap);

            boolean canAdd = false;

            for (String word : sequence) {
                Pattern pattern = Pattern.compile("(ar|od|cs|hp|star)(=|<|>|<=|>=)(\\d+)");
                Matcher matcher = pattern.matcher(word);

                if (matcher.find()) {
                    canAdd = containsAttrs(beatmap, matcher);
                    continue;
                }

                if (data.contains(word)) {
                    canAdd = true;
                }
            }

            if (canAdd) {
                newList.add(beatmap);
            }
        }

        mList = newList;
        sort(mOrder, true);
    }

    private boolean containsAttrs(BeatmapInfo beatmap, Matcher matcher) {
        String key = matcher.group(1);
        String opt = matcher.group(2);
        String value = matcher.group(3);

        if (key == null || opt == null || value == null) {
            return false;
        }

        for (TrackInfo track : beatmap.getTracks()) {
            boolean bool = false;

            switch (key) {
                case "ar":
                    bool = operate(track.getApproachRate(), Float.parseFloat(value), opt);
                    break;
                case "od":
                    bool = operate(track.getOverallDifficulty(), Float.parseFloat(value), opt);
                    break;
                case "cs":
                    bool = operate(track.getCircleSize(), Float.parseFloat(value), opt);
                    break;
                case "hp":
                    bool = operate(track.getHpDrain(), Float.parseFloat(value), opt);
                    break;
                case "star":
                    bool = operate(track.getDifficulty(), Float.parseFloat(value), opt);
                    break;
            }

            if (bool) {
                return true; // It'll iterate and check between all tracks
            }
        }
        return false;
    }

    private boolean operate(float val1, float val2, String opt) {
        switch (opt) {
            case "=":
                return val1 == val2;
            case "<":
                return val1 < val2;
            case ">":
                return val1 > val2;
            case "<=":
                return val1 <= val2;
            case ">=":
                return val1 >= val2;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public void sort(SortOrder order) {
        sort(order, false);
    }

    public void sort(SortOrder order, boolean force) {
        if (this.mOrder == order && !force) {
            return;
        }
        this.mOrder = order;

        PreferenceManager.getDefaultSharedPreferences(Game.activity)
                .edit()
                .putInt("sortOrder", order.ordinal())
                .commit();

        mList.sort((a, b) -> {
            String s1;
            String s2;

            switch (order) {
                case Artist:
                    s1 = a.getArtist();
                    s2 = b.getArtist();
                    break;

                case Creator:
                    s1 = a.getCreator();
                    s2 = b.getCreator();
                    break;

                case Date:
                    long int1 = a.getDate();
                    long int2 = b.getDate();
                    return Long.compare(int2, int1);

                case BPM:
                    float bpm1 = a.getLastTrack().getBpmMax();
                    float bpm2 = b.getLastTrack().getBpmMax();
                    return Float.compare(bpm2, bpm1);

                case Stars:
                    float float1 = a.getLastTrack().getDifficulty();
                    float float2 = b.getLastTrack().getDifficulty();
                    return Float.compare(float2, float1);

                case Length:
                    long length1 = a.getLastTrack().getMusicLength();
                    long length2 = b.getLastTrack().getMusicLength();
                    return Long.compare(length2, length1);

                default:
                    s1 = a.getTitle();
                    s2 = b.getTitle();
            }

            return s1.compareToIgnoreCase(s2);
        });

        notifyChange();
    }

    //--------------------------------------------------------------------------------------------//

    public ArrayList<BeatmapInfo> getList() {
        return mList;
    }

    public SortOrder getOrder() {
        return mOrder;
    }

    //--------------------------------------------------------------------------------------------//

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void notifyLibraryChange() {
        mList.clear();
        mList.addAll(Game.libraryManager.getLibrary());
        sort(mOrder, true);
        mFilter = null;
        Log.i("BeatmapCollection", "Library has been updated");
    }

    public void notifyChange() {
        mListeners.forEach(listener -> listener.onLibraryChange(mList));
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface Listener {
        void onLibraryChange(ArrayList<BeatmapInfo> pLibrary);
    }
}
