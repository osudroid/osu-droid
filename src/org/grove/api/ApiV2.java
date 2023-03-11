package org.grove.api;

import org.grove.api.v2.*;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class ApiV2 {

    static {
        System.out.println("Loading API V1...");
    }

    public static final ApiV2 instance = new ApiV2();

    public final Avatar avatar = Avatar.getInstance();
    public final Leaderboard leaderboard = Leaderboard.getInstance();
    public final Login login = Login.getInstance();
    public final Odr odr = Odr.getInstance();
    public final Play play = Play.getInstance();
    public final Rank rank = Rank.getInstance();
    public final Statistic statistic = Statistic.getInstance();
    public final Submit submit = Submit.getInstance();

    private ApiV2() { }
}
