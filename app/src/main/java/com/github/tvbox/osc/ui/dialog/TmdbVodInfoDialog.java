package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.TmdbVodInfo;
import com.github.tvbox.osc.databinding.DialogTmdbVodInfoBinding;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.core.CenterPopupView;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

public class TmdbVodInfoDialog extends CenterPopupView {

    private final TmdbVodInfo.ResultsDTO mVod;

    public TmdbVodInfoDialog(@NonNull @NotNull Context context, TmdbVodInfo.ResultsDTO vod) {
        super(context);
        mVod = vod;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_tmdb_vod_info;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        DialogTmdbVodInfoBinding binding = DialogTmdbVodInfoBinding.bind(getPopupImplView());
        binding.tvTitle.setText(mVod.getTitle());
        binding.tvRating.setText("评分: "+mVod.getVote_average());
        binding.tvDes.setText(mVod.getOverview());
    }
}