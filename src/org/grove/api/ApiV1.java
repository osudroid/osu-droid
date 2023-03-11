package org.grove.api;


import org.grove.api.v1.ProfileStats;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ApiV1 {

    static {
        System.out.println("Loading API V1...");
    }

    public static final ApiV1 instance = new ApiV1();

    public final ProfileStats profileStats = ProfileStats.getInstance();

    private ApiV1() { }
}
