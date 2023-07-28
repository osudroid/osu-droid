package com.reco1l.framework.net;

import androidx.core.util.Consumer;
import com.reco1l.framework.util.Logging;
import okhttp3.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Reco1l
 */
public class Requester implements AutoCloseable
{

    public static final int DEFAULT_TIMEOUT = 10000;

    public static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient
            .Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

    //----------------------------------------------------------------------------------------------------------------//

    protected OkHttpClient mClient;
    protected Request mRequest;

    protected ResponseBody mResponseBody;
    protected Response mResponse;
    protected Call mCall;

    protected QueryContent mQuery;

    //----------------------------------------------------------------------------------------------------------------//

    public Requester()
    {
        this(null);
    }

    public Requester(String url)
    {
        super();
        mClient = DEFAULT_CLIENT;

        if (url != null)
        {
            mRequest = new Request
                    .Builder()
                    .url(url)
                    .build();
        }
    }

    //----------------------------------------------------------------------------------------------------------------//

    public void setClient(OkHttpClient client)
    {
        mClient = client;
    }

    public void setRequest(Request request)
    {
        mRequest = request;
    }

    public void setQuery(QueryContent query)
    {
        mQuery = query;
    }

    //----------------------------------------------------------------------------------------------------------------//

    public void buildClient(Consumer<OkHttpClient.Builder> consumer)
    {
        var builder = mClient != null ? mClient.newBuilder() : new OkHttpClient.Builder();

        if (consumer != null)
        {
            consumer.accept(builder);
        }
        mClient = builder.build();
    }

    public void buildRequest(Consumer<Request.Builder> consumer)
    {
        var builder = mRequest != null ? mRequest.newBuilder() : new Request.Builder();

        if (consumer != null)
        {
            consumer.accept(builder);
        }
        mRequest = builder.build();
    }

    //----------------------------------------------------------------------------------------------------------------//

    public OkHttpClient getClient()
    {
        return mClient;
    }

    public Request getRequest()
    {
        return mRequest;
    }

    //----------------------------------------------------------------------------------------------------------------//

    public Call getCall()
    {
        Objects.requireNonNull(mCall, "You need to call execute() first!");
        return mCall;
    }

    public Response getResponse()
    {
        Objects.requireNonNull(mResponse, "You need to call execute() first!");
        return mResponse;
    }

    public ResponseBody getResponseBody()
    {
        Objects.requireNonNull(mResponseBody, "You need to call execute() first!");
        return mResponseBody;
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Called as soon the response and response body is created.
     */
    protected void onResponse(Response response) throws Exception
    {
    }

    /**
     * Called when response code isn't 200-299, this can also be called from inherited onResponseSuccess() exception thrown.
     */
    protected void onResponseFail(Exception exception) throws Exception
    {
    }

    /**
     * Called when response code is 200-299.
     */
    protected void onResponseSuccess(Response response) throws Exception
    {
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    public void close()
    {
        if (mResponse != null)
        {
            mResponse.close();

            if (mResponseBody != null)
            {
                mResponseBody.close();
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Make sure to call before call close() or inside the try-with-resources statement.
     */
    public Requester execute() throws Exception
    {
        Objects.requireNonNull(mClient, "Client cannot be null!");
        Objects.requireNonNull(mRequest, "Request cannot be null!");

        if (mQuery != null)
        {
            mRequest = mRequest.newBuilder()
                    .url(mRequest.url() + mQuery.toString())
                    .build();
        }

        Logging.i(this, "Inserted URL: " + mRequest.url());

        mCall = mClient.newCall(mRequest);
        try
        {
            mResponse = mCall.execute();
            mResponseBody = mResponse.body();

            onResponse(mResponse);

            if (mResponse.isSuccessful())
            {
                onResponseSuccess(mResponse);
            }
            else
            {
                throw new ResponseException(mResponse);
            }
        }
        catch (Exception e)
        {
            onResponseFail(e);
            throw e;
        }
        return this;
    }

    /**
     * Make sure to call before call close() or inside the try-with-resources statement.
     *
     * @return The response body.
     */
    public ResponseBody executeAndGetBody() throws Exception
    {
        //noinspection resource
        return execute().getResponseBody();
    }
}
