package org.grove.result;

import com.google.gson.Gson;
import org.grove.utils.ISelfNew;

import java.util.UUID;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class CreateApi2TokenResult implements ISelfNew<CreateApi2TokenResult> {
    public UUID token;
    public boolean usernameFalse;
    public boolean passwdFalse;

    public CreateApi2TokenResult selfNew() {
        return new CreateApi2TokenResult();
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public boolean getUsernameFalse() {
        return usernameFalse;
    }

    public void setUsernameFalse(boolean usernameFalse) {
        this.usernameFalse = usernameFalse;
    }

    public boolean getPasswdFalse() {
        return passwdFalse;
    }

    public void setPasswdFalse(boolean passwdFalse) {
        this.passwdFalse = passwdFalse;
    }

    public static CreateApi2TokenResult FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), CreateApi2TokenResult.class);
    }
}