package org.grove.api.v2;

import org.grove.cmt.Api2GroundWithHash;
import org.grove.cmt.Work;
import org.grove.lib.result.Result;
import org.grove.result.PushPlayStartResult200;
import org.grove.result.PushReplayResult200;
import org.grove.prop.*;
import org.grove.Settings;
import org.grove.utils.Request;

import java.util.UUID;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Submit {

    /**
     * @return if ok, you submit Play start of the Map  . If Error, you get are error as string
     */
    public Result<PushPlayStartResult200, Exception> pushPlayStart(PushPlayStartProp prop, UUID token) {
        final String addLink = "/api2/submit/play-start";
        return Request.postSendJsonGetJson((Class<PushPlayStartResult200>) (new PushPlayStartResult200()).getClass(),Settings.getDomain() + addLink, Api2GroundWithHash.factory(prop, token));
    }

    /**
     * @return if ok, you submit play end of the Map. If Error, you get are error as string
     */
    public Result<PushReplayResult200, Exception> pushReplay(PushPlayProp prop, UUID token) {
        final String addLink = "/api2/submit/play-end";
        return Request.postSendJsonGetJson((Class<PushReplayResult200>) (new PushReplayResult200()).getClass(), Settings.getDomain() + addLink, Api2GroundWithHash.factory(prop, token));
    }

    /**
     * @return if ok, you submit replay file of the Map. If Error, you get are error as string
     */
    public Result<Work, Exception> uploadReplayFile(Api2UploadReplayFileProp prop, UUID token, byte[] uploadedFile) {
        final String addLink = "/api2/submit/replay-file";
        return Request.postGetJsonSendFormFileAndFormPropJson((Class<Work>) (new Work()).getClass(), Settings.getDomain() + addLink, uploadedFile, Api2GroundWithHash.factory(prop, token));
    }

    private Submit() {
    }

    private static Submit self;
    public static Submit getInstance() {
        if (self == null)
            self = new Submit();
        return self;
    }
}





















