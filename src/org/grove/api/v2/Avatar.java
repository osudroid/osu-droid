package org.grove.api.v2;

import org.grove.Settings;
import org.grove.cmt.Api2GroundNoHeader;
import org.grove.lib.result.Result;
import org.grove.result.AvatarHashes;
import org.grove.result.AvatarHashesByUserIdsProp;
import org.grove.utils.Request;

import java.util.List;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Avatar {

    /**
     * @param size ImageWith in px
     * @param id UserID
     * @return if ok, you get the img as bytes. If Error, you get are error as string
     */
    public Result<byte[], Exception> getAvatar(int size, long id) {
        final String addLink = "/api2/avatar/" + size +  "/" + id;
        return Request.getGetByteArray(Settings.getDomain() + addLink);
    }

    public Result<AvatarHashes, Exception> getAvatarHashByUserId(long size, long id) {
        final String addLink = "/api2/avatar/hash/" + size + "/" + id;
        return Request.getGetJson(
                (Class<AvatarHashes>) (new AvatarHashes()).getClass(),
                Settings.getDomain() + addLink
        );
    }

    public Result<AvatarHashes, Exception> getAvatarHashesByUserIds(AvatarHashesByUserIdsProp prop) {
        final String addLink = "/api2/avatar/hash/";

        return Request.postSendJsonGetJson(
                (Class<AvatarHashes>) (new AvatarHashes()).getClass(),
                Settings.getDomain() + addLink,
                Api2GroundNoHeader.factory(prop)
                );
    }


    private Avatar() {
    }

    private static Avatar self;
    public static Avatar getInstance() {
        if (self == null)
            self = new Avatar();
        return self;
    }
}
