package com.github.tvbox.osc.ui.activity;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.blankj.utilcode.util.ColorUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.VideoFolder;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.constant.CacheConst;
import com.github.tvbox.osc.databinding.ActivityMovieFoldersBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.FolderAdapter;
import com.github.tvbox.osc.ui.adapter.LocalVideoAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.Utils;
import com.lxj.xpopup.XPopup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VideoListActivity extends BaseVbActivity<ActivityMovieFoldersBinding> {


    private String mBucketDisplayName;
    private LocalVideoAdapter mLocalVideoAdapter;
    private int mSelectedCount = 0;

    @Override
    protected void init() {

        mBucketDisplayName = getIntent().getExtras().getString("bucketDisplayName");
        mBinding.titleBar.setTitle(mBucketDisplayName);
        mLocalVideoAdapter = new LocalVideoAdapter();
        mBinding.rv.setAdapter(mLocalVideoAdapter);
        mLocalVideoAdapter.setOnItemClickListener((adapter, view, position) -> {
            VideoInfo videoInfo = (VideoInfo) adapter.getItem(position);
            if (mLocalVideoAdapter.isSelectMode()) {
                videoInfo.setChecked(!videoInfo.isChecked());
                mLocalVideoAdapter.notifyDataSetChanged();
            }else {
                Bundle bundle = new Bundle();
//                    bundle.putString("path",videoInfo.getPath());
                bundle.putString("videoList", GsonUtils.toJson(mLocalVideoAdapter.getData()));
                bundle.putInt("position", position);
                jumpActivity(LocalPlayActivity.class,bundle);
            }
        });

        mLocalVideoAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            openListSelectMode(true);

            VideoInfo videoInfo = (VideoInfo) adapter.getItem(position);
            videoInfo.setChecked(true);
            mLocalVideoAdapter.notifyDataSetChanged();
            return true;
        });

        mBinding.tvAllCheck.setOnClickListener(view -> {//全选
            FastClickCheckUtil.check(view);
            for (VideoInfo item : mLocalVideoAdapter.getData()) {
                item.setChecked(true);
            }
            mLocalVideoAdapter.notifyDataSetChanged();
        });

        mBinding.tvCancelAllChecked.setOnClickListener(view -> {//取消全选
            FastClickCheckUtil.check(view);
            cancelAll();
        });

        mLocalVideoAdapter.setOnSelectCountListener(count -> {
            mSelectedCount = count;
            if (mSelectedCount>0){
                mBinding.tvDelete.setEnabled(true);
                mBinding.tvDelete.setTextColor(ColorUtils.getColor(R.color.colorPrimary));
            }else {
                mBinding.tvDelete.setEnabled(false);
                mBinding.tvDelete.setTextColor(ColorUtils.getColor(R.color.disable_text));
            }
        });

        mBinding.tvDelete.setOnClickListener(view -> {
            FastClickCheckUtil.check(view);
            new XPopup.Builder(this)
                    .isDarkTheme(Utils.isDarkTheme())
                    .asConfirm("提示","确定删除所选视频吗？",() -> {
                        List<VideoInfo> data = mLocalVideoAdapter.getData();
                        List<VideoInfo> deleteList = new ArrayList<>();
                        for (VideoInfo item : data) {
                            if (item.isChecked()) {
                                deleteList.add(item);
                                if (FileUtils.delete(item.getPath())) {
                                    // 删除缓存的影片时长、进度
                                    SPUtils.getInstance(CacheConst.VIDEO_DURATION_SP).remove(item.getPath());
                                    SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).remove(item.getPath());
                                    // 文件增删需要通知系统扫描,否则删除文件后还能查出来
                                    // 这个工具类直接传文件路径不知道为啥通知失败,手动获取一下
                                    FileUtils.notifySystemToScan(FileUtils.getDirName(item.getPath()));
                                }
                            }
                        }
                        data.removeAll(deleteList);
                        mLocalVideoAdapter.notifyDataSetChanged();
                        openListSelectMode(false);
                    }).show();
        });
    }

    private void openListSelectMode(boolean open){
        mLocalVideoAdapter.setSelectMode(open);
        mBinding.llMenu.setVisibility(open ? View.VISIBLE : View.GONE);
        if (!open){// 开启时设置了当前item为选中状态已经刷新了.所以只在关闭刷新列表
            mLocalVideoAdapter.notifyDataSetChanged();
        }
    }

    private void cancelAll(){
        for (VideoInfo item : mLocalVideoAdapter.getData()) {
            item.setChecked(false);
        }
        mLocalVideoAdapter.notifyDataSetChanged();
    }
    @Override
    public void refresh(RefreshEvent event) {
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

    @Override
    public void onBackPressed() {
        if (mLocalVideoAdapter.isSelectMode()){
            if (mSelectedCount>0){
                cancelAll();
            }else {
                openListSelectMode(false);
            }
        }else {
            super.onBackPressed();
        }
    }
}