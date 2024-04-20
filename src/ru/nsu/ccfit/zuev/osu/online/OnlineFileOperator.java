package ru.nsu.ccfit.zuev.osu.online;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Response;
import okhttp3.Request;
import okio.BufferedSink;
import okio.Okio;

import org.anddev.andengine.util.Debug;

public class OnlineFileOperator {
    public static boolean downloadFile(String urlstr, String filename) {
        return downloadFile(urlstr, filename, false);
    }

    public static boolean downloadFile(String urlstr, String filename, boolean checkModificationDate) {
        Debug.i("Starting download " + urlstr);
        File file = new File(filename);
        try {
            if (!checkModificationDate && file.exists()) {
                Debug.i(file.getName() + " already exists");
                return true;
            }
            // Cheching for errors
            Debug.i("Connected to " + urlstr);

            var builder = new Request.Builder().url(urlstr);

            if (checkModificationDate && file.exists()) {
                var lastModifiedDate = new Date(file.lastModified());
                var df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
                df.setTimeZone(TimeZone.getTimeZone("GMT"));

                builder.addHeader("If-Modified-Since", df.format(lastModifiedDate) + " GMT");
            }

            Request request = builder.build();
            try (Response response = OnlineManager.client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    BufferedSink sink = Okio.buffer(Okio.sink(file));
                    sink.writeAll(response.body().source());
                    sink.close();
                }
            }

            return true;
        } catch (final IOException e) {
            Debug.e("downloadFile IOException " + e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            Debug.e("downloadFile Exception " + e.getMessage(), e);
            return false;
        }
    }
}
