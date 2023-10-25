package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.widget.GridSpacingItemDecoration;
import com.github.tvbox.osc.util.Utils;
import com.lxj.xpopup.core.DrawerPopupView;

import org.jetbrains.annotations.NotNull;

/**
 * 点播右侧全集弹窗
 */
public class AllVodSeriesRightDialog extends DrawerPopupView {

    @NonNull
    private final DetailActivity mDetailActivity;

    public AllVodSeriesRightDialog(@NonNull @NotNull Context context) {
        super(context);
        mDetailActivity = (DetailActivity) context;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_all_series_with_group;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        RecyclerView mGridViewFlag = findViewById(R.id.mGridViewFlag);
        mGridViewFlag.setHasFixedSize(true);
        mGridViewFlag.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        if (mDetailActivity.seriesFlagAdapter != null){//复用activity的adapter
            mGridViewFlag.setAdapter(mDetailActivity.seriesFlagAdapter);
        }

        if (mDetailActivity.seriesAdapter!=null){//复用activity的adapter
            RecyclerView rv = findViewById(R.id.rv);
            rv.setLayoutManager(new GridLayoutManager(getContext(),Utils.getSeriesSpanCount(mDetailActivity.seriesAdapter.getData())));
            rv.addItemDecoration(new GridSpacingItemDecoration(Utils.getSeriesSpanCount(mDetailActivity.seriesAdapter.getData()), 20, true));
            mDetailActivity.seriesAdapter.setGird(true);
            mDetailActivity.seriesAdapter.notifyDataSetChanged();
            rv.setAdapter(mDetailActivity.seriesAdapter);
        }

        findViewById(R.id.tvSort).setOnClickListener(view -> mDetailActivity.sortSeries());
   }

    @Override
    protected void onDismiss() {
        super.onDismiss();
        if (mDetailActivity.seriesAdapter!=null){//重置状态,避免竖屏时显示异常
            mDetailActivity.seriesAdapter.setGird(false);
            mDetailActivity.seriesAdapter.notifyDataSetChanged();
        }
    }
}