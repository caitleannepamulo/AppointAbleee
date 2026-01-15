package com.example.appointable;

public class SummaryItem {
    private String childName;
    private String service;
    private int progress;

    public SummaryItem(String childName, String service, int progress) {
        this.childName = childName;
        this.service = service;
        this.progress = progress;
    }

    public String getChildName() { return childName; }
    public String getService() { return service; }
    public int getProgress() { return progress; }
}
