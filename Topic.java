package src;

public class Topic {
    private String id;
    private String title;
    private boolean completed;

    public Topic() {}

    public Topic(String id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String toJson() {
        return String.format("{\"id\":\"%s\",\"title\":\"%s\",\"completed\":%b}",
            escape(id), escape(title), completed);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}