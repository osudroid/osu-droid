package ru.nsu.ccfit.zuev.osu.online;

import android.util.Log;

import com.dgsrz.bancho.security.SecurityUtils;

import org.anddev.andengine.util.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class PostBuilder {
    private StringBuilder builder = new StringBuilder();
    private StringBuilder values = new StringBuilder();

    public void addParam(final String key, final String value) {
        try {
            if (builder.length() > 0) {
                builder.append("&");
                values.append("_");
            }
            builder.append(URLEncoder.encode(key, "UTF-8"));
            builder.append("=");
            builder.append(URLEncoder.encode(value, "UTF-8"));
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

    public ArrayList<String> request(final String scriptUrl) throws RequestException {
        String data = builder.toString();
        //TODO debug code
		/*Debug.i("Sending request to " + scriptUrl);
		Debug.i("Request data = " + data);*/
        ArrayList<String> response = new ArrayList<String>();

        try {
            URL url = new URL(scriptUrl);
            URLConnection connection = url.openConnection();
            ((HttpURLConnection) connection).setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(data);
            out.flush();
            out.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            if (BuildConfig.DEBUG) {
                Log.i("request", "url=" + scriptUrl);
                Log.i("request", "--------Content---------");
            }
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                response.add(s);
                if (BuildConfig.DEBUG) {
                    Log.i("request", s);
                }
            }
            if (BuildConfig.DEBUG) {
                Log.i("request", "url=" + scriptUrl);
                Log.i("request", "-----End of content-----");
            }
        } catch (MalformedURLException e) {
            throw new RequestException(e);
        } catch (IOException e) {
            throw new RequestException(e);
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
