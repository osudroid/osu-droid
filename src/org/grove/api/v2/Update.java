package org.grove.api.v2;

import org.grove.Settings;
import org.grove.lib.result.Result;
import org.grove.result.ApiUpdateInfoV2;
import org.grove.utils.Request;

public class Update {
    public Result<ApiUpdateInfoV2, Exception> GetUpdateInfoV2(String language) {
        final String addLink = "/api2/update/" + language;
        return Request.getGetJson((Class<ApiUpdateInfoV2>) (new ApiUpdateInfoV2()).getClass(), Settings.getDomain() + addLink
        );
    }
}
