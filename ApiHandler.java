package src;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ApiHandler implements HttpHandler {
    private final AuthService authService;
    private final FileStorageService storageService;
    private final StudyPlanService planService;

    public ApiHandler(AuthService auth, FileStorageService storage, StudyPlanService plan) {
        this.authService = auth;
        this.storageService = storage;
        this.planService = plan;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            // Serve Static Frontend Assets
            if (!path.startsWith("/api/")) {
                serveStaticFile(exchange, path);
                return;
            }

            // Route API Endpoints
            if (path.equals("/api/auth/register") && method.equalsIgnoreCase("POST")) {
                handleRegister(exchange);
            } else if (path.equals("/api/auth/login") && method.equalsIgnoreCase("POST")) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/logout") && method.equalsIgnoreCase("POST")) {
                handleLogout(exchange);
            } else if (path.equals("/api/auth/me") && method.equalsIgnoreCase("GET")) {
                handleGetMe(exchange);
            } else if (path.equals("/api/subjects") && method.equalsIgnoreCase("GET")) {
                handleGetSubjects(exchange);
            } else if (path.equals("/api/subjects") && method.equalsIgnoreCase("POST")) {
                handleAddSubject(exchange);
            } else if (path.startsWith("/api/subjects/delete") && method.equalsIgnoreCase("POST")) {
                handleDeleteSubject(exchange);
            } else if (path.equals("/api/schedule/daily") && method.equalsIgnoreCase("GET")) {
                handleGetDailySchedule(exchange);
            } else if (path.equals("/api/schedule/regenerate") && method.equalsIgnoreCase("POST")) {
                handleRegenerateSchedule(exchange);
            } else if (path.equals("/api/schedule/multiday") && method.equalsIgnoreCase("GET")) {
                handleGetMultiDayPlan(exchange);
            } else if (path.equals("/api/sessions/complete") && method.equalsIgnoreCase("POST")) {
                handleToggleSessionComplete(exchange);
            } else if (path.equals("/api/topics/complete") && method.equalsIgnoreCase("POST")) {
                handleToggleTopicComplete(exchange);
            } else {
                sendJsonResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + escape(e.getMessage()) + "\"}");
        }
    }

    // --- STATIC FILES ---
    private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        File file = new File("public" + path);

        if (!file.exists() || file.isDirectory()) {
            file = new File("public/index.html");
        }

        String mime = "text/html";
        if (path.endsWith(".css")) mime = "text/css";
        else if (path.endsWith(".js")) mime = "application/javascript";
        else if (path.endsWith(".ico")) mime = "image/x-icon";

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    // --- AUTH HANDLERS ---
    private void handleRegister(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        String username = FileStorageService.extractStringField(body, "username");
        String password = FileStorageService.extractStringField(body, "password");
        double dailyHours = FileStorageService.extractDoubleField(body, "dailyHours", 4.0);

        if (username.isBlank() || password.isBlank()) {
            sendJsonResponse(exchange, 400, "{\"error\":\"Username and password required\"}");
            return;
        }

        try {
            String token = authService.registerUser(username, password, dailyHours);
            setSessionCookie(exchange, token);
            sendJsonResponse(exchange, 200, "{\"message\":\"Registration successful!\"}");
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        String username = FileStorageService.extractStringField(body, "username");
        String password = FileStorageService.extractStringField(body, "password");

        try {
            String token = authService.loginUser(username, password);
            setSessionCookie(exchange, token);
            sendJsonResponse(exchange, 200, "{\"message\":\"Login successful!\"}");
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 401, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = getSessionToken(exchange);
        authService.logout(token);
        exchange.getResponseHeaders().add("Set-Cookie", "sessionToken=; Path=/; HttpOnly; Max-Age=0");
        sendJsonResponse(exchange, 200, "{\"message\":\"Logged out\"}");
    }

    private void handleGetMe(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        sendJsonResponse(exchange, 200, user.toJson());
    }

    // --- SUBJECT HANDLERS ---
    private void handleGetSubjects(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        List<Subject> subjects = storageService.loadSubjects(user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < subjects.size(); i++) {
            sb.append(subjects.get(i).toJson());
            if (i < subjects.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleAddSubject(HttpExchange exchange) throws Exception {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        String body = readRequestBody(exchange);
        String name = FileStorageService.extractStringField(body, "name");
        String examDate = FileStorageService.extractStringField(body, "examDate");
        int difficulty = (int) FileStorageService.extractDoubleField(body, "difficulty", 3);
        int confidence = (int) FileStorageService.extractDoubleField(body, "confidence", 3);
        int weightage = (int) FileStorageService.extractDoubleField(body, "weightage", 50);
        double estHours = FileStorageService.extractDoubleField(body, "estimatedHours", 10);

        if (name.isBlank() || examDate.isBlank()) {
            sendJsonResponse(exchange, 400, "{\"error\":\"Subject Name and Exam Date are required\"}");
            return;
        }

        List<Subject> subjects = storageService.loadSubjects(user.getId());
        Subject sub = new Subject("sub_" + System.currentTimeMillis(), user.getId(), name, examDate, difficulty, confidence, weightage, estHours);

        // Extract topics array string
        int topicsIdx = body.indexOf("\"topics\":");
        if (topicsIdx != -1) {
            String topicsRaw = body.substring(topicsIdx);
            List<String> topicObjs = FileStorageService.extractJsonObjects(topicsRaw);
            for (String tObj : topicObjs) {
                String title = FileStorageService.extractStringField(tObj, "title");
                if (!title.isBlank()) {
                    sub.getTopics().add(new Topic("top_" + UUID.randomUUID().toString().substring(0, 5), title, false));
                }
            }
        }

        subjects.add(sub);
        storageService.saveSubjects(user.getId(), subjects);

        // Auto regenerate schedule
        planService.regenerateDailySchedule(user);

        sendJsonResponse(exchange, 200, "{\"message\":\"Subject added successfully\"}");
    }

    private void handleDeleteSubject(HttpExchange exchange) throws Exception {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        String body = readRequestBody(exchange);
        String subjectId = FileStorageService.extractStringField(body, "subjectId");

        List<Subject> subjects = storageService.loadSubjects(user.getId());
        subjects.removeIf(s -> s.getId().equals(subjectId));
        storageService.saveSubjects(user.getId(), subjects);

        planService.regenerateDailySchedule(user);
        sendJsonResponse(exchange, 200, "{\"message\":\"Subject deleted\"}");
    }

    // --- SCHEDULE HANDLERS ---
    private void handleGetDailySchedule(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        List<StudySession> sessions = planService.generateDailySchedule(user);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sessions.size(); i++) {
            sb.append(sessions.get(i).toJson());
            if (i < sessions.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleRegenerateSchedule(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        List<StudySession> sessions = planService.regenerateDailySchedule(user);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sessions.size(); i++) {
            sb.append(sessions.get(i).toJson());
            if (i < sessions.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleGetMultiDayPlan(HttpExchange exchange) throws IOException {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        Map<String, Object> plan = planService.generateMultiDayPlan(user);
        // Serialize map to JSON
        sendJsonResponse(exchange, 200, mapToJson(plan));
    }

    private void handleToggleSessionComplete(HttpExchange exchange) throws Exception {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        String body = readRequestBody(exchange);
        String sessionId = FileStorageService.extractStringField(body, "sessionId");
        boolean completed = FileStorageService.extractBooleanField(body, "completed");

        List<StudySession> sessions = storageService.loadSessions(user.getId());
        for (StudySession s : sessions) {
            if (s.getId().equals(sessionId)) {
                s.setCompleted(completed);
                break;
            }
        }
        storageService.saveSessions(user.getId(), sessions);
        sendJsonResponse(exchange, 200, "{\"message\":\"Session updated\"}");
    }

    private void handleToggleTopicComplete(HttpExchange exchange) throws Exception {
        User user = getAuthenticatedUser(exchange);
        if (user == null) { sendJsonResponse(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

        String body = readRequestBody(exchange);
        String subjectId = FileStorageService.extractStringField(body, "subjectId");
        String topicId = FileStorageService.extractStringField(body, "topicId");
        boolean completed = FileStorageService.extractBooleanField(body, "completed");

        List<Subject> subjects = storageService.loadSubjects(user.getId());
        for (Subject s : subjects) {
            if (s.getId().equals(subjectId)) {
                for (Topic t : s.getTopics()) {
                    if (t.getId().equals(topicId)) {
                        t.setCompleted(completed);
                        break;
                    }
                }
            }
        }
        storageService.saveSubjects(user.getId(), subjects);
        sendJsonResponse(exchange, 200, "{\"message\":\"Topic updated\"}");
    }

    // --- UTILITIES ---
    private User getAuthenticatedUser(HttpExchange exchange) {
        String token = getSessionToken(exchange);
        return authService.validateSession(token);
    }

    private String getSessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] cookiePairs = cookieHeader.split(";");
                for (String pair : cookiePairs) {
                    String[] kv = pair.trim().split("=");
                    if (kv.length == 2 && kv[0].equals("sessionToken")) {
                        return kv[1];
                    }
                }
            }
        }
        return null;
    }

    private void setSessionCookie(HttpExchange exchange, String token) {
        exchange.getResponseHeaders().add("Set-Cookie", "sessionToken=" + token + "; Path=/; HttpOnly; SameSite=Lax");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @SuppressWarnings("unchecked")
    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(escape((String) val)).append("\"");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else if (val instanceof List) {
                sb.append(listToJson((List<?>) val));
            } else if (val instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) val));
            }
            if (++count < map.size()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Object val = list.get(i);
            if (val instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) val));
            } else if (val instanceof String) {
                sb.append("\"").append(escape((String) val)).append("\"");
            } else {
                sb.append(val);
            }
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}