package com.example.cosmicnews;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    @SuppressLint("SimpleDateFormat")
    public static String convertDate(String time){
        String displayDate = null;
        try {
            Date parsedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(time);
            displayDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return displayDate;
    }

}