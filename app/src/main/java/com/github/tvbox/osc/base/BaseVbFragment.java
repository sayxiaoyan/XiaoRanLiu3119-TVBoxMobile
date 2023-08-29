package com.github.tvbox.osc.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.event.RefreshEvent;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import me.jessyan.autosize.AutoSize;
import me.jessyan.autosize.internal.CustomAdapt;

/**
 * Fragment的基类(vb)
 */
public abstract class BaseVbFragment<T extends ViewBinding> extends Fragment implements CustomAdapt {


    protected Context mContext;
    protected Activity mActivity;

    protected T mBinding;
    private LoadService mLoadService;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AutoSize.autoConvertDensity(getActivity(), getSizeInDp(), isBaseOnWidth());
        return initBindingViewRoot(container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = (Activity) context;
    }

    /**
     * 初始化viewBinding返回根布局
     */
    private View initBindingViewRoot(ViewGroup container) {
        ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
        Class<?> aClass = (Class<?>) type.getActualTypeArguments()[0];
        Method method;
        try {
            method = aClass.getDeclaredMethod("inflate", LayoutInflater.class, ViewGroup.class, boolean.class);
            mBinding = (T) method.invoke(null, getLayoutInflater(), container, false);
            return mBinding.getRoot();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * 在滑动或者跳转的过程中，第一次创建fragment的时候均会调用onResume方法
     */
    @Override
    public void onResume() {
        AutoSize.autoConvertDensity(getActivity(), getSizeInDp(), isBaseOnWidth());
        super.onResume();
    }

    protected abstract void init();

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void setLoadSir2(View view) {
        mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
            @Override
            public void onReload(View v) {
            }
        });
    }

    protected void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    protected void showEmpty() {
        if (null != mLoadService) {
            mLoadService.showCallback(EmptyCallback.class);
        }
    }

    protected void showSuccess() {
        if (null != mLoadService) {
            mLoadService.showSuccess();
        }
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz) {
        Intent intent = new Intent(mContext, clazz);
        startActivity(intent);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz, Bundle bundle) {
        Intent intent = new Intent(mContext, clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public float getSizeInDp() {
        if (getActivity() != null && getActivity() instanceof CustomAdapt)
            return ((CustomAdapt) getActivity()).getSizeInDp();
        return 0;
    }

    @Override
    public boolean isBaseOnWidth() {
        if (getActivity() != null && getActivity() instanceof CustomAdapt)
            return ((CustomAdapt) getActivity()).isBaseOnWidth();
        return true;
    }
}
