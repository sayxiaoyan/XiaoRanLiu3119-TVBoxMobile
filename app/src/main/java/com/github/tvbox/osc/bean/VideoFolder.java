package com.github.tvbox.osc.bean;

import java.util.List;

/**
 * @Author : Liu XiaoRan
 * @Email : 592923276@qq.com
 * @Date : on 2023/8/15 10:58.
 * @Description :
 */
public class VideoFolder {
    public VideoFolder(String name, List<VideoInfo> videoList) {
        this.name = name;
        this.videoList = videoList;
    }

    String name;
    List<VideoInfo> videoList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<VideoInfo> getVideoList() {
        return videoList;
    }

    public void setVideoList(List<VideoInfo> videoList) {
        this.videoList = videoList;
    }
}