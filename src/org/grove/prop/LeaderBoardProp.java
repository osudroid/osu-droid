package org.grove.prop;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */

public class LeaderBoardProp {
    /**
     * limit: How many will be showed
     */
    private int limit;

    /**
     * region: User region
     */
    private String region;


    /**
     * @return get Limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit set Limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return get Region
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region set region
     */
    public void setRegion(String region) {
        this.region = region;
    }

    public static LeaderBoardProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), LeaderBoardProp.class);
    }
}
