package com.reco1l.data;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import android.database.Cursor;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.Game;
import com.reco1l.data.adapters.ScoreboardAdapter;
import com.reco1l.utils.helpers.ScoringHelper;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.data.tables.ResourceTable;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 18/9/22 12:40

public class Scoreboard {

    public String errorMessage;
    public final List<Item> boardScores;

    private ScoreboardAdapter adapter;
    private RecyclerView container;
    private AsyncTask loadingTask;
    private Cursor currentCursor;

    //--------------------------------------------------------------------------------------------//

    public Scoreboard() {
        boardScores = new ArrayList<>();
    }

    public void setContainer(RecyclerView container) {
        this.container = container;

        if (container != null) {
            container.setLayoutManager(new LinearLayoutManager(Game.activity, VERTICAL, true));
            container.setAdapter(adapter);
            container.setNestedScrollingEnabled(false);
        } else {
            clear();
        }
    }

    //----------------------------------------------------------------------------------------//

    private void notifyDataChange() {
        if (container == null)
            return;

        container.post(() -> {
            if (boardScores.isEmpty()) {
                container.setAdapter(null);
            } else {
                adapter = new ScoreboardAdapter(boardScores);
                container.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void clear() {
        errorMessage = null;
        if (loadingTask != null && !loadingTask.isCompleted()) {
            loadingTask.cancel(true);
            loadingTask = null;
        }
        if (currentCursor != null) {
            currentCursor.close();
            currentCursor = null;
        }
        boardScores.clear();
        notifyDataChange();
    }

    public boolean loadScores(TrackInfo track, boolean fromOnline) {
        clear();
        return fromOnline ? loadOnlineBoard(track) : loadLocalBoard(track);
    }

    // From offline
    //----------------------------------------------------------------------------------------//
    private boolean loadLocalBoard(TrackInfo track) {
        if (track == null) {
            errorMessage = "No track selected";
            return false;
        }

        String[] columns = {"id", "playername", "score", "combo", "mark", "accuracy", "mode"};
        Cursor cursor = Game.scoreLibrary.getMapScores(columns, track.getFilename());
        currentCursor = cursor;

        if (cursor.getCount() == 0) {
            cursor.close();
            errorMessage = ResourceTable.str(R.string.beatmap_panel_empty_prompt);
            return false;
        }
        cursor.moveToFirst();

        loadingTask = new AsyncTask() {
            @Override
            public void run() {
                long lastScore = 0;

                for (int i = cursor.getCount() - 1; i >= 0; i--) {
                    cursor.moveToPosition(i);
                    Item item = new Item();

                    long currentScore = cursor.getLong(cursor.getColumnIndexOrThrow("score"));

                    item.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    item.rank = "" + (i + 1);
                    item.mark = cursor.getString(cursor.getColumnIndexOrThrow("mark"));
                    item.name = cursor.getString(cursor.getColumnIndexOrThrow("playername"));

                    item.setDifference(currentScore - lastScore);
                    item.setCombo(cursor.getInt(cursor.getColumnIndexOrThrow("combo")));
                    item.setScore(cursor.getLong(cursor.getColumnIndexOrThrow("score")));
                    item.setMods(cursor.getString(cursor.getColumnIndexOrThrow("mode")));
                    item.setAccuracy(cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")));

                    lastScore = currentScore;
                    boardScores.add(item);
                    //printScore(item);
                }
            }

            @Override
            public void onComplete() {
                cursor.close();
                currentCursor = null;
                notifyDataChange();
            }
        };
        loadingTask.execute();
        return true;
    }

    // From online
    //----------------------------------------------------------------------------------------//
    private boolean loadOnlineBoard(TrackInfo track) {
        if (track == null || !Game.onlineManager.isStayOnline() || BuildConfig.DEBUG) {
            errorMessage = ResourceTable.str(R.string.beatmap_panel_offline_prompt);
            return false;
        }

        File file = new File(track.getFilename());
        ArrayList<String> scores;
        try {
            scores = Game.onlineManager.getTop(file, FileUtils.getMD5Checksum(file));
        } catch (OnlineManager.OnlineManagerException exception) {
            errorMessage = ResourceTable.str(R.string.beatmap_panel_error_prompt) + "\n(" + exception.getMessage() + ")";
            return false;
        }

        if (scores.size() == 0) {
            errorMessage = ResourceTable.str(R.string.beatmap_panel_empty_prompt);
            return false;
        }

        loadingTask = new AsyncTask() {
            @Override
            public void run() {
                long lastScore = 0;

                for (int i = scores.size() - 1; i >= 0; i--) {
                    Item item = new Item();

                    String[] data = scores.get(i).split("\\s+");
                    if (data.length < 8)
                        continue;

                    long currentScore = Long.parseLong(data[2]);

                    item.id = Integer.parseInt(data[0]);
                    item.rank = data.length == 10 ? data[7] : "" + i + 1;
                    item.mark = data[4];
                    item.name = data.length == 10 ? data[8] : data[1];
                    item.avatar = data.length == 10 ? data[9] : data[7];

                    item.setDifference(currentScore - lastScore);
                    item.setAccuracy(Float.parseFloat(data[6]));
                    item.setCombo(Integer.parseInt(data[3]));
                    item.setScore(Long.parseLong(data[2]));
                    item.setMods(data[5]);

                    lastScore = currentScore;
                    boardScores.add(item);
                }
            }

            @Override
            public void onComplete() {
                notifyDataChange();
            }
        };
        loadingTask.execute();
        return true;
    }

    // Debug
    /*private void printScore(Scoreboard.Item s) {
        StringBuilder b = new StringBuilder();

        b.append("\n");
        b.append("New score found: ").append(s.name).append("\n");
        b.append("rank: ").append(s.rank).append(" score: ").append(s.score).append("\n");
        b.append("combo: ").append(s.combo).append(" accuracy: ").append(s.accuracy).append("\n");
        b.append("mods: ");

        if (s.mods.isEmpty()) {
            b.append("none");
        } else {
            for (GameMod mod : s.mods) {
                b.append(mod.name()).append(" ");
            }
        }

        Log.i("Scoreboard", b.toString());
    }*/

    //----------------------------------------------------------------------------------------//

    public static class Item {

        public int id = -1;
        public long rawScore;

        public String rank, avatar, mark, name;

        private List<GameMod> mods;
        private String score, difference, combo, accuracy;

        //--------------------------------------------------------------------------------------------//

        public void setScore(long score) {
            this.score = NumberFormat.getNumberInstance(Locale.US).format(score);
            rawScore = score;
        }

        public String getScore() {
            return score;
        }
        //--------------------------------------------------------------------------------------------//

        public void setDifference(long difference) {
            this.difference = NumberFormat.getNumberInstance(Locale.US).format(difference);
        }

        public String getDifference() {
            return difference;
        }
        //--------------------------------------------------------------------------------------------//

        public void setCombo(int combo) {
            this.combo = NumberFormat.getNumberInstance(Locale.US).format(combo);
        }

        public String getCombo() {
            return combo;
        }
        //--------------------------------------------------------------------------------------------//

        public void setAccuracy(float accuracy) {
            this.accuracy = String.format("%.2f", GameHelper.Round(accuracy * 100, 2));
        }

        public String getAccuracy() {
            return accuracy;
        }
        //--------------------------------------------------------------------------------------------//

        public void setMods(String mods) {
            this.mods = ScoringHelper.parseMods(mods);
        }

        public List<GameMod> getMods() {
            return mods;
        }

        //--------------------------------------------------------------------------------------------//
        // Workaround for DuringGameScoreBoard (old UI)

        public String get() {
            return name + "\n"
                    + NumberFormat.getNumberInstance(Locale.US).format(score) + "\n"
                    + NumberFormat.getNumberInstance(Locale.US).format(combo) + "x";
        }
    }
}
