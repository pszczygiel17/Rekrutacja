package com.example.cosmicnews.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Launches {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("provider")
    @Expose
    private String provider;
}
