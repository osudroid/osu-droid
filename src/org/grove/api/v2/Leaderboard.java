package org.grove.api.v2;

import com.google.gson.internal.LinkedTreeMap;
import org.grove.Settings;
import org.grove.cmt.Api2GroundNoHeader;
import org.grove.prop.LeaderBoardProp;
import org.grove.prop.LeaderBoardSearchUserProp;
import org.grove.prop.LeaderBoardUserProp;
import org.grove.result.*;
import org.grove.cmt.*;
import org.grove.lib.result.*;
import org.grove.utils.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Leaderboard {

    /**
     * @return if ok, you get the List of UserRanks. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<List<LeaderBoardUser>>, Exception> get(LeaderBoardProp prop) {
        final String addLink = "/api2/leaderboard";

        Result<ExistOrFoundInfo<ArrayList<LinkedTreeMap>>, Exception> result;

        result = Request.postSendJsonGetJson(
                (Class<ExistOrFoundInfo<ArrayList<LinkedTreeMap>>>) (new ExistOrFoundInfo<ArrayList<LinkedTreeMap>>()).getClass(),
                Settings.getDomain() + addLink,
                Api2GroundNoHeader.factory(prop)
        );

        if (result.isErr() || !result.getOkOr(null).valueIsSet())
            return Result.factoryErr(result.getErrOr(new Exception("")));


        ArrayList<LinkedTreeMap> oldArr = result.getOkOr(null).getValue();

        List<LeaderBoardUser> arr = new Vector<>(oldArr.size());
        for (int i = 0; i < oldArr.size(); i++) {
            LinkedTreeMap f = oldArr.get(i);
            arr.add(LeaderBoardUser.FromLinkedTreeMap(f));
        }


        return Result.factoryOk(new ExistOrFoundInfo<List<LeaderBoardUser>>().setValue(arr));
    }

    /**
     * @return if ok, you get the UserRank. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<LeaderBoardUser>, Exception> getUser(LeaderBoardUserProp prop) {
        final String addLink = "/api2/leaderboard/user";

        Result<ExistOrFoundInfo<LinkedTreeMap>, Exception> res =
                Request.postSendJsonGetJson((Class<ExistOrFoundInfo<LinkedTreeMap>>)
                        (new ExistOrFoundInfo<LinkedTreeMap>()).getClass(),Settings.getDomain() + addLink
                        , Api2GroundNoHeader.factory(prop));

        if (res.isErr())
            return Result.factoryErr(res.getErrUnsafe());
        if (res.getOkUnsafe().valueIsSet() == false)
            return Result.factoryOk(new ExistOrFoundInfo<>());

        return Result.factoryOk(new ExistOrFoundInfo<LeaderBoardUser>().setValue(LeaderBoardUser.FromLinkedTreeMap(res.getOkUnsafe().getValue())));
    }

    /**
     * @return if ok, you get the List of UserRank. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<List<LeaderBoardUser>>, Exception> getSearchUser(LeaderBoardSearchUserProp prop) {
        final String addLink = "/api2/leaderboard/search-user";
        Result<ExistOrFoundInfo<ArrayList<LinkedTreeMap>>, Exception> res = Request.postSendJsonGetJson((Class<ExistOrFoundInfo<ArrayList<com.google.gson.internal.LinkedTreeMap>>>) (new ExistOrFoundInfo<ArrayList<com.google.gson.internal.LinkedTreeMap>>()).getClass(), Settings.getDomain() + addLink, Api2GroundNoHeader.factory(prop));

        if (res.isErr())
            return Result.factoryErr(res.getErrUnsafe());
        if (res.getOkUnsafe().valueIsSet() == false)
            return Result.factoryOk(new ExistOrFoundInfo<>());


        ArrayList<LinkedTreeMap> oldArr = res.getOkUnsafe().getValue();

        List<LeaderBoardUser> arr = new Vector<>(oldArr.size());
        for (int i = 0; i < oldArr.size(); i++) {
            LinkedTreeMap f = oldArr.get(i);
            arr.add(LeaderBoardUser.FromLinkedTreeMap(f));
        }


        return Result.factoryOk(new ExistOrFoundInfo<List<LeaderBoardUser>>().setValue(arr));
    }

    private Leaderboard() {
    }

    private static Leaderboard self;
    public static Leaderboard getInstance() {
        if (self == null)
            self = new Leaderboard();
        return self;
    }
}
