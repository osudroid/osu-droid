package com.edlplan.replay;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class OsuDroidReplayPack {

    public static void packTo(File file, OsuDroidReplay replay) throws Exception {
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(pack(replay));
        outputStream.close();
    }

    public static byte[] pack(OsuDroidReplay replay) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream outputStream = new ZipOutputStream(byteArrayOutputStream);
        //try {
        outputStream.putNextEntry(new ZipEntry("entry.json"));

        JSONObject entryJson = new JSONObject();
        entryJson.put("version", 1);
        entryJson.put("replaydata", replay.toJSON());

        outputStream.write(entryJson.toString(2).getBytes());

        outputStream.putNextEntry(new ZipEntry(replay.getReplayFileName()));

        File file = replay.isAbsoluteReplay() ?
                new File(replay.getReplayFile()) : new File(OdrConfig.getScoreDir(), replay.getReplayFileName());
        FileInputStream inputStream = new FileInputStream(file);

        byte[] buffer = new byte[1024];
        int l;
        while ((l = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, l);
        }

        outputStream.finish();
        return byteArrayOutputStream.toByteArray();
    }

    public static ReplayEntry unpack(InputStream raw) throws IOException, JSONException {
        ZipInputStream inputStream = new ZipInputStream(raw);
        ReplayEntry entry = new ReplayEntry();
        Map<String, byte[]> zipEntryMap = new HashMap<>();
        for (ZipEntry zipEntry = inputStream.getNextEntry(); zipEntry != null; zipEntry = inputStream.getNextEntry()) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int l;
            while ((l = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, l);
            }
            zipEntryMap.put(zipEntry.getName(), byteArrayOutputStream.toByteArray());
            System.out.println("解压文件：" + zipEntry.getName() + " size: " + zipEntryMap.get(zipEntry.getName()).length);
        }
        inputStream.close();

        entry.replay = OsuDroidReplay.parseJSON(new JSONObject(new String(zipEntryMap.get("entry.json"))).getJSONObject("replaydata"));
        entry.replayFile = zipEntryMap.get(entry.replay.getReplayFileName());

        return entry;
    }

    public static class ReplayEntry {
        public OsuDroidReplay replay;
        public byte[] replayFile;
    }
}
