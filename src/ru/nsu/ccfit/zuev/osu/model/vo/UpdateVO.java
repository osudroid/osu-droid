package ru.nsu.ccfit.zuev.osu.model.vo;

import com.google.gson.annotations.SerializedName;

/**
 * @author dgsrz.
 */
public class UpdateVO {

    @SerializedName("v_code")
    private Integer versionCode;

    @SerializedName("desc")
    private String description;

    @SerializedName("link")
    private String link;

    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
