package com.example.appointable;

public class ScheduleModel {

    private String childName;
    private String service;
    private String time;
    private int sortTimeMinutes;
    private int status = 0;   // 0=accepted,1=completed,2=canceled
    private int dayOfWeek;    // 1..7 (Mon..Sun)

    private String documentId;
    private String dateString;   // "MM/dd/yyyy"
    private int weekYear;
    private int weekOfYear;

    public ScheduleModel(String childName,
                         String service,
                         String time,
                         int sortTimeMinutes,
                         int dayOfWeek,
                         String documentId,
                         String dateString,
                         int weekYear,
                         int weekOfYear) {
        this.childName = childName;
        this.service = service;
        this.time = time;
        this.sortTimeMinutes = sortTimeMinutes;
        this.dayOfWeek = dayOfWeek;
        this.documentId = documentId;
        this.dateString = dateString;
        this.weekYear = weekYear;
        this.weekOfYear = weekOfYear;
    }

    public String getChildName() {
        return childName;
    }

    public String getService() {
        return service;
    }

    public String getTime() {
        return time;
    }

    public int getSortTimeMinutes() {
        return sortTimeMinutes;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDateString() {
        return dateString;
    }

    public int getWeekYear() {
        return weekYear;
    }

    public int getWeekOfYear() {
        return weekOfYear;
    }
}
