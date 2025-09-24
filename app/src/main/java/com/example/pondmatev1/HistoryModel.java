package com.example.pondmatev1;

public class HistoryModel {
    private String action;
    private String date;
    private String pdfPath;

    public HistoryModel(String action, String date, String pdfPath) {
        this.action = action;
        this.date = date;
        this.pdfPath = pdfPath;
    }

    public String getAction() { return action; }
    public String getDate() { return date; }
    public String getPdfPath() { return pdfPath; }
}
