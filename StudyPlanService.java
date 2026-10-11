package src;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class StudyPlanService {
    private final FileStorageService storageService;

    public StudyPlanService(FileStorageService storageService) {
        this.storageService = storageService;
    }

    public double calculateSubjectPriority(Subject s) {
        LocalDate today = LocalDate.now();
        LocalDate examDate;
        try {
            examDate = LocalDate.parse(s.getExamDate());
        } catch (Exception e) {
            examDate = today.plusDays(7);
        }

        long daysLeft = ChronoUnit.DAYS.between(today, examDate);
        if (daysLeft < 0) daysLeft = 0; // Exam today or past

        // 1. Urgency Component (Max 10 points)
        double urgencyScore = Math.max(10.0 - daysLeft, 0.5);

        // 2. Difficulty Component (1 to 5)
        double difficultyScore = s.getDifficulty();

        // 3. Preparation Gap Component (Low confidence = High Priority) (1 to 5)
        double gapScore = (6.0 - s.getConfidence());

        // 4. Unfinished Topics Factor
        int totalTopics = s.getTopics().size();
        long uncompletedTopics = s.getTopics().stream().filter(t -> !t.isCompleted()).count();
        double unfinishedFactor = totalTopics == 0 ? 1.0 : ((double) uncompletedTopics / totalTopics) * 5.0;

        // Multi-Factor Weight Formula
        return (urgencyScore * 3.5) + (difficultyScore * 2.0) + (gapScore * 2.0) + (unfinishedFactor * 1.5);
    }

    public List<StudySession> generateDailySchedule(User user) {
        List<Subject> subjects = storageService.loadSubjects(user.getId());
        List<StudySession> existingSessions = storageService.loadSessions(user.getId());
        
        String todayStr = LocalDate.now().toString();

        // Preserve manually toggled sessions for today if available
        List<StudySession> todaySessions = new ArrayList<>();
        for (StudySession sess : existingSessions) {
            if (sess.getDate().equals(todayStr)) {
                todaySessions.add(sess);
            }
        }

        if (!todaySessions.isEmpty()) {
            return todaySessions; // Return saved state if already calculated
        }

        return regenerateDailySchedule(user);
    }

    public List<StudySession> regenerateDailySchedule(User user) {
        List<Subject> subjects = storageService.loadSubjects(user.getId());
        String todayStr = LocalDate.now().toString();

        // Filter out subjects whose exams are passed or fully completed
        List<Subject> activeSubjects = new ArrayList<>();
        for (Subject s : subjects) {
            long uncompleted = s.getTopics().stream().filter(t -> !t.isCompleted()).count();
            if (uncompleted > 0) {
                activeSubjects.add(s);
            }
        }

        if (activeSubjects.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort by Priority Score descending
        activeSubjects.sort((a, b) -> Double.compare(calculateSubjectPriority(b), calculateSubjectPriority(a)));

        List<StudySession> newSessions = new ArrayList<>();
        double availableHours = user.getDailyAvailableHours();
        int totalAvailableMinutes = (int) (availableHours * 60);
        int allocatedMinutes = 0;

        LocalTime currentTime = LocalTime.of(9, 0); // Start at 09:00 AM

        for (Subject sub : activeSubjects) {
            if (allocatedMinutes >= totalAvailableMinutes) break;

            // Pick first uncompleted topic
            Topic targetTopic = null;
            for (Topic t : sub.getTopics()) {
                if (!t.isCompleted()) {
                    targetTopic = t;
                    break;
                }
            }

            int sessionDuration = 90; // standard 1.5 hour block
            if (allocatedMinutes + sessionDuration > totalAvailableMinutes) {
                sessionDuration = totalAvailableMinutes - allocatedMinutes;
            }

            if (sessionDuration < 15) break; // Don't create trivial sessions

            LocalTime endTime = currentTime.plusMinutes(sessionDuration);

            String topicTitle = targetTopic != null ? targetTopic.getTitle() : "General Revision & Practice";
            double score = calculateSubjectPriority(sub);
            String reason = String.format("Priority Score: %.1f | High difficulty/upcoming exam", score);

            StudySession sess = new StudySession(
                "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4),
                user.getId(),
                todayStr,
                sub.getId(),
                sub.getName(),
                topicTitle,
                currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                sessionDuration,
                reason,
                false
            );

            newSessions.add(sess);
            allocatedMinutes += sessionDuration;
            
            // Add 15 min break
            currentTime = endTime.plusMinutes(15);
        }

        // Save updated sessions
        List<StudySession> allSessions = storageService.loadSessions(user.getId());
        allSessions.removeIf(s -> s.getDate().equals(todayStr));
        allSessions.addAll(newSessions);
        storageService.saveSessions(user.getId(), allSessions);

        return newSessions;
    }

    public Map<String, Object> generateMultiDayPlan(User user) {
        List<Subject> subjects = storageService.loadSubjects(user.getId());
        Map<String, Object> response = new HashMap<>();

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> daysList = new ArrayList<>();

        double totalRequiredHours = 0;
        int maxDays = 14; // 2 week lookahead window

        for (Subject s : subjects) {
            long uncompleted = s.getTopics().stream().filter(t -> !t.isCompleted()).count();
            totalRequiredHours += uncompleted * 2.0; // Assume 2 hours per topic
        }

        double totalCapacity = user.getDailyAvailableHours() * maxDays;
        boolean overloaded = totalRequiredHours > totalCapacity;

        for (int dayOffset = 0; dayOffset < maxDays; dayOffset++) {
            LocalDate currentDate = today.plusDays(dayOffset);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", currentDate.toString());
            dayData.put("dayName", currentDate.getDayOfWeek().toString());

            List<Map<String, Object>> dayTasks = new ArrayList<>();
            double allocatedToday = 0;

            // Prioritize subjects for this specific offset
            for (Subject sub : subjects) {
                try {
                    LocalDate examDate = LocalDate.parse(sub.getExamDate());
                    if (currentDate.isAfter(examDate)) continue; // Don't study after exam date
                } catch (Exception ignored) {}

                long uncompletedCount = sub.getTopics().stream().filter(t -> !t.isCompleted()).count();
                if (uncompletedCount == 0) continue;

                if (allocatedToday < user.getDailyAvailableHours()) {
                    double duration = Math.min(1.5, user.getDailyAvailableHours() - allocatedToday);
                    allocatedToday += duration;

                    Map<String, Object> task = new HashMap<>();
                    task.put("subjectName", sub.getName());
                    task.put("durationHours", duration);
                    task.put("priorityScore", Math.round(calculateSubjectPriority(sub) * 10.0) / 10.0);
                    task.put("focus", "Topic distribution & revision");
                    dayTasks.add(task);
                }
            }

            dayData.put("tasks", dayTasks);
            dayData.put("totalHours", allocatedToday);
            daysList.add(dayData);
        }

        response.put("days", daysList);
        response.put("isOverloaded", overloaded);
        response.put("totalRequiredHours", totalRequiredHours);
        response.put("totalCapacityHours", totalCapacity);
        return response;
    }
}