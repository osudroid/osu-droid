package org.grove.result;

import com.google.gson.Gson;
import org.grove.prop.Api2UploadReplayFileProp;

public class ApiUpdateInfoV2 {
    private long versionCode;
    private String link;
    private String changelog;

    public long getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(long versionCode) {
        this.versionCode = versionCode;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public static ApiUpdateInfoV2 FromLinkedTreeMap(com.google.gson.internal.LinkedTreeMap linkedTreeMap) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(linkedTreeMap), ApiUpdateInfoV2.class);
    }
}
