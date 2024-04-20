package ru.nsu.ccfit.zuev.osu.online;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class URLEncodedPostBuilder extends PostBuilder {
    private final FormBody.Builder formBodyBuilder = new FormBody.Builder();

    @Override
    protected void addParamInternal(final String key, final String value) {
        formBodyBuilder.add(key, value);
    }

    @Override
    protected RequestBody getRequestBody() {
        return formBodyBuilder.build();
    }
}
