package org.grove.api.v1;

import com.google.gson.internal.LinkedTreeMap;

import org.grove.Settings;
import org.grove.cmt.ExistOrFoundInfo;
import org.grove.lib.result.Result;
import org.grove.prop.ProfileStatsProp;
import org.grove.result.ProfileStatistic;
import org.grove.utils.Request;

public class ProfileStats {

    public Result<ExistOrFoundInfo<ProfileStatistic>, Exception> profileStats(ProfileStatsProp prop) {
        final String addLink = "/api/profile/stats/";
        Result<ExistOrFoundInfo<LinkedTreeMap>, Exception> res = Request.getGetJson((Class<ExistOrFoundInfo<LinkedTreeMap>>) (new ExistOrFoundInfo<LinkedTreeMap>()).getClass(), Settings.getDomain() + addLink);

        if (res.isErr())
            return Result.factoryErr(res.getErrUnsafe());
        if (res.getOkUnsafe().valueIsSet() == false)
            return Result.factoryOk(new ExistOrFoundInfo<>());
        return Result.factoryOk(new ExistOrFoundInfo<ProfileStatistic>().setValue(ProfileStatistic.FromLinkedTreeMap(res.getOkUnsafe().getValue())));
    }

    private ProfileStats() {
    }

    private static ProfileStats self;
    public static ProfileStats getInstance() {
        if (self == null)
            self = new ProfileStats();
        return self;
    }
}
