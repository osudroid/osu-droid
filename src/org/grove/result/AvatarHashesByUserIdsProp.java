package org.grove.result;

import com.google.gson.Gson;

public class AvatarHashesByUserIdsProp {
    private int size;
    private Long[] userIds;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Long[] getUserIds() {
        return userIds;
    }

    public void setUserIds(Long[] userIds) {
        this.userIds = userIds;
    }

    public static AvatarHashesByUserIdsProp FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), AvatarHashesByUserIdsProp.class);
    }
}
