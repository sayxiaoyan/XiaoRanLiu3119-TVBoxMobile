package com.github.tvbox.osc.ui.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.databinding.ActivityMainBinding;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.HomeFragment;
import com.github.tvbox.osc.ui.fragment.MyFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseVbActivity<ActivityMainBinding> {

    List<Fragment> fragments = new ArrayList<>();

    public boolean useCacheConfig = false;

    @Override
    protected void init() {
        useCacheConfig = false;
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            useCacheConfig = bundle.getBoolean("useCache", false);
        }

        initVp();
        mBinding.bottomNav.setOnNavigationItemSelectedListener(menuItem -> {
            mBinding.vp.setCurrentItem(menuItem.getOrder(), false);
            return true;
        });
        mBinding.vp.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.bottomNav.getMenu().getItem(position).setChecked(true);
            }
        });
    }

    private void initVp() {
        fragments.add(new HomeFragment());
        fragments.add(new MyFragment());
        mBinding.vp.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }
        });
        mBinding.vp.setOffscreenPageLimit(fragments.size());
    }

    private long exitTime = 0L;

    @Override
    public void onBackPressed() {
        if (mBinding.vp.getCurrentItem() == 1) {
            mBinding.vp.setCurrentItem(0);
            return;
        }
        HomeFragment homeFragment = (HomeFragment) fragments.get(0);
        if (!homeFragment.isAdded()) {// 资源不足销毁重建时未挂载到activity时getChildFragmentManager会崩溃
            confirmExit();
            return;
        }
        List<BaseLazyFragment> childFragments = homeFragment.getAllFragments();
        if (childFragments.isEmpty()) {//加载中(没有tab)
            confirmExit();
            return;
        }
        Fragment fragment = childFragments.get(homeFragment.getTabIndex());
        if (fragment instanceof GridFragment) {// 首页数据源动态加载的tab
            GridFragment item = (GridFragment) fragment;
            if (!item.restoreView()) {// 有回退的view,先回退(AList等文件夹列表),没有可回退的,返到主页tab
                if (!homeFragment.scrollToFirstTab()){
                    confirmExit();
                }
            }
        } else {
            confirmExit();
        }
    }

    private void confirmExit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtils.showShort("再按一次退出程序");
            exitTime = System.currentTimeMillis();
        } else {
            ActivityUtils.finishAllActivities(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

}