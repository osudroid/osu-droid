package org.grove.result;

import com.google.gson.Gson;
import org.grove.result.entities.BblUserStats;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class PushReplayResult200 {
    private BblUserStats userStats;
    private long bestPlayScoreId;

    public BblUserStats getUserStats() {
        return userStats;
    }

    public void setUserStats(BblUserStats userStats) {
        this.userStats = userStats;
    }

    public long getBestPlayScoreId() {
        return bestPlayScoreId;
    }

    public void setBestPlayScoreId(long bestPlayScoreId) {
        this.bestPlayScoreId = bestPlayScoreId;
    }

    public static PushReplayResult200 FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), PushReplayResult200.class);
    }
}
