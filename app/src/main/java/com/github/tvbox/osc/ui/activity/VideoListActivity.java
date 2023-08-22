package com.github.tvbox.osc.ui.activity;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.VideoFolder;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.databinding.ActivityMovieFoldersBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.FolderAdapter;
import com.github.tvbox.osc.ui.adapter.LocalVideoAdapter;
import com.github.tvbox.osc.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VideoListActivity extends BaseVbActivity<ActivityMovieFoldersBinding> {


    private String mBucketDisplayName;
    private LocalVideoAdapter mLocalVideoAdapter;

    @Override
    protected void init() {

        mBucketDisplayName = getIntent().getExtras().getString("bucketDisplayName");
        mBinding.titleBar.setTitle(mBucketDisplayName);
        mLocalVideoAdapter = new LocalVideoAdapter();
        mBinding.rv.setAdapter(mLocalVideoAdapter);
        mLocalVideoAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                VideoInfo videoInfo = (VideoInfo) adapter.getItem(position);
                if (videoInfo != null) {
                    Bundle bundle = new Bundle();
//                    bundle.putString("path",videoInfo.getPath());
                    bundle.putString("videoList", GsonUtils.toJson(mLocalVideoAdapter.getData()));
                    bundle.putInt("position", position);
                    jumpActivity(LocalPlayActivity.class,bundle);
                }
            }
        });
    }

    @Override
    public void onEvent(RefreshEvent event) {
        new Handler().postDelayed(this::groupVideos,1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        groupVideos();
    }

    /**
     * 根据文件夹名字筛选视频
     */
    private void groupVideos(){
        List<VideoInfo> videoList = Utils.getVideoList();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<VideoInfo> collect = videoList.stream().filter(videoInfo -> videoInfo.getBucketDisplayName().equals(mBucketDisplayName)).collect(Collectors.toList());
            mLocalVideoAdapter.setNewData(collect);
        }
    }
}