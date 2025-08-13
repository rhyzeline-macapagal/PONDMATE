package com.example.pondmatev1;

public class PondModel {
    private String name;
    private String breed;
    private int fishCount;
    private double costPerFish;
    private String dateStarted;
    private String dateHarvest;
    private String mode; // Could be used to mark "ONLINE" or "OFFLINE"
    private String id;

    // Default constructor (needed for some JSON libraries like Gson/Retrofit)
    public PondModel() {}

    public PondModel(String name, String breed, int fishCount, double costPerFish,
                     String dateStarted, String dateHarvest, String mode) {
        this.name = name;
        this.breed = breed;
        this.fishCount = fishCount;
        this.costPerFish = costPerFish;
        this.dateStarted = dateStarted;
        this.dateHarvest = dateHarvest;
        this.mode = mode;
    }

    public PondModel(String mode) {
        this.mode = mode;
    }


    // Getters and Setters (for JSON serialization/deserialization)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public int getFishCount() {
        return fishCount;
    }

    public void setFishCount(int fishCount) {
        this.fishCount = fishCount;
    }

    public double getCostPerFish() {
        return costPerFish;
    }

    public void setCostPerFish(double costPerFish) {
        this.costPerFish = costPerFish;
    }

    public String getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(String dateStarted) {
        this.dateStarted = dateStarted;
    }

    public String getDateHarvest() {
        return dateHarvest;
    }

    public void setDateHarvest(String dateHarvest) {
        this.dateHarvest = dateHarvest;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
