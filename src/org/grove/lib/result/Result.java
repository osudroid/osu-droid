package org.grove.lib.result;

import java.util.Map;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Result<Ok, Err extends Exception> {
    private final EResult mode;
    private Object obj;

    public Result(EResult mode, Object obj) {
        this.mode = mode;
        this.obj = obj;
    }

    public static <Ok, Err extends Exception> Result<Ok, Err> factoryOk(Ok ok) {
        return new Result<>(EResult.Ok, ok);
    }

    public static <Ok, Err extends Exception> Result<Ok, Err> factoryErr(Err err) {
        return new Result<>(EResult.Err, err);
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
            throw new RuntimeException(e);
        }
    }

    public Ok getOkUnsafe() {
        return (Ok) obj;
    }

    public Ok getOk() throws Exception {
        if (isErr())
            throw new Exception("Result is not Ok");
        return (Ok) obj;
    }

    public Err getErr() throws Exception {
        if (isOk())
            throw new Exception("Result is not Err");
        return (Err) obj;
    }

    public Err getErrUnsafe() {
        return (Err) obj;
    }

    public Ok getOkOr(Ok or) {
        if (isErr())
            return or;
        return (Ok) obj;
    }

    public Err getErrOr(Err or) {
        if (isOk())
            return or;
        return (Err) obj;
    }
}
