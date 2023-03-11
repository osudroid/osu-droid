package org.grove.prop;

import com.google.gson.Gson;
import org.grove.utils.IToSingleString;
import org.grove.utils.Merge;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2PlayById implements IToSingleString {

    /**
     * playID: playID of playing Map
     */
    private long playId;

    /**
     * @return get PlayID of playing Map
     */
    public long getPlayId() {
        return playId;
    }

    /**
     * @param playId setPlayID of playing Map
     */
    public void setPlayId(long playId) {
        this.playId = playId;
    }

    /**
     * @return to SingleString of playing Map
     */
    public String toSingleString() {
        return Merge.ObjectsToString(getPlayId());
    }

    public static Api2PlayById FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), Api2PlayById.class);
    }
}
