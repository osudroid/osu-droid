package com.reco1l.ui.scenes.listing.fragments;

import android.os.Environment;

import com.reco1l.ui.custom.Notification;
import com.reco1l.Game;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.tables.NotificationTable;
import com.reco1l.ui.fragments.WebViewFragment;
import com.reco1l.framework.execution.AsyncTask;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipException;

import main.osu.Config;
import main.osu.helper.FileUtils;

public class BeatmapListing extends WebViewFragment {

    public static final BeatmapListing instance = new BeatmapListing();

    public static final String SERVER = "https://chimu.moe/en/beatmaps?mode=0";

    //--------------------------------------------------------------------------------------------//

    public BeatmapListing() {
        super(SERVER, Scenes.listing);
        setLockToHostname(true);
    }

    @Override
    protected void onLoad() {
        super.onLoad();
    }

    @Override
    protected boolean isExtra() {
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onDownloadRequested(String url, String file, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        if (file == null || !file.endsWith(".osz")) {
            return;
        }

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        new AsyncTask() {

            private boolean mSuccess;

            private String
                    mPath,
                    mName;

            private Exception mException;
            private Notification mNotification;

            public void run() {
                mName = file.substring(0, file.length() - 4);

                try {
                    mName = URLDecoder.decode(mName, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException ignored) {}

                mNotification = new Notification("Beatmap downloader")
                        .setMessage("Downloading " + mName)
                        .showProgress(true)
                        .commit();

                String folder = mName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
                File osz = new File(directory, folder + ".osz");

                try {
                    URL website = new URL(url);
                    ReadableByteChannel channel = Channels.newChannel(website.openStream());

                    try (FileOutputStream output = new FileOutputStream(osz)) {
                        output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
                        mSuccess = true;
                    }
                } catch (IOException e) {
                    mSuccess = false;
                    mException = e;
                    return;
                }

                mNotification.setMessage("Importing " + mName);

                try (ZipFile zip = new ZipFile(osz)) {
                    mSuccess = zip.isValidZipFile();
                } catch (IOException e) {
                    mSuccess = false;
                    mException = e;
                    return;
                }

                mSuccess = FileUtils.extractZip(osz.getPath(), Config.getBeatmapPath());

                if (!mSuccess) {
                    mException = new ZipException("Failed to extract ZIP to beatmaps folder!");
                    return;
                }

                mPath = Config.getBeatmapPath() + folder;
                Game.libraryManager.savetoCache(Game.activity);

                if (!Game.libraryManager.loadLibraryCache(Game.activity, true)) {
                    Game.libraryManager.scanLibrary(Game.activity);
                }
            }

            public void onComplete() {
                mNotification.showProgress(false);

                if (mSuccess) {
                    mNotification.setMessage(mName + " successfully downloaded!");
                    mNotification.runOnClick(() -> {
                        Game.musicManager.change(Game.libraryManager.findBeatmapByPath(mPath));
                        Game.engine.setScene(Scenes.selector);
                    });
                    return;
                }

                mNotification.setMessage("Failed to download " + mName);
                if (mException != null) {
                    NotificationTable.exception(mException);
                }
            }
        }.execute();

        Game.activity.checkNewBeatmaps();
    }

    @Override
    public void close() {
        super.close();
    }
}
