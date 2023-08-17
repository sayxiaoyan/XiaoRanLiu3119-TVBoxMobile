package com.github.tvbox.osc.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.ui.fragment.HomeFragment;
import com.github.tvbox.osc.ui.fragment.MyFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity {

    private BottomNavigationView mBottomNav;
    private ViewPager mVp;
    List<Fragment> fragments = new ArrayList<>();
    @Override
    protected int getLayoutResID() {
        return R.layout.activity_main;
    }

    public boolean useCacheConfig = false;

    @Override
    protected void init() {
        useCacheConfig = false;
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            useCacheConfig = bundle.getBoolean("useCache", false);
        }

        mBottomNav = findViewById(R.id.bottom_nav);
        mVp = findViewById(R.id.vp);

        initVp();
        mBottomNav.setOnNavigationItemSelectedListener(menuItem -> {
            mVp.setCurrentItem(menuItem.getOrder(), false);
            return true;
        });
        mVp.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBottomNav.getMenu().getItem(position).setChecked(true);
            }
        });
    }

    private void initVp() {
        fragments.add(new HomeFragment());
        fragments.add(new MyFragment());
        mVp.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
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
        mVp.setOffscreenPageLimit(fragments.size());
    }

    private long exitTime = 0L;
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtils.showShort("再按一次退出程序");
            exitTime = System.currentTimeMillis();
        } else {
            ActivityUtils.finishAllActivities(true);
        }
    }

}