package com.example.pondmatev1;
public class SelectableCaretaker {
    private String id;
    private String name;
    private boolean selected;

    public SelectableCaretaker(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
