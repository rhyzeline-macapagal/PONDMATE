package com.example.pondmatev1;

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

    // Full constructor
    public PondModel(String id, String name, String breed, int fishCount, double costPerFish,
                     String dateStarted, String dateHarvest, String imagePath,
                     float actualROI, float estimatedROI) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.fishCount = fishCount;
        this.costPerFish = costPerFish;
        this.dateStarted = dateStarted;
        this.dateHarvest = dateHarvest;
        this.imagePath = imagePath;
        this.actualROI = actualROI;
        this.estimatedROI = estimatedROI;
    }

    public PondModel(String mode) {
        this.mode = mode;
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }   // ✅ Added back

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
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }   // ✅ Added back

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }

    public float getActualROI() { return actualROI; }
    public void setActualROI(float actualROI) { this.actualROI = actualROI; }

    public float getEstimatedROI() { return estimatedROI; }
    public void setEstimatedROI(float estimatedROI) { this.estimatedROI = estimatedROI; }
}
