package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.widget.GridSpacingItemDecoration;
import com.github.tvbox.osc.util.Utils;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.enums.PopupPosition;
import com.lxj.xpopup.interfaces.OnSelectListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AllSeriesDialog extends BottomPopupView {

    List<VodInfo.VodSeries> mList;
    private final OnSelectListener mSelectListener;

    public AllSeriesDialog(@NonNull @NotNull Context context, List<VodInfo.VodSeries> list,OnSelectListener selectListener) {
        super(context);
        mList = list;
        mSelectListener = selectListener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_all_series;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        RecyclerView rv = findViewById(R.id.rv);

        rv.setLayoutManager(new GridLayoutManager(getContext(), Utils.getSeriesSpanCount(mList)));
        rv.addItemDecoration(new GridSpacingItemDecoration(Utils.getSeriesSpanCount(mList), 20, true));

        SeriesAdapter seriesAdapter = new SeriesAdapter(true);
        seriesAdapter.setNewData(mList);
        rv.setAdapter(seriesAdapter);

        seriesAdapter.setOnItemClickListener((adapter, view, position) -> {
            for (int j = 0; j < seriesAdapter.getData().size(); j++) {
                seriesAdapter.getData().get(j).selected = false;
                seriesAdapter.notifyItemChanged(j);
            }
            seriesAdapter.getData().get(position).selected = true;
            seriesAdapter.notifyItemChanged(position);
            mSelectListener.onSelect(position,"");
        });

        seriesAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodInfo.VodSeries vodSeries = seriesAdapter.getData().get(position);
            new XPopup.Builder(getContext())
                    .hasShadowBg(false)
                    .atView(view)
                    .popupPosition(PopupPosition.Top)
                    .asAttachList(new String[]{vodSeries.name},null,null)
                    .show()
                    .delayDismiss(3000);
            return true;
        });
    }
}