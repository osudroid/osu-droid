package org.grove.prop;

import com.google.gson.Gson;
import org.grove.utils.IToSingleString;
import org.grove.utils.Merge;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2MapFileRankProp implements IToSingleString {

    /**
     * Filename: of Map name
     */
    public String Filename;

    /**
     * FileHash: Map hash
     */
    public String FileHash;

    /**
     * @return get Filename of Map
     */
    public String getFilename() {
        return Filename;
    }

    /**
     * @param filename set Filename of Map
     */
    public void setFilename(String filename) {
        Filename = filename;
    }

    /**
     * @return get FileHash of Map
     */
    public String getFileHash() {
        return FileHash;
    }

    /**
     * @param fileHash set FileHash of Map
     */
    public void setFileHash(String fileHash) {
        FileHash = fileHash;
    }

    /**
     * @return Combine all to a Single String
     */
    public String toSingleString() {
        return Merge.ObjectsToString(getFilename(), getFileHash());
    }

    public static Api2MapFileRankProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), Api2MapFileRankProp.class);
    }
}
