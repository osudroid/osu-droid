package com.edlplan.favorite;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ru.nsu.ccfit.zuev.osu.Config;

public class FavoriteLibrary {

    private static FavoriteLibrary library = new FavoriteLibrary();
    private File json;
    private HashMap<String, HashSet<String>> favorites = new HashMap<>();

    public static FavoriteLibrary get() {
        return library;
    }

    private static void ensureFile(File file) throws IOException {
        checkExistDir(file.getParentFile());
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    private static void checkExistDir(File dir) {
        if (!dir.exists()) dir.mkdirs();
    }

    private static String readFull(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        inputStream.close();
        return new String(bytes, Charset.forName("UTF-8"));
    }

    public static void cover(String string, File file) throws IOException {
        ensureFile(file);
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(string.getBytes());
        outputStream.close();
    }

    public void load() {
        String jsonPath = Config.getCorePath() + "json/favorite.json";
        json = new File(jsonPath);
        try {
            ensureFile(json);
            JSONObject favorite;
            String jsonTxt = readFull(json);
            if (jsonTxt.isEmpty()) {
                favorite = new JSONObject();
            } else {
                try {
                    favorite = new JSONObject(jsonTxt);
                } catch (JSONException e) {
                    e.printStackTrace();
                    favorite = new JSONObject();
                }
            }
            Iterator<String> iterator = favorite.keys();
            while (iterator.hasNext()) {
                String floder = iterator.next();
                JSONArray array = favorite.optJSONArray(floder);
                if (!favorites.containsKey(floder)) {
                    favorites.put(floder, new HashSet<>());
                }
                for (int i = 0; i < array.length(); i++) {
                    favorites.get(floder).add(array.optString(i));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> getMaps(String floder) {
        return favorites.get(floder);
    }

    public Set<String> getFolders() {
        return favorites.keySet();
    }

    public void addFolder(String name) {
        if (!favorites.containsKey(name)) {
            favorites.put(name, new HashSet<>());
            save();
        }
    }

    public void add(String folder, String path) {
        if (!favorites.containsKey(folder)) {
            favorites.put(folder, new HashSet<>());
        }
        favorites.get(folder).add(path);
        save();
    }

    public void remove(String folder) {
        if (favorites.containsKey(folder)) {
            favorites.remove(folder);
            save();
        }
    }

    public void remove(String folder, String path) {
        if (favorites.containsKey(folder)) {
            if (favorites.get(folder).contains(path)) {
                favorites.get(folder).remove(path);
                save();
            }
        }
    }

    public boolean inFolder(String folder, String path) {
        return favorites.containsKey(folder) && favorites.get(folder).contains(path);
    }

    /**
     * 保存到文件
     */
    public void save() {
        try {
            JSONObject object = new JSONObject();
            for (Map.Entry<String, HashSet<String>> entry : favorites.entrySet()) {
                JSONArray array = new JSONArray();
                for (String path : entry.getValue()) {
                    array.put(path);
                }
                object.put(entry.getKey(), array);
            }
            cover(object.toString(2), json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}
