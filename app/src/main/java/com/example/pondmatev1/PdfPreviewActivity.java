package com.example.pondmatev1;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_PATH = "extra_pdf_path";
    public static final String EXTRA_PDF_URL  = "extra_pdf_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_preview);

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.7)
            );
        }

        PDFView pdfView = findViewById(R.id.pdfView);

        String pdfPath = getIntent().getStringExtra(EXTRA_PDF_PATH);
        String pdfUrl  = getIntent().getStringExtra(EXTRA_PDF_URL);

        if (pdfPath != null && !pdfPath.isEmpty()) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                pdfView.fromFile(pdfFile)
                        .defaultPage(0)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .load();
            } else {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            }
        } else if (pdfUrl != null && !pdfUrl.isEmpty()) {

            loadPdfFromUrl(pdfUrl, pdfView);
        } else {
            Toast.makeText(this, "No PDF file or URL provided", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPdfFromUrl(String pdfUrl, PDFView pdfView) {
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

                runOnUiThread(() -> pdfView.fromFile(tempFile)
                        .defaultPage(0)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .load());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
