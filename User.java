package src;

public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String salt;
    private double dailyAvailableHours;

    public User() {
        this.dailyAvailableHours = 4.0; // default 4 hours daily limit
    }

    public User(String id, String username, String passwordHash, String salt, double dailyAvailableHours) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.dailyAvailableHours = dailyAvailableHours <= 0 ? 4.0 : dailyAvailableHours;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public double getDailyAvailableHours() { return dailyAvailableHours; }
    public void setDailyAvailableHours(double dailyAvailableHours) { this.dailyAvailableHours = dailyAvailableHours; }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"username\":\"%s\",\"passwordHash\":\"%s\",\"salt\":\"%s\",\"dailyAvailableHours\":%.2f}",
            escape(id), escape(username), escape(passwordHash), escape(salt), dailyAvailableHours
        );
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}