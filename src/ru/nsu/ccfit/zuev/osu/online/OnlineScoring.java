package ru.nsu.ccfit.zuev.osu.online;

import android.os.AsyncTask;

import com.reco1l.ui.data.tables.NotificationTable;
import com.reco1l.interfaces.IMainClasses;
import com.reco1l.ui.platform.UI;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class OnlineScoring implements IMainClasses, UI {
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

    public void login() {
        if (OnlineManager.getInstance().isStayOnline() == false)
            return;
        NotificationTable.accountLogIn(null, 0);
        avatarLoaded = false;

        new AsyncTaskLoader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new OsuAsyncCallback() {

            public void run() {
                synchronized (onlineMutex) {
                    boolean success = false;
                    onlineHelper.clear();
                    //Trying to send request
                    for (int i = 0; i < 3; i++) {
                        NotificationTable.accountLogIn("try", i);

                        try {
                            success = OnlineManager.getInstance().logIn();
                        } catch (OnlineManagerException e) {
                            Debug.e("Login error: " + e.getMessage());
                            NotificationTable.accountLogIn("fail", i);
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
                        NotificationTable.accountLogIn("success", 0);
                        OnlineManager.getInstance().setStayOnline(true);
                    } else {
                        NotificationTable.accountLogIn("error", 0);
                        OnlineManager.getInstance().setStayOnline(false);
                    }
                }
            }

            public void onComplete() {
                onlineHelper.update();
            }
        });
    }

    public void startPlay(final TrackInfo track, final String hash) {
        if (OnlineManager.getInstance().isStayOnline() == false)
            return;
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                synchronized (onlineMutex) {

                    for (int i = 0; i < attemptCount; i++) {
                        try {
                            OnlineManager.getInstance().startPlay(track, hash);
                        } catch (OnlineManagerException e) {
                            Debug.e("Login error: " + e.getMessage());
                            continue;
                        }
                        break;
                    }

                    if (OnlineManager.getInstance().getFailMessage().length() > 0) {
                        ToastLogger.showText(OnlineManager.getInstance().getFailMessage(), true);
                    }
                }
            }


            public void onComplete() {
                // TODO Auto-generated method stub

            }
        });
    }

    public void sendRecord(final StatisticV2 record, final SendingPanel panel, final String replay) {
        if (OnlineManager.getInstance().isStayOnline() == false)
            return;
        if (OnlineManager.getInstance().isReadyToSend() == false)
            return;

        Debug.i("Sending score");

        final String recordData = record.compile();

        new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                boolean success = false;
                synchronized (onlineMutex) {
                    for (int i = 0; i < attemptCount; i++) {
                        if (!record.isScoreValid()) {
                            Debug.e("Detected illegal actions.");
                            break;
                        }

                        try {
                            success = OnlineManager.getInstance().sendRecord(recordData);
                        } catch (OnlineManagerException e) {
                            Debug.e("Login error: " + e.getMessage());
                            success = false;
                        }

                        if (OnlineManager.getInstance().getFailMessage().length() > 0) {
                            ToastLogger.showText(OnlineManager.getInstance().getFailMessage(), true);
                            if (OnlineManager.getInstance().getFailMessage().equals("Invalid record data"))
                                i = attemptCount;
                        } else if (success) {
                            onlineHelper.update();
                            OnlineManager mgr = OnlineManager.getInstance();
                            panel.show(mgr.getMapRank(), mgr.getScore(), mgr.getRank(), mgr.getAccuracy());
                            OnlineManager.getInstance().sendReplay(replay);
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


            public void onComplete() {
                // TODO Auto-generated method stub

            }
        });
    }

    public ArrayList<String> getTop(final File trackFile, final String hash) {
        synchronized (onlineMutex) {
            try {
                return OnlineManager.getInstance().getTop(trackFile, hash);
            } catch (OnlineManagerException e) {
                Debug.e("Cannot load scores " + e.getMessage());
                return new ArrayList<String>();
            }
        }
    }

}
