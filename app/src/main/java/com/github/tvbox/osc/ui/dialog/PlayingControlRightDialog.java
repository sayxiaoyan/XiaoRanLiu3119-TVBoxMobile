package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ColorUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.DialogPlayingControlBinding;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.player.controller.VodController;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.core.DrawerPopupView;

import org.jetbrains.annotations.NotNull;

public class PlayingControlRightDialog extends DrawerPopupView {

    private final VodController mController;
    MyVideoView mPlayer;
    private DialogPlayingControlBinding mBinding;

    public PlayingControlRightDialog(@NonNull @NotNull Context context, VodController controller, MyVideoView videoView) {
        super(context);
        mController = controller;
        mPlayer = videoView;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_playing_control;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mBinding = DialogPlayingControlBinding.bind(getPopupImplView());

        initView();
        initListener();
    }

    private void initView(){
        mBinding.scale.setText(mController.mPlayerScaleBtn.getText());
        mBinding.playTimeStart.setText(mController.mPlayerTimeStartBtn.getText());
        mBinding.playTimeEnd.setText(mController.mPlayerTimeSkipBtn.getText());
        mBinding.player.setText(mController.mPlayerBtn.getText());
        mBinding.decode.setText(mController.mPlayerIJKBtn.getText());
        updateSpeedUi();
    }

    private void initListener(){
        mBinding.speed1.setOnClickListener(view -> setSpeed(mBinding.speed1));
        mBinding.speed2.setOnClickListener(view -> setSpeed(mBinding.speed2));
        mBinding.speed3.setOnClickListener(view -> setSpeed(mBinding.speed3));

        mBinding.scale.setOnClickListener(view -> changeAndUpdateText(mBinding.scale,mController.mPlayerScaleBtn));
        mBinding.playTimeStart.setOnClickListener(view -> changeAndUpdateText(mBinding.playTimeStart,mController.mPlayerTimeStartBtn));
        mBinding.playTimeEnd.setOnClickListener(view -> changeAndUpdateText(mBinding.playTimeEnd,mController.mPlayerTimeSkipBtn));
        mBinding.player.setOnClickListener(view -> changeAndUpdateText(mBinding.player,mController.mPlayerBtn));
        mBinding.decode.setOnClickListener(view -> changeAndUpdateText(mBinding.decode,mController.mPlayerIJKBtn));

        mBinding.startEndReset.setOnClickListener(view -> resetSkipStartEnd());
        mBinding.replay.setOnClickListener(view -> changeAndUpdateText(null,mController.mPlayRetry));
        mBinding.refresh.setOnClickListener(view -> changeAndUpdateText(null,mController.mPlayRefresh));
        mBinding.subtitle.setOnClickListener(view -> dismissWith(() -> changeAndUpdateText(null,mController.mZimuBtn)));
    }

    /**
     * 点击直接调用controller里面声明好的点击事件,(不改动原逻辑,隐藏controller里的设置view,全由弹窗设置)
     * @param view 不为空变更配置文字,如更换播放器/缩放, 为空只操作点击之间,不需改变文字,如刷新/重播
     * @param targetView
     */
    private void changeAndUpdateText(TextView view,TextView targetView){
        targetView.performClick();
        if (view!=null){
            view.setText(targetView.getText());
        }
    }

    private void setSpeed(TextView textView){
        mController.setSpeed(textView.getText().toString().replace("x",""));
        updateSpeedUi();
    }

    private void updateSpeedUi(){
        for (int i = 0; i <mBinding.containerSpeed.getChildCount(); i++) {
            TextView tv= (TextView) mBinding.containerSpeed.getChildAt(i);
            if (String.valueOf(mPlayer.getSpeed()).equals(tv.getText().toString().replace("x",""))){
                tv.setBackground(getResources().getDrawable(R.drawable.bg_r_common_solid_primary));
                tv.setTextColor(ColorUtils.getColor(R.color.white));
            }else {
                tv.setBackground(getResources().getDrawable(R.drawable.bg_r_common_stroke_primary));
                tv.setTextColor(ColorUtils.getColor(R.color.text_gray));
            }
        }
    }

    /**
     * 重置片头/尾,刷新文字
     */
    private void resetSkipStartEnd(){
        changeAndUpdateText(null,mController.mPlayerTimeResetBtn);
        mBinding.playTimeStart.setText(mController.mPlayerTimeStartBtn.getText());
        mBinding.playTimeEnd.setText(mController.mPlayerTimeSkipBtn.getText());
    }

}