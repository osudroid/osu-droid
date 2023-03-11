package org.grove.result;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;

public class AvatarHashes {
    private List<AvatarHash> list;

    public List<AvatarHash> getList() {
        return list;
    }

    public void setList(List<AvatarHash> list) {
        this.list = list;
    }

    public static AvatarHashes FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        AvatarHashes avatarHashes = gson.fromJson(gson.toJson(linkedTreeMap), AvatarHashes.class);
        List<com.google.gson.internal.LinkedTreeMap> listOld = (List<com.google.gson.internal.LinkedTreeMap>)((Object)avatarHashes.list);

        List<AvatarHash> list = new ArrayList<>(listOld.size());
        for (LinkedTreeMap treeMap : listOld) {
            list.add(AvatarHash.FromLinkedTreeMap(treeMap));
        }
        avatarHashes.list = list;
        return avatarHashes;
    }
}
