package com.example.appointable;

public class ChatMessageModel {

    private String message;
    private String time;
    private boolean isSender;
    private boolean isSeen;

    // date header
    private boolean isDateHeader;
    private String dateLabel;

    public ChatMessageModel() {
    }

    // normal message
    public ChatMessageModel(String message,
                            String time,
                            boolean isSender,
                            boolean isSeen) {
        this.message = message;
        this.time = time;
        this.isSender = isSender;
        this.isSeen = isSeen;
        this.isDateHeader = false;
        this.dateLabel = null;
    }

    // date header
    public ChatMessageModel(String dateLabel) {
        this.isDateHeader = true;
        this.dateLabel = dateLabel;
        this.message = null;
        this.time = null;
        this.isSender = false;
        this.isSeen = false;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public boolean isSender() {
        return isSender;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }

    public boolean isDateHeader() {
        return isDateHeader;
    }

    public String getDateLabel() {
        return dateLabel;
    }
}
