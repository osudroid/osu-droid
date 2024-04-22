package ru.nsu.ccfit.zuev.osu;

import androidx.annotation.StringRes;

import ru.nsu.ccfit.zuev.osuplus.R;

public enum RankedStatus {

    ranked(R.string.ranked_status_ranked),
    approved(R.string.ranked_status_approved),
    qualified(R.string.ranked_status_qualified),
    loved(R.string.ranked_status_loved),
    pending(R.string.ranked_status_pending),
    workInProgress(R.string.ranked_status_wip),
    graveyard(R.string.ranked_status_graveyard);

    /**
     * The string resource ID of the status name, recommended to use this rather than name().
     */
    public final int stringId;


    RankedStatus(@StringRes int stringId) {
        this.stringId = stringId;
    }


    public static RankedStatus valueOf(int value) {
        return switch (value) {
            case 1 -> ranked;
            case 2 -> approved;
            case 3 -> qualified;
            case 4 -> loved;
            case 0 -> pending;
            case -1 -> workInProgress;
            case -2 -> graveyard;
            default -> throw new IllegalArgumentException("Invalid value: " + value);
        };
    }
}
