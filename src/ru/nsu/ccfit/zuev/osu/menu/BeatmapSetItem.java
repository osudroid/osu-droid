package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.osu.BeatmapSetInfo;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import com.reco1l.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapSetItem {
    private final BeatmapItem[] beatmapSprites;
    private final BeatmapSetInfo beatmapSetInfo;
    private final String beatmapSetDir;
    private final int bgHeight;
    private final String titleStr;
    private final String creatorStr;
    public float percentAppeared = 0;
    MenuItemBackground background;
    Scene scene;
    boolean selected = false;
    WeakReference<MenuItemListener> listener;
    private BeatmapItem selectedBeatmapItem = null;
    private boolean visible = true;
    private boolean favorite;
    private boolean deleted = false;
    private Entity layer = null;
    private int beatmapId = -1;

    public BeatmapSetItem(final MenuItemListener listener, final BeatmapSetInfo beatmapSetInfo) {
        this.listener = new WeakReference<>(listener);
        this.beatmapSetInfo = beatmapSetInfo;
        beatmapSetDir = ScoreLibrary.getBeatmapSetDirectory(this.beatmapSetInfo.getPath());
        bgHeight = ResourceManager.getInstance()
                .getTexture("menu-button-background").getHeight()
                - Utils.toRes(25);
//        titleStr = (beatmap.getArtistUnicode() == null ? beatmap.getArtist() : beatmap.getArtistUnicode()) + " - "
//                + (beatmap.getTitleUnicode() == null ? beatmap.getTitle() : beatmap.getTitleUnicode());
        var beatmapInfo = this.beatmapSetInfo.getBeatmaps().get(0);

        var artist = Config.isForceRomanized() ? beatmapInfo.getArtist() : beatmapInfo.getArtistUnicode();
        var title = Config.isForceRomanized() ? beatmapInfo.getTitle() : beatmapInfo.getTitleUnicode();
        titleStr = artist + " - " + title;

        creatorStr = StringTable.format(R.string.menu_creator,
                beatmapInfo.getCreator());
        beatmapSprites = new BeatmapItem[beatmapSetInfo.getCount()];

        final BeatmapProperties props = PropertiesLibrary.getInstance()
                .getProperties(beatmapSetInfo.getPath());
        favorite = props != null && props.isFavorite();

    }

    public BeatmapSetItem(final MenuItemListener listener, final BeatmapSetInfo beatmapSetInfo, int id) {
        this.listener = new WeakReference<>(listener);
        this.beatmapSetInfo = beatmapSetInfo;
        beatmapSetDir = ScoreLibrary.getBeatmapSetDirectory(this.beatmapSetInfo.getPath());
        bgHeight = ResourceManager.getInstance()
                .getTexture("menu-button-background").getHeight()
                - Utils.toRes(25);
//        titleStr = (beatmap.getArtistUnicode() == null ? beatmap.getArtist() : beatmap.getArtistUnicode()) + " - "
//                + (beatmap.getTitleUnicode() == null ? beatmap.getTitle() : beatmap.getTitleUnicode());
        var beatmapInfo = this.beatmapSetInfo.getBeatmaps().get(0);

        var artist = Config.isForceRomanized() ? beatmapInfo.getArtist() : beatmapInfo.getArtistUnicode();
        var title = Config.isForceRomanized() ? beatmapInfo.getTitle() : beatmapInfo.getTitleUnicode();
        titleStr = artist + " - " + title;

        creatorStr = StringTable.format(R.string.menu_creator, beatmapInfo.getCreator());
        beatmapSprites = new BeatmapItem[1];
        beatmapId = id;
        final BeatmapProperties props = PropertiesLibrary.getInstance().getProperties(beatmapSetInfo.getPath());
        favorite = props != null && props.isFavorite();

    }

    public BeatmapSetInfo getBeatmapSetInfo() {
        return beatmapSetInfo;
    }

    public void updateMarks() {
        for (final BeatmapItem tr : beatmapSprites) {
            if (tr != null) {
                tr.updateMark();
            }
        }
    }

    public void attachToScene(final Scene scene, final Entity layer) {
        this.scene = scene;
        this.layer = layer;
        // initBackground();
    }

    public float getHeight() {
        if (!visible) {
            return 0;
        }
        if (selected) {
            return bgHeight + percentAppeared * (bgHeight)
                    * (beatmapSprites.length - 1);
        }
        return bgHeight - Utils.toRes(5);
    }

    public float getInitialHeight() {
        if (!visible) {
            return 0;
        }
        return bgHeight - Utils.toRes(5);
    }

    public float getTotalHeight() {
        return (bgHeight) * (beatmapSprites.length);
    }

    public void setPos(final float x, final float y) {
        if (background != null) {
            background.setPosition(x, y);
            if (y > Config.getRES_HEIGHT() || y < -background.getHeight()) {
                freeBackground();
            }
        }
        if (!selected) {
            if (visible && background == null
                    && y < Config.getRES_HEIGHT() && y > -bgHeight) {
                initBackground();
                background.setPosition(x, y);
            }
            return;
        }
        float oy = 0;
        for (final Sprite s : beatmapSprites) {
            if (s == null) {
                continue;
            }
            final float cy = y + oy + Config.getRES_HEIGHT() / 2f
                    + s.getHeight() / 2;
            final float ox = x
                    + Utils.toRes(170 * (float) Math.abs(Math.cos(cy * Math.PI
                    / (Config.getRES_HEIGHT() * 2))));
            s.setPosition(ox - Utils.toRes(100), y + oy);
            oy += (s.getHeight() - Utils.toRes(25)) * percentAppeared;
        }
    }

    public void select(boolean reloadMusic, boolean reloadBG) {
        if (!listener.get().isSelectAllowed() || scene == null) {
            return;
        }

        freeBackground();
        selected = true;
        listener.get().select(this);
        initTracks();
        percentAppeared = 0;

        var beatmapInfo = beatmapSetInfo.getBeatmaps().get(0);

        final String musicFileName = beatmapInfo.getAudio();
        if (reloadMusic) {
            listener.get().playMusic(musicFileName, beatmapInfo.getPreviewTime());
        }

        selectBeatmap(beatmapSprites[0], reloadBG);
        beatmapSprites[0].setSelectedColor();
    }

    public void deselect() {
        if (scene == null ) {
            return;
        }

        if (deleted) {
            return;
        }
        initBackground();
        selected = false;
        percentAppeared = 0;
        deselectTrack();
        freeTracks();
    }

    public void deselectTrack() {
        if (scene == null) {
            return;
        }
        if (selectedBeatmapItem != null) {
            selectedBeatmapItem.setDeselectColor();
        }
        selectedBeatmapItem = null;
    }

    public void applyFilter(final String filter, final boolean favs, Set<String> limit) {
        if ((favs && !isFavorite())
                || (limit != null && !limit.contains(beatmapSetDir))) {
            //System.out.println(trackDir);
            if (selected) {
                deselect();
            }
            freeBackground();
            visible = false;
            return;
        }

        var beatmapInfo = beatmapSetInfo.getBeatmaps().get(0);

        final StringBuilder builder = new StringBuilder();
        builder.append(beatmapInfo.getTitle());
        builder.append(' ');
        builder.append(beatmapInfo.getArtist());
        builder.append(' ');
        builder.append(beatmapInfo.getCreator());
        builder.append(' ');
        builder.append(beatmapInfo.getTags());
        builder.append(' ');
        builder.append(beatmapInfo.getSource());
        builder.append(' ');
        builder.append(beatmapInfo.getId());
        for (BeatmapInfo beatmap : beatmapSetInfo.getBeatmaps()) {
            builder.append(' ');
            builder.append(beatmap.getVersion());
        }

        boolean canVisible = true;
        final String lowerText = builder.toString().toLowerCase();
        final String[] lowerFilterTexts = filter.toLowerCase().split("[ ]");
        for (String filterText : lowerFilterTexts) {
            Pattern pattern = Pattern.compile("(ar|od|cs|hp|star)(=|<|>|<=|>=)(\\d+)");
            Matcher matcher = pattern.matcher(filterText);
            if (matcher.find()) {
                String key = matcher.group(1);
                String opt = matcher.group(2);
                String value = matcher.group(3);
                boolean vis = false;
                if(beatmapId < 0){
                    for (BeatmapInfo beatmap : beatmapSetInfo.getBeatmaps()) {
                        if (key != null) {
                            vis |= visibleBeatmap(beatmap, key, opt, value);
                        }
                    }
                }
                else{
                    if (key != null) {
                        vis = visibleBeatmap(beatmapSetInfo.get(beatmapId), key, opt, value);
                    }
                }
                canVisible &= vis;
            } else {
                if (!lowerText.contains(filterText)) {
                    canVisible = false;
                    break;
                }
            }
        }

        if (filter.isEmpty()) {
            canVisible = true;
        }

        if (canVisible) {
            if (!visible) {
                visible = true;
                // initBackground();
                selected = false;
                percentAppeared = 0;
            }
            return;
        }

        if (selected) {
            deselect();
        }
        freeBackground();
        visible = false;
    }

    private boolean visibleBeatmap(BeatmapInfo beatmap, String key, String opt, String value) {
        switch (key) {
            case "ar":
                return calOpt(beatmap.getApproachRate(), Float.parseFloat(value), opt);
            case "od":
                return calOpt(beatmap.getOverallDifficulty(), Float.parseFloat(value), opt);
            case "cs":
                return calOpt(beatmap.getCircleSize(), Float.parseFloat(value), opt);
            case "hp":
                return calOpt(beatmap.getHpDrainRate(), Float.parseFloat(value), opt);
            case "droidstar":
                return calOpt(beatmap.getDroidStarRating(), Float.parseFloat(value), opt);
            case "standardstar":
            case "star":
                return calOpt(beatmap.getStandardStarRating(), Float.parseFloat(value), opt);
            default:
                return false;
        }
    }

    private boolean calOpt(float val1, float val2, String opt) {
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
            default:
                return false;
        }
    }

    public void delete() {

        if (selected) {
            deselect();
        }
        freeBackground();
        visible = false;
        deleted = true;
        LibraryManager.deleteBeatmapSet(beatmapSetInfo);
    }

    public boolean isVisible() {
        return visible && (!deleted);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void stopScroll(final float y) {
        listener.get().stopScroll(y);
    }

    public void selectBeatmap(final BeatmapItem beatmapInfo, boolean reloadBG) {
        selectedBeatmapItem = beatmapInfo;
        listener.get().selectBeatmap(beatmapInfo.getBeatmapInfo(), reloadBG);
    }

    public boolean isBeatmapSelected(final BeatmapItem track) {
        return selectedBeatmapItem == track;
    }

    private void freeBackground() {
        // scene.unregisterTouchArea(background);
        if (background == null) {
            return;
        }
        background.setVisible(false);
        SongMenuPool.getInstance().putBackground(background);
        background = null;

    }

    private synchronized void initBackground() {
        if (background == null) {
            background = SongMenuPool.getInstance().newBackground();
        }
        background.setItem(this);
        background.setTitle(titleStr);
        background.setAuthor(creatorStr);
        background.setVisible(true);
        if (!background.hasParent()) {
            layer.attachChild(background);
            scene.registerTouchArea(background);
        }
    }

    private void freeTracks() {

        for (int i = 0; i < beatmapSprites.length; i++) {
            beatmapSprites[i].setVisible(false);
            scene.unregisterTouchArea(beatmapSprites[i]);
            beatmapSprites[i].setVisible(false);
            SongMenuPool.getInstance().putTrack(beatmapSprites[i]);
            beatmapSprites[i] = null;
        }

    }

    public void reloadTracks() {
        if (beatmapId == -1) {
            // Tracks are originally sorted by osu!droid difficulty, so for osu!standard difficulty they need to be sorted again.
            if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) {
                Collections.sort(beatmapSetInfo.getBeatmaps(), (o1, o2) -> Float.compare(o1.getStandardStarRating(), o2.getStandardStarRating()));
            } else {
                Collections.sort(beatmapSetInfo.getBeatmaps(), (o1, o2) -> Float.compare(o1.getDroidStarRating(), o2.getDroidStarRating()));
            }

            var selectedTrack = selectedBeatmapItem != null ? selectedBeatmapItem.getBeatmapInfo() : null;

            for (int i = 0; i < beatmapSprites.length; i++) {
                beatmapSprites[i].setBeatmapInfo(beatmapSetInfo.get(i));

                // Ensure the selected track is still selected after reloading.
                if (selectedTrack != null && selectedTrack == beatmapSetInfo.get(i)) {
                    beatmapSprites[i].setSelectedColor();
                    selectedBeatmapItem = beatmapSprites[i];
                } else {
                    beatmapSprites[i].setDeselectColor();
                }
            }
        } else {
            beatmapSprites[0].setBeatmapInfo(beatmapSetInfo.get(beatmapId));
        }
    }

    private void initTracks() {
        if (beatmapId == -1) {
            // Tracks are originally sorted by osu!droid difficulty, so for osu!standard difficulty they need to be sorted again.
            if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) {
                Collections.sort(beatmapSetInfo.getBeatmaps(), (o1, o2) -> Float.compare(o1.getStandardStarRating(), o2.getStandardStarRating()));
            } else {
                Collections.sort(beatmapSetInfo.getBeatmaps(), (o1, o2) -> Float.compare(o1.getDroidStarRating(), o2.getDroidStarRating()));
            }

            for (int i = 0; i < beatmapSprites.length; i++) {
                beatmapSprites[i] = SongMenuPool.getInstance().newTrack();
                beatmapSprites[i].setItem(this);
                beatmapSprites[i].setBeatmapInfo(beatmapSetInfo.get(i));
                if (!beatmapSprites[i].hasParent()) {
                    layer.attachChild(beatmapSprites[i]);
                }
                scene.registerTouchArea(beatmapSprites[i]);
                beatmapSprites[i].setVisible(true);
            }
        } else {
            beatmapSprites[0] = SongMenuPool.getInstance().newTrack();
            beatmapSprites[0].setItem(this);
            beatmapSprites[0].setBeatmapInfo(beatmapSetInfo.get(beatmapId));
            if (!beatmapSprites[0].hasParent()) {
                layer.attachChild(beatmapSprites[0]);
            }
            scene.registerTouchArea(beatmapSprites[0]);
            beatmapSprites[0].setVisible(true);
        }
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    public void showPropertiesMenu() {
        listener.get().showPropertiesMenu(this);
    }

    public void update(final float dt) {
        if (deleted) {
            return;
        }
        for (final BeatmapItem tr : beatmapSprites) {
            if (tr != null) {
                tr.update(dt);
            }
        }
    }

    public BeatmapInfo getFirstBeatmap(){
        return beatmapSetInfo.get(Math.max(beatmapId, 0));
    }

    public void removeFromScene() {
        if (scene == null) {
            return;
        }
        if (selected) {
            deselect();
        }
        freeBackground();
        visible = false;
        scene = null;
    }

    public int tryGetCorrespondingBeatmapId(String oldBeatmapPath){
        if (beatmapId <= -1){
            int i = 0;
            for (BeatmapInfo track : beatmapSetInfo.getBeatmaps()){
                if (track == null) continue;
                if (track.getPath().equals(oldBeatmapPath)){
                    return i;
                }
                i++;
            }
        }
        else if (beatmapSetInfo.get(beatmapId).getPath().equals(oldBeatmapPath)){
            return beatmapId;
        }
        return -1;
    }

    public BeatmapItem getBeatmapSpritesById(int index){
        return beatmapSprites[index];
    }
}
