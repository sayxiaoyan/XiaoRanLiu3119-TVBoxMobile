package com.github.tvbox.osc.util;

import android.text.TextUtils;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.constant.URL;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.vector.update_app.UpdateAppManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class Github {

    public interface OnProxyAvailableListener {
        void available(boolean isAvailable);

    }

    private OkHttpClient mClient;

    private Github() {

    }

    private static volatile Github instance = null;

    public static Github getInstance() {
        if (instance == null) {
            // 加锁
            synchronized (Github.class) {
                // 这一次判断也是必须的，不然会有并发问题
                if (instance == null) {
                    instance = new Github();
                }
            }
        }
        return instance;
    }

    public void checkProxy(OnProxyAvailableListener onProxyAvailableListener) {

        if (mClient == null) {
            mClient = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .build();
        }

        OkGo.<String>get(URL.DOMAIN_NAME_PROXY)
                .client(mClient)
                .execute(new StringCallback() {

                    boolean isAvailable;

                    @Override
                    public void onSuccess(Response<String> response) {
                        isAvailable = true;
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        isAvailable = false;
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        if (onProxyAvailableListener != null) {
                            onProxyAvailableListener.available(isAvailable);
                        }
                    }
                });
    }

}
