package ru.nsu.ccfit.zuev.osu.online;

import com.dgsrz.bancho.security.SecurityUtils;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class OnlineFileOperator {

    public static void sendFile(String urlstr, String filename, String replayID) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                Debug.i(filename + " does not exist.");
                return;
            }

            String checksum = FileUtils.getSHA256Checksum(file);
            String sb = URLEncoder.encode(checksum, StandardCharsets.UTF_8) +
                    "_" +
                    URLEncoder.encode(replayID, StandardCharsets.UTF_8);
            String signature = SecurityUtils.signRequest(sb);

            MediaType mime = MediaType.parse("application/octet-stream");
            RequestBody fileBody = RequestBody.create(mime, file);
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("uploadedfile", file.getName(), fileBody)
                .addFormDataPart("hash", checksum)
                .addFormDataPart("replayID", replayID)
                .addFormDataPart("sign", signature)
                .build();
            Request request = new Request.Builder().url(urlstr)
                .post(requestBody).build();
            try (Response response = OnlineManager.client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseMsg = response.body().string();
                    Debug.i("sendFile signatureResponse " + responseMsg);
                }
            }
        } catch (final IOException e) {
            Debug.e("sendFile IOException " + e.getMessage(), e);
        } catch (final Exception e) {
            Debug.e("sendFile Exception " + e.getMessage(), e);
        }
    }

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
