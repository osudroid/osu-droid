package org.grove.api.v2;

import org.grove.Settings;
import org.grove.lib.result.Result;
import org.grove.utils.Request;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Odr {

    /**
     * @param replayId replayID
     * @return if ok, you get the img as bytes. If Error, you get are error as string
     */
    public Result<byte[], Exception> getAsOdr(long replayId) {
        final String addLink = "/api2/odr/" + replayId +  ".odr";
        return Request.getGetByteArray(Settings.getDomain() + addLink);
    }
    public Result<byte[], Exception> getAsZip(long replayId) {
        final String addLink = "/api2/odr/" + replayId +  ".zip";
        return Request.getGetByteArray(Settings.getDomain() + addLink);
    }
    public Result<byte[], Exception> getAsZipFullname(long replayId, String fullname) {
        final String addLink = "/api2/odr/fullname/" + replayId + "/" + fullname + ".zip";
        return Request.getGetByteArray(Settings.getDomain() + addLink);
    }
    public static Result<byte[], Exception> getAsZipRedirect(long replayId) {
        final String addLink = "/api2/odr/redirect/" + replayId + ".zip";
        return Request.getGetByteArray(Settings.getDomain() + addLink);
    }

    private Odr() {
    }

    private static Odr self;
    public static Odr getInstance() {
        if (self == null)
            self = new Odr();
        return self;
    }
}
