package org.grove.prop;

import com.google.gson.Gson;
import org.grove.utils.IToSingleString;
import org.grove.utils.Merge;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class PushPlayProp extends SubmitScoreProp implements IToSingleString {
    public String toSingleString() {
        return Merge.ObjectsToString(
                getMode(),
                getMark(),
                getId(),
                getScore(),
                getCombo(),
                getUid(),
                getGeki(),
                getPerfect(),
                getKatu(),
                getGood(),
                getBad(),
                getMiss(),
                getAccuracy()
        );
    }

    public static PushPlayProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), PushPlayProp.class);
    }
}
