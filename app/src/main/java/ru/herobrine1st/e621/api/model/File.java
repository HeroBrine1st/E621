package ru.herobrine1st.e621.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class File {
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public String getMd5() {
        return md5;
    }

    public String getUrl() {
        return url;
    }

    int width;
    int height;
    @JsonProperty("ext")
    String extension;
    long size;
    String md5;
    String url;
}
