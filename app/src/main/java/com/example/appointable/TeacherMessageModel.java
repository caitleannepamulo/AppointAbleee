package com.example.appointable;

public class TeacherMessageModel {

    private String userId;
    private String name;
    private String lastMessage;
    private String time;
    private int unreadCount;
    private String profileImageUrl;

    public TeacherMessageModel() {
    }

    public TeacherMessageModel(String userId,
                               String name,
                               String lastMessage,
                               String time,
                               int unreadCount,
                               String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unreadCount = unreadCount;
        this.profileImageUrl = profileImageUrl;
    }

    public TeacherMessageModel(String userId,
                               String name,
                               String lastMessage,
                               String time,
                               int unreadCount) {
        this(userId, name, lastMessage, time, unreadCount, null);
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTime() {
        return time;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
