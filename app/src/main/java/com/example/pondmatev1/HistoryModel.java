package com.example.pondmatev1;

public class HistoryModel {
    private String pondId;
    private String pondName;
    private String action;
    private String date;
    private String pdfPath;

    public HistoryModel(String pondId, String pondName, String action, String date, String pdfPath) {
        this.pondId = pondId;
        this.pondName = pondName;
        this.action = action;
        this.date = date;
        this.pdfPath = pdfPath;
    }

    public String getPondId() { return pondId; }
    public String getPondName() { return pondName; }
    public String getAction() { return action; }
    public String getDate() { return date; }
    public String getPdfPath() { return pdfPath; }
}
