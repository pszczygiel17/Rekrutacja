package com.example.cosmicnews.api;


import com.example.cosmicnews.models.Articles;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiInterface {


    @GET("articles")
    Call<List<Articles>> getArticles();

    @GET("articles/{id}")
    Call<Articles> getArticle(@Path("id") int id);

    @GET("articles/{id}")
    Observable<Articles> getArticle1(@Path("id") int id);

}
