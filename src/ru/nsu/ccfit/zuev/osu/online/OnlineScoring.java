package ru.nsu.ccfit.zuev.osu.online;

import android.content.Intent;
import android.net.Uri;

import com.google.android.material.snackbar.Snackbar;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.Execution;
import com.reco1l.osu.multiplayer.LobbyScene;
import com.reco1l.osu.multiplayer.RoomScene;
import com.rian.osu.ui.SendingPanel;

import org.anddev.andengine.util.Debug;

import java.util.concurrent.CancellationException;

import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class OnlineScoring {
    private static final int attemptCount = 5;
    private static OnlineScoring instance = null;
    private final Boolean onlineMutex = Boolean.FALSE;
    private OnlinePanel panel = null;
    private OnlinePanel secondPanel = null;
    private boolean avatarLoaded = false;
    private final Snackbar snackbar = Snackbar.make(
            GlobalManager.getInstance().getMainActivity().getWindow().getDecorView(),
            "", 10000);

    private Job loginJob, avatarJob;

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
        if (!OnlineManager.getInstance().isStayOnline())
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
        if (!OnlineManager.getInstance().isStayOnline())
            return;
        avatarLoaded = false;

        if (loginJob != null) {
            loginJob.cancel(new CancellationException("Login cancelled"));
        }

        loginJob = Execution.async((scope) -> {
            synchronized (onlineMutex) {
                boolean success = false;

                //Trying to send request
                for (int i = 0; i < 3; i++) {
                    setPanelMessage("Logging in...", "");

                    try {
                        JobKt.ensureActive(scope.getCoroutineContext());
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

                    if (OnlineManager.getInstance().getFailMessage().equals("Cannot connect to server")) {
                        Execution.mainThread(() -> {
                            snackbar.dismiss();
                            snackbar.setText("Cannot connect to server. Please check the following article for troubleshooting.");

                            snackbar.setAction("Check", (v) -> {
                                var intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://neroyuki.github.io/osudroid-guide/help/login_fail"));

                                GlobalManager.getInstance().getMainActivity().startActivity(intent);
                            });

                            snackbar.show();
                        });
                    }
                }
            }
        });
    }

    public void startPlay(final BeatmapInfo beatmapInfo, final String hash) {
        if (!OnlineManager.getInstance().isStayOnline())
            return;

        Execution.async(() -> {
            synchronized (onlineMutex) {
                for (int i = 0; i < attemptCount; i++) {
                    try {
                        OnlineManager.getInstance().startPlay(beatmapInfo, hash);
                    } catch (OnlineManager.OnlineManagerException e) {
                        Debug.e("Login error: " + e.getMessage());
                        continue;
                    }
                    break;
                }

                if (OnlineManager.getInstance().getFailMessage().length() > 0) {
                    ToastLogger.showText(OnlineManager.getInstance().getFailMessage(), true);
                }
            }
        });
    }

    public void sendRecord(final StatisticV2 record, final SendingPanel panel, final String replay) {
        if (!OnlineManager.getInstance().isStayOnline() || !OnlineManager.getInstance().isReadyToSend())
            return;

        Debug.i("Sending score");

        final String recordData = record.compile();

        Execution.async(() -> {
            boolean success = false;
            synchronized (onlineMutex) {
                for (int i = 0; i < attemptCount; i++) {
                    if (!record.isScoreValid()) {
                        Debug.e("Detected illegal actions.");
                        break;
                    }

                    try {
                        success = OnlineManager.getInstance().sendRecord(recordData, replay);
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
                        panel.show(mgr.getMapRank(), mgr.getRank(), mgr.getScore(), mgr.getAccuracy(), mgr.getPP());
                        break;
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                    }
                }

                if (!success) {
                    panel.setFail();
                }
            }
        });
    }

    public void loadAvatar(final boolean both) {
        if (!OnlineManager.getInstance().isStayOnline()) return;
        final String avatarUrl = OnlineManager.getInstance().getAvatarURL();
        if (avatarUrl == null || avatarUrl.length() == 0)
            return;

        if (avatarJob != null) {
            avatarJob.cancel(new CancellationException("Avatar loading cancelled"));
        }

        avatarJob = Execution.async((scope) -> {
            synchronized (onlineMutex) {
                avatarLoaded = OnlineManager.getInstance().loadAvatarToTextureManager();
                JobKt.ensureActive(scope.getCoroutineContext());
                if (both)
                    updatePanelAvatars();
                else if (secondPanel != null)
                    secondPanel.setAvatar(avatarLoaded ? avatarUrl : null);
            }
        });
    }

    public boolean isAvatarLoaded() {
        return avatarLoaded;
    }
}
