package com.github.tvbox.osc.ui.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;

import com.blankj.utilcode.util.ClickUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ShellUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.gzuliyujiang.filepicker.ExplorerConfig;
import com.github.gzuliyujiang.filepicker.FilePicker;
import com.github.gzuliyujiang.filepicker.annotation.ExplorerMode;
import com.github.gzuliyujiang.filepicker.contract.OnFilePickedListener;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.Source;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.databinding.ActivitySubscriptionBinding;
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter;
import com.github.tvbox.osc.ui.dialog.ChooseSourceDialog;
import com.github.tvbox.osc.ui.dialog.SubsTipDialog;
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog;
import com.github.tvbox.osc.util.HawkConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupPosition;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionActivity extends BaseVbActivity<ActivitySubscriptionBinding> {


    private String mBeforeUrl;
    private String mSelectedUrl;
    private List<Subscription> mSubscriptions;
    private SubscriptionAdapter mSubscriptionAdapter;
    private List<Source> mSources = new ArrayList<>();

    /**
     * 单线路格式
     * "http://top啊啊啊阿萨啊/duo"
     * <p>
     * 多线路格式
     * {
     * "urls": [
     * {
     * "url": "http://",
     * "name": "庭版"
     * },
     * {
     * "url": "http://",
     * "name": "用"
     * }
     * ]
     * }
     * <p>
     * 多仓
     * {
     * "storeHouse":[
     * {"sourceName":"公众号吾爱有三日月与卿"
     * ,"sourceUrl": "http://52bsj.vip:81/api/v3/file/get/67776/1.json?sign=Suz6jqs5w4g4FFTohPyWDC82QQdpOkbs8UN4OR9QJsI%3D%3A0"}
     * ]
     * }
     */
    @Override
    protected void init() {

        mSubscriptionAdapter = new SubscriptionAdapter();
        mBinding.rv.setAdapter(mSubscriptionAdapter);
        mSubscriptions = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());

        mBeforeUrl = Hawk.get(HawkConfig.API_URL, "");
        mSubscriptions.forEach(item -> {
            if (item.isChecked()) {
                mSelectedUrl = item.getUrl();
            }
        });

        mSubscriptionAdapter.setNewData(mSubscriptions);

        mBinding.ivUseTip.setOnClickListener(view -> {
            new XPopup.Builder(this)
                    .asCustom(new SubsTipDialog(this))
                    .show();
        });

        mBinding.titleBar.getRightView().setOnClickListener(view -> {//添加订阅
            new XPopup.Builder(this)
                    .autoFocusEditText(false)
                    .asCustom(new SubsciptionDialog(this, "订阅: " + (mSubscriptions.size() + 1), new SubsciptionDialog.OnSubsciptionListener() {
                        @Override
                        public void onConfirm(String name, String url) {
                            for (Subscription item : mSubscriptions) {
                                if (item.getUrl().equals(url)) {
                                    ToastUtils.showLong("订阅地址与" + item.getName() + "相同");
                                    return;
                                }
                            }
                            addSubscription(name, url);
                        }

                        @Override
                        public void chooseLocal() {//本地导入
                            if (!XXPermissions.isGranted(mContext, Permission.MANAGE_EXTERNAL_STORAGE)) {
                                showPermissionTipPopup();
                            } else {
                                pickFile();
                            }
                        }
                    })).show();
        });

        mSubscriptionAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            LogUtils.d("删除订阅");
            if (view.getId() == R.id.iv_del) {
                if (mSubscriptions.get(position).isChecked()) {
                    ToastUtils.showShort("不能删除当前使用的订阅");
                    return;
                }

                new XPopup.Builder(SubscriptionActivity.this)
                        .asConfirm("删除订阅", "确定删除订阅吗？", () -> {
                            mSubscriptions.remove(position);
                            //删除/选择只刷新,不触发重新排序
                            mSubscriptionAdapter.notifyDataSetChanged();
                        }).show();
            }
        });


        mSubscriptionAdapter.setOnItemClickListener((adapter, view, position) -> {//选择订阅
            for (int i = 0; i < mSubscriptions.size(); i++) {
                Subscription subscription = mSubscriptions.get(i);
                if (i == position) {
                    subscription.setChecked(true);
                    mSelectedUrl = subscription.getUrl();
                } else {
                    subscription.setChecked(false);
                }
            }
            //删除/选择只刷新,不触发重新排序
            mSubscriptionAdapter.notifyDataSetChanged();
        });

        mSubscriptionAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            Subscription item = mSubscriptions.get(position);
            new XPopup.Builder(this)
                    .atView(view.findViewById(R.id.tv_name))
                    .hasShadowBg(false)
                    .asAttachList(new String[]{item.isTop() ? "取消置顶" : "置顶", "复制地址"}, null, (index, text) -> {
                        if (index == 0) {
                            item.setTop(!item.isTop());
                            mSubscriptions.set(position, item);
                            mSubscriptionAdapter.setNewData(mSubscriptions);
                        } else {
                            ClipboardUtils.copyText(mSubscriptions.get(position).getUrl());
                            ToastUtils.showLong("已复制");
                        }
                    }).show();
            return true;
        });
    }

    private void showPermissionTipPopup() {
        new XPopup.Builder(SubscriptionActivity.this)
                .asConfirm("提示", "这将访问您设备文件的读取权限", () -> {
                    XXPermissions.with(this)
                            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                            .request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(List<String> permissions, boolean all) {
                                    if (all) {
                                        pickFile();
                                    } else {
                                        ToastUtils.showLong("部分权限未正常授予,请授权");
                                    }
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    if (never) {
                                        ToastUtils.showLong("读写文件权限被永久拒绝，请手动授权");
                                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                        XXPermissions.startPermissionActivity(SubscriptionActivity.this, permissions);
                                    } else {
                                        ToastUtils.showShort("获取权限失败");
                                        showPermissionTipPopup();
                                    }
                                }
                            });
                }).show();
    }

    private void pickFile() {
        ExplorerConfig config = new ExplorerConfig(SubscriptionActivity.this);
        config.setAllowExtensions(new String[]{".txt", ".json", ".jar"});
        config.setExplorerMode(ExplorerMode.FILE);
        config.setOnFilePickedListener(file -> {
            //返回:storage/emulated/0/Download/wuge/wuge/wuge.json
            //期望:clan://localhost/Download/wuge/wuge/wuge.json
            LogUtils.d(file.getAbsolutePath());
            String clanPath = file.getAbsolutePath().replace("/storage/emulated/0", "clan://localhost");
            addSubscription(file.getName(), clanPath);
        });
        FilePicker picker = new FilePicker(SubscriptionActivity.this);
        picker.setExplorerConfig(config);
        picker.getOkView().setText("选择");
        picker.show();
    }

    private void addSubscription(String name, String url) {
        if (url.startsWith("clan://")) {
            mSubscriptions.add(new Subscription(name, url));
            mSubscriptionAdapter.notifyDataSetChanged();
        } else if (url.startsWith("http")) {
            showLoadingDialog();
            OkGo.<String>get(url)
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            dismissLoadingDialog();
                            try {
                                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                                // 多线路?
                                JsonElement urls = json.get("urls");
                                // 多仓?
                                JsonElement storeHouse = json.get("storeHouse");
                                if (urls != null && urls.isJsonArray()) {// 多线路
                                    JsonArray urlList = urls.getAsJsonArray();
                                    if (urlList != null && urlList.size() > 0
                                            && urlList.get(0).isJsonObject()
                                            && urlList.get(0).getAsJsonObject().has("url")
                                            && urlList.get(0).getAsJsonObject().has("name")) {//多线路格式
                                        for (int i = 0; i < urlList.size(); i++) {
                                            JsonObject obj = (JsonObject) urlList.get(i);
                                            String name = obj.get("name").getAsString().trim().replaceAll("<|>|《|》|-", "");
                                            String url = obj.get("url").getAsString().trim();
                                            mSubscriptions.add(new Subscription(name, url));
                                        }
                                    }
                                } else if (storeHouse != null && storeHouse.isJsonArray()) {// 多仓
                                    JsonArray storeHouseList = storeHouse.getAsJsonArray();
                                    if (storeHouseList != null && storeHouseList.size() > 0
                                            && storeHouseList.get(0).isJsonObject()
                                            && storeHouseList.get(0).getAsJsonObject().has("sourceName")
                                            && storeHouseList.get(0).getAsJsonObject().has("sourceUrl")) {//多仓格式
                                        mSources.clear();
                                        for (int i = 0; i < storeHouseList.size(); i++) {
                                            JsonObject obj = (JsonObject) storeHouseList.get(i);
                                            String name = obj.get("sourceName").getAsString().trim().replaceAll("<|>|《|》|-", "");
                                            String url = obj.get("sourceUrl").getAsString().trim();
                                            mSources.add(new Source(name, url));
                                        }
                                        new XPopup.Builder(SubscriptionActivity.this)
                                                .asCustom(new ChooseSourceDialog(SubscriptionActivity.this, mSources, (position, url1) -> {
                                                    // 再根据多线路格式获取配置,如果仓内是正常多线路模式,name没用,直接使用线路的命名
                                                    addSubscription(mSources.get(position).getSourceName(), mSources.get(position).getSourceUrl());
                                                }))
                                                .show();
                                    }
                                } else {// 单线路/其余
                                    mSubscriptions.add(new Subscription(name, url));
                                }
                            } catch (Throwable th) {
                                mSubscriptions.add(new Subscription(name, url));
                            }
                            mSubscriptionAdapter.setNewData(mSubscriptions);
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            return response.body().string();
                        }

                        @Override
                        public void onError(Response<String> response) {
                            super.onError(response);
                            dismissLoadingDialog();
                            ToastUtils.showLong("订阅失败,请检查地址或网络状态");
                        }
                    });
        } else {
            ToastUtils.showShort("订阅格式不正确");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // 更新缓存
        Hawk.put(HawkConfig.API_URL, mSelectedUrl);
        Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions);
    }

    @Override
    public void finish() {
        //切换了订阅地址
        if (!TextUtils.isEmpty(mSelectedUrl) && !mBeforeUrl.equals(mSelectedUrl)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        super.finish();
    }
}