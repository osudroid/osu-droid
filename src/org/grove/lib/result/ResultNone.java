package org.grove.lib.result;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ResultNone {
    private EResult mode;

    private ResultNone(EResult mode) {
        this.mode = mode;
    }

    public boolean isErr() {
        return mode == EResult.Err;
    }

    public boolean isOk() { return mode == EResult.Ok; }

    @Override
    public boolean equals(Object result) {
        if (result instanceof EResult)
            return this.mode == result;
        try {
            throw new Exception("Must be instanceof EResult");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static ResultNone factoryOk() { return new ResultNone(EResult.Ok); }
    public static ResultNone factoryErr() { return new ResultNone(EResult.Err); }

}
