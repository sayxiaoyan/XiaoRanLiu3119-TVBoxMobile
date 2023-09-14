package com.github.tvbox.osc.util;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.constant.URL;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.google.gson.Gson;
import com.vector.update_app.HttpManager;
import com.vector.update_app.UpdateAppManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Author : xia chuanqi
 * @Email : 751528989@qq.com
 * @Date : on 2020/10/13 16:18.
 * @Description :
 */
public class UpdateAppHttpUtil implements HttpManager {

    private final boolean mProxyAvailable;

    public UpdateAppHttpUtil(boolean proxyAvailable) {
        mProxyAvailable = proxyAvailable;
    }

    @Override
    public void asyncGet(@NonNull String url, @NonNull Map<String, String> params, @NonNull Callback callBack) {
        OkHttpUtils.get()
                .url(mProxyAvailable ? URL.DOMAIN_NAME_PROXY + URL.GITHUB_VERSION_PATH : URL.GITHUB_VERSION_PATH)
                .params(params)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Response response, Exception e, int id) {
                        callBack.onError(validateError(e, response));
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int newVersionCode = jsonObject.optInt("version_code");
                            if (AppUtils.getAppVersionCode() < newVersionCode) {//有新版本
                                String apkFileUrl = jsonObject.optString("apk_file_url");
                                if (mProxyAvailable){//代理地址可用,设置代理并覆盖参数
                                    jsonObject.put("apk_file_url", URL.DOMAIN_NAME_PROXY+ apkFileUrl);
                                }
                                callBack.onResponse(jsonObject.toString());
                            }
                        } catch (JSONException e) {
                            LogUtils.d(e.toString());
                        }
                    }
                });
    }

    @Override
    public void asyncPost(@NonNull String url, @NonNull Map<String, String> params, @NonNull Callback callBack) {

    }

    @Override
    public void download(@NonNull String url, @NonNull String path, @NonNull String fileName, @NonNull FileCallback callback) {
        OkHttpUtils.get()
                .url(url)
                .build()
                .execute(new FileCallBack(path, fileName) {
                    @Override
                    public void inProgress(float progress, long total, int id) {
                        callback.onProgress(progress, total);
                    }

                    @Override
                    public void onError(Call call, Response response, Exception e, int id) {
                        callback.onError(validateError(e, response));
                    }

                    @Override
                    public void onResponse(File response, int id) {
                        callback.onResponse(response);

                    }

                    @Override
                    public void onBefore(Request request, int id) {
                        super.onBefore(request, id);
                        callback.onBefore();
                    }
                });
    }
}
