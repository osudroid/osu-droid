package ru.nsu.ccfit.zuev.osu.menu;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Process;

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.favorite.FavoriteLibrary;
import com.edlplan.replay.OdrDatabase;
import com.edlplan.ui.fragment.PropsMenuFragment;
import com.edlplan.ui.fragment.ScoreMenuFragment;
import com.reco1l.utils.interfaces.UI;

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

public class SongMenu implements IUpdateHandler, MenuItemListener, UI {
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

    public void setScoringScene(final ScoringScene ss) {
        scoreScene = ss;
    }


    public void init(final Activity context, final Engine engine,
                     final GameScene pGame) {
        this.engine = engine;
        game = pGame;
        this.context = context;
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
        increaseVolume();
        increaseBackgroundLuminance(pSecondsElapsed);
        beatmapList.update();

        secondsSinceLastSelect += pSecondsElapsed;

        expandSelectedItem(pSecondsElapsed);
    }

    public void increaseVolume() {
        if (GlobalManager.getInstance().getSongService() != null) {
            synchronized (musicMutex) {
                if (GlobalManager.getInstance().getSongService() != null && GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING && GlobalManager.getInstance().getSongService().getVolume() < Config.getBgmVolume()) {
                    float vol = Math.min(1, GlobalManager.getInstance().getSongService().getVolume() + 0.01f);
                    GlobalManager.getInstance().getSongService().setVolume(vol);
                }
            }
        }
    }

    public void increaseBackgroundLuminance(final float pSecondsElapsed) {
        if (bg != null) {
            synchronized (backgroundMutex) {
                if (bg != null && bg.getRed() < 1) {
                    final float col = Math.min(1, bg.getRed() + pSecondsElapsed);
                    bg.setColor(col, col, col);
                }
            }
        }
    }

    public void expandSelectedItem(float pSecondsElapsed) {
        if (selectedItem != null) {
            if (selectedItem.percentAppeared < 1) {
                selectedItem.percentAppeared += 2 * pSecondsElapsed;
            } else {
                selectedItem.percentAppeared = 1;
            }
            selectedItem.update(pSecondsElapsed);
        }
    }

    public void reset() {}

    public void select(final MenuItem item) {
        secondsSinceLastSelect = 0;
        if (selectedItem != null) {
            selectedItem.deselect();
        }

        selectedItem = item;
        selectedTrack = null;
    }

    public void updateInfo(TrackInfo track) {
        beatmapPanel.updateProperties(track);
    }

    public void selectTrack(final TrackInfo track, boolean reloadBG) {

        if (selectedTrack == track) {
            synchronized (bgLoaded) {
                if (!bgLoaded) {
                    return;
                }
            }

            ResourceManager.getInstance().getSound("menuhit").play();
            stopMusic();
            game.startGame(track, null);
            unload();
            return;
        }
        isSelectComplete = false;
        selectedTrack = track;
        EdExtensionHelper.onSelectTrack(selectedTrack);
        GlobalManager.getInstance().setSelectedTrack(track);
        updateInfo(track);
        beatmapPanel.updateScoreboard();

        final int quality = Config.getBackgroundQuality();
        synchronized (backgroundMutex) {

            if (!reloadBG && (track.getBackground() == null || bgName.equals(track.getBackground()))) {
                isSelectComplete = true;
                return;
            }
            bgName = track.getBackground();
            bg = null;
            bgLoaded = false;
            scene.setBackground(new ColorBackground(0, 0, 0));
            if (quality == 0) {
                Config.setBackgroundQuality(4);
            }
        }
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                // Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                synchronized (backgroundMutex) {
                    final TextureRegion tex = Config.isSafeBeatmapBg() || track.getBackground() == null?
                        ResourceManager.getInstance().getTexture("menu-background") :
                        ResourceManager.getInstance().loadBackground(bgName);
                    if (tex != null) {
                        float height = tex.getHeight();
                        height *= Config.getRES_WIDTH()
                                / (float) tex.getWidth();
                        bg = new Sprite(0,
                                (Config.getRES_HEIGHT() - height) / 2, Config
                                .getRES_WIDTH(), height, tex);
                        bg.setColor(0, 0, 0);
                    }

                    // run()
                    SyncTaskManager.getInstance().run(() -> {
                        synchronized (backgroundMutex) {
                            if (bg == null) {
                                final TextureRegion tex1 = ResourceManager
                                        .getInstance().getTexture("menu-background");
                                float height = tex1.getHeight();
                                height *= Config.getRES_WIDTH()
                                        / (float) tex1.getWidth();
                                bg = new Sprite(
                                        0,
                                        (Config.getRES_HEIGHT() - height) / 2,
                                        Config.getRES_WIDTH(), height, tex1);
                                bgName = "";
                            }
                            scene.setBackground(new SpriteBackground(bg));
                            Config.setBackgroundQuality(quality);
                            synchronized (bgLoaded) {
                                bgLoaded = true;
                            }
                        }
                    });// SyncTask.run

                }
            }


            public void onComplete() {
                isSelectComplete = true;
            }// onComplete
        });
    }

    public void stopScroll(final float y) {
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

    public void unload() {
    }

    public void back() {
        unbindDataBaseChangedListener();
        GlobalManager.getInstance().getMainScene().show();
    }

    public void bindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(beatmapPanel::updateScoreboard);
    }

    public void unbindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(null);
    }

    public void setY(final float y) {
    }

    public void stopMusic() {
        synchronized (musicMutex) {
            if (GlobalManager.getInstance().getSongService() != null) {
                GlobalManager.getInstance().getSongService().stop();
            }
        }
    }

    public void playMusic(final String filename, final int previewTime) {
        if (!Config.isPlayMusicPreview()) {
            return;
        }
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                synchronized (musicMutex) {
                    if (GlobalManager.getInstance().getSongService() != null) {
                        GlobalManager.getInstance().getSongService().stop();
                    }

                    try {
//                        GlobalManager.getInstance().getSongService().preLoadWithLoop(filename);
                        GlobalManager.getInstance().getSongService().preLoad(filename);
                        GlobalManager.getInstance().getSongService().play();
                        GlobalManager.getInstance().getSongService().setVolume(0);
                        if (previewTime >= 0) {
                            GlobalManager.getInstance().getSongService().seekTo(previewTime);
                        } else {
                            GlobalManager.getInstance().getSongService().seekTo(GlobalManager.getInstance().getSongService().getLength() / 2);
                        }
                    } catch (final Exception e) {
                        Debug.e("LoadingMusic: " + e.getMessage(), e);
                    }

                }
            }


            public void onComplete() {
                // TODO Auto-generated method stub

            }
        });
    }

    public boolean isSelectAllowed() {
        if (!bgLoaded) {
            return false;
        }
        return secondsSinceLastSelect > 0.5f;
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

    public void select() {
        if (GlobalManager.getInstance().getMainScene().getBeatmapInfo() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getMainScene().getBeatmapInfo();
            /*for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getBeatmap().getArtist().equals(beatmapInfo.getArtist()) &&
                        items.get(i).getBeatmap().getTitle().equals(beatmapInfo.getTitle()) &&
                        items.get(i).getBeatmap().getCreator().equals(beatmapInfo.getCreator())) {
                    secondsSinceLastSelect = 2;
                    items.get(i).select(false, true);
                    break;
                }
            }*/
        }
    }

    public TrackInfo getSelectedTrack() {
        return selectedTrack;
    }

    public boolean checkBG(String filePath) {
        if (filePath == null) return false;
        if (filePath.trim().equals("")) return false;

        BitmapFactory.Options bgOption = new BitmapFactory.Options();
        bgOption.inJustDecodeBounds = true;
        int w = BitmapFactory.decodeFile(filePath).getWidth();
        int h = BitmapFactory.decodeFile(filePath).getHeight();
        bgOption.inJustDecodeBounds = false;
        bgOption = null;
        return (w * h) > 0;
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
