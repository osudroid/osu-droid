package org.grove.result;

import com.google.gson.Gson;
import org.grove.result.entities.BblScore;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class PlayInfoById {
    private BblScore Score;
    private String username;
    private String region;

    public BblScore getScore() {
        return Score;
    }

    public void setScore(BblScore score) {
        Score = score;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public static PlayInfoById FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), PlayInfoById.class);
    }
}
