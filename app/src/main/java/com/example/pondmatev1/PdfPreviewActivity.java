package com.example.pondmatev1;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PDF_PATH = "extra_pdf_path";

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

        String path = getIntent().getStringExtra(EXTRA_PDF_PATH);
        if (path != null) {
            File pdfFile = new File(path);
            if (pdfFile.exists()) {
                pdfView.fromFile(pdfFile)
                        .defaultPage(0)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .load();
            } else {
                Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No PDF path provided", Toast.LENGTH_SHORT).show();
        }
    }


}
