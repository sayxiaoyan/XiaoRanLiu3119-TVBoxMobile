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
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.databinding.ActivitySubscriptionBinding;
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter;
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog;
import com.github.tvbox.osc.util.HawkConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lxj.xpopup.XPopup;
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

    /**
     * 单线路格式
     * "http://top啊啊啊阿萨啊/duo"
     *
     * 多线路格式
     * {
     *     "urls": [
     *         {
     *             "url": "http://",
     *             "name": "庭版"
     *         },
     *         {
     *             "url": "http://",
     *             "name": "用"
     *         }
     *     ]
     * }
     */
    @Override
    protected void init() {

        mSubscriptionAdapter = new SubscriptionAdapter();
        mBinding.rv.setAdapter(mSubscriptionAdapter);
        mSubscriptions = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());

        mBeforeUrl = Hawk.get(HawkConfig.API_URL,"");
        mSubscriptions.forEach(item -> {
            if(item.isChecked()){
                mSelectedUrl = item.getUrl();
            }
        });

        mSubscriptionAdapter.setNewData(mSubscriptions);

        mBinding.titleBar.getRightView().setOnClickListener(view -> {//添加订阅
            new XPopup.Builder(this)
                    .autoFocusEditText(false)
                    .asCustom(new SubsciptionDialog(this, "订阅: " + (mSubscriptions.size() + 1), new SubsciptionDialog.OnSubsciptionListener() {
                        @Override
                        public void onConfirm(String name, String url) {
                            for (Subscription item : mSubscriptions) {
                                if (item.getUrl().equals(url)){
                                    ToastUtils.showLong("订阅地址与"+item.getName()+"相同");
                                    return;
                                }
                            }
                            addSubscription(name, url);
                        }

                        @Override
                        public void chooseLocal() {//本地导入
                            ExplorerConfig config = new ExplorerConfig(SubscriptionActivity.this);
                            config.setAllowExtensions(new String[]{".txt",".json",".jar"});
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
                    })).show();
        });


        mSubscriptionAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            LogUtils.d("删除订阅");
            if (view.getId() == R.id.iv_del) {
                new XPopup.Builder(SubscriptionActivity.this)
                        .asConfirm("删除订阅", "确定删除订阅吗？", () -> {
                            mSubscriptions.remove(position);
                            mSubscriptionAdapter.setNewData(mSubscriptions);
                        }).show();
            }
        });


        mSubscriptionAdapter.setOnItemClickListener((adapter, view, position) -> {//选择订阅
            for (int i = 0; i < mSubscriptions.size(); i++) {
                Subscription subscription = mSubscriptions.get(i);
                if (i==position){
                    subscription.setChecked(true);
                    mSelectedUrl = subscription.getUrl();
                }else {
                    subscription.setChecked(false);
                }
            }
            adapter.notifyDataSetChanged();
        });

        mSubscriptionAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                //长按复制url到剪切板
                ClipboardUtils.copyText(mSubscriptions.get(position).getUrl());
                ToastUtils.showLong("已复制订阅地址");
                return true;
            }
        });
    }

    private void addSubscription(String name,String url) {
        if (url.startsWith("clan://")){
            mSubscriptions.add(new Subscription(name,url));
            mSubscriptionAdapter.setNewData(mSubscriptions);
        }else if (url.startsWith("http")){
            showLoadingDialog();
            OkGo.<String>get(url)
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            dismissLoadingDialog();
                            try {
                                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                                JsonArray itemList = json.get("urls").getAsJsonArray();
                                if (itemList!=null && itemList.size()>0
                                        && itemList.get(0).isJsonObject()
                                        && itemList.get(0).getAsJsonObject().has("url")
                                        && itemList.get(0).getAsJsonObject().has("name")){//严格的多仓格式
                                    for (int i = 0; i < itemList.size(); i++) {
                                        JsonObject obj = (JsonObject) itemList.get(i);
                                        String name = obj.get("name").getAsString().trim().replaceAll("<|>|《|》|-", "");
                                        String url = obj.get("url").getAsString().trim();
                                        mSubscriptions.add(new Subscription(name,url));
                                    }
                                }
                            } catch (Throwable th) {//只要是能连接通的路径,json解析异常也当单线路处理
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
        }else {
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
        if (!TextUtils.isEmpty(mSelectedUrl) && !mBeforeUrl.equals(mSelectedUrl))  {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            super.finish();
        }
    }
}