package com.github.tvbox.osc.ui.adapter;

import android.widget.ImageView;

import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.bean.VideoFolder;
import com.github.tvbox.osc.bean.VideoInfo;

import java.util.List;

public class SubscriptionAdapter extends BaseQuickAdapter<Subscription, BaseViewHolder> {
    public SubscriptionAdapter() {
        super(R.layout.item_subscription);
    }

    @Override
    protected void convert(BaseViewHolder helper, Subscription item) {
        helper.setText(R.id.tv_name,item.getName())
        .setText(R.id.tv_url,item.getUrl())
        .setChecked(R.id.cb,item.isChecked());
    }
}