package src;

public class StudySession {
    private String id;
    private String userId;
    private String date; // YYYY-MM-DD
    private String subjectId;
    private String subjectName;
    private String topicTitle;
    private String startTime; // e.g. "09:00"
    private String endTime;   // e.g. "10:30"
    private int durationMinutes;
    private String reason;
    private boolean completed;

    public StudySession() {}

    public StudySession(String id, String userId, String date, String subjectId, String subjectName, String topicTitle, String startTime, String endTime, int durationMinutes, String reason, boolean completed) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.topicTitle = topicTitle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.reason = reason;
        this.completed = completed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"userId\":\"%s\",\"date\":\"%s\",\"subjectId\":\"%s\",\"subjectName\":\"%s\",\"topicTitle\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"durationMinutes\":%d,\"reason\":\"%s\",\"completed\":%b}",
            escape(id), escape(userId), escape(date), escape(subjectId), escape(subjectName), escape(topicTitle), escape(startTime), escape(endTime), durationMinutes, escape(reason), completed
        );
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}