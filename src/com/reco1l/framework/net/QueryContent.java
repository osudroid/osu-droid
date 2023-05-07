package com.reco1l.framework.net;

/**
 * @author Reco1l
 */
public class QueryContent
{

    private final StringBuilder mBuilder = new StringBuilder("?");

    private boolean mUseAmpersand = false;

    //----------------------------------------------------------------------------------------------------------------//

    public void put(String key, Object value)
    {
        var string = value != null ? value.toString() : null;

        if (string == null || string.length() == 0)
        {
            return;
        }

        if (mUseAmpersand)
        {
            mBuilder.append('&');
        }
        else
        {
            mUseAmpersand = true;
        }
        mBuilder.append(key).append('=').append(string);
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    public String toString()
    {
        return mBuilder.toString();
    }
}
