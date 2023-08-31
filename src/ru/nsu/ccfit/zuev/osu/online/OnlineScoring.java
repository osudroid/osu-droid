package ru.nsu.ccfit.zuev.osu.online;

import com.reco1l.legacy.ui.multiplayer.LobbyScene;
import com.reco1l.legacy.ui.multiplayer.RoomScene;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.async.AsyncTask;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class OnlineScoring {
    private static final int attemptCount = 5;
    private static OnlineScoring instance = null;
    private Boolean onlineMutex = new Boolean(false);
    private OnlinePanel panel = null;
    private OnlinePanel secondPanel = null;
    private boolean avatarLoaded = false;

    public static OnlineScoring getInstance() {
        if (instance == null)
            instance = new OnlineScoring();
        return instance;
    }

    public void createPanel() {
        panel = new OnlinePanel();
    }

    public OnlinePanel getPanel() {
        return panel;
    }

    public OnlinePanel createSecondPanel() {
        if (OnlineManager.getInstance().isStayOnline() == false)
            return null;
        secondPanel = new OnlinePanel();
        secondPanel.setInfo();
        String avatarURL = OnlineManager.getInstance().getAvatarURL();
        secondPanel.setAvatar(avatarLoaded && !avatarURL.isEmpty() ? avatarURL : null);
        return secondPanel;
    }

    public OnlinePanel getSecondPanel() {
        return secondPanel;
    }

    public void setPanelMessage(String message, String submessage) {
        panel.setMessage(message, submessage);
        if (secondPanel != null)
            secondPanel.setMessage(message, submessage);
    }

    public void updatePanels() {
        panel.setInfo();
        if (secondPanel != null)
            secondPanel.setInfo();

        LobbyScene.updateOnlinePanel();
        RoomScene.updateOnlinePanel();
    }

    public void updatePanelAvatars() {
        final String avatarUrl = OnlineManager.getInstance().getAvatarURL();
        String texname = avatarLoaded && !avatarUrl.isEmpty() ? avatarUrl : null;
        panel.setAvatar(texname);
        if (secondPanel != null)
            secondPanel.setAvatar(texname);

        LobbyScene.updateOnlinePanel();
        RoomScene.updateOnlinePanel();
    }

    public void login() {
        if (OnlineManager.getInstance().isStayOnline() == false)
            return;
        avatarLoaded = false;
        new AsyncTask() {
            @Override
            public void run() {
                synchronized (onlineMutex) {
                    boolean success = false;

                    //Trying to send request
                    for (int i = 0; i < 3; i++) {
                        setPanelMessage("Logging in...", "");

                        try {
                            success = OnlineManager.getInstance().logIn();
                        } catch (OnlineManager.OnlineManagerException e) {
                            Debug.e("Login error: " + e.getMessage());
                            setPanelMessage("Login failed", "Retrying in 5 sec");
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e1) {
                                break;
                            }
                            continue;
                        }
                        break;
                    }
                    if (success) {
                        updatePanels();
                        OnlineManager.getInstance().setStayOnline(true);
                        loadAvatar(true);
                    } else {
                        setPanelMessage("Cannot log in", OnlineManager.getInstance().getFailMessage());
                        OnlineManager.getInstance().setStayOnline(false);
                    }
                }
            }
        }.execute();
    }

    public void sendRecord(final StatisticV2 record, final SendingPanel panel, final String mapMD5, final String replay) {
        if (!OnlineManager.getInstance().isStayOnline())
            return;

        Debug.i("Sending score");

        final String recordData = record.compile();

        new AsyncTask() {
            @Override
            public void run() {
                boolean success = false;
                synchronized (onlineMutex) {
                    for (int i = 0; i < attemptCount; i++) {
                        if (!record.isScoreValid()) {
                            Debug.e("Detected illegal actions.");
                            break;
                        }

                        try {
                            success = OnlineManager.getInstance().sendRecord(recordData, mapMD5, replay);
                        } catch (OnlineManager.OnlineManagerException e) {
                            Debug.e("Login error: " + e.getMessage());
                            success = false;
                        }

                        if (OnlineManager.getInstance().getFailMessage().length() > 0) {
                            ToastLogger.showText(OnlineManager.getInstance().getFailMessage(), true);
                            if (OnlineManager.getInstance().getFailMessage().equals("Invalid record data"))
                                i = attemptCount;
                        } else if (success) {
                            updatePanels();
                            OnlineManager mgr = OnlineManager.getInstance();
                            panel.show(mgr.getMapRank(), mgr.getScore(), mgr.getRank(), mgr.getAccuracy());
                            break;
                        }


                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                        }
                    }

                    if (!success) {
                        panel.setFail();
                    }

                }
            }
        }.execute();
    }

    public void loadAvatar(final boolean both) {
        if (!OnlineManager.getInstance().isStayOnline()) return;
        final String avatarUrl = OnlineManager.getInstance().getAvatarURL();
        if (avatarUrl == null || avatarUrl.length() == 0)
            return;

        new AsyncTask() {
            @Override
            public void run() {
                synchronized (onlineMutex) {
                    avatarLoaded = OnlineManager.getInstance().loadAvatarToTextureManager();
                    if (both)
                        updatePanelAvatars();
                    else if (secondPanel != null)
                        secondPanel.setAvatar(avatarLoaded ? avatarUrl : null);
                }
            }
        }.execute();
    }

    public boolean isAvatarLoaded() {
        return avatarLoaded;
    }
}
