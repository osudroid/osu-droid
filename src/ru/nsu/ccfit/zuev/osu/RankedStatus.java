package ru.nsu.ccfit.zuev.osu;

import androidx.annotation.StringRes;


public enum RankedStatus {

    ranked(com.edlplan.osudroidresource.R.string.ranked_status_ranked),
    approved(com.edlplan.osudroidresource.R.string.ranked_status_approved),
    qualified(com.edlplan.osudroidresource.R.string.ranked_status_qualified),
    loved(com.edlplan.osudroidresource.R.string.ranked_status_loved),
    pending(com.edlplan.osudroidresource.R.string.ranked_status_pending),
    workInProgress(com.edlplan.osudroidresource.R.string.ranked_status_wip),
    graveyard(com.edlplan.osudroidresource.R.string.ranked_status_graveyard);

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
