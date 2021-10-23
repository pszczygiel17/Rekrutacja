package com.example.cosmicnews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.cosmicnews.models.Articles;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {

    private final List<Articles> articles;
    private final Context context;
    private OnItemClickListener onItemClickListener;
    DatabaseHelper db;

    public Adapter(List<Articles> articles, Context context) {
        this.articles = articles;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
        return new MyViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holders, int position) {
        final MyViewHolder holder = holders;
        final Articles model = articles.get(position);

        RequestOptions requestOptions = new RequestOptions();
        db = new DatabaseHelper(context);

        Glide.with(context)
                .load(model.getImageUrl())
                .apply(requestOptions)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imageView);

        holder.title.setText(model.getTitle());
        holder.desc.setText(model.getSummary());
        holder.published_at.setText(Utils.convertDate(model.getPublishedAt()));
        if (db.getFav().contains(model.getId())) holder.star.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
        holder.star.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                if(db.getFav().contains(model.getId())){
                    if(db.deleteFromFav(String.valueOf(model.getId()))) {
                        holder.star.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey)));
                        Toast.makeText(context.getApplicationContext(), "Removed from favorites", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(context.getApplicationContext(), "Failed to remove from favorites", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    if(db.addToFav(model.getId())){
                        holder.star.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorPrimary)));
                        Toast.makeText(context.getApplicationContext(), "Added to favorites", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(context.getApplicationContext(), "Failed to add to favorites", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void OnItemClick(View view, int position);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, desc, published_at;
        ImageView imageView, star;
        ProgressBar progressBar;
        OnItemClickListener onItemClickListener;

        public MyViewHolder(View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);

            itemView.setOnClickListener(this);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.desc);
            published_at = itemView.findViewById(R.id.publishedAt);
            imageView = itemView.findViewById(R.id.img);
            progressBar = itemView.findViewById(R.id.progress_load_photo);
            star = itemView.findViewById(R.id.fav);

            this.onItemClickListener = onItemClickListener;

        }

        @Override
        public void onClick(View v) {

            onItemClickListener.OnItemClick(v, getAdapterPosition());

        }
    }
}
