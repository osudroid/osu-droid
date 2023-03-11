package org.grove.utils;

import com.google.gson.Gson;
import okhttp3.*;
import org.grove.lib.result.Result;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Request {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static Result<byte[], Exception> getGetByteArray(String url) {
        try {
            OkHttpClient client = new OkHttpClient();

            okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();

            Call call = client.newCall(request);
            Response response = call.execute();

            return ResponseBodyToByteArray(response);
        } catch (Exception e) {
            return Result.factoryErr(e);
        }
    }

    public static <T> Result<T, Exception> getGetJson(Class<T> clazz, String url) {
        try {
            OkHttpClient client = new OkHttpClient();

            okhttp3.Request request = new okhttp3.Request.Builder().url(url).get().build();

            Call call = client.newCall(request);
            Response response = call.execute();

            return responseBodyToJson(clazz, response);
        } catch (Exception e) {
            return Result.factoryErr(e);
        }
    }

    public static <T, E>Result<T, Exception> postSendJsonGetJson(Class<T> clazz, String url, E objJson) {
        try {
            OkHttpClient client = new OkHttpClient();

            final Gson gson = new Gson();
            final String jsonString = gson.toJson(objJson);
            //System.out.println("Insert Json: " + jsonString);
            RequestBody body = RequestBody.create(jsonString, JSON);
            okhttp3.Request request = new okhttp3.Request
                    .Builder()
                    .url(url)
                    .post(body)
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();

            return responseBodyToJson(clazz, response);
        } catch (Exception e) {
            return Result.factoryErr(e);
        }
    }

    public static <T, E>Result<T, Exception> postGetJsonSendFormFileAndFormPropJson(Class<T> clazz, String url, byte[] file, E objJson) {
        try {
            OkHttpClient client = new OkHttpClient();

            final Gson gson = new Gson();

            RequestBody fileBodyFile = RequestBody.create(MediaType.parse("application/octet-stream"), file);

            MultipartBody.Builder builder = new MultipartBody
                    .Builder().setType(MultipartBody.FORM);

            builder.addFormDataPart("File", "file.file", fileBodyFile)
                    .addFormDataPart("Prop", gson.toJson(objJson));

            okhttp3.Request request = new okhttp3.Request
                    .Builder()
                    .url(url)
                    .post(builder.build())
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();

            return responseBodyToJson(clazz, response);
        } catch (Exception e) {
            return Result.factoryErr(e);
        }
    }

    private static <T> Result<T, Exception> responseBodyToJson(Class<T> clazz, Response response) {
        try {
            if (response.code() != 200)
                return Result.factoryErr(new Exception("Code is " + response.code()));

            final Gson gson = new Gson();
            String jsonString = response.body().string();
            T conv = gson.fromJson(jsonString, clazz);
            return Result.factoryOk(conv);
        } catch (Exception e) {
            return Result.factoryErr(e);
        }
    }

    private static Result<byte[], Exception> ResponseBodyToByteArray(Response response) {
        try {
            if (response.code() != 200)
                return Result.factoryErr(new Exception("Code is " + response.code()));

            return Result.factoryOk(response.body().bytes());
        } catch (Exception e) {
            return Result.factoryErr(e);
        }
    }
}
