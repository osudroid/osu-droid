package ru.nsu.ccfit.zuev.osu.online;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;

public class OnlineMapInfo {


    private static final String ppyosu = "https://osu.ppy.sh/osu/";
    private static final String ppyAPI = "https://osu.ppy.sh/api/get_";
    private static final String bloodcatAPI = "https://bloodcat.com/osu/?mod=json&";
    private static final String bloodcatosu = "https://bloodcat.com/osu/b/";
    private static String key;
    private static boolean correctkey;
    private static HttpURLConnection update = null;
    private static JSONObject result;
    private static TrackInfo map;
    private static boolean updateNeccessary;
    private URLConnection urlconn = null;
    private BufferedReader buffread = null;

    public OnlineMapInfo() {
        key = Config.getAPIKey();
        correctkey = key.matches("[a-f|\\d]{40}");
        map = null;
        updateNeccessary = false;
    }

    public boolean saveJSON(String urladdress) {
        result = null;
        JSONArray jsar;
        if (!correctkey && urladdress.startsWith(ppyAPI)) return false;
        try {
            if ((jsar = getJSON(urladdress)) != null) {
                if ((result = jsar.getJSONObject(0)) != null) {
                    if (result.has("error")) {
                        switch (result.getInt("error")) {
                            case 404:
                                return true;
                            case 403:
                                ToastLogger.showText("Invalid API key\nUsing bloodcat", true);
                                correctkey = false;
                            default:
                                return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        } catch (JSONException e) {
            return false;
        } catch (Exception e) {
            ToastLogger.showText(e.getMessage(), true);
            return false;
        }
    }

    public JSONArray getJSON(String url) throws JSONException {
        String data;
        try {
            URL u = new URL(url);
            urlconn = u.openConnection();
            buffread = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
            StringBuffer stringBuffer = new StringBuffer();
            while ((data = buffread.readLine()) != null) {
                stringBuffer.append(data);
            }
            data = stringBuffer.toString();
            return new JSONArray(data.equals("[]") ? "[{\"error\":\"404\"}]" : data);
        } catch (FileNotFoundException e) {
            return new JSONArray("[{\"error\":\"403\"}]");
        } catch (UnknownHostException e) {
            return null;
        } catch (IOException e) {
            ToastLogger.showText(e.toString(), false);
            return null;
        } catch (Exception e) {
            ToastLogger.showText(e.getMessage(), true);
            return null;
        }
    }

    /*
        public JSONObject ppyGetBeatmaps(String parameter, String query) {
            String url = ppyAPI + "beatmaps?k=" + key + "&" + parameter + "=" + query;
            return saveJSON(url)?result:null;

        }
    */
    private int ppyGetBeatmapsFromHash() {
        final String url = ppyAPI + "beatmaps?k=" + this.key + "&h=" + MD5Calcuator.getFileMD5(new File(map.getFilename()));
        if (saveJSON(url)) {
            try {
                if (correctkey)
                    return !result.has("approved") || result == null ? 404 : result.getInt("approved");
                else return 520;
            } catch (JSONException e) {
                return 404;
            } catch (NullPointerException e) {
                return 404;
            } catch (Exception e) {
                ToastLogger.showText(e.getMessage(), false);
                return 520;
            }
        }
        return 520;
    }

    private int bldcatGetBeatmapsFromHash() {
        final String url = bloodcatAPI + "q=md5=" + MD5Calcuator.getFileMD5(new File(map.getFilename()));
        if (saveJSON(url)) {
            try {
                return !result.has("status") || result == null ? 404 : result.getInt("status");
            } catch (JSONException e) {
                return 404;
            } catch (NullPointerException e) {
                return 404;
            } catch (Exception e) {
                ToastLogger.showText(e.getMessage(), false);
                return 520;
            }
        }
        return 520;
    }

    public int getBeatmapsStateFromHash(TrackInfo t) {
        if (!Config.isRetrieveBeatmapInfo()) {
            return 7;
        }
        this.map = t;
        update = null;
        updateNeccessary = false;
        // 0"No connection", 1"ranking_disabled", 2"ranking_ranked", 3"ranking_latest" , 4"ranking_loved", 5"ranking_unsubmitted", 6"ranking_download", 7"ranking_unknown"
        //ppy                   |bloodcat
        //4 loved               |4          |4
        //2, 1 ranked           |2, 1       |2
        //0, -1, -2, 3 latest   |0          |3
        //404 unsubmitted                   |5 -> 6
        //520 connection                    |0

        int i = correctkey ? ppyGetBeatmapsFromHash() : bldcatGetBeatmapsFromHash();
        switch (i) {
            case 4:
            case 3:
            case 2:
                break;
            case 1:
                i = 2;
                break;
            case 404:
                i = isUpdateAvailable() ? 6 : 5;
                break;
            default:
                i = i < 1 ? 3 : 0;
        }
        return i;
    }

    /*
        public JSONObject ppyGetUser(String parameter, String query) {
            String url = ppyAPI + "user?k=" + key + "&" + parameter + "=" + query;
            return saveJSON(url)?result:null;
        }

        public String ppyGetUser(String parameter, String query, String key) {
            String url = ppyAPI + "beatmaps?k=" + this.key + "&" + parameter + "=" + query;
            try {
                return saveJSON(url) ? String.valueOf(result.getString(key)) : null;
            }
            catch (Exception e){
                return null;
            }

        }
    */
    private boolean isUpdateAvailable() {
        int bid = map.getBeatmapID();
        int sid = map.getBeatmapSetID();
        if (bid < 1 || sid < 1) return false;

        try {
            URL u = new URL((correctkey ? ppyosu : bloodcatosu) + bid);
            update = (HttpURLConnection) u.openConnection();

            if (!update.getHeaderField("Content-Disposition").endsWith(".osu")) return false;

            JSONObject js;
            if ((js = getJSON(correctkey ? (ppyAPI + "beatmaps?k=" + key + "&b=" + bid) : (bloodcatAPI + "q=" + bid + "&c=b")).getJSONObject(0)) != null) {
                if (correctkey) {

                    return js.getInt("download_unavailable") == 0
                            && js.getInt("beatmap_id") == bid
                            && js.getInt("beatmapset_id") == sid;

                } else {
                    if (js.getInt("id") != sid) return false;

                    JSONArray j = js.getJSONArray("beatmaps");

                    for (int i = 0; i < j.length(); i++) {
                        if (j.getJSONObject(i).getInt("id") == bid) return true;
                    }

                    return false;
                }
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        } catch (Exception e) {
            ToastLogger.showText(e.getMessage(), false);
            return false;
        }
    }

    public boolean isUpdateNeccessary() {
        return updateNeccessary;
    }

    public void setUpdateNeccessary(boolean updateNeccessary) {
        this.updateNeccessary = updateNeccessary;
    }

    public boolean updateMap() {
        try {
            String NewFile = update.getHeaderField("Content-Disposition");
            if (!NewFile.contains("filename=\"")) return false;
            NewFile = NewFile.split("filename=\"")[1].split("\"")[0];
            if (!NewFile.endsWith(".osu")) return false;
            File f = new File(Config.getBeatmapPath(), NewFile + ".download");
            f.createNewFile();
            InputStream i = update.getInputStream();
            OutputStream o = new FileOutputStream(f);

            byte[] buffer = new byte[2048];
            int length;

            while ((length = i.read(buffer)) != -1) {
                o.write(buffer, 0, length);
            }
            o.close();

            f.renameTo(new File(map.getFilename()));
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    public boolean isCorrectKey() {
        return correctkey;
    }
}
