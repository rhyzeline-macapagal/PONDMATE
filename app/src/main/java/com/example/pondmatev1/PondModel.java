package com.example.pondmatev1;

import java.util.ArrayList;
import java.util.List;

public class PondModel {
    private String id;
    private String name;
    private String breed;
    private int fishCount;
    private double costPerFish;
    private String dateStarted;
    private String dateHarvest;
    private String imagePath;
    private String mode;
    private String extraData;
    private float actualROI;
    private float estimatedROI;
    private String pdfPath;




    // 🆕 Added fields
    private double pondArea;      // in square meters or hectares
    private String dateStocking;  // date of stocking
    private double mortalityRate; // percentage of fish that died

    private List<ActivityItem> activities = new ArrayList<>();

    // --- Constructors ---
    public PondModel(String id, String name, String breed, int fishCount, double costPerFish,
                     String dateStarted, String dateHarvest, String dateStocking, double pondArea,
                     String imagePath, String mode, float actualROI, float estimatedROI, String pdfPath, double mortalityRate) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.fishCount = fishCount;
        this.costPerFish = costPerFish;
        this.dateStarted = dateStarted;
        this.dateHarvest = dateHarvest;
        this.dateStocking = dateStocking;
        this.pondArea = pondArea;
        this.imagePath = imagePath;
        this.mode = mode;
        this.actualROI = actualROI;
        this.estimatedROI = estimatedROI;
        this.pdfPath = pdfPath;
        this.mortalityRate = mortalityRate; 
    }



    public PondModel(String name, double pondArea, String dateStarted, String dateStocking, String imagePath) {
        this.name = name;
        this.pondArea = pondArea;
        this.dateStarted = dateStarted;
        this.dateStocking = dateStocking;
        this.imagePath = imagePath;
        this.mortalityRate = 0.0;
    }

    public PondModel(String mode) {
        this.mode = mode;
        this.mortalityRate = 0.0;
    }



    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public int getFishCount() { return fishCount; }
    public void setFishCount(int fishCount) { this.fishCount = fishCount; }

    public double getCostPerFish() { return costPerFish; }
    public void setCostPerFish(double costPerFish) { this.costPerFish = costPerFish; }

    public String getDateStarted() { return dateStarted; }
    public void setDateStarted(String dateStarted) { this.dateStarted = dateStarted; }

    public String getDateHarvest() { return dateHarvest; }
    public void setDateHarvest(String dateHarvest) { this.dateHarvest = dateHarvest; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }

    public float getActualROI() { return actualROI; }
    public void setActualROI(float actualROI) { this.actualROI = actualROI; }

    public float getEstimatedROI() { return estimatedROI; }
    public void setEstimatedROI(float estimatedROI) { this.estimatedROI = estimatedROI; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public List<ActivityItem> getActivities() { return activities; }
    public void setActivities(List<ActivityItem> activities) { this.activities = activities; }

    public double getPondArea() { return pondArea; }
    public void setPondArea(double pondArea) { this.pondArea = pondArea; }

    public String getDateStocking() { return dateStocking; }
    public void setDateStocking(String dateStocking) { this.dateStocking = dateStocking; }

    // 🆕 Mortality Rate
    public double getMortalityRate() { return mortalityRate; }
    public void setMortalityRate(double mortalityRate) { this.mortalityRate = mortalityRate; }
}
