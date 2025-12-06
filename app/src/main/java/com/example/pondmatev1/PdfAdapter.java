package com.example.pondmatev1;

import android.graphics.Bitmap;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PageViewHolder> {

    private final List<Bitmap> pages = new ArrayList<>();

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PhotoView photoView = new PhotoView(parent.getContext());
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 20); // spacing between pages
        photoView.setLayoutParams(params);
        photoView.setAdjustViewBounds(true);
        photoView.setBackgroundColor(0xFFFFFFFF); // white background
        return new PageViewHolder(photoView);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.photoView.setImageBitmap(pages.get(position));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public void setPages(List<Bitmap> newPages) {
        pages.clear();
        pages.addAll(newPages);
        notifyDataSetChanged();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public PageViewHolder(@NonNull PhotoView itemView) {
            super(itemView);
            photoView = itemView;
        }
    }

}
