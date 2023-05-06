package com.reco1l.framework.net;

import okhttp3.Response;

import java.io.IOException;

/**
 * @author Reco1l
 */
public class ResponseException extends IOException
{

    private final int mCode;

    private final String mMessage;

    //----------------------------------------------------------------------------------------------------------------//

    public ResponseException(Response response)
    {
        super();
        mCode = response.code();
        mMessage = "Unexpected HTTP response: " + response;
    }

    //----------------------------------------------------------------------------------------------------------------//

    public int getCode()
    {
        return mCode;
    }

    @Override
    public String getMessage()
    {
        return mMessage;
    }
}
