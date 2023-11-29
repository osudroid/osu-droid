package ru.nsu.ccfit.zuev.osu;

import java.io.Serializable;
import java.util.ArrayList;

public class BeatmapInfo implements Serializable {

    private static final long serialVersionUID = -3865268984942011628L;

    private final ArrayList<TrackInfo> tracks = new ArrayList<>();

    private String title;

    private String titleUnicode;

    private String artist;

    private String artistUnicode;

    private String creator;

    private String path;

    private String source;

    private String tags;

    private long date;

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(final String tags) {
        this.tags = tags;
    }

    public String getMusic() {
        return tracks.get(0).getAudioFilename();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(final String artist) {
        this.artist = artist;
    }

    public String getTitleUnicode() {
        return titleUnicode;
    }

    public void setTitleUnicode(String titleUnicode) {
        this.titleUnicode = titleUnicode;
    }

    public String getArtistUnicode() {
        return artistUnicode;
    }

    public void setArtistUnicode(String artistUnicode) {
        this.artistUnicode = artistUnicode;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public void addTrack(final TrackInfo track) {
        tracks.add(track);
    }

    public TrackInfo getTrack(final int index) {
        return tracks.get(index);
    }

    public int getCount() {
        return tracks.size();
    }

    public ArrayList<TrackInfo> getTracks() {
        return tracks;
    }

    public long getDate() {
        return date;
    }

    public void setDate(final long date) {
        this.date = date;
    }

    public int getPreviewTime() {
        return tracks.get(0).getPreviewTime();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof BeatmapInfo && ((BeatmapInfo) o).getPath().equals(path);
    }

}
