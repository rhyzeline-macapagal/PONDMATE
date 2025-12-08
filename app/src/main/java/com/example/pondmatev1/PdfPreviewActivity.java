package com.example.pondmatev1;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_PATH = "extra_pdf_path";
    public static final String EXTRA_PDF_URL  = "extra_pdf_url";

    private RecyclerView pdfRecyclerView;
    private PdfAdapter pdfAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_preview);

        pdfRecyclerView = findViewById(R.id.pdfViewRecycler);
        pdfRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pdfAdapter = new PdfAdapter();
        pdfRecyclerView.setAdapter(pdfAdapter);

        FloatingActionButton btnExit = findViewById(R.id.btnExit);
        btnExit.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnExit.setRippleColor(Color.TRANSPARENT); // disables ripple color if needed
        btnExit.setOnClickListener(v -> finish());

        String pdfPath = getIntent().getStringExtra(EXTRA_PDF_PATH);
        String pdfUrl  = getIntent().getStringExtra(EXTRA_PDF_URL);

        if (pdfPath != null && !pdfPath.isEmpty()) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                displayPdf(pdfFile);
            } else {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            }
        } else if (pdfUrl != null && !pdfUrl.isEmpty()) {
            loadPdfFromUrl(pdfUrl);
        } else {
            Toast.makeText(this, "No PDF file or URL provided", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayPdf(File pdfFile) {
        new Thread(() -> {
            try (ParcelFileDescriptor parcelFileDescriptor =
                         ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)) {

                PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                List<Bitmap> pages = new ArrayList<>();

                // Get screen width for scaling
                int screenWidth = getResources().getDisplayMetrics().widthPixels;

                for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);

                    // Scale bitmap width to screen width, preserve aspect ratio
                    float scale = (float) screenWidth / page.getWidth();
                    int scaledWidth = screenWidth;
                    int scaledHeight = (int) (page.getHeight() * scale);

                    Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);

                    // Scale canvas to fit
                    canvas.scale(scale, scale);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    pages.add(bitmap);
                    page.close();
                }

                pdfRenderer.close();

                runOnUiThread(() -> pdfAdapter.setPages(pages));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to render PDF", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


    private void loadPdfFromUrl(String pdfUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                File tempFile = new File(getCacheDir(), "temp.pdf");
                FileOutputStream output = new FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) > 0) {
                    output.write(buffer, 0, len);
                }

                output.close();
                input.close();

                displayPdf(tempFile);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // RecyclerView Adapter for PDF pages
    private static class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PageViewHolder> {

        private final List<Bitmap> pages = new ArrayList<>();

        void setPages(List<Bitmap> newPages) {
            pages.clear();
            pages.addAll(newPages);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setAdjustViewBounds(true);
            return new PageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.imageView.setImageBitmap(pages.get(position));
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class PageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            PageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }

}
