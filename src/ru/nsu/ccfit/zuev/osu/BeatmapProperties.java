package ru.nsu.ccfit.zuev.osu;

import java.io.Serializable;

import kotlin.Deprecated;

@Deprecated(message = "This class is used to do legacy migration, should not be used and will be removed in future releases.")
public class BeatmapProperties implements Serializable {
    private static final long serialVersionUID = -7229486402310659139L;

    public int offset;
    public boolean favorite;

    public int getOffset() {
        return (int) (Math.signum(offset) * Math.min(250, Math.abs(offset)));
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

}
