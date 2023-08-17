package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.databinding.ActivitySubscriptionBinding;
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter;
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog;
import com.github.tvbox.osc.util.HawkConfig;
import com.lxj.xpopup.XPopup;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionActivity extends BaseVbActivity<ActivitySubscriptionBinding> {


    private String mBeforeUrl;
    private String mSelectedUrl;
    private List<Subscription> mSubscriptions;

    /**
     * 单线路格式
     * "http://yydsys.top/duo"
     *
     * 多线路格式
     * {
     *     "urls": [
     *         {
     *             "url": "http://yydsys.top/duo",
     *             "name": "🏡应用多多家庭版"
     *         },
     *         {
     *             "url": "http://cdn.yydsys.top/duo",
     *             "name": "🏡应用多多备用"
     *         }
     *     ]
     * }
     */
    @Override
    protected void init() {

        SubscriptionAdapter subscriptionAdapter = new SubscriptionAdapter();
        mBinding.rv.setAdapter(subscriptionAdapter);
        mSubscriptions = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());

        mBeforeUrl = Hawk.get(HawkConfig.API_URL,"");
        mSubscriptions.forEach(item -> {
            if(item.isChecked()){
                mSelectedUrl = item.getUrl();
            }
        });

        subscriptionAdapter.setNewData(mSubscriptions);

        mBinding.titleBar.getRightView().setOnClickListener(view -> {//添加订阅
            new XPopup.Builder(this)
                    .asCustom(new SubsciptionDialog(this,"订阅: "+(mSubscriptions.size()+1), (name, url) -> {
                        for (Subscription item : mSubscriptions) {
                            if (item.getUrl().equals(url)){
                                ToastUtils.showLong("订阅地址与"+item.getName()+"相同");
                                return;
                            }
                        }
                        mSubscriptions.add(0,new Subscription(name,url));
                        subscriptionAdapter.setNewData(mSubscriptions);
                    })).show();

        });


        subscriptionAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                LogUtils.d("删除订阅");
                if (view.getId() == R.id.iv_del) {
                    new XPopup.Builder(SubscriptionActivity.this)
                            .asConfirm("删除订阅", "确定删除订阅吗？", () -> {
                                mSubscriptions.remove(position);
                                subscriptionAdapter.setNewData(mSubscriptions);
                            }).show();
                }
            }
        });


        subscriptionAdapter.setOnItemClickListener((adapter, view, position) -> {//选择订阅
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Hawk.put(HawkConfig.API_URL, mSelectedUrl);
        // 更新操作后的订阅列表
        Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions);
    }

    @Override
    public void onBackPressed() {
        //切换了订阅地址
        if (!TextUtils.isEmpty(mSelectedUrl) && !mBeforeUrl.equals(mSelectedUrl))  {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            super.onBackPressed();
        }
    }
}