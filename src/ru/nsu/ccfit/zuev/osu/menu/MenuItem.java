package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MenuItem {
    private final MenuItemTrack[] trackSprites;
    private final BeatmapInfo beatmap;
    private final String trackDir;
    private final int bgHeight;
    private final String titleStr;
    private final String creatorStr;
    public float percentAppeared = 0;
    MenuItemBackground background;
    Scene scene;
    boolean selected = false;
    WeakReference<MenuItemListener> listener;
    private MenuItemTrack selTrack = null;
    private boolean visible = true;
    private String filterText;
    private boolean favorite;
    private boolean deleted = false;
    private Entity layer = null;

    public MenuItem(final MenuItemListener listener, final BeatmapInfo info,
                    final float x, final float y) {
        this.listener = new WeakReference<MenuItemListener>(listener);
        beatmap = info;
        trackDir = ScoreLibrary.getTrackDir(beatmap.getPath());
        bgHeight = ResourceManager.getInstance()
                .getTexture("menu-button-background").getHeight()
                - Utils.toRes(25);
//        titleStr = (beatmap.getArtistUnicode() == null ? beatmap.getArtist() : beatmap.getArtistUnicode()) + " - "
//                + (beatmap.getTitleUnicode() == null ? beatmap.getTitle() : beatmap.getTitleUnicode());
        titleStr = beatmap.getArtist() + " - " + beatmap.getTitle();
        creatorStr = StringTable.format(R.string.menu_creator,
                beatmap.getCreator());
        trackSprites = new MenuItemTrack[info.getCount()];

        final BeatmapProperties props = PropertiesLibrary.getInstance()
                .getProperties(info.getPath());
        favorite = props != null && props.isFavorite();

    }

    public BeatmapInfo getBeatmap() {
        return beatmap;
    }

    public void updateMarks(final TrackInfo track) {
        for (final MenuItemTrack tr : trackSprites) {
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
                    * (trackSprites.length - 1);
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
        return (bgHeight) * (trackSprites.length);
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
        for (final Sprite s : trackSprites) {
            if (s == null) {
                continue;
            }
            final float cy = y + oy + Config.getRES_HEIGHT() / 2
                    + s.getHeight() / 2;
            final float ox = x
                    + Utils.toRes(170 * (float) Math.abs(Math.cos(cy * Math.PI
                    / (Config.getRES_HEIGHT() * 2))));
            s.setPosition(ox - Utils.toRes(100), y + oy);
            oy += (s.getHeight() - Utils.toRes(25)) * percentAppeared;
        }
    }

    public void select(boolean reloadMusic, boolean reloadBG) {
        if (listener.get().isSelectAllowed() == false) {
            return;
        }
        if (scene == null) {
            return;
        }
        freeBackground();
        selected = true;
        listener.get().select(this);
        initTracks();
        percentAppeared = 0;
        final String musicFileName = beatmap.getMusic();
        if (reloadMusic) {
            listener.get().playMusic(musicFileName, beatmap.getPreviewTime());
        }

        selectTrack(trackSprites[0], reloadBG);
        trackSprites[0].setSelectedColor();
    }

    public void deselect() {
        if (scene == null) {
            return;
        }
        if (deleted == true) {
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
        if (selTrack != null) {
            selTrack.setDeselectColor();
        }
        selTrack = null;
    }

    public void applyFilter(final String filter, final boolean favs, Set<String> limit) {
        if ((favs == true && isFavorite() == false)
                || (limit != null && !limit.contains(trackDir))) {
            //System.out.println(trackDir);
            if (selected) {
                deselect();
            }
            freeBackground();
            visible = false;
            return;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(beatmap.getTitle());
        builder.append(' ');
        builder.append(beatmap.getArtist());
        builder.append(' ');
        builder.append(beatmap.getCreator());
        builder.append(' ');
        builder.append(beatmap.getTags());
        builder.append(' ');
        builder.append(beatmap.getSource());
        builder.append(' ');
        builder.append(beatmap.getTracks().get(0).getBeatmapSetID());
        filterText = filter;
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
                for (TrackInfo track : beatmap.getTracks()) {
                    vis |= visibleTrack(track, key, opt, value);
                }
                canVisible &= vis;
            } else {
                if (!lowerText.contains(filterText)) {
                    canVisible &= false;
                    break;
                }
            }
        }

        if (filter == null || filter == "") {
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

    private boolean visibleTrack(MenuItemTrack track, String key, String opt, String value) {
        if (key.equals("ar")) {
            return calOpt(track.getTrack().getApproachRate(), Float.parseFloat(value), opt);
        } else if (key.equals("od")) {
            return calOpt(track.getTrack().getOverallDifficulty(), Float.parseFloat(value), opt);
        } else if (key.equals("cs")) {
            return calOpt(track.getTrack().getCircleSize(), Float.parseFloat(value), opt);
        } else if (key.equals("hp")) {
            return calOpt(track.getTrack().getHpDrain(), Float.parseFloat(value), opt);
        } else if (key.equals("star")) {
            return calOpt(track.getTrack().getDifficulty(), Float.parseFloat(value), opt);
        }
        return false;
    }

    private boolean visibleTrack(TrackInfo track, String key, String opt, String value) {
        if (key.equals("ar")) {
            return calOpt(track.getApproachRate(), Float.parseFloat(value), opt);
        } else if (key.equals("od")) {
            return calOpt(track.getOverallDifficulty(), Float.parseFloat(value), opt);
        } else if (key.equals("cs")) {
            return calOpt(track.getCircleSize(), Float.parseFloat(value), opt);
        } else if (key.equals("hp")) {
            return calOpt(track.getHpDrain(), Float.parseFloat(value), opt);
        } else if (key.equals("star")) {
            return calOpt(track.getDifficulty(), Float.parseFloat(value), opt);
        }
        return false;
    }

    private boolean calOpt(float val1, float val2, String opt) {
        if (opt.equals("=")) {
            return val1 == val2;
        } else if (opt.equals("<")) {
            return val1 < val2;
        } else if (opt.equals(">")) {
            return val1 > val2;
        } else if (opt.equals("<=")) {
            return val1 <= val2;
        } else if (opt.equals(">=")) {
            return val1 >= val2;
        }
        return false;
    }

    public void delete() {

        if (selected) {
            deselect();
        }
        freeBackground();
        visible = false;
        deleted = true;
        LibraryManager.getInstance().deleteMap(beatmap);
    }

    public boolean isVisible() {
        return visible && (deleted == false);
    }

    public void stopScroll(final float y) {
        listener.get().stopScroll(y);
    }

    public void selectTrack(final MenuItemTrack track, boolean reloadBG) {
        selTrack = track;
        listener.get().selectTrack(track.getTrack(), reloadBG);
    }

    public boolean isTrackSelected(final MenuItemTrack track) {
        return selTrack == track;
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
        if (background.hasParent() == false) {
            layer.attachChild(background);
            scene.registerTouchArea(background);
        }
    }

    private void freeTracks() {

        for (int i = 0; i < trackSprites.length; i++) {
            trackSprites[i].setVisible(false);
            scene.unregisterTouchArea(trackSprites[i]);
            trackSprites[i].setVisible(false);
            SongMenuPool.getInstance().putTrack(trackSprites[i]);
            trackSprites[i] = null;
        }

    }

    private void initTracks() {
        for (int i = 0; i < trackSprites.length; i++) {
            trackSprites[i] = SongMenuPool.getInstance().newTrack();
            trackSprites[i].setItem(this);
            trackSprites[i].setTrack(beatmap.getTrack(i), beatmap);
            beatmap.getTrack(i).setBeatmap(beatmap);
            if (trackSprites[i].hasParent() == false) {
                layer.attachChild(trackSprites[i]);
            }
            scene.registerTouchArea(trackSprites[i]);
            trackSprites[i].setVisible(true);
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
        for (final MenuItemTrack tr : trackSprites) {
            if (tr != null) {
                tr.update(dt);
            }
        }
    }
}
