package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;

import android.os.Handler;
import android.view.Gravity;
import android.widget.TextView;

import com.angcyo.tablayout.delegate.ViewPager1Delegate;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.BaseVbFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.databinding.FragmentHomeBinding;
import com.github.tvbox.osc.server.ControlManager;

import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseVbFragment<FragmentHomeBinding> {
    private SourceViewModel sourceViewModel;
    private List<BaseLazyFragment> fragments = new ArrayList<>();
    private Handler mHandler = new Handler();

    /**
     * 顶部tabs分类集合,用于渲染tab页,每个tab对应fragment内的数据
     */
    private List<MovieSort.SortData> mSortDataList = new ArrayList<>();
    private boolean dataInitOk = false;
    private boolean jarInitOk = false;


    @Override
    protected void init() {
        ControlManager.get().startServer();

        mBinding.nameContainer.setOnClickListener(v -> {
            if(dataInitOk && jarInitOk){
                showSiteSwitch();
            }else {
                ToastUtils.showShort("数据源未加载，长按刷新或切换订阅");
            }
        });

        mBinding.nameContainer.setOnLongClickListener(v -> {
            refreshHomeSouces();
            return true;
        });

        mBinding.ivSearch.setOnClickListener(view -> jumpActivity(FastSearchActivity.class));
        mBinding.ivHistory.setOnClickListener(view -> jumpActivity(HistoryActivity.class));
        setLoadSir(mBinding.contentLayout);

        initViewModel();

        initData();
    }


    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.sortResult.observe(this, absXml -> {
            showSuccess();
            if (absXml != null && absXml.classes != null && absXml.classes.sortList != null) {
                mSortDataList = DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), absXml.classes.sortList, true);
            } else {
                mSortDataList = DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), new ArrayList<>(), true);
            }
            initViewPager(absXml);
        });
    }

    private void initData() {

        MainActivity mainActivity = (MainActivity)mActivity;

        SourceBean home = ApiConfig.get().getHomeSourceBean();
        if (home != null && home.getName() != null && !home.getName().isEmpty()){
            mBinding.tvName.setText(home.getName());
            mBinding.tvName.postDelayed(() -> mBinding.tvName.setSelected(true),2000);
        }
        if (dataInitOk && jarInitOk) {
            showLoading();
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            return;
        }
        showLoading();
        if (dataInitOk && !jarInitOk) {
            if (!ApiConfig.get().getSpider().isEmpty()) {
                ApiConfig.get().loadJar(mainActivity.useCacheConfig, ApiConfig.get().getSpider(), new ApiConfig.LoadConfigCallback() {
                    @Override
                    public void success() {
                        jarInitOk = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!mainActivity.useCacheConfig)
                                    ToastUtils.showShort("更新订阅成功");
                                initData();
                            }
                        }, 50);
                    }

                    @Override
                    public void retry() {

                    }

                    @Override
                    public void error(String msg) {
                        jarInitOk = true;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtils.showShort("更新订阅失败");
                                initData();
                            }
                        });
                    }
                });
            }
            return;
        }
        ApiConfig.get().loadConfig(mainActivity.useCacheConfig, new ApiConfig.LoadConfigCallback() {
            TipDialog dialog = null;

            @Override
            public void retry() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                });
            }

            @Override
            public void success() {
                dataInitOk = true;
                if (ApiConfig.get().getSpider().isEmpty()) {
                    jarInitOk = true;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                }, 50);
            }

            @Override
            public void error(String msg) {
                if (msg.equalsIgnoreCase("-1")) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dataInitOk = true;
                            jarInitOk = true;
                            initData();
                        }
                    });
                    return;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog == null)
                            dialog = new TipDialog(getActivity(), msg, "重试", "取消", new TipDialog.OnListener() {
                                @Override
                                public void left() {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void right() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void cancel() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }
                            });
                        if (!dialog.isShowing())
                            dialog.show();
                    }
                });
            }
        }, getActivity());
    }

    private TextView getTabTextView(String text){
        TextView textView = new TextView(mContext);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(ConvertUtils.dp2px(20), ConvertUtils.dp2px(10), ConvertUtils.dp2px(5), ConvertUtils.dp2px(10));
        return textView;
    }

    private void initViewPager(AbsSortXml absXml) {
        if (mSortDataList.size() > 0) {
            mBinding.tabLayout.removeAllViews();
            fragments.clear();
            for (MovieSort.SortData data : mSortDataList) {
                mBinding.tabLayout.addView(getTabTextView(data.name));

                if (data.id.equals("my0")) {//tab是主页,添加主页fragment 根据设置项显示豆瓣热门/站点推荐(每个源不一样)/历史记录
                    if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && absXml.videoList.size() > 0) {//站点推荐
                        fragments.add(UserFragment.newInstance(absXml.videoList));
                    } else {//豆瓣热门/历史记录
                        fragments.add(UserFragment.newInstance(null));
                    }
                } else {//来自源的分类
                    fragments.add(GridFragment.newInstance(data));
                }
            }

            //重新渲染vp
            mBinding.mViewPager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {
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
            //tab和vp绑定
            ViewPager1Delegate.Companion.install(mBinding.mViewPager, mBinding.tabLayout,true);
        }
    }

    /**
     * 提供给主页返回操作
     */
    public void scrollToFirstTab(){
        if (mBinding.tabLayout.getCurrentItemIndex()!=0){
            mBinding.mViewPager.setCurrentItem(0, false);
        }
    }

    /**
     * 提供给主页返回操作
     */
    public int getTabIndex(){
        return mBinding.tabLayout.getCurrentItemIndex();
    }

    /**
     * 提供给主页返回操作
     */
    public List<BaseLazyFragment> getAllFragments(){
        return fragments;
    }


    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    void showSiteSwitch() {
        List<SourceBean> sites = ApiConfig.get().getSourceBeanList();
        if (sites.size() > 0) {
            SelectDialog<SourceBean> dialog = new SelectDialog<>(getActivity());
            TvRecyclerView tvRecyclerView = dialog.findViewById(R.id.list);

            tvRecyclerView.setLayoutManager(new V7GridLayoutManager(dialog.getContext(), 2));

            dialog.setTip("请选择首页数据源");
            dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<SourceBean>() {
                @Override
                public void click(SourceBean value, int pos) {
                    ApiConfig.get().setSourceBean(value);
                    refreshHomeSouces();
                }

                @Override
                public String getDisplay(SourceBean val) {
                    return val.getName();
                }
            }, new DiffUtil.ItemCallback<SourceBean>() {
                @Override
                public boolean areItemsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                    return oldItem == newItem;
                }

                @Override
                public boolean areContentsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                    return oldItem.getKey().equals(newItem.getKey());
                }
            }, sites, sites.indexOf(ApiConfig.get().getHomeSourceBean()));
            dialog.show();
        }else {
            ToastUtils.showLong("暂无可用数据源");
        }
    }

    private void refreshHomeSouces(){
        Intent intent = new Intent(App.getInstance(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Bundle bundle = new Bundle();
        bundle.putBoolean("useCache", true);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ControlManager.get().stopServer();
    }
}