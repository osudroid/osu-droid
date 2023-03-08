package org.grove.prop;

import com.google.gson.Gson;

import java.util.UUID;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class SimpleTokenProp {

    /**
     * token: UserToken
     */
    private UUID token;

    /**
     * @return get UserToken
     */
    public UUID getToken() {
        return token;
    }

    /**
     * @param token setUserToken
     */
    public void setToken(UUID token) {
        this.token = token;
    }

    public static SimpleTokenProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), SimpleTokenProp.class);
    }
}
