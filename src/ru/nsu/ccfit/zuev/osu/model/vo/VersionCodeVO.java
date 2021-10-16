package ru.nsu.ccfit.zuev.osu.model.vo;

public class VersionCodeVO {

    private long version_code;

    public void setValue(long version_code){
        this.version_code = version_code;
    }

    public long getValue(){
        return this.version_code;
    }

}