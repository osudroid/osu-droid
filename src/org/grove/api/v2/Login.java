package org.grove.api.v2;

import org.grove.Settings;
import org.grove.cmt.ExistOrFoundInfo;
import org.grove.cmt.Work;
import org.grove.lib.result.Result;
import org.grove.prop.CreateApi2TokenProp;
import org.grove.prop.SimpleTokenProp;
import org.grove.result.CreateApi2TokenResult;
import org.grove.utils.Request;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Login {

    /**
     * @return if ok, you get the Token and information if its misspellings the UserLogin form . If Error, you get are error as string
     */
    public Result<CreateApi2TokenResult, Exception> tokenCreate(CreateApi2TokenProp prop) {
        final String addLink = "/api2/token-create";
        return Request.postSendJsonGetJson((Class<CreateApi2TokenResult>) (new CreateApi2TokenResult()).getClass(), Settings.getDomain() + addLink, prop);
    }

    /**
     * @return if ok, you get information if refresh work. If Error, you get are error as string
     */
    public Result<Work, Exception> tokenRefresh(SimpleTokenProp prop) {
        final String addLink = "/api2/token-refresh";
        return Request.postSendJsonGetJson((Class<Work>) (new Work()).getClass(), Settings.getDomain() + addLink, prop);
    }

    /**
     * @return if ok, you get information if it's removed. If Error, you get are error as string
     */
    public Result<Work, Exception> tokenRemove(SimpleTokenProp prop) {
        final String addLink = "/api2/token-remove";
        return Request.postSendJsonGetJson((Class<Work>) (new Work()).getClass(), Settings.getDomain() + addLink, prop);
    }

    /**
     * @return if ok, you get UserID if Token is valid. If Error, you get are error as string
     */
    public Result<ExistOrFoundInfo<Long>, Exception> tokenUserId(SimpleTokenProp prop) {
        final String addLink = "/api2/token-user-id";
        Result<ExistOrFoundInfo<Long>, Exception> holder =  Request.postSendJsonGetJson((Class<ExistOrFoundInfo<Long>>) (new ExistOrFoundInfo<Long>()).getClass(),Settings.getDomain() + addLink, prop);
        if (holder.isOk()) {
            try {
                holder.getOk().setValue(((Double) (Object) holder.getOk().getValue()).longValue());
            } catch (Exception e) {
                return Result.factoryErr(e);
            }
        }
        return holder;
    }

    private Login() {
    }

    private static Login self;
    public static Login getInstance() {
        if (self == null)
            self = new Login();
        return self;
    }
}
