package ru.nsu.ccfit.zuev.osu.online;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.toolkt.kotlin.StringsKt;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import okhttp3.Request;
import okhttp3.RequestBody;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.online.PostBuilder.RequestException;
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode;

public class OnlineManager {
    public static final String hostname = "osudroid.moe";
    public static final String endpoint = "https://" + hostname + "/api/";
    public static final String updateEndpoint = endpoint + "update.php?lang=";
    public static final String defaultAvatarURL = "https://" + hostname + "/user/avatar/0.png";
    private static final String onlineVersion = "41";

    public static final OkHttpClient client = new OkHttpClient();

    private static OnlineManager instance = null;
    private String failMessage = "";

    private boolean stayOnline = true;
    private String ssid = "";
    private long userId = -1L;
    private String playID = "";

    private String username = "";
    private String password = "";
    private String deviceID = "";
    private long rank = 0;
    private long score = 0;
    private float accuracy = 0;
    private float pp = 0;
    private String avatarURL = "";
    private int mapRank;

    public static OnlineManager getInstance() {
        if (instance == null) {
            instance = new OnlineManager();
        }
        return instance;
    }

    public static String getReplayURL(int playID) {
        return endpoint + "upload/" + playID + ".odr";
    }

    public void init() {
        this.stayOnline = Config.isStayOnline();
        this.username = Config.getOnlineUsername();
        this.password = Config.getOnlinePassword();
        this.deviceID = Config.getOnlineDeviceID();
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

    public boolean logIn() throws OnlineManagerException {
        return logIn(username, password);
    }

    public boolean logIn(String username) throws OnlineManagerException {
        return logIn(username, password);
    }

    public synchronized boolean logIn(String username, String password) throws OnlineManagerException {
        this.username = username;
        this.password = password;

        PostBuilder post = new URLEncodedPostBuilder();
        post.addParam("username", username);
        post.addParam(
                "password",
                MD5Calculator.getStringMD5(
                        escapeHTMLSpecialCharacters(addSlashes(String.valueOf(password).trim())) + "taikotaiko"
                ));
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
        pp = Math.round(Float.parseFloat(params[4]));
        accuracy = Float.parseFloat(params[5]);
        this.username = params[6];
        if (params.length >= 8) {
            avatarURL = params[7];
        } else {
            avatarURL = "";
        }

        Bundle bParams = new Bundle();
        bParams.putString(FirebaseAnalytics.Param.METHOD, "ingame");
        GlobalManager.getInstance().getMainActivity().getAnalytics().logEvent(FirebaseAnalytics.Event.LOGIN, bParams);

        return true;
    }

    boolean tryToLogIn() throws OnlineManagerException {
        if (logIn(username, password) == false) {
            stayOnline = false;
            return false;
        }
        return true;
    }

    public void startPlay(final BeatmapInfo beatmapInfo, final String hash) throws OnlineManagerException {
        Debug.i("Starting play...");
        playID = null;

        PostBuilder post = new URLEncodedPostBuilder();
        post.addParam("userID", String.valueOf(userId));
        post.addParam("ssid", ssid);
        post.addParam("filename", StringsKt.replaceAlphanumeric(beatmapInfo.getFullBeatmapName(), "_"));
        post.addParam("hash", hash);
        post.addParam("songTitle", beatmapInfo.getTitle());
        post.addParam("songArtist", beatmapInfo.getArtist());
        post.addParam("songCreator", beatmapInfo.getCreator());

        Long beatmapId = beatmapInfo.getId();
        if (beatmapId != null && beatmapId != -1) {
            post.addParam("songID", String.valueOf(beatmapId));
        }

        ArrayList<String> response = sendRequest(post, endpoint + "submit.php");

        if (response == null) {
            if (failMessage.equals("Cannot log in") && stayOnline) {
                if (tryToLogIn()) {
                    startPlay(beatmapInfo, hash);
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

    public boolean sendRecord(String data, String replayFilename) throws OnlineManagerException {
        if (playID == null || playID.isEmpty()) {
            failMessage = "I don't have play ID";
            return false;
        }

        Debug.i("Sending record...");

        File replayFile = new File(replayFilename);
        if (!replayFile.exists()) {
            failMessage = "Replay file not found";
            Debug.e("Replay file not found");
            return false;
        }

        var post = new FormDataPostBuilder();
        post.addParam("userID", String.valueOf(userId));
        post.addParam("playID", playID);
        post.addParam("data", data);

        MediaType replayMime = MediaType.parse("application/octet-stream");
        RequestBody replayFileBody = RequestBody.create(replayFile, replayMime);

        post.addParam("replayFile", replayFile.getName(), replayFileBody);
        post.addParam("replayFileChecksum", FileUtils.getSHA256Checksum(replayFile));

        ArrayList<String> response = sendRequest(post, endpoint + "submit.php");

        if (response == null) {
            return false;
        }

        if (failMessage.equals("Invalid record data"))
            return false;

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
        accuracy = Float.parseFloat(resp[2]);
        mapRank = Integer.parseInt(resp[3]);
        pp = Math.round(Float.parseFloat(resp[4]));

        return true;
    }

    public ArrayList<String> getTop(final String hash) throws OnlineManagerException {
        PostBuilder post = new URLEncodedPostBuilder();
        post.addParam("hash", hash);
        post.addParam("uid", String.valueOf(userId));

        if (Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP) {
            post.addParam("type", "pp");
        } else {
            post.addParam("type", "score");
        }

        ArrayList<String> response = sendRequest(post, endpoint + "getrank.php");

        if (response == null) {
            return new ArrayList<>();
        }

        response.remove(0);

        return response;
    }

    public RankedStatus getBeatmapStatus(String md5) throws OnlineManagerException {
        var builder = new Request.Builder().url("https://osu.direct/api/v2/md5/" + md5);
        var request = builder.build();

        try (var response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                var json = new JSONObject(response.body().string());
                return RankedStatus.valueOf(json.optInt("ranked"));
            }
        } catch (final IOException e) {
            Debug.e("getBeatmapStatus IOException " + e.getMessage(), e);
        } catch (final JSONException e) {
            Debug.e("getBeatmapStatus JSONException " + e.getMessage(), e);
        } catch (final IllegalArgumentException e) {
            Debug.e("getBeatmapStatus IllegalArgumentException " + e.getMessage(), e);
        }

        return null;
    }

    public boolean loadAvatarToTextureManager() {
        return loadAvatarToTextureManager(avatarURL);
    }

    public boolean loadAvatarToTextureManager(String avatarURL) {
        if (avatarURL == null || avatarURL.length() == 0) return false;

        String filename = MD5Calculator.getStringMD5(avatarURL);
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
            String defaultAvatarFilename = MD5Calculator.getStringMD5(defaultAvatarURL);
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

    public String getScorePack(int playid) throws OnlineManagerException {
        PostBuilder post = new URLEncodedPostBuilder();
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

    public float getPP() {
        return pp;
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

    private String escapeHTMLSpecialCharacters(String str) {
        return str
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String addSlashes(String str) {
        return str
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\\", "\\\\");
    }
}