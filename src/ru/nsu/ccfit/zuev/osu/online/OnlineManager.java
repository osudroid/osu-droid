package ru.nsu.ccfit.zuev.osu.online;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.dgsrz.bancho.security.SecurityUtils;

import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.online.PostBuilder.RequestException;

public class OnlineManager {
    private static final String host = "http://ops.dgsrz.com/api/";
    private static final String onlineVersion = "29";
    public static HttpGet get;
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
        return host + "upload/" + playID + ".odr";
    }

    public void Init(Context context) {
        this.stayOnline = Config.isStayOnline();
        this.username = Config.getOnlineUsername();
        this.password = Config.getOnlinePassword();
        this.deviceID = Config.getOnlineDeviceID();
        this.context = context;
        Log.i("setDeviceToken", SecurityUtils.getDeviceId(context));
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

        if (response.get(0).equals("SUCCESS") == false) {
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

        PostBuilder post = new PostBuilder();
        post.addParam("username", username);
        post.addParam("password", MD5Calcuator.getStringMD5(password + "taikotaiko"));
        post.addParam("version", onlineVersion);

        ArrayList<String> response = sendRequest(post, host + "login.php");

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
        userId = params[0];
        ssid = params[1];
        rank = Integer.parseInt(params[2]);
        score = Long.parseLong(params[3]);
        accuracy = Float.parseFloat(params[4]);
        this.username = params[5];
        if (params.length >= 7) {
            avatarURL = params[6];
        } else {
            avatarURL = "";
        }

        return true;
    }

    boolean tryToLogIn() throws OnlineManagerException {
        if (logIn(username, password) == false) {
            stayOnline = false;
            return false;
        }
        return true;
    }

    public boolean register(final String username, final String password, final String email,
                            final String deviceID) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("username", username);
        post.addParam("password", MD5Calcuator.getStringMD5(password + "taikotaiko"));
        post.addParam("email", email);
        post.addParam("deviceID", deviceID);

        ArrayList<String> response = sendRequest(post, host + "register.php");

        return (response != null);
    }

    public void startPlay(final TrackInfo track, final String hash) throws OnlineManagerException {
        Debug.i("Starting play...");
        playID = null;
        final BeatmapInfo beatmap = track.getBeatmap();
        if (beatmap == null) return;

        File trackfile = new File(track.getFilename());
        trackfile.getParentFile().getName();
        String osuID = trackfile.getParentFile().getName();
        Debug.i("osuid = " + osuID);
        if (osuID.matches("^[0-9]+ .*"))
            osuID = osuID.substring(0, osuID.indexOf(' '));
        else
            osuID = null;

        PostBuilder post = new PostBuilder();
        post.addParam("userID", userId);
        post.addParam("ssid", ssid);
        post.addParam("filename", trackfile.getName());
        post.addParam("hash", hash);
        post.addParam("songTitle", beatmap.getTitle());
        post.addParam("songArtist", beatmap.getArtist());
        post.addParam("songCreator", beatmap.getCreator());
        if (osuID != null)
            post.addParam("songID", osuID);

        ArrayList<String> response = sendRequest(post, host + "submit.php");

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

    public boolean sendRecord(String data) throws OnlineManagerException {
        if (playID == null || playID.length() == 0) {
            failMessage = "I don't have play ID";
            return false;
        }

        Debug.i("Sending record...");

        PostBuilder post = new PostBuilder();
        post.addParam("userID", userId);
        post.addParam("playID", playID);
        post.addParam("data", data);

        ArrayList<String> response = sendRequest(post, host + "submit.php");

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

        if (resp.length >= 5) {
            replayID = Integer.parseInt(resp[4]);
        } else
            replayID = 0;

        return true;
    }

    public ArrayList<String> getTop(final File trackFile, final String hash) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("filename", trackFile.getName());
        post.addParam("hash", hash);

        ArrayList<String> response = sendRequest(post, host + "getrank.php");

        if (response == null) {
            return new ArrayList<String>();
        }

        response.remove(0);

        return response;
    }

    public boolean loadAvatarToTextureManager() {
        return loadAvatarToTextureManager(this.avatarURL, "userAvatar");
    }

    public boolean loadAvatarToTextureManager(String avatarURL, String userName) {
        if (avatarURL == null || avatarURL.length() == 0) return false;

        String filename = MD5Calcuator.getStringMD5(avatarURL) +
                avatarURL.substring(avatarURL.lastIndexOf('.'));
        Debug.i("Loading avatar from " + avatarURL);
        Debug.i("filename = " + filename);
        final File picfile = new File(Config.getCachePath(), filename);
        if (picfile.exists() && picfile.length() != 0) {

            //检测缓存头像的有效性
            boolean fileAvabile = true;
            BitmapFactory.Options options;
            int h = 0, w = 0;
            try {
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                h = BitmapFactory.decodeFile(picfile.getPath()).getHeight();
                w = BitmapFactory.decodeFile(picfile.getPath()).getWidth();
                options.inJustDecodeBounds = false;
                options = null;
            } catch (NullPointerException e) {
                fileAvabile = false;
            }
            if (fileAvabile && (h * w) > 0) {
                //头像已经缓存好在本地
                ResourceManager.getInstance().loadHighQualityFile(userName, picfile);
                if (ResourceManager.getInstance().getTextureIfLoaded(userName) != null) {
                    return true;
                }
            }
        }
        try {
            if (picfile.exists()) {

                //删除之前大小为 0 的无效头像文件
                picfile.delete();
            }
            //从网络读取头像
            File avatarDir = picfile.getParentFile();
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }
            FileOutputStream outStream = new FileOutputStream(picfile);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36");

            HttpClient client = new DefaultHttpClient(params);
            get = new HttpGet(avatarURL);
            HttpResponse response;

            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            if (is != null) {
                byte[] buf = new byte[1024];
                int ch = -1;

                while ((ch = is.read(buf)) > 0) {
                    outStream.write(buf, 0, ch);
                }
            } else {
                return false;
            }
            outStream.flush();
            outStream.close();
            is.close();
            if (picfile.exists() && picfile.length() > 0) {
                //Loading texture
                ResourceManager.getInstance().loadHighQualityFile(userName, picfile);
                final TextureRegion tex = ResourceManager.getInstance()
                        .getTextureIfLoaded(userName);
                if (tex != null) {
                    return true;
                }
            }
        } catch (ClientProtocolException e) {
            Debug.e("ClientProtocolException " + e.getMessage(), e);
        } catch (IOException e) {
            Debug.e("IOException " + e.getMessage(), e);
            return false;
        }

        Debug.i("Success!");
        return false;
    }

    public void sendReplay(String filename) {
        if (replayID <= 0) return;
        Debug.i("Sending replay '" + filename + "' for id = " + replayID);
        OnlineFileOperator.sendFile(host + "upload.php?replayID=" + replayID + "&ssid=" + ssid, filename);
    }

    public String getScorePack(int playid) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("playID", String.valueOf(playid));

        ArrayList<String> response = sendRequest(post, host + "gettop.php");

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
