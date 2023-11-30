package ru.nsu.ccfit.zuev.osu.online;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;

public class OnlineManager {

    public static final String hostname = "osudroid.moe";

    public static final String endpoint = "https://" + hostname + "/api/";

    public static final String updateEndpoint = endpoint + "update.php?lang=";

    public static final String defaultAvatarURL = "https://" + hostname + "/user/avatar/0.png";

    public static final OkHttpClient client = new OkHttpClient();

    private static final String onlineVersion = "36";

    private static OnlineManager instance = null;

    private String failMessage = "";

    private boolean stayOnline = true;

    private String ssid = "";

    private long userId = -1L;

    private String playID = "";

    private String username = "";

    private String password = "";

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

    public void Init() {
        this.stayOnline = Config.isStayOnline();
        this.username = Config.getOnlineUsername();
        this.password = Config.getOnlinePassword();
    }

    private ArrayList<String> sendRequest(PostBuilder post, String url) {
        ArrayList<String> response;
        response = post.requestWithAttempts(url, 3);
        failMessage = "";

        if (response.isEmpty() || response.get(0).isEmpty()) {
            failMessage = "Got empty response";
            Debug.i("Received empty response!");
            return null;
        }

        if (!response.get(0).equals("SUCCESS")) {
            Debug.i("sendRequest response code:  " + response.get(0));
            if (response.size() >= 2) {
                failMessage = response.get(1);
            } else {
                failMessage = "Unknown server error";
            }
            Debug.i("Received fail: " + failMessage);
            return null;
        }


        return response;
    }

    public boolean logIn() {
        return logIn(username, password);
    }

    public synchronized boolean logIn(String username, String password) {
        this.username = username;
        this.password = password;

        PostBuilder post = new PostBuilder();
        post.addParam("username", username);
        post.addParam("password", MD5Calcuator.getStringMD5(escapeHTMLSpecialCharacters(addSlashes(String.valueOf(password).trim())) + "taikotaiko"));
        post.addParam("version", onlineVersion);

        ArrayList<String> response = sendRequest(post, endpoint + "login.php");

        if (response == null) {
            return false;
        }
        if (response.size() < 2) {
            failMessage = "Invalid server response";
            return false;
        }

        String[] params = response.get(1).split("\\s+");
        if (params.length < 6) {
            failMessage = "Invalid server response";
            return false;
        }
        userId = Long.parseLong(params[0]);
        ssid = params[1];
        rank = Integer.parseInt(params[2]);
        score = Long.parseLong(params[3]);
        accuracy = Integer.parseInt(params[4]) / 100000f;
        this.username = params[5];
        if (params.length >= 7) {
            avatarURL = params[6];
        } else {
            avatarURL = "";
        }

        Bundle bParams = new Bundle();
        bParams.putString(FirebaseAnalytics.Param.METHOD, "ingame");
        GlobalManager.getInstance().getMainActivity().getAnalytics().logEvent(FirebaseAnalytics.Event.LOGIN, bParams);

        return true;
    }

    boolean tryToLogIn() {
        if (!logIn(username, password)) {
            stayOnline = false;
            return false;
        }
        return true;
    }

    public void startPlay(final TrackInfo track, final String hash) {
        Debug.i("Starting play...");
        playID = null;
        final BeatmapInfo beatmap = track.getBeatmap();
        if (beatmap == null) {
            return;
        }

        File trackfile = new File(track.getFilename());
        trackfile.getParentFile().getName();
        String osuID = trackfile.getParentFile().getName();
        Debug.i("osuid = " + osuID);
        if (osuID.matches("^[0-9]+ .*")) {
            osuID = osuID.substring(0, osuID.indexOf(' '));
        } else {
            osuID = null;
        }

        PostBuilder post = new PostBuilder();
        post.addParam("userID", String.valueOf(userId));
        post.addParam("ssid", ssid);
        post.addParam("filename", trackfile.getName());
        post.addParam("hash", hash);
        post.addParam("songTitle", beatmap.getTitle());
        post.addParam("songArtist", beatmap.getArtist());
        post.addParam("songCreator", beatmap.getCreator());
        if (osuID != null) {
            post.addParam("songID", osuID);
        }

        ArrayList<String> response = sendRequest(post, endpoint + "submit.php");

        if (response == null) {
            if (failMessage.equals("Cannot log in") && stayOnline) {
                if (tryToLogIn()) {
                    startPlay(track, hash);
                }
            }
            return;
        }

        if (response.size() < 2) {
            failMessage = "Invalid server response";
            return;
        }

        String[] resp = response.get(1).split("\\s+");
        if (resp.length < 2) {
            failMessage = "Invalid server response";
            return;
        }

        if (!resp[0].equals("1")) {
            return;
        }

        playID = resp[1];
        Debug.i("Getting play ID = " + playID);
    }

    public boolean sendRecord(String data) {
        if (playID == null || playID.isEmpty()) {
            failMessage = "I don't have play ID";
            return false;
        }

        Debug.i("Sending record...");

        PostBuilder post = new PostBuilder();
        post.addParam("userID", String.valueOf(userId));
        post.addParam("playID", playID);
        post.addParam("data", data);

        ArrayList<String> response = sendRequest(post, endpoint + "submit.php");

        if (response == null) {
            return false;
        }

        if (failMessage.equals("Invalid record data")) {
            return false;
        }

        if (response.size() < 2) {
            failMessage = "Invalid server response";
            return false;
        }

        String[] resp = response.get(1).split("\\s+");
        if (resp.length < 4) {
            failMessage = "Invalid server response";
            return false;
        }


        rank = Integer.parseInt(resp[0]);
        score = Long.parseLong(resp[1]);
        accuracy = Integer.parseInt(resp[2]) / 100000f;
        mapRank = Integer.parseInt(resp[3]);

        if (resp.length >= 5) {
            replayID = Integer.parseInt(resp[4]);
        } else {
            replayID = 0;
        }

        return true;
    }

    public ArrayList<String> getTop(final File trackFile, final String hash) {
        PostBuilder post = new PostBuilder();
        post.addParam("filename", trackFile.getName());
        post.addParam("hash", hash);
        post.addParam("uid", String.valueOf(userId));

        ArrayList<String> response = sendRequest(post, endpoint + "getrank.php");

        if (response == null) {
            return new ArrayList<>();
        }

        response.remove(0);

        return response;
    }

    public boolean loadAvatarToTextureManager() {
        return loadAvatarToTextureManager(avatarURL);
    }

    public boolean loadAvatarToTextureManager(String avatarURL) {
        if (avatarURL == null || avatarURL.isEmpty()) {
            return false;
        }

        String filename = MD5Calcuator.getStringMD5(avatarURL);
        Debug.i("Loading avatar from " + avatarURL);
        Debug.i("filename = " + filename);
        File picfile = new File(Config.getCachePath(), filename);
        OnlineFileOperator.downloadFile(avatarURL, picfile.getAbsolutePath(), true);

        var bitmap = loadAvatarToBitmap(picfile);
        int imageWidth = 0, imageHeight = 0;

        if (bitmap != null) {
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
        }

        if (imageWidth * imageHeight > 0) {
            // Avatar has been cached locally
            ResourceManager.getInstance().loadHighQualityFile(filename, picfile);
            if (ResourceManager.getInstance().getAvatarTextureIfLoaded(avatarURL) != null) {
                return true;
            }
        } else {
            // Avatar not found, download the default avatar
            String defaultAvatarFilename = MD5Calcuator.getStringMD5(defaultAvatarURL);
            File avatarFile = new File(Config.getCachePath(), defaultAvatarFilename);
            OnlineFileOperator.downloadFile(defaultAvatarURL, avatarFile.getAbsolutePath());

            bitmap = loadAvatarToBitmap(avatarFile);
            if (bitmap != null) {
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
            }

            if (imageWidth * imageHeight > 0) {
                //Avatar has been cached locally
                ResourceManager.getInstance().loadHighQualityFile(defaultAvatarFilename, avatarFile);
                if (ResourceManager.getInstance().getAvatarTextureIfLoaded(defaultAvatarURL) != null) {
                    return true;
                }
            }
        }

        Debug.i("Success!");
        return false;
    }

    private Bitmap loadAvatarToBitmap(File avatarFile) {
        if (!avatarFile.exists()) {
            return null;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            return BitmapFactory.decodeFile(avatarFile.getPath());
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void sendReplay(String filename) {
        Debug.i("Sending replay '" + filename + "' for id = " + replayID);
        OnlineFileOperator.sendFile(endpoint + "upload.php", filename, String.valueOf(replayID));
    }

    public String getScorePack(int playid) {
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

    public long getUserId() {
        return userId;
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

    private String escapeHTMLSpecialCharacters(String str) {
        return str.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&apos;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String addSlashes(String str) {
        return str.replace("'", "\\'").replace("\"", "\\\"").replace("\\", "\\\\");
    }

}