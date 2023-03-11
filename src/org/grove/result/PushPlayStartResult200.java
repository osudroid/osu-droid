package org.grove.result;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class PushPlayStartResult200 {
    private long playId;

    public long getPlayId() {
        return playId;
    }

    public void setPlayId(long playId) {
        this.playId = playId;
    }

    public static PushPlayStartResult200 FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), PushPlayStartResult200.class);
    }
}
