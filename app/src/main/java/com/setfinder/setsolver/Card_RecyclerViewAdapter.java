package com.setfinder.setsolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class Card_RecyclerViewAdapter extends RecyclerView.Adapter<Card_RecyclerViewAdapter.MyViewHolder> {
    Context context;
    ArrayList<Mat> isolatedCards;

    public Card_RecyclerViewAdapter(Context context, ArrayList<Mat> isolatedCards) {
        this.context = context;
        this.isolatedCards = isolatedCards;
    }

    @NonNull
    @Override
    public Card_RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This is where we inflate the layout (Giving a look to our rows)
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recycler_view_row, parent, false);
        return new Card_RecyclerViewAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Card_RecyclerViewAdapter.MyViewHolder holder, int position) {
        // Assign values to the views we created in the recycler view recycler_view_row layout file
        // based on teh position of the recycler view
        Mat mat = isolatedCards.get(position);
        Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bm);

        holder.imageView.setImageBitmap(bm);
    }

    @Override
    public int getItemCount() {
        // The recycler view just wants to know the number of items you want displayed
        return  isolatedCards.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // Grabbing the views from our recycler_view_row layout file
        // Kind of like a onCreate method
        ImageView imageView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
