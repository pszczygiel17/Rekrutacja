package com.example.cosmicnews;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


//implments offset bla bla
public class NewsDetailActivity extends AppCompatActivity {

    private String mUrl, mTitle; //mImg, mTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        Toolbar toolbar = findViewById(R.id.appbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        mUrl = intent.getStringExtra("url");
        //mImg = intent.getStringExtra("img");
        mTitle = intent.getStringExtra("title");

        initWebView(mUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(String url) {
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        supportFinishAfterTransition();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    //@Override
    //public void onOffsetChanged(AppBarLayout appBarLayout, int i) {

    //}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_news, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.share) {
            try {
                //Toast.makeText(getApplicationContext(), "siema", Toast.LENGTH_LONG).show();
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plan");
                //i.putExtra(Intent.EXTRA_SUBJECT, "Headline from " + mSource);
                String body = mTitle + "\n\n" + mUrl + "\n\n" + "Shared via Sport News App" + "\n";
                i.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(i, "Share with:"));
            } catch (Exception e) {
                Toast.makeText(this, "Something went wrong with sharing", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
