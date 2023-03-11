package org.grove.prop;

import com.google.gson.Gson;
import org.grove.utils.IToSingleString;
import org.grove.utils.Merge;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2UploadReplayFileProp implements IToSingleString {

    /**
     * MapHash: of Map hash
     */
    private String MapHash;

    /**
     * ReplayID: replay ID
     */
    private long ReplayId;

    /**
     * @return get MapHash of Map
     */
    public String getMapHash() {
        return MapHash;
    }

    /**
     * @param mapHash set MapHash of map
     */
    public void setMapHash(String mapHash) {
        MapHash = mapHash;
    }

    /**
     * @return get ReplayID of Map
     */
    public long getReplayId() {
        return ReplayId;
    }

    /**
     * @param replayId set ReplayID of Map
     */
    public void setReplayId(long replayId) {
        ReplayId = replayId;
    }

    /**
     * @return Combine all to a Single String
     */
    @Override
    public String toSingleString() {
        return Merge.ObjectsToString(getMapHash(), getReplayId());
    }

    public static Api2UploadReplayFileProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), Api2UploadReplayFileProp.class);
    }
}
