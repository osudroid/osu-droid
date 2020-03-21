package com.edlplan.ext;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class EdExtensionHelper {

    public static final String EXT_BROADCAST_ANY = "osu.droid.ext.broadcast.any";

    public static boolean downloadExtension() {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.edplan.cn/osu/droid/extension/latest_ext.php"));
        GlobalManager.getInstance().getMainActivity().startActivity(intent);
        return true;
    }

    public static boolean isExtensionEnable() {
        return Config.isEnableExtension();
    }

    public static void broadcastMsg(String apiName, String data) {
        if (!isExtensionEnable()) return;
        Intent intent = new Intent(EXT_BROADCAST_ANY);
        intent.putExtra("api", apiName);
        intent.putExtra("type", "anyBroadcast");
        intent.putExtra("data", data);
        GlobalManager.getInstance().getMainActivity().sendBroadcast(intent);
    }

    public static void onSelectTrack(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            broadcastMsg("onSelectTrack", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onStartGame(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onStartGame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onRestartGame(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onRestartGame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onExitGame(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onExitgame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onGameover(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onGameover", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onEndGame(TrackInfo lastTrack, StatisticV2 stat) {
        if (!isExtensionEnable() || lastTrack == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", lastTrack.getFilename());
            game.put("mods", new JSONArray(stat.getMod()));
            game.put("score", stat.getTotalScore());
            game.put("combo", stat.getMaxCombo());
            game.put("hit300", stat.getHit300());
            game.put("hit100", stat.getHit100());
            game.put("hit50", stat.getHit50());
            game.put("miss", stat.getMisses());
            broadcastMsg("onEndGame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onPauseGame(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onPauseGame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onResume(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onResumeGame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void onQuitGame(TrackInfo info) {
        if (!isExtensionEnable() || info == null) {
            return;
        }
        try {
            JSONObject game = new JSONObject();
            game.put("file", info.getFilename());
            game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
            game.put("gameId", GameHelper.getGameid());
            broadcastMsg("onQuitGame", game.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean openBeatmap(String filepath) {
        if (!isExtensionEnable()) return false;
        try {
            Intent intent = new Intent();
            ComponentName componentName = new ComponentName("com.edplan.osu.osudroidextension", "com.edplan.osu.osudroidextension.ApiActivity");
            intent.setComponent(componentName);
            intent.putExtra("api", "openOsuFile");
            try {
                JSONObject game = new JSONObject();
                game.put("file", filepath);
                game.put("mods", new JSONArray(ModMenu.getInstance().getMod()));
                intent.putExtra("data", game.toString());
                GlobalManager.getInstance().getMainActivity().startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        } catch (ActivityNotFoundException e) {
            //e.printStackTrace();
            ToastLogger.showText(StringTable.get(R.string.message_extension_not_found), false);
            return false;
        }
    }
}
