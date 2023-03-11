

package org.grove;

import org.grove.api.ApiV1;
import org.grove.api.ApiV2;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class Grove {

    static {
        System.out.println("Loading API...");
    }

    public static final ApiV1 V1 = ApiV1.instance;
    public static final ApiV2 V2 = ApiV2.instance;

    public static void init() {
        // Statically load the class
    }
}
