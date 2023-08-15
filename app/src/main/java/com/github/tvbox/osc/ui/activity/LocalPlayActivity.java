package com.github.tvbox.osc.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.File;

import xyz.doikki.videocontroller.StandardVideoController;
import xyz.doikki.videoplayer.player.ProgressManager;
import xyz.doikki.videoplayer.player.VideoView;

public class LocalPlayActivity extends BaseActivity {


    private VideoView mVideoView;
    VodController mController;
    JSONObject mVodPlayerCfg;
    @Override
    protected int getLayoutResID() {
        return R.layout.activity_local_play;
    }

    @Override
    protected void init() {
        mVideoView = findViewById(R.id.player);

        String path = getIntent().getExtras().getString("path");
        String uri = "";
        File file = new File(path);
        if(file.exists()){
            uri = Uri.parse("file://"+file.getAbsolutePath()).toString();
        }
        LogUtils.d("path uri====>"+ uri);

        mVideoView.setUrl(uri); //设置视频地址

        initController();
        initPlayerCfg();
        mVideoView.setVideoController(mController); //设置控制器
        mVideoView.setProgressManager(new ProgressManager() {
            @Override
            public void saveProgress(String url, long progress) {
                //有点本地文件确实总时长,设置下总时长,为什么用path,因为电影列表要通过媒体文件的path获取缓存的时长/进度,存取报纸缓存的key一直
                SPUtils.getInstance("videoDurationSp").put(path, mVideoView.getDuration());
                SPUtils.getInstance("videoProgressSp").put(path, progress);
            }

            @Override
            public long getSavedProgress(String url) {
                return SPUtils.getInstance("videoProgressSp").getLong(url);
            }
        });
        mVideoView.start(); //开始播放，不调用则不自动播放
    }

    private void initController() {
        mController = new VodController(this);
        mController.showParse(false);
        mController.setListener(new VodController.VodControlListener() {
            @Override
            public void playNext(boolean rmProgress) {
//                String preProgressKey = progressKey;
//                LocalPlayActivity.this.playNext(rmProgress);

            }

            @Override
            public void playPre() {
                //playPrevious();
            }

            @Override
            public void changeParse(ParseBean pb) {

            }

            @Override
            public void updatePlayerCfg() {

            }

            @Override
            public void replay(boolean replay) {

            }

            @Override
            public void errReplay() {

            }

            @Override
            public void selectSubtitle() {

            }

            @Override
            public void selectAudioTrack() {

            }

            @Override
            public void prepared() {

            }

            @Override
            public void toggleFullScreen() {

            }

            @Override
            public void exit() {

            }
        });

    }

    void initPlayerCfg() {
        try {
            mVodPlayerCfg = new JSONObject();
        } catch (Throwable th) {
            mVodPlayerCfg = new JSONObject();
        }
        try {
            if (!mVodPlayerCfg.has("pl")) {
                mVodPlayerCfg.put("pl", Hawk.get(HawkConfig.PLAY_TYPE, 1));
            }
            if (!mVodPlayerCfg.has("pr")) {
                mVodPlayerCfg.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0));
            }
            if (!mVodPlayerCfg.has("ijk")) {
                mVodPlayerCfg.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, ""));
            }
            if (!mVodPlayerCfg.has("sc")) {
                mVodPlayerCfg.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0));
            }
            if (!mVodPlayerCfg.has("sp")) {
                mVodPlayerCfg.put("sp", 1.0f);
            }
            if (!mVodPlayerCfg.has("st")) {
                mVodPlayerCfg.put("st", 0);
            }
            if (!mVodPlayerCfg.has("et")) {
                mVodPlayerCfg.put("et", 0);
            }
        } catch (Throwable th) {

        }
        mController.setPlayerConfig(mVodPlayerCfg);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
    }


    @Override
    public void onBackPressed() {
        if (!mVideoView.onBackPressed()) {
            super.onBackPressed();
        }
    }

}