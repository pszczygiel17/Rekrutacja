package com.example.cosmicnews;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String database_name = "CosmicNewsDB";
    private static final String fav = "fav";

    DatabaseHelper(@Nullable Context context) {
        super(context, database_name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(" create table " + fav + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, NUM INTEGER) ");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(" DROP TABLE IF EXISTS " + fav);

        onCreate(db);
    }

    //adding id of favourite article to db
    public boolean addToFav(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("Num", id);
        return db.insert(fav, null, cv) != -1;
    }

    //getting list of ids favourites articles
    public List<Integer> getFav(){
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        @SuppressLint("Recycle") SQLiteCursor cursor = (SQLiteCursor) db.rawQuery(" SELECT * FROM " + fav, null);
        while(cursor.moveToNext()){
            ids.add(cursor.getInt(1));
        }
        cursor.close();
        db.close();
        return ids;
    }

    //deleting id of favourite article from db
    public  boolean deleteFromFav(String num){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(fav, "NUM = ?", new String[]{num}) > 0;
    }

}
