package org.grove.prop;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class CreateApi2TokenProp {
    /**
     * username: username
     */
    public String username;

    /**
     * passwd: password
     */
    public String passwd;

    /**
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username set Username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return get Password
     */
    public String getPasswd() {
        return passwd;
    }

    /**
     * @param passwd set Password
     */
    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public static CreateApi2TokenProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), CreateApi2TokenProp.class);
    }
}
