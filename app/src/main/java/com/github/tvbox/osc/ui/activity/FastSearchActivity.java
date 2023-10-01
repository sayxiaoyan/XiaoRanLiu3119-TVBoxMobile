package com.github.tvbox.osc.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.angcyo.tablayout.DslTabLayout;
import com.angcyo.tablayout.DslTabLayoutConfig;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.constant.CacheConst;
import com.github.tvbox.osc.databinding.ActivityFastSearchBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.adapter.FastListAdapter;
import com.github.tvbox.osc.ui.adapter.FastSearchAdapter;
import com.github.tvbox.osc.ui.dialog.SearchCheckboxDialog;
import com.github.tvbox.osc.ui.dialog.SearchSuggestionsDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.util.js.JSEngine;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.enums.PopupAnimation;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.lxj.xpopup.interfaces.SimpleCallback;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;
import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import SevenZip.Compression.LZMA.Base;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function4;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class FastSearchActivity extends BaseVbActivity<ActivityFastSearchBinding> implements TextWatcher{

    SourceViewModel sourceViewModel;


    private FastSearchAdapter searchAdapter;
    private FastSearchAdapter searchAdapterFilter;
    private String searchTitle = "";
    private HashMap<String, String> spNames;
    private boolean isFilterMode = false;
    private String searchFilterKey = "";    // 过滤的key
    private HashMap<String, ArrayList<Movie.Video>> resultVods; // 搜索结果
    private List<String> quickSearchWord = new ArrayList<>();
    private static HashMap<String, String> mCheckSources = null;
    private List<Runnable> pauseRunnable = null;
    private SearchSuggestionsDialog mSearchSuggestionsDialog;
    private SearchCheckboxDialog mSearchCheckboxDialog;

    @Override
    protected void init() {
        spNames = new HashMap<String, String>();
        resultVods = new HashMap<String, ArrayList<Movie.Video>>();
        initView();
        initViewModel();
        initData();
        //历史搜索
        initHistorySearch();
        // 热门搜索
        getHotWords();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (pauseRunnable != null && pauseRunnable.size() > 0) {
            searchExecutorService = Executors.newFixedThreadPool(10);
            allRunCount.set(pauseRunnable.size());
            for (Runnable runnable : pauseRunnable) {
                searchExecutorService.execute(runnable);
            }
            pauseRunnable.clear();
            pauseRunnable = null;
        }
    }

    private void initView() {

        mBinding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(mBinding.etSearch.getText().toString());
                return true;
            }
            return false;
        });

        mBinding.etSearch.addTextChangedListener(this);

        findViewById(R.id.iv_filter).setOnClickListener(view -> {
            filterSearchSource();
        });
        findViewById(R.id.iv_back).setOnClickListener(view -> {
            finish();
        });
        findViewById(R.id.iv_search).setOnClickListener(view -> {
            search(mBinding.etSearch.getText().toString());
        });

        mBinding.tabLayout.configTabLayoutConfig(dslTabLayoutConfig -> {
            dslTabLayoutConfig.setOnSelectViewChange((view, views, aBoolean, aBoolean2) -> {
                TextView tvItem = (TextView) views.get(0);
                filterResult(tvItem.getText().toString());
                return null;
            });
            return null;
        });

        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new LinearLayoutManager(this.mContext));

        searchAdapter = new FastSearchAdapter();
        mBinding.mGridView.setAdapter(searchAdapter);

        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapter.getData().get(position);
                if (video != null) {
                    try {
                        if (searchExecutorService != null) {
                            pauseRunnable = searchExecutorService.shutdownNow();
                            searchExecutorService = null;
                            JSEngine.getInstance().stopAll();
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });


        mBinding.mGridViewFilter.setLayoutManager(new LinearLayoutManager(mContext));
        searchAdapterFilter = new FastSearchAdapter();
        mBinding.mGridViewFilter.setAdapter(searchAdapterFilter);
        searchAdapterFilter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapterFilter.getData().get(position);
                if (video != null) {
                    try {
                        if (searchExecutorService != null) {
                            pauseRunnable = searchExecutorService.shutdownNow();
                            searchExecutorService = null;
                            JSEngine.getInstance().stopAll();
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });

        setLoadSir(mBinding.llLayout);
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
    }

    /**
     * 指定搜索源(过滤)
     */
    private void filterSearchSource(){
        if (mSearchCheckboxDialog == null) {
            List<SourceBean> allSourceBean = ApiConfig.get().getSourceBeanList();
            List<SourceBean> searchAbleSource = new ArrayList<>();
            for(SourceBean sourceBean : allSourceBean) {
                if (sourceBean.isSearchable()) {
                    searchAbleSource.add(sourceBean);
                }
            }
            mSearchCheckboxDialog = new SearchCheckboxDialog(FastSearchActivity.this, searchAbleSource, mCheckSources);
        }
        mSearchCheckboxDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        mSearchCheckboxDialog.show();
    }

    public static void setCheckedSourcesForSearch(HashMap<String,String> checkedSources) {
        mCheckSources = checkedSources;
    }

    private void filterResult(String spName) {
        if (spName == "全部显示") {
            mBinding.mGridView.setVisibility(View.VISIBLE);
            mBinding.mGridViewFilter.setVisibility(View.GONE);
            return;
        }
        mBinding.mGridView.setVisibility(View.GONE);
        mBinding.mGridViewFilter.setVisibility(View.VISIBLE);
        String key = spNames.get(spName);
        if (key.isEmpty()) return;

        if (searchFilterKey == key) return;
        searchFilterKey = key;

        List<Movie.Video> list = resultVods.get(key);
        searchAdapterFilter.setNewData(list);
    }

    private void fenci() {
        if (!quickSearchWord.isEmpty()) return; // 如果经有分词了，不再进行二次分词
        // 分词
        OkGo.<String>get("http://api.pullword.com/get.php?source=" + URLEncoder.encode(searchTitle) + "&param1=0&param2=0&json=1")
                .tag("fenci")
                .execute(new AbsCallback<String>() {
                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        if (response.body() != null) {
                            return response.body().string();
                        } else {
                            throw new IllegalStateException("网络请求错误");
                        }
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        String json = response.body();
                        quickSearchWord.clear();
                        try {
                            for (JsonElement je : new Gson().fromJson(json, JsonArray.class)) {
                                quickSearchWord.add(je.getAsJsonObject().get("t").getAsString());
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle));
                        List<String> words = new ArrayList<>(new HashSet<>(quickSearchWord));
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words));
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });
    }

    private void initData() {
        initCheckedSourcesForSearch();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            if (!TextUtils.isEmpty(title)){
                showLoading();
                search(title);
            }
        }
    }


    private void hideHotAndHistorySearch(boolean isHide){
        if(isHide){
            mBinding.llSearchSuggest.setVisibility(View.GONE);
            mBinding.llSearchResult.setVisibility(View.VISIBLE);
        }else{
            mBinding.llSearchSuggest.setVisibility(View.VISIBLE);
            mBinding.llSearchResult.setVisibility(View.GONE);
        }
    }

    private void initHistorySearch(){

        List<String> mSearchHistory = Hawk.get(CacheConst.HISTORY_SEARCH, new ArrayList<>());

        mBinding.llHistory.setVisibility(mSearchHistory.size() > 0 ? View.VISIBLE : View.GONE);
        mBinding.flHistory.setAdapter(new TagAdapter<String>(mSearchHistory)
        {
            @Override
            public View getView(FlowLayout parent, int position, String s)
            {
                TextView tv = (TextView) LayoutInflater.from(FastSearchActivity.this).inflate(R.layout.item_search_word_hot,
                        mBinding.flHistory, false);
                tv.setText(s);
                return tv;
            }
        });

        mBinding.flHistory.setOnTagClickListener((view, position, parent) -> {
            search(mSearchHistory.get(position));
            return true;
        });

        findViewById(R.id.iv_clear_history).setOnClickListener(view -> {
            Hawk.put(CacheConst.HISTORY_SEARCH, new ArrayList<>());
            //FlowLayout及其adapter貌似没有清空数据的api,简单粗暴重置
            view.postDelayed(this::initHistorySearch,300);
        });
    }

    /**
     * 热门搜索
     */
    private void getHotWords(){
        // 加载热词
        OkGo.<String>get("https://node.video.qq.com/x/api/hot_search")
//        OkGo.<String>get("https://api.web.360kan.com/v1/rank")
//                .params("cat", "1")
                .params("channdlId", "0")
                .params("_", System.currentTimeMillis())
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            ArrayList<String> hots = new ArrayList<>();
                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonObject().get("mapResult").getAsJsonObject().get("0").getAsJsonObject().get("listInfo").getAsJsonArray();
//                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonArray();
                            for (JsonElement ele : itemList) {
                                JsonObject obj = (JsonObject) ele;
                                hots.add(obj.get("title").getAsString().trim().replaceAll("<|>|《|》|-", "").split(" ")[0]);
                            }
                            mBinding.flHot.setAdapter(new TagAdapter<String>(hots)
                            {
                                @Override
                                public View getView(FlowLayout parent, int position, String s)
                                {
                                    TextView tv = (TextView) LayoutInflater.from(FastSearchActivity.this).inflate(R.layout.item_search_word_hot,
                                            mBinding.flHot, false);
                                    tv.setText(s);
                                    return tv;
                                }
                            });

                            mBinding.flHot.setOnTagClickListener((view, position, parent) -> {
                                search(hots.get(position));
                                return true;
                            });
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });

    }


    /**
     * 联想搜索
     */
    private void getSuggest(String text){
        // 加载热词
        OkGo.<String>get("https://suggest.video.iqiyi.com/?if=mobile&key=" + text)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        List<String> titles = new ArrayList<>();
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            JsonArray datas = json.get("data").getAsJsonArray();
                            for (JsonElement data : datas) {
                                JsonObject item = (JsonObject)data;
                                titles.add(item.get("name").getAsString().trim());
                            }
                        } catch (Throwable th) {
                            LogUtils.d(th.toString());
                        }
                        if (!titles.isEmpty()){
                            showSuggestDialog(titles);
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });

    }

    private void showSuggestDialog(List<String> list){
        if (mSearchSuggestionsDialog==null){
            mSearchSuggestionsDialog = new SearchSuggestionsDialog(FastSearchActivity.this, list, new OnSelectListener() {
                @Override
                public void onSelect(int position, String text) {
                    LogUtils.d("搜索:"+text);
                    mSearchSuggestionsDialog.dismissWith(() -> search(text));
                }
            });

            new XPopup.Builder(FastSearchActivity.this)
                    .atView(mBinding.etSearch)
                    .notDismissWhenTouchInView(mBinding.etSearch)
                    .isViewMode(true)      //开启View实现
                    .isRequestFocus(false) //不强制焦点
                    .setPopupCallback(new SimpleCallback() {
                        @Override
                        public void onDismiss(BasePopupView popupView) {// 弹窗关闭了就置空对象,下次重新new
                            super.onDismiss(popupView);
                            mSearchSuggestionsDialog = null;
                        }
                    })
                    .asCustom(mSearchSuggestionsDialog)
                    .show();
        }else {// 不为空说明弹窗为打开状态(关闭就置空了).直接刷新数据
            mSearchSuggestionsDialog.updateSuggestions(list);
        }
    }

    private void saveSearchHistory(String searchWord){
        if (!searchWord.isEmpty()) {
            ArrayList<String> history = Hawk.get(CacheConst.HISTORY_SEARCH, new ArrayList<>());
            if (!history.contains(searchWord)){
                history.add(0, searchWord);
            }else {
                history.remove(searchWord);
                history.add(0, searchWord);
            }
            if (history.size() > 30){
                history.remove(30);
            }
            Hawk.put(CacheConst.HISTORY_SEARCH, history);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            search(title);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    private void search(String title) {
        if (TextUtils.isEmpty(title)) {
            ToastUtils.showShort("请输入搜索内容");
            return;
        }

        //先移除监听,避免重新设置要搜索的文字触发搜索建议并弹窗
        mBinding.etSearch.removeTextChangedListener(this);
        mBinding.etSearch.setText(title);
        mBinding.etSearch.setSelection(title.length());
        mBinding.etSearch.addTextChangedListener(this);

        if (mSearchSuggestionsDialog!=null && mSearchSuggestionsDialog.isShow()){
            mSearchSuggestionsDialog.dismiss();
        }

        if (!Hawk.get(HawkConfig.PRIVATE_BROWSING, false)) {//无痕浏览不存搜索历史
            saveSearchHistory(title);
        }
        hideHotAndHistorySearch(true);
        KeyboardUtils.hideSoftInput(this);
        cancel();
        showLoading();
        this.searchTitle = title;
        fenci();
        mBinding.mGridView.setVisibility(View.INVISIBLE);
        mBinding.mGridViewFilter.setVisibility(View.GONE);
        searchAdapter.setNewData(new ArrayList<>());
        searchAdapterFilter.setNewData(new ArrayList<>());

        resultVods.clear();
        searchFilterKey = "";
        isFilterMode = false;
        spNames.clear();
        mBinding.tabLayout.removeAllViews();

        searchResult();
    }

    private ExecutorService searchExecutorService = null;
    private AtomicInteger allRunCount = new AtomicInteger(0);

    private TextView getSiteTextView(String text){
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        DslTabLayout.LayoutParams params = new DslTabLayout.LayoutParams(-2, -2);
        params.topMargin = 20;
        params.bottomMargin = 20;
        textView.setPadding(20, 10, 20, 10);
        textView.setLayoutParams(params);
        return textView;
    }
    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JSEngine.getInstance().stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            searchAdapter.setNewData(new ArrayList<>());
            searchAdapterFilter.setNewData(new ArrayList<>());
            allRunCount.set(0);
        }
        searchExecutorService = Executors.newFixedThreadPool(10);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);


        ArrayList<String> siteKey = new ArrayList<>();

        mBinding.tabLayout.addView(getSiteTextView("全部显示"));
        mBinding.tabLayout.setCurrentItem(0, true,false);
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            this.spNames.put(bean.getName(), bean.getKey());
            allRunCount.incrementAndGet();
        }

        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        sourceViewModel.getSearch(key, searchTitle);
                    } catch (Exception e) {

                    }
                }
            });
        }
    }

    /**
     * 添加到最后面并返回最后一个key
     * @param key
     * @return
     */
    private String addWordAdapterIfNeed(String key) {
        try {
            String name = "";
            for (String n : spNames.keySet()) {
                if (Objects.equals(spNames.get(n), key)) {
                    name = n;
                }
            }
            if (Objects.equals(name, "")) return key;

            for (int i = 0; i < mBinding.tabLayout.getChildCount(); ++i) {
                TextView item = (TextView)mBinding.tabLayout.getChildAt(i);
                if (Objects.equals(name, item.getText().toString())) {
                    return key;
                }
            }

            mBinding.tabLayout.addView(getSiteTextView(name));
            return key;
        } catch (Exception e) {
            return key;
        }
    }

    private boolean matchSearchResult(String name, String searchTitle) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false;
        searchTitle = searchTitle.trim();
        String[] arr = searchTitle.split("\\s+");
        int matchNum = 0;
        for(String one : arr) {
            if (name.contains(one)) matchNum++;
        }
        return matchNum == arr.length ? true : false;
    }

    private void searchData(AbsXml absXml) {
        String lastSourceKey = "";

        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                if (!matchSearchResult(video.name, searchTitle)) continue;
                data.add(video);
                if (!resultVods.containsKey(video.sourceKey)) {
                    resultVods.put(video.sourceKey, new ArrayList<Movie.Video>());
                }
                resultVods.get(video.sourceKey).add(video);
                if (video.sourceKey != lastSourceKey) {// 添加到最后面并记录最后一个key用于下次判断
                    lastSourceKey = this.addWordAdapterIfNeed(video.sourceKey);
                }
            }

            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                showSuccess();
                if (!isFilterMode)
                    mBinding.mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
            }
        }

        int count = allRunCount.decrementAndGet();
        if (count <= 0) {
            if (searchAdapter.getData().size() <= 0) {
                showEmpty();
            }
            cancel();
        }
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("search");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JSEngine.getInstance().stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        String text = editable.toString();
        if (TextUtils.isEmpty(text) && mSearchSuggestionsDialog!=null){
            mSearchSuggestionsDialog.dismiss();
        }else {
            getSuggest(text);
        }
    }
}