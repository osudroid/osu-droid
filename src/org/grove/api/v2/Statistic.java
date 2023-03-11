/*
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 *
 */

package org.grove.api.v2;

import com.google.gson.internal.LinkedTreeMap;
import org.grove.Settings;
import org.grove.cmt.ExistOrFoundInfo;
import org.grove.lib.result.Result;
import org.grove.result.StatisticActiveUser;
import org.grove.result.UsernameAndId;

import java.util.ArrayList;
import java.util.List;
import org.grove.utils.Request;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Statistic {

    /**
     * @return if ok, you get Active Users. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<StatisticActiveUser>, Exception> activeUser() {
        final String addLink = "/api2/statistic/active-user";
        Result<ExistOrFoundInfo<LinkedTreeMap>, Exception> res = Request.getGetJson((Class<ExistOrFoundInfo<LinkedTreeMap>>) (new ExistOrFoundInfo<LinkedTreeMap>()).getClass(), Settings.getDomain() + addLink);

        if (res.isErr())
            return Result.factoryErr(res.getErrUnsafe());
        if (res.getOkUnsafe().valueIsSet() == false)
            return Result.factoryOk(new ExistOrFoundInfo<>());
        return Result.factoryOk(new ExistOrFoundInfo<StatisticActiveUser>().setValue(StatisticActiveUser.FromLinkedTreeMap(res.getOkUnsafe().getValue())));
    }

    /**
     * @return if ok, you get All Patron Supported Users. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<List<UsernameAndId>>, Exception> allPatreon() {
        final String addLink = "/api2/statistic/all-patreon";

        Result<ExistOrFoundInfo<List<LinkedTreeMap>>, Exception> res = Request.getGetJson((Class<ExistOrFoundInfo<List<LinkedTreeMap>>>) (new ExistOrFoundInfo<List<LinkedTreeMap>>()).getClass(), Settings.getDomain() + addLink);

        if (res.isErr())
            return Result.factoryErr(res.getErrUnsafe());
        if (res.getOkUnsafe().valueIsSet() == false)
            return Result.factoryOk(new ExistOrFoundInfo<>());

        List<UsernameAndId> list = new ArrayList<>(res.getOkUnsafe().getValue().size());

        for (LinkedTreeMap linkedTreeMap : res.getOkUnsafe().getValue()) {
            list.add(UsernameAndId.FromLinkedTreeMap(linkedTreeMap));
        }

        return Result.factoryOk(new ExistOrFoundInfo<List<UsernameAndId>>().setValue(list));
    }

    private Statistic() {
    }

    private static Statistic self;
    public static Statistic getInstance() {
        if (self == null)
            self = new Statistic();
        return self;
    }
}
