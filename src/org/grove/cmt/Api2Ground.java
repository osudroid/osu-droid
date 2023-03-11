package org.grove.cmt;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2Ground<T> {
    private Api2GroundHeader header;
    private T body;

    public Api2GroundHeader getHeader() {
        return header;
    }

    public void setHeader(Api2GroundHeader header) {
        this.header = header;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }
}