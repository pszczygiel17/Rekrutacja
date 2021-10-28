package com.example.cosmicnews;

import android.annotation.SuppressLint;

import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
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
import android.widget.Toast;

import com.example.cosmicnews.api.ApiClient;
import com.example.cosmicnews.api.ApiInterface;
import com.example.cosmicnews.models.Articles;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private RecyclerView recyclerView;
    private List<Articles> articles = new ArrayList<>();
    private Adapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RelativeLayout errorLayout;


    private NestedScrollView nestedSV;
    int count = 0;
    LinearLayout linearLayout;
    ProgressBar progressBar;


    private ImageView errorImage;
    private TextView errorTitle, errorMessage;
    private Button errorButton;
    private boolean fav = false;

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

            @RequiresApi(api = Build.VERSION_CODES.O)
            @SuppressLint({"ResourceAsColor", "NonConstantResourceId"})
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId())
                {
                    case R.id.home:
                        fav = false; //top articles
                        nestedSV.smoothScrollTo(0, nestedSV.getTop());
                        count = 0;
                        menuItem.setChecked(true); //changing item appearance with a selector
                        LoadJson();
                        break;
                    case R.id.favourites:
                        fav = true; //only favourites
                        nestedSV.smoothScrollTo(0, nestedSV.getTop());
                        progressBar.setVisibility(View.GONE);
                        menuItem.setChecked(true); //changing item appearance with a selector
                        LoadJson();
                        break;
                }
                return false;
            }
        });



        nestedSV.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(!fav) {
                    if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                        count++;
                        Toast.makeText(getApplicationContext(), String.valueOf(articles.get(articles.size()-1).getId()), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.VISIBLE);
                        if (count >= 1) progressBar.setVisibility(View.GONE);
                        if (count < 40) {
                            //11314, 11312, 11311 correct for example
                            //11310 error for example
                            List<Integer> l = Arrays.asList(11314,11312,11311);
                            getMoreArticles(l);
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
            //loading list of favourites using list of ids from database
            getMoreArticles(db.getFav());

            for (int i = 2; i < linearLayout.getChildCount(); i++){
                View child = linearLayout.getChildAt(i);
                child.setVisibility(View.GONE);
            }

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
        for (int i = 2; i < linearLayout.getChildCount(); i++){
            View child = linearLayout.getChildAt(i);
            child.setVisibility(View.GONE);
        }
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
                for (int i = 2; i < linearLayout.getChildCount(); i++){
                    View child = linearLayout.getChildAt(i);
                    child.setVisibility(View.GONE);
                }
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

    private RecyclerView addRV(){
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setVisibility(View.VISIBLE);
        linearLayout.addView(rv);
        return rv;
    }

    private ProgressBar addPB(){
        ProgressBar pb = new ProgressBar(this);
        pb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pb.setVisibility(View.VISIBLE);
        linearLayout.addView(pb);
        return pb;
    }


    @SuppressLint("CheckResult")
    private void getMoreArticles(List<Integer> l) {
        if (l.isEmpty() && fav){
            swipeRefreshLayout.setRefreshing(false);
            showErrorMessage("No favourites!", "Add an article to your favorites,", R.drawable.no_fav, false);
        }
        else {
            ApiInterface apiInterface = ApiClient.getApiClientRX().create(ApiInterface.class);

            List<Observable<?>> requests = new ArrayList<>();
            for (int id : l) {
                requests.add(apiInterface.getArticle1(id).onErrorResumeNext(Observable.<Articles>empty()));
            }

            Observable.zip(requests, new Function<Object[], List<Articles>>() {
                @Override
                public List<Articles> apply(@NonNull Object[] objects) {
                    List<Articles> articlesArrayList = new ArrayList<>();
                    for (Object response : objects) {
                        articlesArrayList.add((Articles) response);
                    }
                    return articlesArrayList;
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable error) {
                        }
                    })
                    .onErrorResumeNext(Observable.<List<Articles>>empty())
                    .subscribe(
                            new Consumer<List<Articles>>() {
                                @Override
                                public void accept(List<Articles> articlesList) {
                                    adapter = new Adapter(articlesList, MainActivity.this);

                                    if (fav) recyclerView.setAdapter(adapter);
                                    else addRV().setAdapter(adapter);

                                    if (!fav) addPB();
                                    adapter.notifyDataSetChanged();
                                    initListener();
                                    swipeRefreshLayout.setRefreshing(false);

                                    if (!fav) linearLayout.getChildAt(linearLayout.getChildCount()-3).setVisibility(View.GONE);
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable e) {
                                    //Exception ex = new Exception(e);
                                    //try {
                                      //  throw ex;
                                    //} catch (NetworkErrorException exception) {
                                        //swipeRefreshLayout.setRefreshing(false);
                                        //showErrorMessage("Network error!", "Please check your network connection.", R.drawable.no_conn, true);
                                   // }

                                }
                            }
                    ).isDisposed();
        }

    }



}
