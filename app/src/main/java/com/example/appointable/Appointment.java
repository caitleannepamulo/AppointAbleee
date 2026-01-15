package com.example.appointable;

public class Appointment {

    private String id;
    private String studentId;
    private String childName;
    private String teacherId;
    private String teacherName;
    private String service;
    private String date;   // "M/d/yyyy" or "MM/dd/yyyy"
    private String time;   // "h:mm a"
    private String status;

    private String rescheduleComment;

    public Appointment() {}

    public Appointment(String id, String studentId, String childName,
                       String teacherId, String teacherName,
                       String service, String date, String time,
                       String status) {

        this.id = id;
        this.studentId = studentId;
        this.childName = childName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.service = service;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getChildName() { return childName; }
    public String getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getService() { return service; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public String getRescheduleComment() { return rescheduleComment; }

    public void setId(String id) { this.id = id; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setChildName(String childName) { this.childName = childName; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setService(String service) { this.service = service; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setStatus(String status) { this.status = status; }
    public void setRescheduleComment(String rescheduleComment) {
        this.rescheduleComment = rescheduleComment;
    }
}