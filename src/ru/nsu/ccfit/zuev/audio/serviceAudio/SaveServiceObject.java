package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import ru.nsu.ccfit.zuev.osu.AppException;
import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class SaveServiceObject extends Application {

    static private SongService songService;
    private String string = "NONONNOONONO";

    public static void finishAllActivities() {
        if (GlobalManager.getInstance().getMainActivity() != null)
            GlobalManager.getInstance().getMainActivity().finish();
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public SongService getSongService() {
        return songService;
    }

    public void setSongService(SongService object) {
        songService = object;
        if (songService != null) {
            System.out.println("SongService Created!");
        } else {
            System.out.println("SongService is NULL");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.w("onActivityDestroyed", "I'm going to Dead O_x");
                if (songService != null) {
                    Log.w("onActivityDestroyed", "I'm Dead x_x");
                    songService.hideNotifyPanel();
                }
            }
        });
    }
}
