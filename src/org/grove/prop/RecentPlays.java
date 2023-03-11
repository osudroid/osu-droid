package org.grove.prop;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class RecentPlays {

    /**
     * filterPlays: filters the Map play
     */
    private String filterPlays;

    /**
     * orderBy: order by Map play
     */
    private String orderBy;

    /**
     * limit: How many will be showed
     */
    private int limit;

    /**
     * startAt: start at Map play
     */
    private int startAt;

    /**
     * @return get Filter Plays of Map play
     */
    public String getFilterPlays() {
        return filterPlays;
    }

    /**
     * @param filterPlays filter the Map play
     */
    public void setFilterPlays(String filterPlays) {
        this.filterPlays = filterPlays;
    }

    /**
     * @return get OrderBy Map play
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * @param orderBy set OrderBy Map play
     */
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * @return get Limit of showing list
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit set Limit of List
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return get Start at Map Play
     */
    public int getStartAt() {
        return startAt;
    }

    /**
     * @param startAt set Start at Map play
     */
    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    public static RecentPlays FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), RecentPlays.class);
    }
}
