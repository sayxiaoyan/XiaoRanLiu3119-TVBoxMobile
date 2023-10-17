package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.databinding.ActivityCollectBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.lxj.xpopup.XPopup;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class CollectActivity extends BaseVbActivity<ActivityCollectBinding> {
    private CollectAdapter collectAdapter;


    @Override
    protected void init() {
        initView();
        initData();
    }

    private void initView() {


        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));
        collectAdapter = new CollectAdapter();
        mBinding.mGridView.setAdapter(collectAdapter);

        mBinding.titleBar.getRightView().setOnClickListener(view -> {
            new XPopup.Builder(this)
                    .asConfirm("提示", "确定清空?", () -> {
                        RoomDataManger.deleteVodCollectAll();
                        collectAdapter.setNewData(new ArrayList<>());
                        mBinding.topTip.setVisibility(View.GONE);
                    }).show();
        });

        collectAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodCollect vodInfo = collectAdapter.getData().get(position);
            if (vodInfo!=null){
                collectAdapter.remove(position);
                RoomDataManger.deleteVodCollect(vodInfo.getId());
            }
            if (collectAdapter.getData().isEmpty()){
                mBinding.topTip.setVisibility(View.GONE);
            }
            return true;
        });
        collectAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                VodCollect vodInfo = collectAdapter.getData().get(position);
                if (vodInfo != null) {
                    if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", vodInfo.vodId);
                        bundle.putString("sourceKey", vodInfo.sourceKey);
                        jumpActivity(DetailActivity.class, bundle);
                    } else {
//                            Intent newIntent = new Intent(mContext, SearchActivity.class);
                        Intent newIntent = new Intent(mContext, FastSearchActivity.class);
                        newIntent.putExtra("title", vodInfo.name);
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(newIntent);
                    }
                }
            }
        });
    }

    private void initData() {
        List<VodCollect> allVodRecord = RoomDataManger.getAllVodCollect();
        List<VodCollect> vodInfoList = new ArrayList<>();
        for (VodCollect vodInfo : allVodRecord) {
            vodInfoList.add(vodInfo);
        }
        collectAdapter.setNewData(vodInfoList);
        if (!vodInfoList.isEmpty()){
            mBinding.topTip.setVisibility(View.VISIBLE);
        }
    }
}