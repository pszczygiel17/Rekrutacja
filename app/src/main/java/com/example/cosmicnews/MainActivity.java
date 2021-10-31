package com.example.cosmicnews;

import android.annotation.SuppressLint;

import android.content.Intent;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;

import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cosmicnews.api.ApiClient;
import com.example.cosmicnews.api.ApiInterface;
import com.example.cosmicnews.models.Articles;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private RecyclerView recyclerView;
    private List<Articles> articles = new ArrayList<>(); //top articles
    private Adapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RelativeLayout errorLayout;

    private NestedScrollView nestedSV;
    int count = 0; //counting pages
    int pages = 10; //max number of pages to display
    int numLoadingArticles = 5;
    LinearLayout linearLayout;
    ProgressBar progressBar;

    private ImageView errorImage;
    private TextView errorTitle, errorMessage;
    private Button errorButton;
    private boolean fav = false; //fav view or home view

    BottomNavigationView bottomNavigationView;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        recyclerView = findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);

        onLoadingSwipeRefresh();

        nestedSV = findViewById(R.id.nestedSV);
        linearLayout = findViewById(R.id.linear);
        progressBar = findViewById(R.id.progress_bar);

        errorLayout = findViewById(R.id.errorLayout);
        errorImage = findViewById(R.id.errorImage);
        errorTitle = findViewById(R.id.errorTitle);
        errorMessage = findViewById(R.id.errorMessage);
        errorButton = findViewById(R.id.errorButton);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        //handling bottom navigation items
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId())
                {
                    case R.id.home:
                        fav = false; //top articles
                        nestedSV.smoothScrollTo(0, nestedSV.getTop()); //focus on the top
                        progressBar.setVisibility(View.VISIBLE);
                        count = 0;
                        menuItem.setChecked(true); //changing item appearance with a selector
                        LoadJson();
                        break;
                    case R.id.favourites:
                        fav = true; //only favourites
                        nestedSV.smoothScrollTo(0, nestedSV.getTop()); //focus on the top
                        progressBar.setVisibility(View.GONE);
                        menuItem.setChecked(true); //changing item appearance with a selector
                        LoadJson();
                        break;
                }
                return false;
            }
        });


        //handling scrolling
        nestedSV.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(!fav) {
                    if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                        count++; //counting pages
                        if (count < pages) {
                            List<Integer> l = getNextIds(articles.get(articles.size()-1).getId()); //list of next articles
                            getMoreArticles(l);
                        }
                        else{
                            //hiding the last progress bar
                            linearLayout.getChildAt(linearLayout.getChildCount()-1).setVisibility(View.GONE);
                        }
                    }
                }
            }
        });
    }

    //loading list of favourites articles or top articles
    public void LoadJson(){

        errorLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);
        db = new DatabaseHelper(this);

        if (fav){
            getMoreArticles(db.getFav()); //loading list of favourites using list of ids from database
            hideUnnecessaryViews();
        }
        else {
            getAllArticles();
        }
    }

    //sending url and title to NewsDetailActivity from the selected article
    private void initListener(){
        adapter.setOnItemClickListener(new Adapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {

                Intent intent = new Intent(MainActivity.this, NewsDetailActivity.class);

                Articles article = articles.get(position);
                intent.putExtra("url", article.getUrl());
                intent.putExtra("title", article.getTitle());

                startActivity(intent);
            }
        });

    }


    @Override
    public void onRefresh() {
        hideUnnecessaryViews();
        count = 0;
        LoadJson();
    }

    private void onLoadingSwipeRefresh(){
        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        LoadJson();
                    }
                });
    }

    //calling errorLayout with selected title, message, img and optional button
    private void showErrorMessage(String title, String message, int imageView, boolean btn){

        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
        }

        if(btn) errorButton.setVisibility(View.VISIBLE);
        else errorButton.setVisibility(View.INVISIBLE);

        errorImage.setImageResource(imageView);
        errorTitle.setText(title);
        errorMessage.setText(message);

        errorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideUnnecessaryViews();
                count = 0;
                onLoadingSwipeRefresh();
                nestedSV.smoothScrollTo(0, nestedSV.getTop());
            }
        });

        //not touchable invisible layout
        disableEnableControls(false, swipeRefreshLayout);

    }


    //handling top articles with retrofit
    private void getAllArticles(){
        final ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<List<Articles>> call = apiInterface.getArticles();

        call.enqueue(new Callback<List<Articles>>() {
            @Override
            public void onResponse(@NonNull Call<List<Articles>> call, @NonNull Response<List<Articles>> response) {
                if (response.isSuccessful() && response.body() != null){

                    if (!articles.isEmpty()){
                        articles.clear();
                    }

                    articles = response.body();
                    adapter = new Adapter(articles, MainActivity.this);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    initListener();
                    swipeRefreshLayout.setRefreshing(false);

                } else {

                    swipeRefreshLayout.setRefreshing(false);

                    String errorCode;
                    switch (response.code()) {
                        case 404:
                            errorCode = "404 not found";
                            break;
                        case 500:
                            errorCode = "500 server broken";
                            break;
                        default:
                            errorCode = "unknown error";
                            break;
                    }
                    showErrorMessage("No Result", "Try again later.\n" + errorCode, R.drawable.no_result, true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Articles>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showErrorMessage("Network error!", "Please check your network connection.", R.drawable.no_conn, true);
            }
        });


    }

    //not touchable invisible layout and its children with recursion
    private void disableEnableControls(boolean enable, ViewGroup vg){
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup){
                disableEnableControls(enable, (ViewGroup)child);
            }
        }
    }

    //adds a recycler view for next articles
    private RecyclerView addRV(){
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setVisibility(View.VISIBLE);
        linearLayout.addView(rv);
        return rv;
    }

    //adds a progress bar
    private void addPB(){
        ProgressBar pb = new ProgressBar(this);
        pb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pb.setVisibility(View.VISIBLE);
        linearLayout.addView(pb);
    }


    //handling favourites and more articles with retrofit/rxjava
    private void getMoreArticles(List<Integer> l) {
        if (l.isEmpty() && fav){
            swipeRefreshLayout.setRefreshing(false);
            showErrorMessage("No favourites!", "Add an article to your favorites,", R.drawable.no_fav, false);
        }
        else {
            ApiInterface apiInterface = ApiClient.getApiClientRX().create(ApiInterface.class);
            List<Observable<?>> requests = new ArrayList<>();
            for (int id : l) {
                requests.add(apiInterface.getArticle(id).onErrorReturnItem(new Articles()));
            }

            Observable.zip(requests, new Function<Object[], List<Articles>>() {
                @Override
                public List<Articles> apply(@NonNull Object[] objects) {
                    List<Articles> articlesArrayList = new ArrayList<>();
                    for (Object response : objects) {
                        if (((Articles) response).getId() != 0) {
                            articlesArrayList.add((Articles) response);
                        }
                    }
                    return articlesArrayList;
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            new Consumer<List<Articles>>() {
                                @Override
                                public void accept(List<Articles> articlesList) {
                                    adapter = new Adapter(articlesList, MainActivity.this);

                                    if (fav) recyclerView.setAdapter(adapter); //favourites
                                    else addRV().setAdapter(adapter); //articles added during scrolling

                                    if (!fav) addPB();
                                    adapter.notifyDataSetChanged();
                                    initListener();
                                    swipeRefreshLayout.setRefreshing(false);

                                    if (!fav) linearLayout.getChildAt(linearLayout.getChildCount()-3).setVisibility(View.GONE); //hiding the previous progress bar
                                    if(progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable e) {

                                }
                            }
                    ).isDisposed();
        }
    }

    //returns a ids list of the next 5 articles
    private List<Integer> getNextIds(int last){
        List<Integer> integerList = new ArrayList<>();
        int z = numLoadingArticles*(count-1);
        for(int i = last - 1 - z; i >= last - numLoadingArticles - z; i--){
            integerList.add(i);
        }
        return  integerList;
    }

    //hides dynamically created views
    private void hideUnnecessaryViews(){
        for (int i = 2; i < linearLayout.getChildCount(); i++){
            View child = linearLayout.getChildAt(i);
            child.setVisibility(View.GONE);
        }
    }
}
