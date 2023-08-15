package com.github.tvbox.osc.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;

import android.os.Bundle;
import android.text.TextUtils;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.VideoFolder;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.databinding.ActivityMovieFoldersBinding;
import com.github.tvbox.osc.ui.adapter.FolderAdapter;
import com.github.tvbox.osc.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MovieFoldersActivity extends BaseVbActivity<ActivityMovieFoldersBinding> {

    private FolderAdapter mFolderAdapter;

    @Override
    protected void init() {

        mFolderAdapter = new FolderAdapter();
        mBinding.rv.setAdapter(mFolderAdapter);
        groupVideos();
    }

    /**
     * 按文件夹名字分组视频
     */
    private void groupVideos(){
        List<VideoInfo> videoList = Utils.getVideoList();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Map<String, List<VideoInfo>> videoMap = videoList.stream()
                    .collect(Collectors.groupingBy(VideoInfo::getBucketDisplayName));
            List<VideoFolder> videoFolders = new ArrayList<>();
            videoMap.forEach((key, value) -> {
                VideoFolder videoFolder = new VideoFolder(key,value);
                videoFolders.add(videoFolder);
            });
            mFolderAdapter.setNewData(videoFolders);
        }
    }
}