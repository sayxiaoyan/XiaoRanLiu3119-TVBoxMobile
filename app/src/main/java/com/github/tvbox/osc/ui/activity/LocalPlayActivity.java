package com.github.tvbox.osc.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.constant.CacheConst;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.util.HawkConfig;
import com.google.common.reflect.TypeToken;
import com.orhanobut.hawk.Hawk;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import xyz.doikki.videoplayer.player.ProgressManager;
import xyz.doikki.videoplayer.player.VideoView;

public class LocalPlayActivity extends BaseActivity {


    private VideoView mVideoView;
    VodController mController;
    JSONObject mVodPlayerCfg;
    private List<VideoInfo> mVideoList = new ArrayList<>();
    private int mPosition;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_local_play;
    }

    @Override
    protected void init() {
        mVideoView = findViewById(R.id.player);
        mVideoView.startFullScreen();
        Bundle bundle = getIntent().getExtras();
        String videoListJson =  bundle.getString("videoList");
        mVideoList = GsonUtils.fromJson(videoListJson, new TypeToken<List<VideoInfo>>(){}.getType());
        mPosition = bundle.getInt("position", 0);

        initController();
        initPlayerCfg();
        mVideoView.setVideoController(mController); //设置控制器
        play(false);
    }

    /**
     * 跳转到上/下一集,需重新播放
     */
    private void play(boolean fromSkip) {
        VideoInfo videoInfo = mVideoList.get(mPosition);

        String path = videoInfo.getPath();

        String uri = "";
        File file = new File(path);
        if(file.exists()){
            uri = Uri.parse("file://"+file.getAbsolutePath()).toString();
        }
        mController.setTitle(videoInfo.getDisplayName());
        mVideoView.setUrl(uri); //设置视频地址

        mVideoView.setProgressManager(new ProgressManager() {
            @Override
            public void saveProgress(String url, long progress) {
                //有点本地文件确实总时长,设置下总时长,为什么用path,因为电影列表要通过媒体文件的path获取缓存的时长/进度,存取报纸缓存的key一直
                SPUtils.getInstance(CacheConst.VIDEO_DURATION_SP).put(path, mVideoView.getDuration());
                SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).put(path, progress);
            }

            @Override
            public long getSavedProgress(String url) {
                return SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).getLong(path);
            }
        });

        if (fromSkip){
            mVideoView.replay(false);
        }else {
            mVideoView.start(); //开始播放，不调用则不自动播放
        }
    }

    private void initController() {
        mController = new VodController(this);
        mController.showParse(false);
        mController.setListener(new VodController.VodControlListener() {
            @Override
            public void playNext(boolean rmProgress) {
//                String preProgressKey = progressKey;
//                LocalPlayActivity.this.playNext(rmProgress);
                if (mPosition == mVideoList.size() - 1){
                    ToastUtils.showShort("当前已经是最后一集了");
                } else {
                    mPosition++;
                    play(true);
                }
            }

            @Override
            public void playPre() {
                //playPrevious();
                if (mPosition == 0){
                    ToastUtils.showShort("当前已经是第一集了");
                }else {
                    mPosition--;
                    play(true);
                }
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
                finish();
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