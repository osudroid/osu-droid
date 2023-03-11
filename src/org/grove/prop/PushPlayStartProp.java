package org.grove.prop;

import com.google.gson.Gson;
import org.grove.utils.IToSingleString;
import org.grove.utils.Merge;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class PushPlayStartProp extends SubmitScoreProp implements IToSingleString {

    /**
     * filename: of Map name
     */
    private String filename;

    /**
     * fileHash: of Map hash
     */
    private String fileHash;

    /**
     * @return get FileName of Map
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename of Map
     */

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return get FileHash of Map
     */
    public String getFileHash() {
        return fileHash;
    }

    /**
     * @param fileHash set FileHash of Map
     */
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    /**
     * @return set Start at Map play
     */
    public String toSingleString() {
        return Merge.ObjectsToString(getFilename(), getFileHash());
    }

    public static PushPlayStartProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), PushPlayStartProp.class);
    }
}
