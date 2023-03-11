package org.grove.cmt;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Api2GroundNoHeader<T> {
    private T body;

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public static <T> Api2GroundNoHeader<T> factory(T body) {
        Api2GroundNoHeader<T> res = new Api2GroundNoHeader<T>();
        res.setBody(body);
        return res;
    }
}