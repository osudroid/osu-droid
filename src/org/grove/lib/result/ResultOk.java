package org.grove.lib.result;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ResultOk<Ok> {
    private final EResult mode;
    private Ok obj;

    public ResultOk(EResult mode, Ok obj) {
        this.mode = mode;
        this.obj = obj;
    }

    public static <Ok> ResultOk<Ok> factoryOk(Ok ok) {
        return new ResultOk<>(EResult.Ok, ok);
    }

    public static <Ok> ResultOk<Ok> factoryErr() {
        return new ResultOk<>(EResult.Err, null);
    }

    public boolean isErr() {
        return mode == EResult.Err;
    }

    public boolean isOk() {
        return mode == EResult.Ok;
    }

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

    public Ok getOk() throws Exception {
        if (isErr())
            throw new Exception("Result is not Ok");
        return (Ok) obj;
    }

    public Ok getOkOr(Ok or) {
        if (isErr())
            return or;
        return (Ok) obj;
    }
}
