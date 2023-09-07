package com.github.tvbox.osc.bean;

/**
 * @Author : Liu XiaoRan
 * @Email : 592923276@qq.com
 * @Date : on 2023/9/7 16:29.
 * @Description : 仓库(多仓的单个实例),每个仓库的url都是多线路
 */
public class Source {

    public Source() {
    }

    public Source(String sourceName, String sourceUrl) {
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
    }

    String sourceName;
    String sourceUrl;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}