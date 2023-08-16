package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VideoFolder;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.constant.CacheConst;
import com.github.tvbox.osc.util.Utils;

import java.util.List;

public class LocalVideoAdapter extends BaseQuickAdapter<VideoInfo, BaseViewHolder> {
    public LocalVideoAdapter() {
        super(R.layout.item_local_video);
    }

    @Override
    protected void convert(BaseViewHolder helper, VideoInfo item) {

        Glide.with(mContext)
                .load(item.getPath())
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.iv_video)
                .centerCrop()
                .into((ImageView) helper.getView(R.id.iv));

//
//        String progress = "";
//        String spacer = " / ";
//        long cacheProgress = SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).getLong(item.getPath(), -1);
//        if (cacheProgress != -1) {
//            progress = Utils.stringForTime(cacheProgress);
//        }
//        //总时长,缺失时长的,从缓存取(如果有)
//        String duration = Utils.stringForTime((int)item.getDuration()==0?SPUtils.getInstance(CacheConst.VIDEO_DURATION_SP).getLong(item.getPath(), 0):item.getDuration());

        helper.setText(R.id.tv_name,item.getDisplayName())
                .setText(R.id.tv_video_size, ConvertUtils.byte2FitMemorySize(item.getSize()))
                .setText(R.id.tv_video_resolution,item.getResolution());



        ProgressBar progressBar = helper.getView(R.id.progressBar);
        TextView tvDuration = helper.getView(R.id.tv_duration);
        // 总时长
        String duration = "";
        if ((int)item.getDuration() == 0) { // 缺失时长/缩略图的,从缓存取(如果有)
            long cacheDuration = SPUtils.getInstance(CacheConst.VIDEO_DURATION_SP).getLong(item.getPath(), -1);
            if (cacheDuration != -1) { // 有缓存
                duration = Utils.stringForTime(cacheDuration);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setMax((int) cacheDuration);
            }
        } else { // 正常视频
            duration = Utils.stringForTime(item.getDuration());
            progressBar.setMax((int)item.getDuration());
        }

        // 已播放进度时长
        long progressPlayed = SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).getLong(item.getPath(), -1);
        if (progressPlayed != -1 && !duration.isEmpty()) { // 有进度且有时长都显示
            tvDuration.setText(Utils.stringForTime(progressPlayed) + "/" + duration);
            progressBar.setProgress((int) progressPlayed);
        } else { // 没有进度(也包括没时长,此时是空串,不耽误)
            tvDuration.setText(duration);
        }
    }
}