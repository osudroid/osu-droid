package com.reco1l.management.online;

public interface IOnlineObserver {

    void onLogin(UserInfo user);

    void onClear();
}
