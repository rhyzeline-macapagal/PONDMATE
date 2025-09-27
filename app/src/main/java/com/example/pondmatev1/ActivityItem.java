package com.example.pondmatev1;

public class ActivityItem {
    private String name;
    private String type;
    private String scheduledDate;
    private boolean checked; // track if user has checked this activity

    public ActivityItem(String name, String type, String scheduledDate) {
        this.name = name;
        this.type = type;
        this.scheduledDate = scheduledDate;
        this.checked = false;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getScheduledDate() { return scheduledDate; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ActivityItem)) return false;
        ActivityItem other = (ActivityItem) obj;
        return name.equals(other.name)
                && type.equals(other.type)
                && scheduledDate.equals(other.scheduledDate);
    }

    @Override
    public int hashCode() {
        return name.hashCode() + type.hashCode() + scheduledDate.hashCode();
    }
}
