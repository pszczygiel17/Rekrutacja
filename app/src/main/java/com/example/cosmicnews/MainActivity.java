package com.example.cosmicnews;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.cosmicnews.api.ApiClient;
import com.example.cosmicnews.api.ApiInterface;
import com.example.cosmicnews.models.Articles;

import java.util.ArrayList;
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
                        menuItem.setChecked(true); //changing item appearance with a selector
                        LoadJson();
                        break;
                    case R.id.favourites:
                        fav = true; //only favourites
                        menuItem.setChecked(true); //changing item appearance with a selector
                        LoadJson();
                        break;
                }
                return false;
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
            getFavouriteArticles(db.getFav());
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
                onLoadingSwipeRefresh();
            }
        });

        //not touchable invisible layout
        disableEnableControls(false, swipeRefreshLayout);

    }

    //handling favourites articles with retrofit
    private void getFavouriteArticles(final List<Integer> tab){
        if (tab.isEmpty()){
            swipeRefreshLayout.setRefreshing(false);
            showErrorMessage("No favourites!", "Add an article to your favorites,", R.drawable.no_fav, false);
        }
        else{
            final List<Articles> ar = new ArrayList<>();
            for(int i = 0; i < tab.size() ; i++) {
                ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
                Call<Articles> call = apiInterface.getArticle(tab.get(i));

                final int finalI = i;
                call.enqueue(new Callback<Articles>() {
                    @Override
                    public void onResponse(@NonNull Call<Articles> call, @NonNull Response<Articles> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ar.add(response.body());
                            if (finalI == tab.size() - 1) {
                                articles = ar;
                                adapter = new Adapter(articles, MainActivity.this);
                                recyclerView.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                                initListener();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }else {

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
                    public void onFailure(@NonNull Call<Articles> call, @NonNull Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        showErrorMessage("Network error!", "Please check your network connection.", R.drawable.no_conn, true);
                    }
                });
            }

        }
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


}
