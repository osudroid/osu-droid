package ru.nsu.ccfit.zuev.osu.model.vo;

public class UpdateVO {

    private long version_code;
    private String link;
    private String changelog;

    public long getVersionCode(){
        return version_code;
    }

    public String getLink(){
        return link;
    }

    public String getChangelog(){
        return changelog;
    }

    public void setVersionCode(long version_code){
        this.version_code = version_code;
    }

    public void setLink(String link){
        this.link = link;
    }

    public void setChangelog(String changelog){
        this.changelog = changelog;
    }

}