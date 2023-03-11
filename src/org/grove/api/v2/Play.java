package org.grove.api.v2;

import com.google.gson.internal.LinkedTreeMap;
import org.grove.Settings;
import org.grove.cmt.Api2GroundNoHeader;
import org.grove.cmt.Api2GroundWithHash;
import org.grove.cmt.ExistOrFoundInfo;
import org.grove.lib.result.Result;
import org.grove.prop.Api2PlayById;
import org.grove.prop.RecentPlays;
import org.grove.result.PlayInfoById;
import org.grove.result.entities.BblScoreWithUsername;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.grove.utils.Request;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Play {

    /**
     * @return if ok, you get play of by PlayID. If Error, you get are error as string
     */
    public Result<PlayInfoById, Exception> getById(Api2PlayById prop, UUID token) {
        final String addLink = "/api2/play/by-id";
        return Request.postSendJsonGetJson((Class<PlayInfoById>) (new PlayInfoById()).getClass(), Settings.getDomain() + addLink, Api2GroundWithHash.factory(prop, token));
    }

    /**
     * @return if ok, you get recent plays. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<List<BblScoreWithUsername>>, Exception> getRecent(RecentPlays prop) {
        final String addLink = "/api2/play/recent";

        Result<ExistOrFoundInfo<ArrayList<LinkedTreeMap>>, Exception> res = Request.postSendJsonGetJson((Class<ExistOrFoundInfo<ArrayList<LinkedTreeMap>>>) (new ExistOrFoundInfo<ArrayList<LinkedTreeMap>>()).getClass(), Settings.getDomain() + addLink, Api2GroundNoHeader.factory(prop));

        if (res.isErr())
            return Result.factoryErr(res.getErrUnsafe());
        if (res.getOkUnsafe().valueIsSet() == false)
            return Result.factoryOk(new ExistOrFoundInfo<>());

        List<BblScoreWithUsername> arr = new Vector<>(res.getOkUnsafe().getValue().size());
        for (LinkedTreeMap linkedTreeMap : res.getOkUnsafe().getValue()) {
            arr.add(BblScoreWithUsername.FromLinkedTreeMap(linkedTreeMap));
        }

        return Result.factoryOk(new ExistOrFoundInfo<List<BblScoreWithUsername>>().setValue(arr));
    }

    private Play() {
    }

    private static Play self;
    public static Play getInstance() {
        if (self == null)
            self = new Play();
        return self;
    }
}
