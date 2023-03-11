package org.grove.result;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class UsernameAndId {
    private String Username;
    private long Id;

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }

    public static UsernameAndId FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        UsernameAndId data = new UsernameAndId();
        data.setUsername((String)linkedTreeMap.get("username"));
        data.setId(((Double)linkedTreeMap.get("id")).longValue());

        return data;
    }
}
