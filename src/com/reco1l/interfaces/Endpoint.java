package com.reco1l.interfaces;

import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

// New backend support in client should replace this
public interface Endpoint {
    String Avatar_URL = "https://" + OnlineManager.hostname + "/user/avatar/?s=100&id=";

}
