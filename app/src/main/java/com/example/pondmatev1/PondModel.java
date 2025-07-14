package com.example.pondmatev1;

public class PondModel {
    private String name;
    private String breed;
    private int fishCount;
    private double costPerFish;
    private String dateStarted;
    private String dateHarvest;
    private String mode;


    public PondModel(String name, String breed, int fishCount, double costPerFish, String dateStarted, String dateHarvest, String mode) {
        this.name = name;
        this.breed = breed;
        this.fishCount = fishCount;
        this.costPerFish = costPerFish;
        this.dateStarted = dateStarted;
        this.dateHarvest = dateHarvest;
        this.mode = mode;
    }


    public PondModel(String name, String breed, int fishCount, double costPerFish, String dateStarted, String dateHarvest) {
        this(name, breed, fishCount, costPerFish, dateStarted, dateHarvest, null);
    }


    public PondModel(String mode) {
        this.mode = mode;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getBreed() {
        return breed;
    }

    public int getFishCount() {
        return fishCount;
    }

    public double getCostPerFish() {
        return costPerFish;
    }

    public String getDateStarted() {
        return dateStarted;
    }

    public String getDateHarvest() {
        return dateHarvest;
    }

    public String getMode() {
        return mode;
    }
}
