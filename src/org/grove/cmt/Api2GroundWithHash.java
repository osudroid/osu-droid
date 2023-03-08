package org.grove.cmt;

import org.grove.utils.IToSingleString;

import java.util.UUID;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2GroundWithHash<T extends IToSingleString> {
    private Api2GroundHeaderWithHash header;
    private T body;

    public Api2GroundHeaderWithHash getHeader() {
        return header;
    }

    public void setHeader(Api2GroundHeaderWithHash header) {
        this.header = header;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public static <T extends IToSingleString> Api2GroundWithHash<T> factory(T body, UUID token) {
        Api2GroundWithHash<T> res = new Api2GroundWithHash<T>();
        res.body = body;
        res.header = Api2GroundHeaderWithHash.factory(body, token);
        return res;
    }
}
