package ru.nsu.ccfit.zuev.osu.model.vo;

import java.util.List;

public class GithubReleaseVO {

    private String html_url;
    private List<Asset> assets;
    private String body;
    
    public void setHtml_url(String html_url){
        this.html_url = html_url;
    }

    public String getHtml_url(){
        return this.html_url;
    }

    public void setAssets(List<Asset> assets){
        this.assets = assets;
    }

    public List<Asset> getAssets(){
        return this.assets;
    }

    public void setBody(String body){
        this.body = body;
    }

    public String getBody(){
        return this.body;
    }

    public class Asset {
        private String name;
        private String content_type;
        private int size;
        private String browser_download_url;

        public void setName(String name){
            this.name = name;
        }

        public String getName(){
            return this.name;
        }

        public void setContent_type(String content_type){
            this.content_type = content_type;
        }

        public String getContent_type(){
            return this.content_type;
        }

        public void setSize(int size){
            this.size = size;
        }

        public int getSize(){
            return this.size;
        }

        public void setBrowser_download_url(String browser_download_url){
            this.browser_download_url = browser_download_url;
        }

        public String getBrowser_download_url(){
            return this.browser_download_url;
        }
    }

}