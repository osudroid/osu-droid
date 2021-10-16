package ru.nsu.ccfit.zuev.osu.online;

import com.dgsrz.bancho.security.SecurityUtils;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request;

import org.anddev.andengine.util.Debug;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class PostBuilder {
    private FormBody.Builder formBodyBuilder = new FormBody.Builder();
    private StringBuilder values = new StringBuilder();

    public void addParam(final String key, final String value) {
        try {
            if (values.length() > 0) {
                values.append("_");
            }
            formBodyBuilder.add(key, value);
            values.append(URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return;
        }

    }

    public ArrayList<String> requestWithAttempts(final String scriptUrl, int attempts) throws RequestException {
        ArrayList<String> response = null;
        String signature = SecurityUtils.signRequest(values.toString());

        if (signature != null) {
            addParam("sign", signature);
        }
        for (int i = 0; i < attempts; i++) {
            try {
                response = request(scriptUrl);
            } catch (RequestException e) {
                if (e.getCause() instanceof UnknownHostException) {
                    Debug.e("Cannot resolve server name");
                    break;
                }
                Debug.e("Received error, continuing... ", e);
                response = null;
            }

            if (response == null || response.isEmpty() || response.get(0).length() == 0
                    || !(response.get(0).equals("FAIL") || response.get(0).equals("SUCCESS"))) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                continue;
            }
            break;
        }

        if (response == null) response = new ArrayList<String>();

        if (response.isEmpty()) {
            response.add("");
        }
        return response;
    }

    private ArrayList<String> request(final String scriptUrl) throws RequestException {
        String data = values.toString();
        ArrayList<String> response = new ArrayList<String>();

        try {
            Request request = new Request.Builder()
                .url(scriptUrl)
                .post(formBodyBuilder.build())
                .build();
            Response resp = OnlineManager.client.newCall(request).execute();

            Debug.i("request url=" + scriptUrl);
            Debug.i("request --------Content---------");
            String line = null;
            BufferedReader reader = new BufferedReader(new StringReader(resp.body().string()));
            while((line = reader.readLine()) != null) {
                Debug.i(String.format("request [%d]: %s", response.size(), line));
                response.add(line);
            }
            Debug.i("request url=" + scriptUrl);
            Debug.i("request -----End of content-----");
        } catch(Exception e) {
            Debug.e(e.getMessage(), e);
        }

        if (response.isEmpty()) {
            response.add("");
        }
        return response;
    }

    public static class RequestException extends Exception {
        private static final long serialVersionUID = 671773899432746143L;

        public RequestException(final Throwable cause) {
            super(cause);
        }
    }
}
