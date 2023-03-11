package org.grove.result;

import com.google.gson.Gson;

/**
 * @author Lambda ( GitHub: chaosmac1, Discord: Lambda#0018 )
 * If you have any questions about the API ask me at Discord Development Server (<a href="https://discord.gg/Jumudbq7pz">...</a>) and ping me :)
 */
public class StatisticActiveUser {
    private long ActiveUserLast1h;
    private long ActiveUserLast1Day;
    private long RegisterUser;

    public long getActiveUserLast1h() {
        return ActiveUserLast1h;
    }

    public void setActiveUserLast1h(long activeUserLast1h) {
        ActiveUserLast1h = activeUserLast1h;
    }

    public long getActiveUserLast1Day() {
        return ActiveUserLast1Day;
    }

    public void setActiveUserLast1Day(long activeUserLast1Day) {
        ActiveUserLast1Day = activeUserLast1Day;
    }

    public long getRegisterUser() {
        return RegisterUser;
    }

    public void setRegisterUser(long registerUser) {
        RegisterUser = registerUser;
    }

    public static StatisticActiveUser FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        StatisticActiveUser res = new StatisticActiveUser();
        res.setActiveUserLast1h(((Double)linkedTreeMap.get("activeUserLast1h")).longValue());
        res.setActiveUserLast1Day(((Double)linkedTreeMap.get("activeUserLast1Day")).longValue());
        res.setRegisterUser(((Double)linkedTreeMap.get("registerUser")).longValue());
        return res;
    }
}
