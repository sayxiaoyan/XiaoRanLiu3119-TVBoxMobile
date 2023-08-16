package com.github.tvbox.osc.ui.activity;

import android.view.View;

import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.databinding.ActivitySubscriptionBinding;
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter;
import com.github.tvbox.osc.util.HawkConfig;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionActivity extends BaseVbActivity<ActivitySubscriptionBinding> {


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
        List<Subscription> subscriptions = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());
        subscriptionAdapter.setNewData(subscriptions);

        mBinding.titleBar.getRightView().setOnClickListener(view -> {
            new XPopup.Builder(this)
                    .asInputConfirm("添加订阅", null, "请输入地址", new OnInputConfirmListener() {
                        @Override
                        public void onConfirm(String text) {
                            if (text.isEmpty()) {
                                ToastUtils.showShort("订阅地址为空");
                                return;
                            }
                            Subscription subscription = new Subscription();
                            subscription.setUrl(text);
                            subscription.setName("订阅: "+(subscriptions.size()+1));
                            subscriptions.add(subscription);
                            Hawk.put(HawkConfig.SUBSCRIPTIONS, subscriptions);
                            subscriptionAdapter.setNewData(subscriptions);
                        }
                    }).show();
        });

        subscriptionAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            new XPopup.Builder(this)
                    .asConfirm("删除订阅", "确定删除订阅吗？", () -> {
                        subscriptions.remove(position);
                        Hawk.put(HawkConfig.SUBSCRIPTIONS, subscriptions);
                        subscriptionAdapter.setNewData(subscriptions);
                    }).show();
        });

        subscriptionAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                for (int i = 0; i < subscriptions.size(); i++) {
                    Subscription subscription = subscriptions.get(i);
                    subscription.setChecked(i==position);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}