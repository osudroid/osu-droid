package main.osu.online;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import main.osu.BeatmapInfo;
import main.osu.Config;
import main.osu.GlobalManager;
import main.osu.ResourceManager;
import main.osu.TrackInfo;
import okhttp3.OkHttpClient;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.util.ArrayList;

import main.osu.helper.MD5Calcuator;
import main.osu.online.PostBuilder.RequestException;

public class OnlineManager {
    public static final String hostname = "osudroid.moe";
    public static final String endpoint = "https://" + hostname + "/api/";
    private static final String onlineVersion = "29";

    public static final OkHttpClient client = new OkHttpClient();

    private static OnlineManager instance = null;
    private Context context;
    private String failMessage = "";

    private boolean stayOnline = true;
    private String ssid = "";
    private String userId = "";
    private String playID = "";

    private String username = "";
    private String password = "";
    private String deviceID = "";
    private long rank = 0;
    private long score = 0;
    private float accuracy = 0;
    private String avatarURL = "";
    private int mapRank;
    private int replayID = 0;

    public static OnlineManager getInstance() {
        if (instance == null) {
            instance = new OnlineManager();
        }
        return instance;
    }

    public static String getReplayURL(int playID) {
        return endpoint + "upload/" + playID + ".odr";
    }

    public void Init(Context context) {
        this.stayOnline = Config.isStayOnline();
        this.username = Config.getOnlineUsername();
        this.password = Config.getOnlinePassword();
        this.deviceID = Config.getOnlineDeviceID();
        this.context = context;
    }

    private ArrayList<String> sendRequest(PostBuilder post, String url) throws OnlineManagerException {
        ArrayList<String> response;
        try {
            response = post.requestWithAttempts(url, 3);
        } catch (RequestException e) {
            Debug.e(e.getMessage(), e);
            failMessage = "Cannot connect to server";
            throw new OnlineManagerException("Cannot connect to server", e);
        }
        failMessage = "";

        //TODO debug code
		/*Debug.i("Received " + response.size() + " lines");
		for(String str: response)
		{
			Debug.i(str);
		}*/

        if (response.size() == 0 || response.get(0).length() == 0) {
            failMessage = "Got empty response";
            Debug.i("Received empty response!");
            return null;
        }

        if (!response.get(0).equals("SUCCESS")) {
            Debug.i("sendRequest response code:  " + response.get(0));
            if (response.size() >= 2) {
                failMessage = response.get(1);
            } else
                failMessage = "Unknown server error";
            Debug.i("Received fail: " + failMessage);
            return null;
        }


        return response;
    }

    public ArrayList<String> getTop(final File trackFile, final String hash) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("filename", trackFile.getName());
        post.addParam("hash", hash);

        ArrayList<String> response = sendRequest(post, endpoint + "getrank.php");

        if (response == null) {
            return new ArrayList<String>();
        }

        response.remove(0);

        return response;
    }

    public String getScorePack(int playid) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("playID", String.valueOf(playid));

        ArrayList<String> response = sendRequest(post, endpoint + "gettop.php");

        if (response == null || response.size() < 2) {
            return "";
        }

        return response.get(1);
    }

    public String getFailMessage() {
        return failMessage;
    }

    public long getRank() {
        return rank;
    }

    public long getScore() {
        return score;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public boolean isStayOnline() {
        return stayOnline;
    }

    public void setStayOnline(boolean stayOnline) {
        this.stayOnline = stayOnline;
    }

    public boolean isReadyToSend() {
        return (playID != null);
    }

    public int getMapRank() {
        return mapRank;
    }

    public static class OnlineManagerException extends Exception {
        private static final long serialVersionUID = -5703212596292949401L;

        public OnlineManagerException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public OnlineManagerException(final String message) {
            super(message);
        }
    }
}
