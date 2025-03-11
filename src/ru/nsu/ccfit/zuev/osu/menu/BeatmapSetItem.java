package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.osu.data.BeatmapSetInfo;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.DatabaseManager;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;

public class BeatmapSetItem {
    private final BeatmapItem[] beatmapItems;
    private BeatmapSetInfo beatmapSetInfo;
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
        beatmapSetDir = beatmapSetInfo.getDirectory();
        bgHeight = ResourceManager.getInstance()
                .getTexture("menu-button-background").getHeight()
                - Utils.toRes(25);
//        titleStr = (beatmap.getArtistUnicode() == null ? beatmap.getArtist() : beatmap.getArtistUnicode()) + " - "
//                + (beatmap.getTitleUnicode() == null ? beatmap.getTitle() : beatmap.getTitleUnicode());
        var beatmapInfo = this.beatmapSetInfo.getBeatmaps().get(0);

        titleStr = beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText();

        creatorStr = StringTable.format(com.osudroid.resources.R.string.menu_creator,
                beatmapInfo.getCreator());
        beatmapItems = new BeatmapItem[beatmapSetInfo.getCount()];

        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(beatmapSetInfo.getDirectory());
        favorite = props != null && props.isFavorite();

    }

    public BeatmapSetItem(final MenuItemListener listener, final BeatmapSetInfo beatmapSetInfo, int id) {
        this.listener = new WeakReference<>(listener);
        this.beatmapSetInfo = beatmapSetInfo;
        beatmapSetDir = this.beatmapSetInfo.getDirectory();
        bgHeight = ResourceManager.getInstance()
                .getTexture("menu-button-background").getHeight()
                - Utils.toRes(25);
//        titleStr = (beatmap.getArtistUnicode() == null ? beatmap.getArtist() : beatmap.getArtistUnicode()) + " - "
//                + (beatmap.getTitleUnicode() == null ? beatmap.getTitle() : beatmap.getTitleUnicode());
        var beatmapInfo = this.beatmapSetInfo.getBeatmaps().get(0);

        titleStr = beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText();

        creatorStr = StringTable.format(com.osudroid.resources.R.string.menu_creator, beatmapInfo.getCreator());
        beatmapItems = new BeatmapItem[1];
        beatmapId = id;

        var props = DatabaseManager.getBeatmapOptionsTable().getOptions(beatmapSetInfo.getDirectory());
        favorite = props != null && props.isFavorite();

    }


    public void setBeatmapSetInfo(BeatmapSetInfo beatmapSetInfo) {
        this.beatmapSetInfo = beatmapSetInfo;
    }

    public BeatmapSetInfo getBeatmapSetInfo() {
        return beatmapSetInfo;
    }


    public void updateMarks() {
        for (final BeatmapItem tr : beatmapItems) {
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
                    * (beatmapItems.length - 1);
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
        return (bgHeight) * (beatmapItems.length);
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
        for (final Sprite s : beatmapItems) {
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

    public void select() {
        if (!listener.get().isSelectAllowed() || scene == null) {
            return;
        }

        freeBackground();
        selected = true;
        listener.get().select(this);
        initBeatmaps();
        percentAppeared = 0;

        selectBeatmap(beatmapItems[0], true);
        beatmapItems[0].setSelectedColor();
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
        deselectBeatmap();
        freeBeatmaps();
    }

    public void deselectBeatmap() {
        if (scene == null) {
            return;
        }
        if (selectedBeatmapItem != null) {
            selectedBeatmapItem.setDeselectColor();
        }
        selectedBeatmapItem = null;
    }

    public void applyFilter(final String filter, final boolean favs, List<String> limit) {
        if ((favs && !isFavorite())
                || (limit != null && !limit.isEmpty() && !limit.contains(beatmapSetDir))) {
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
        for (var i = beatmapSetInfo.getCount() - 1; i >= 0; i--) {
            builder.append(' ');
            builder.append(beatmapSetInfo.getBeatmap(i).getVersion());
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
                    for (var i = beatmapSetInfo.getCount() - 1; i >= 0; i--) {
                        if (key != null) {
                            vis |= visibleBeatmap(beatmapSetInfo.getBeatmap(i), key, opt, value);
                        }
                    }
                }
                else{
                    if (key != null) {
                        vis = visibleBeatmap(beatmapSetInfo.getBeatmap(beatmapId), key, opt, value);
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

    private boolean visibleBeatmap(BeatmapInfo beatmapInfo, String key, String opt, String value) {
        switch (key) {
            case "ar":
                return calOpt(beatmapInfo.getApproachRate(), Float.parseFloat(value), opt);
            case "od":
                return calOpt(beatmapInfo.getOverallDifficulty(), Float.parseFloat(value), opt);
            case "cs":
                return calOpt(beatmapInfo.getCircleSize(), Float.parseFloat(value), opt);
            case "hp":
                return calOpt(beatmapInfo.getHpDrainRate(), Float.parseFloat(value), opt);
            case "droidstar":
                return calOpt(beatmapInfo.getStarRating(DifficultyAlgorithm.droid), Float.parseFloat(value), opt);
            case "standardstar":
            case "star":
                return calOpt(beatmapInfo.getStarRating(DifficultyAlgorithm.standard), Float.parseFloat(value), opt);
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

    public boolean isBeatmapSelected(final BeatmapItem beatmapItem) {
        return selectedBeatmapItem == beatmapItem;
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

    private void freeBeatmaps() {

        for (int i = 0; i < beatmapItems.length; i++) {
            beatmapItems[i].setVisible(false);
            scene.unregisterTouchArea(beatmapItems[i]);
            beatmapItems[i].setVisible(false);
            SongMenuPool.getInstance().putBeatmapItem(beatmapItems[i]);
            beatmapItems[i] = null;
        }

    }

    public void reloadBeatmaps() {
        if (beatmapId == -1) {
            // Tracks are originally sorted by osu!droid difficulty, so for osu!standard difficulty they need to be sorted again.
            Collections.sort(beatmapSetInfo.getBeatmaps(), (o1, o2) -> Float.compare(o1.getStarRating(), o2.getStarRating()));

            var selectedBeatmap = selectedBeatmapItem != null ? selectedBeatmapItem.getBeatmapInfo() : null;

            for (int i = 0; i < beatmapItems.length; i++) {
                beatmapItems[i].setBeatmapInfo(beatmapSetInfo.getBeatmap(i));

                // Ensure the selected track is still selected after reloading.
                if (selectedBeatmap != null && selectedBeatmap == beatmapSetInfo.getBeatmap(i)) {
                    beatmapItems[i].setSelectedColor();
                    selectedBeatmapItem = beatmapItems[i];
                } else {
                    beatmapItems[i].setDeselectColor();
                }
            }
        } else {
            beatmapItems[0].setBeatmapInfo(beatmapSetInfo.getBeatmap(beatmapId));
        }
    }

    private void initBeatmaps() {
        if (beatmapId == -1) {
            // Tracks are originally sorted by osu!droid difficulty, so for osu!standard difficulty they need to be sorted again.
            Collections.sort(beatmapSetInfo.getBeatmaps(), (o1, o2) -> Float.compare(o1.getStarRating(), o2.getStarRating()));

            for (int i = 0; i < beatmapItems.length; i++) {
                beatmapItems[i] = SongMenuPool.getInstance().newBeatmapItem();
                beatmapItems[i].setItem(this);
                beatmapItems[i].setBeatmapInfo(beatmapSetInfo.getBeatmap(i));
                if (!beatmapItems[i].hasParent()) {
                    layer.attachChild(beatmapItems[i]);
                }
                scene.registerTouchArea(beatmapItems[i]);
                beatmapItems[i].setVisible(true);
            }
        } else {
            beatmapItems[0] = SongMenuPool.getInstance().newBeatmapItem();
            beatmapItems[0].setItem(this);
            beatmapItems[0].setBeatmapInfo(beatmapSetInfo.getBeatmap(beatmapId));
            if (!beatmapItems[0].hasParent()) {
                layer.attachChild(beatmapItems[0]);
            }
            scene.registerTouchArea(beatmapItems[0]);
            beatmapItems[0].setVisible(true);
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
        for (final BeatmapItem tr : beatmapItems) {
            if (tr != null) {
                tr.update(dt);
            }
        }
    }

    public BeatmapInfo getFirstBeatmap(){
        return beatmapSetInfo.getBeatmap(Math.max(beatmapId, 0));
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

    public int tryGetCorrespondingBeatmapId(String beatmapFilename){
        if (beatmapId <= -1){
            for (var i = beatmapSetInfo.getCount() - 1; i >= 0; i--) {
                if (beatmapSetInfo.getBeatmap(i).getFilename().equals(beatmapFilename)){
                    return i;
                }
            }
        } else if (beatmapSetInfo.getBeatmap(beatmapId).getFilename().equals(beatmapFilename)){
            return beatmapId;
        }
        return -1;
    }

    public BeatmapItem getBeatmapSpritesById(int index){
        return beatmapItems[index % beatmapItems.length];
    }
}
