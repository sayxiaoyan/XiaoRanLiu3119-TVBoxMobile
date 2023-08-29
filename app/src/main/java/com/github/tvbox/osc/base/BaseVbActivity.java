package com.github.tvbox.osc.base;

import android.content.Intent;
import android.view.LayoutInflater;

import androidx.viewbinding.ViewBinding;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

/**
 * @Author : Liu XiaoRan
 * @Email : 592923276@qq.com
 * @Date : on 2022/7/7 17:07.
 * @Description :
 */
public abstract class BaseVbActivity<T extends ViewBinding> extends BaseActivity {

    protected T mBinding;

    @Override
    public int getLayoutResID() {
        return -1;
    }

    /**
     * 初始化viewBinding
     */
    @Override
    protected void initVb() {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        Class cls = (Class) type.getActualTypeArguments()[0];
        try {
            Method inflate = cls.getDeclaredMethod("inflate", LayoutInflater.class);
            mBinding = (T) inflate.invoke(null, getLayoutInflater());
            setContentView(mBinding.getRoot());
        } catch (NoSuchMethodException | IllegalAccessException| InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}