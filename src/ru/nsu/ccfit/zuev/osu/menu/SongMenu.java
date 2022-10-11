package ru.nsu.ccfit.zuev.osu.menu;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Process;

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.favorite.FavoriteLibrary;
import com.edlplan.replay.OdrDatabase;
import com.edlplan.ui.fragment.PropsMenuFragment;
import com.edlplan.ui.fragment.ScoreMenuFragment;
import com.reco1l.UI;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;

import java.util.Set;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SongMenu implements IUpdateHandler {
    private final static Boolean musicMutex = true;
    private final Boolean backgroundMutex = true;
    public Scene scene;
    SortOrder sortOrder = SortOrder.Title;
    private Engine engine;
    private GameScene game;
    private ScoringScene scoreScene;
    private Activity context;
    private MenuItem selectedItem = null;
    private TrackInfo selectedTrack;
    private Sprite bg = null;
    private Boolean bgLoaded = false;
    private String bgName = "";
    private String filterText = "";
    private boolean favsOnly = false;
    private Set<String> limitC;
    private float secondsSinceLastSelect = 0;
    private int pointerId = -1;
    private float secPassed = 0, tapTime;
    private boolean isSelectComplete = true;
    private GroupType groupType = GroupType.MapSet;

    public SongMenu() {
    }

    public static void stopMusicStatic() {
        synchronized (musicMutex) {
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().stop();
            }
        }
    }

    public void loadFilter(IFilterMenu filterMenu) {
        setFilter(filterMenu.getFilter(), filterMenu.getOrder(), filterMenu.isFavoritesOnly(),
                filterMenu.getFavoriteFolder() == null ? null : FavoriteLibrary.get().getMaps(filterMenu.getFavoriteFolder()));
    }

    public void reload() {
        scene.unregisterUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(false);
        load();
        GlobalManager.getInstance().getGameScene().setOldScene(scene);
    }

    public synchronized void load() {
        scene = new Scene();
        selectedItem = null;
        selectedTrack = null;
        bgLoaded = true;
        SongMenuPool.getInstance().init();
        FilterMenu.getInstance().loadConfig(context);
        ModMenu.getInstance().reload();
        bindDataBaseChangedListener();

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");
        float height = tex.getHeight();
        height *= Config.getRES_WIDTH() / (float) tex.getWidth();
        final Sprite bg = new Sprite(0, (Config.getRES_HEIGHT() - height) / 2,
                Config.getRES_WIDTH(), height, tex);
        scene.setBackground(new SpriteBackground(bg));

        final Rectangle bgDimRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        bgDimRect.setColor(0, 0, 0, 0.2f);

        sortOrder = SortOrder.Title;
        sort();

        scene.registerUpdateHandler(this);
        scene.setTouchAreaBindingEnabled(true);
    }

    public Scene getScene() {
        return scene;
    }

    public void setFilter(final String filter, final SortOrder order,
                          final boolean favsOnly, Set<String> limit) {
        String oldTrackFileName = "";
        if (selectedTrack != null) {
            oldTrackFileName = selectedTrack.getFilename();
        }
        if (!order.equals(sortOrder)) {
            sortOrder = order;
            tryReloadMenuItems(sortOrder);
            sort();
            reSelectItem(oldTrackFileName);
        }
        if (filter == null || filterText.equals(filter)) {
            if (favsOnly == this.favsOnly && limitC == limit) {
                return;
            }
        }
        limitC = limit;
        filterText = filter;
        final String lowerFilter = filter.toLowerCase();
        /*for (final MenuItem item : items) {
            item.applyFilter(lowerFilter, favsOnly, limit); //TODO filtering
        }*/
        if (favsOnly != this.favsOnly) {
            this.favsOnly = favsOnly;
        } else {
            reSelectItem(oldTrackFileName);
        }
        if (selectedItem != null && !selectedItem.isVisible()) {
            selectedItem = null;
            selectedTrack = null;
        }
        System.gc();
    }

    public void sort() {
       /* if (!sortOrder.equals(FilterMenu.getInstance().getOrder())) {
            sortOrder = FilterMenu.getInstance().getOrder();
        }
        Collections.sort(items, (i1, i2) -> {
            String s1;
            String s2;
            switch (sortOrder) {
                case Artist:
                    s1 = i1.getBeatmap().getArtist();
                    s2 = i2.getBeatmap().getArtist();
                    break;
                case Creator:
                    s1 = i1.getBeatmap().getCreator();
                    s2 = i2.getBeatmap().getCreator();
                    break;
                case Date:
                    final Long int1 = i1.getBeatmap().getDate();
                    final Long int2 = i2.getBeatmap().getDate();
                    return int2.compareTo(int1);
                case Bpm:
                    final float bpm1 = i1.getFirstTrack().getBpmMax();
                    final float bpm2 = i2.getFirstTrack().getBpmMax();
                    return Float.compare(bpm2, bpm1);
                case Stars:
                    final float float1 = i1.getFirstTrack().getDifficulty();
                    final float float2 = i2.getFirstTrack().getDifficulty();
                    return Float.compare(float2, float1);
                case Length:
                    final Long length1 = i1.getFirstTrack().getMusicLength();
                    final Long length2 = i2.getFirstTrack().getMusicLength();
                    return length2.compareTo(length1);
                default:
                    s1 = i1.getBeatmap().getTitle();
                    s2 = i2.getBeatmap().getTitle();
            }

            return s1.compareToIgnoreCase(s2);
        });*/
    }

    public void onUpdate(final float pSecondsElapsed) {
        secPassed += pSecondsElapsed;

        secondsSinceLastSelect += pSecondsElapsed;
    }

    public void updateScore() {
        // board.init(selectedTrack);
        if (selectedItem != null) {
            selectedItem.updateMarks();
        }
    }

    public void openScore(final int id, boolean showOnline, final String playerName) {
        if (showOnline) {
            engine.setScene(LoadingScreen.getInstance().getScene());
            ToastLogger.showTextId(R.string.online_loadrecord, false);
            new AsyncTaskLoader().execute(new OsuAsyncCallback() {


                public void run() {
                    try {
                        String scorePack = OnlineManager.getInstance().getScorePack(id);
                        String[] params = scorePack.split("\\s+");
                        if (params.length < 11) return;

                        StatisticV2 stat = new StatisticV2(params);
                        stat.setPlayerName(playerName);
                        scoreScene.load(stat, null, null, OnlineManager.getReplayURL(id), null, selectedTrack);
                        engine.setScene(scoreScene.getScene());

                    } catch (OnlineManagerException e) {
                        Debug.e("Cannot load play info: " + e.getMessage(), e);
                        engine.setScene(scene);
                    }

                }

                public void onComplete() {
                    // TODO Auto-generated method stub
                }
            });
            return;
        }


        StatisticV2 stat = ScoreLibrary.getInstance().getScore(id);
        scoreScene.load(stat, null, null, stat.getReplayName(), null, selectedTrack);
        engine.setScene(scoreScene.getScene());
    }

    public void back() {
        unbindDataBaseChangedListener();
        GlobalManager.getInstance().getSongService().setGaming(false);
        GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene());
    }

    public void bindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(UI.beatmapPanel::updateScoreboard);
    }

    public void unbindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(null);
    }

    public void stopMusic() {
        synchronized (musicMutex) {
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().stop();
            }
        }
    }

    public void showPropertiesMenu(MenuItem item) {
        if (item == null) {
            if (selectedItem == null) {
                return;
            }
            item = selectedItem;
        }
        (new PropsMenuFragment()).show(SongMenu.this, item);
    }

    public void showDeleteScoreMenu(int scoreId) {
        (new ScoreMenuFragment()).show(scoreId);
        //ScorePropsMenu.getInstance().setSongMenu(SongMenu.this);
        //ScorePropsMenu.getInstance().setScoreId(scoreId);
        //scene.setChildScene(ScorePropsMenu.getInstance().getScene(), false, true, true);
    }

    private void tryReloadMenuItems(SortOrder order) {
        switch (order) {
            case Title:
            case Artist:
            case Creator:
            case Date:
            case Bpm:
                reloadMenuItems(GroupType.MapSet);
                break;
            case Stars:
            case Length:
                reloadMenuItems(GroupType.SingleDiff);
                break;
        }
    }

    private void reloadMenuItems(GroupType type) {
        /*if (!groupType.equals(type)) {
            groupType = type;
            float oy = 10;
            for (MenuItem item : items) {
                item.removeFromScene();
            }
            items.clear();
            switch (type) {
                case MapSet:
                    for (final BeatmapInfo i : LibraryManager.getInstance().getLibrary()) {
                        final MenuItem item = new MenuItem(this, i);
                        items.add(item);
                        item.attachToScene(scene, backLayer);
                        oy += item.getHeight();
                    }
                    break;
                case SingleDiff:
                    for (final BeatmapInfo i : LibraryManager.getInstance().getLibrary()) {
                        for (int j = 0; j < i.getCount(); j++) {
                            final MenuItem item = new MenuItem(this, i, j);
                            items.add(item);
                            item.attachToScene(scene, backLayer);
                            oy += item.getHeight();
                        }
                    }
                    break;
            }
            final String lowerFilter = FilterMenu.getInstance().getFilter().toLowerCase();
            final boolean favsOnly = FilterMenu.getInstance().isFavoritesOnly();
            final Set<String> limit = FilterMenu.getInstance().getFavoriteFolder() == null ? null : FavoriteLibrary.get().getMaps(FilterMenu.getInstance().getFavoriteFolder());
            for (final MenuItem item : items) {
                item.applyFilter(lowerFilter, favsOnly, limit);
            }
            System.gc();
        }*/
    }

    private void reSelectItem(String oldTrackFileName) {
        /*if (!oldTrackFileName.equals("")) {
            if (selectedTrack.getFilename().equals(oldTrackFileName) && items.size() > 1 && selectedItem != null && selectedItem.isVisible()) {
                velocityY = 0;
                float height = 0;
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i) == selectedItem) {
                        break;
                    }
                    height += items.get(i).getInitialHeight();
                }
                camY = height - Config.getRES_HEIGHT() / 2f;
                camY += items.get(0).getTotalHeight() / 2;
                return;
            }
            for (final MenuItem item : items) {
                if (item == null || !item.isVisible()) continue;
                int trackid = item.tryGetCorrespondingTrackId(oldTrackFileName);
                if (trackid >= 0) {
                    item.select(true, true);
                    if (trackid != 0) {
                        item.selectTrack(item.getTrackSpritesById(trackid), false);
                    }
                    break;
                }
            }
        }*/
    }

    public enum SortOrder {
        Title, Artist, Creator, Date, Bpm, Stars, Length
    }

    public enum GroupType {
        MapSet, SingleDiff
    }
}
