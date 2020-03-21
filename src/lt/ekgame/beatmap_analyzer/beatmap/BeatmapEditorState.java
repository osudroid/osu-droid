package lt.ekgame.beatmap_analyzer.beatmap;

import java.util.ArrayList;
import java.util.List;

import lt.ekgame.beatmap_analyzer.parser.BeatmapException;
import lt.ekgame.beatmap_analyzer.parser.FilePart;
import lt.ekgame.beatmap_analyzer.parser.FilePartConfig;

public class BeatmapEditorState {

    private List<Integer> bookmarks = new ArrayList<>();
    private double distanceSpacing;
    private int beatDivisor;
    private int gridSize;
    private double timelineZoom;

    private BeatmapEditorState() {
    }

    public BeatmapEditorState(FilePart part) throws BeatmapException {
        FilePartConfig config = new FilePartConfig(part);
        distanceSpacing = config.getDouble("DistanceSpacing");
        beatDivisor = config.getInt("BeatDivisor");
        gridSize = config.getInt("GridSize");
        timelineZoom = config.getDouble("TimelineZoom", 1);

        if (config.hasProperty("Bookmarks")) {
            String[] line = config.getString("Bookmarks").split(",");
            for (int i = 0; i < line.length; i++) {
                if (!line[i].isEmpty()) {
                    bookmarks.add(Integer.parseInt(line[i].trim()));
                }
            }
			/*
			bookmarks = Arrays.asList(config.getString("Bookmarks").split(","))
				.stream().filter(o->!o.isEmpty())
				.map(o->Integer.parseInt(o.trim()))
				.collect(Collectors.toList());*/
        }
    }

    public BeatmapEditorState clone() {
        BeatmapEditorState clone = new BeatmapEditorState();
        clone.distanceSpacing = this.distanceSpacing;
        clone.beatDivisor = this.beatDivisor;
        clone.gridSize = this.gridSize;
        clone.timelineZoom = this.timelineZoom;
        clone.bookmarks = this.bookmarks;
        return clone;
    }

    public List<Integer> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<Integer> bookmarks) {
        this.bookmarks = bookmarks;
    }

    public double getDistanceSpacing() {
        return distanceSpacing;
    }

    public void setDistanceSpacing(double distanceSpacing) {
        this.distanceSpacing = distanceSpacing;
    }

    public int getBeatDivisor() {
        return beatDivisor;
    }

    public void setBeatDivisor(int beatDivisor) {
        this.beatDivisor = beatDivisor;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public double getTimelineZoom() {
        return timelineZoom;
    }

    public void setTimelineZoom(double timelineZoom) {
        this.timelineZoom = timelineZoom;
    }
}
