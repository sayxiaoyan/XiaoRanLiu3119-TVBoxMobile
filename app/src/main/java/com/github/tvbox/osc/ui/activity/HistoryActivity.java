package com.github.tvbox.osc.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.databinding.ActivityHistoryBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.hjq.bar.TitleBar;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
public class HistoryActivity extends BaseVbActivity<ActivityHistoryBinding> {
    private HistoryAdapter historyAdapter;

    @Override
    protected void init() {
        initView();
        initData();
    }

    private void initView() {

        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));
        historyAdapter = new HistoryAdapter();
        mBinding.mGridView.setAdapter(historyAdapter);
        historyAdapter.setOnItemLongClickListener((BaseQuickAdapter.OnItemLongClickListener) (adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodInfo vodInfo = historyAdapter.getData().get(position);
            if (vodInfo != null) {
                historyAdapter.remove(position);
                RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
            } else {
                ToastUtils.showLong("未查询到该条记录,请重试或清空全部记录");
            }
            return true;
        });

        mBinding.titleBar.getRightView().setOnClickListener(view -> {
            new XPopup.Builder(this)
                    .asConfirm("提示", "确定清空?", () -> {
                        for (VodInfo datum : historyAdapter.getData()) {
                            RoomDataManger.deleteVodRecord(datum.sourceKey, datum);
                        }
                        historyAdapter.setNewData(new ArrayList<>());
                    }).show();
        });

        historyAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodInfo vodInfo = historyAdapter.getData().get(position);
            if (vodInfo != null) {
                Bundle bundle = new Bundle();
                bundle.putString("id", vodInfo.id);
                bundle.putString("sourceKey", vodInfo.sourceKey);
                jumpActivity(DetailActivity.class, bundle);
            } else {
                ToastUtils.showShort("记录失效,请重新点播");
            }
        });
    }

    private void initData() {
        List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(100);
        List<VodInfo> vodInfoList = new ArrayList<>();
        for (VodInfo vodInfo : allVodRecord) {
            if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty())
                vodInfo.note = vodInfo.playNote;
            vodInfoList.add(vodInfo);
        }
        historyAdapter.setNewData(vodInfoList);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            initData();
        }
    }
}