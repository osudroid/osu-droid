package ru.nsu.ccfit.zuev.osu.online;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request;

import org.anddev.andengine.util.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serial;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public abstract class PostBuilder {
    public void addParam(final String key, final String value) {
        addParamInternal(key, value);
    }

    public ArrayList<String> requestWithAttempts(final String scriptUrl, int attempts) throws RequestException {
        ArrayList<String> response = null;

        for (int i = 0; i < attempts; i++) {
            try {
                response = request(OnlineManager.client, scriptUrl);
            } catch (RequestException e) {
                if (e.getCause() instanceof UnknownHostException) {
                    Debug.e("Cannot resolve server name");
                    break;
                }
                Debug.e("Received error, continuing... ", e);
                response = null;
            }

            if (response == null || response.isEmpty() || response.get(0).isEmpty()
                    || !(response.get(0).equals("FAIL") || response.get(0).equals("SUCCESS"))) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            break;
        }

        if (response == null) response = new ArrayList<>();

        if (response.isEmpty()) {
            response.add("");
        }
        return response;
    }

    /**
     * Sends a request once.
     *
     * @param client The client to send the request with.
     * @param scriptUrl The URL to send the request to.
     * @return The lines of the response body, or an empty line if the response was not successful.
     * @throws RequestException If the request could not be completed, such as when the connection fails or times out.
     */
    public ArrayList<String> request(final OkHttpClient client, final String scriptUrl) throws RequestException {
        ArrayList<String> response = new ArrayList<>();

        Request request = new Request.Builder()
            .url(scriptUrl)
            .post(getRequestBody())
            .build();

        try (Response resp = client.newCall(request).execute()) {

            if (resp.isSuccessful() && resp.body() != null) {
                Debug.i("request url=" + scriptUrl);
                Debug.i("request --------Content---------");
                String line;
                BufferedReader reader = new BufferedReader(new StringReader(resp.body().string()));
                while ((line = reader.readLine()) != null) {
                    Debug.i(String.format("request [%d]: %s", response.size(), line));
                    response.add(line);
                }
                Debug.i("request url=" + scriptUrl);
                Debug.i("request -----End of content-----");
            }
        } catch (IOException e) {
            throw new RequestException(e);
        } catch (Exception e) {
            Debug.e(e.getMessage(), e);
        }

        if (response.isEmpty()) {
            response.add("");
        }
        return response;
    }

    protected abstract void addParamInternal(final String key, final String value);

    protected abstract RequestBody getRequestBody();

    public static class RequestException extends Exception {
        @Serial
        private static final long serialVersionUID = 671773899432746143L;

        public RequestException(final Throwable cause) {
            super(cause);
        }

        /**
         * Whether the request failed before it could be sent, so sending it again cannot repeat it.
         */
        public boolean wasNotSent() {
            return getCause() instanceof UnknownHostException || getCause() instanceof ConnectException;
        }
    }
}
