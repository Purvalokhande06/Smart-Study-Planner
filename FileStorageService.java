package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileStorageService {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.json";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FileStorageService() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // --- USER STORAGE ---
    public List<User> loadUsers() {
        lock.readLock().lock();
        try {
            File f = new File(USERS_FILE);
            if (!f.exists()) return new ArrayList<>();
            String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            return parseUsersJson(content);
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveUsers(List<User> users) {
        lock.writeLock().lock();
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < users.size(); i++) {
                sb.append("  ").append(users.get(i).toJson());
                if (i < users.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(Path.of(USERS_FILE), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- USER SPECIFIC DATA STORAGE ---
    private String getUserFilePath(String userId, String type) {
        return DATA_DIR + "/" + type + "_" + userId + ".json";
    }

    public List<Subject> loadSubjects(String userId) {
        lock.readLock().lock();
        try {
            File f = new File(getUserFilePath(userId, "subjects"));
            if (!f.exists()) return new ArrayList<>();
            String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            return parseSubjectsJson(content);
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveSubjects(String userId, List<Subject> subjects) {
        lock.writeLock().lock();
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < subjects.size(); i++) {
                sb.append("  ").append(subjects.get(i).toJson());
                if (i < subjects.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(Path.of(getUserFilePath(userId, "subjects")), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<StudySession> loadSessions(String userId) {
        lock.readLock().lock();
        try {
            File f = new File(getUserFilePath(userId, "sessions"));
            if (!f.exists()) return new ArrayList<>();
            String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            return parseSessionsJson(content);
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveSessions(String userId, List<StudySession> sessions) {
        lock.writeLock().lock();
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < sessions.size(); i++) {
                sb.append("  ").append(sessions.get(i).toJson());
                if (i < sessions.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(Path.of(getUserFilePath(userId, "sessions")), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- MANUAL JSON PARSERS (WITHOUT DEPENDENCIES) ---
    private List<User> parseUsersJson(String json) {
        List<User> list = new ArrayList<>();
        List<String> objects = extractJsonObjects(json);
        for (String obj : objects) {
            User u = new User();
            u.setId(extractStringField(obj, "id"));
            u.setUsername(extractStringField(obj, "username"));
            u.setPasswordHash(extractStringField(obj, "passwordHash"));
            u.setSalt(extractStringField(obj, "salt"));
            u.setDailyAvailableHours(extractDoubleField(obj, "dailyAvailableHours", 4.0));
            list.add(u);
        }
        return list;
    }

    private List<Subject> parseSubjectsJson(String json) {
        List<Subject> list = new ArrayList<>();
        List<String> objects = extractJsonObjects(json);
        for (String obj : objects) {
            Subject s = new Subject();
            s.setId(extractStringField(obj, "id"));
            s.setUserId(extractStringField(obj, "userId"));
            s.setName(extractStringField(obj, "name"));
            s.setExamDate(extractStringField(obj, "examDate"));
            s.setDifficulty((int) extractDoubleField(obj, "difficulty", 3));
            s.setConfidence((int) extractDoubleField(obj, "confidence", 3));
            s.setWeightage((int) extractDoubleField(obj, "weightage", 50));
            s.setEstimatedHours(extractDoubleField(obj, "estimatedHours", 10.0));

            // Parse embedded topics array
            List<Topic> topics = new ArrayList<>();
            int topicsIndex = obj.indexOf("\"topics\":");
            if (topicsIndex != -1) {
                String topicsSub = obj.substring(topicsIndex);
                List<String> topicObjs = extractJsonObjects(topicsSub);
                for (String tObj : topicObjs) {
                    Topic t = new Topic();
                    t.setId(extractStringField(tObj, "id"));
                    t.setTitle(extractStringField(tObj, "title"));
                    t.setCompleted(extractBooleanField(tObj, "completed"));
                    topics.add(t);
                }
            }
            s.setTopics(topics);
            list.add(s);
        }
        return list;
    }

    private List<StudySession> parseSessionsJson(String json) {
        List<StudySession> list = new ArrayList<>();
        List<String> objects = extractJsonObjects(json);
        for (String obj : objects) {
            StudySession s = new StudySession();
            s.setId(extractStringField(obj, "id"));
            s.setUserId(extractStringField(obj, "userId"));
            s.setDate(extractStringField(obj, "date"));
            s.setSubjectId(extractStringField(obj, "subjectId"));
            s.setSubjectName(extractStringField(obj, "subjectName"));
            s.setTopicTitle(extractStringField(obj, "topicTitle"));
            s.setStartTime(extractStringField(obj, "startTime"));
            s.setEndTime(extractStringField(obj, "endTime"));
            s.setDurationMinutes((int) extractDoubleField(obj, "durationMinutes", 60));
            s.setReason(extractStringField(obj, "reason"));
            s.setCompleted(extractBooleanField(obj, "completed"));
            list.add(s);
        }
        return list;
    }

    // Helper regex extractors
    public static List<String> extractJsonObjects(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;
        int depth = 0;
        int start = -1;
        boolean inQuotes = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        list.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return list;
    }

    public static String extractStringField(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        return "";
    }

    public static double extractDoubleField(String json, String key, double def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.]+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
        }
        return def;
    }

    public static boolean extractBooleanField(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(json);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return false;
    }
}