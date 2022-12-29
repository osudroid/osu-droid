package ru.nsu.ccfit.zuev.osu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class BeatmapInfo implements Serializable {
    private static final long serialVersionUID = -3865268984942011628L;
    private final ArrayList<TrackInfo> tracks = new ArrayList<TrackInfo>();
    private String title;
    private String titleUnicode;
    private String artist;
    private String artistUnicode;
    private String creator;
    private String path;
    private String source;
    private String tags;
    private String music = null;
    private long date;
    private int previewTime;

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
        return music;
    }

    public void setMusic(final String music) {
        this.music = music;
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
        return previewTime;
    }

    public void setPreviewTime(final int previewTime) {
        this.previewTime = previewTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BeatmapInfo that = (BeatmapInfo) o;
        return Objects.equals(title, that.title) && Objects.equals(artist, that.artist) && Objects.equals(path, that.path);
    }

    public TrackInfo getLastTrack() {
        return tracks.get(tracks.size() - 1);
    }
}
