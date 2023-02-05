package com.reco1l.interfaces.fields;

import com.reco1l.annotation.Legacy;

import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

// New backend support in client should replace this
@Legacy
public interface Endpoint {

    String Avatar_URL = "https://" + OnlineManager.hostname + "/user/avatar/?s=100&id=";

    String PROFILE_URL = "https://" + OnlineManager.hostname + "/game/profile.php?uid=";
}
