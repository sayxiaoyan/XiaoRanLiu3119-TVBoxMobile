package com.github.tvbox.osc.ui.fragment;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.activity.LocalPlayActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lxj.xpopup.XPopup;

import java.util.List;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class MyFragment extends BaseLazyFragment {

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_my;
    }

    @Override
    protected void init() {
        findViewById(R.id.tvLive).setOnClickListener(v -> jumpActivity(LivePlayActivity.class));

        findViewById(R.id.tvSetting).setOnClickListener(v -> jumpActivity(SettingActivity.class));

        findViewById(R.id.tvHistory).setOnClickListener(v -> jumpActivity(HistoryActivity.class));

        findViewById(R.id.tvFavorite).setOnClickListener(v -> jumpActivity(CollectActivity.class));

        findViewById(R.id.tvLocal).setOnClickListener(v -> {
            if (!XXPermissions.isGranted(mContext, Permission.MANAGE_EXTERNAL_STORAGE)) {
                showPermissionTipPopup();
            } else {
                jumpActivity(LocalPlayActivity.class);
            }
        });

    }

    private void showPermissionTipPopup(){
        new XPopup.Builder(mActivity)
                .asConfirm("提示","为了播放视频、音频等,我们需要访问您设备文件的读写权限", () -> {
                    getPermission();
                }).show();
    }

    private void getPermission(){
        XXPermissions.with(this)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (!all) {
                            ToastUtils.showLong("部分权限未正常授予,请授权");
                        }
                        jumpActivity(LocalPlayActivity.class);
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            ToastUtils.showLong("读写文件权限被永久拒绝，请手动授权");
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(mActivity, permissions);
                        } else {
                            ToastUtils.showShort("获取权限失败");
                            showPermissionTipPopup();
                        }
                    }
                });
    }
}