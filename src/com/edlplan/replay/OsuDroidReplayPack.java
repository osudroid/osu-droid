package com.edlplan.replay;

import static com.reco1l.osu.data.Scores.ScoreInfo;

import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.ScoreInfo;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
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

import ru.nsu.ccfit.zuev.osu.scoring.Replay;

public class OsuDroidReplayPack {

    public static void packTo(File file, BeatmapInfo beatmapInfo, ScoreInfo scoreInfo) throws Exception {
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(pack(beatmapInfo, scoreInfo));
        outputStream.close();
    }

    public static byte[] pack(BeatmapInfo beatmapInfo, ScoreInfo scoreInfo) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream outputStream = new ZipOutputStream(byteArrayOutputStream)){
            outputStream.putNextEntry(new ZipEntry("entry.json"));

            JSONObject entryJson = new JSONObject();
            JSONObject replayData = scoreInfo.toJSON();

            // ScoreInfo does not contain beatmap info
            replayData.put("filename", beatmapInfo.getFullBeatmapsetName() + '/' + beatmapInfo.getFullBeatmapName());

            entryJson.put("version", 2);
            entryJson.put("replaydata", replayData);

            outputStream.write(entryJson.toString(2).getBytes());

            outputStream.putNextEntry(new ZipEntry(scoreInfo.getReplayFilename()));

            File file = new File(scoreInfo.getReplayPath());

            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int l;
                while ((l = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, l);
                }
            }

            outputStream.finish();
            return byteArrayOutputStream.toByteArray();
        }
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

        var json = new JSONObject(new String(zipEntryMap.get("entry.json")));
        int version = json.getInt("version");
        var replayData = json.getJSONObject("replaydata");
        var replayFilename = FilenameUtils.getName(replayData.getString("replayfile"));
        var replayFile = zipEntryMap.get(replayFilename);

        if (version < 2) {
            // Exported replay v1 does not contain MD5 hash, so we need to obtain it from the odr file.
            try (var byteArrayInputStream = new ByteArrayInputStream(replayFile)) {
                var replay = new Replay(false);

                if (!replay.load(byteArrayInputStream, replayFilename, false)) {
                    throw new IOException("Failed to load replay");
                }

                replayData.put("beatmapMD5", replay.getMd5());
            }
        }

        entry.scoreInfo = ScoreInfo(replayData);
        entry.replayFile = replayFile;

        return entry;
    }

    public static class ReplayEntry {
        public ScoreInfo scoreInfo;
        public byte[] replayFile;
    }
}
