package src;

import java.util.ArrayList;
import java.util.List;

public class Subject {
    private String id;
    private String userId;
    private String name;
    private String examDate; // YYYY-MM-DD
    private int difficulty; // 1 to 5
    private int confidence; // 1 to 5
    private int weightage;  // Marks weightage or priority factor (1-100)
    private double estimatedHours;
    private List<Topic> topics;

    public Subject() {
        this.topics = new ArrayList<>();
    }

    public Subject(String id, String userId, String name, String examDate, int difficulty, int confidence, int weightage, double estimatedHours) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.examDate = examDate;
        this.difficulty = difficulty;
        this.confidence = confidence;
        this.weightage = weightage;
        this.estimatedHours = estimatedHours;
        this.topics = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }

    public int getWeightage() { return weightage; }
    public void setWeightage(int weightage) { this.weightage = weightage; }

    public double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(double estimatedHours) { this.estimatedHours = estimatedHours; }

    public List<Topic> getTopics() { return topics; }
    public void setTopics(List<Topic> topics) { this.topics = topics; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(String.format("\"id\":\"%s\",", escape(id)));
        sb.append(String.format("\"userId\":\"%s\",", escape(userId)));
        sb.append(String.format("\"name\":\"%s\",", escape(name)));
        sb.append(String.format("\"examDate\":\"%s\",", escape(examDate)));
        sb.append(String.format("\"difficulty\":%d,", difficulty));
        sb.append(String.format("\"confidence\":%d,", confidence));
        sb.append(String.format("\"weightage\":%d,", weightage));
        sb.append(String.format("\"estimatedHours\":%.2f,", estimatedHours));
        sb.append("\"topics\":[");
        for (int i = 0; i < topics.size(); i++) {
            sb.append(topics.get(i).toJson());
            if (i < topics.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}