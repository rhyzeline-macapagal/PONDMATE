package com.example.pondmatev1;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PondModel {
    private String id;
    private String name;
    private String breed;
    private int fishCount;
    private double costPerFish;
    private String dateStarted;
    private String dateHarvest;
    private String dateStocking;
    private double pondArea;
    private String imagePath;
    private String mode;
    private String extraData;
    private float actualROI;
    private float estimatedROI;
    private String pdfPath;
    private double mortalityRate;
    private String caretakers;
    private String caretakerName;
    private boolean assigned;

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
        this.caretakerName = caretakerName;
        this.actualROI = actualROI;
        this.estimatedROI = estimatedROI;

        this.mortalityRate = mortalityRate;
        this.pdfPath = pdfPath;
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
    public String getCaretakerName() { return caretakerName; }
    public void setCaretakerName(String caretakerName) { this.caretakerName = caretakerName; }
    public boolean isAssigned() {
        return assigned;
    }
    public PondModel() {
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public String getCaretakers() { return caretakers; }
    public void setCaretakers(String caretakers) { this.caretakers = caretakers; }

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

    public double getMortalityRate() { return mortalityRate; }
    public void setMortalityRate(double mortalityRate) { this.mortalityRate = mortalityRate; }

    // --- Pond Cycle Milestones ---
    public List<Milestone> getPondCycleMilestones() {
        List<Milestone> milestones = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        try {
            Calendar start = Calendar.getInstance();
            start.setTime(sdf.parse(this.dateStocking));

            Calendar harvest = Calendar.getInstance();
            harvest.setTime(sdf.parse(this.dateHarvest));

            long totalDays = (harvest.getTimeInMillis() - start.getTimeInMillis()) / (1000 * 60 * 60 * 24);

            // Milestones
            milestones.add(new Milestone("Preparation", start));

            Calendar stockingDate = (Calendar) start.clone();
            milestones.add(new Milestone("Stocking", stockingDate));

            Calendar preStarterEnd = (Calendar) start.clone();
            preStarterEnd.add(Calendar.DAY_OF_MONTH, (int)(totalDays * 0.10));
            milestones.add(new Milestone("Pre-Starter Feeding", preStarterEnd));

            Calendar starterEnd = (Calendar) start.clone();
            starterEnd.add(Calendar.DAY_OF_MONTH, (int)(totalDays * 0.25));
            milestones.add(new Milestone("Starter Feeding", starterEnd));

            Calendar growerEnd = (Calendar) start.clone();
            growerEnd.add(Calendar.DAY_OF_MONTH, (int)(totalDays * 0.60));
            milestones.add(new Milestone("Grower Feeding", growerEnd));

            Calendar finisherEnd = (Calendar) start.clone();
            finisherEnd.add(Calendar.DAY_OF_MONTH, (int)(totalDays * 0.85));
            milestones.add(new Milestone("Finisher Feeding", finisherEnd));

            milestones.add(new Milestone("Harvesting", harvest));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return milestones;
    }

    // --- Milestone Helper Class ---
    public static class Milestone {
        private String name;
        private Calendar date;

        public Milestone(String name, Calendar date) {
            this.name = name;
            this.date = date;
        }

        public String getName() { return name; }
        public Calendar getDate() { return date; }
    }
}
