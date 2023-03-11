package org.grove.api.v2;

import com.google.gson.internal.LinkedTreeMap;
import org.grove.Settings;
import org.grove.cmt.Api2GroundWithHash;
import org.grove.cmt.ExistOrFoundInfo;
import org.grove.lib.result.Result;
import org.grove.prop.Api2MapFileRankProp;
import org.grove.result.LeaderBoardUser;
import org.grove.result.MapTopPlays;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.grove.utils.Request;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Rank {

    /**
     * @return if ok, you get map file of map. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<List<MapTopPlays>>, Exception> mapFile(Api2MapFileRankProp prop, UUID token) {
        final String addLink = "/api2/rank/map-file";
        Result<ExistOrFoundInfo<List<LinkedTreeMap>>, Exception> res = Request.postSendJsonGetJson((Class<ExistOrFoundInfo<List<LinkedTreeMap>>>) (new ExistOrFoundInfo<List<LinkedTreeMap>>()).getClass(), Settings.getDomain() + addLink, Api2GroundWithHash.factory(prop, token));

        List<LinkedTreeMap> oldArr = res.getOkOr(null).getValue();

        List<MapTopPlays> arr = new Vector<>(oldArr.size());
        for (int i = 0; i < oldArr.size(); i++) {
            LinkedTreeMap f = oldArr.get(i);
            arr.add(MapTopPlays.FromLinkedTreeMap(f));
        }

        return Result.factoryOk(new ExistOrFoundInfo<List<MapTopPlays>>().setValue(arr));
    }

    private Rank() {
    }

    private static Rank self;
    public static Rank getInstance() {
        if (self == null)
            self = new Rank();
        return self;
    }
}
