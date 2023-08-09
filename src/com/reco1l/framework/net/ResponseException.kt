package com.reco1l.framework.net

import okhttp3.Response
import java.io.IOException

/**
 * @author Reco1l
 */
class ResponseException(response: Response) : IOException("Unexpected response: $response")
{

    val code: Int = response.code

}
