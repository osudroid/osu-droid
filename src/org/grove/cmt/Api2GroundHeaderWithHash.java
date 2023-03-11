package org.grove.cmt;

import org.grove.Settings;
import org.grove.hash.ApiHashHandler;
import org.grove.utils.IToSingleString;

import java.util.UUID;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2GroundHeaderWithHash {
    private UUID token;
    private String hashBodyData;

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public String getHashBodyData() {
        return hashBodyData;
    }

    public void setHashBodyData(String hashBodyData) {
        this.hashBodyData = hashBodyData;
    }

    public static <T extends IToSingleString> Api2GroundHeaderWithHash factory(T body, UUID token) {
        Api2GroundHeaderWithHash res = new Api2GroundHeaderWithHash();
        res.setToken(token);
        res.setHashBodyData(ApiHashHandler.getInstance().createHash(body.toSingleString(), Settings.getDataToken()).getOkOr(""));
        return res;
    }
}