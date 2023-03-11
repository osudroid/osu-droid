package org.grove.lib.result;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ResultErr<Err> {
    private final EResult mode;
    private Err obj;

    public ResultErr(EResult mode, Err obj) {
        this.mode = mode;
        this.obj = obj;
    }

    public static <Err> ResultErr<Err> factoryOk() {
        return new ResultErr<>(EResult.Ok, null);
    }

    public static <Err> ResultErr<Err> factoryErr(Err err) {
        return new ResultErr<>(EResult.Err, err);
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

    public Err getErr() throws Exception {
        if (isOk())
            throw new Exception("Result is not Err");
        return (Err) obj;
    }

    public Err getErrOr(Err or) {
        if (isOk())
            return or;
        return (Err) obj;
    }
}
