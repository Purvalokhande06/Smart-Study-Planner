package src;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final FileStorageService storageService;
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>(); // token -> userId

    public AuthService(FileStorageService storageService) {
        this.storageService = storageService;
    }

    public String registerUser(String username, String password, double dailyHours) throws Exception {
        List<User> users = storageService.loadUsers();
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                throw new IllegalArgumentException("Username already registered!");
            }
        }

        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        String userId = "usr_" + System.currentTimeMillis();

        User newUser = new User(userId, username.trim(), hash, salt, dailyHours);
        users.add(newUser);
        storageService.saveUsers(users);

        return createSession(userId);
    }

    public String loginUser(String username, String password) throws Exception {
        List<User> users = storageService.loadUsers();
        User target = null;
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                target = u;
                break;
            }
        }

        if (target == null) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String calculatedHash = hashPassword(password, target.getSalt());
        if (!calculatedHash.equals(target.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        return createSession(target.getId());
    }

    public void logout(String token) {
        if (token != null) activeSessions.remove(token);
    }

    public User validateSession(String token) {
        if (token == null || !activeSessions.containsKey(token)) return null;
        String userId = activeSessions.get(token);
        List<User> users = storageService.loadUsers();
        for (User u : users) {
            if (u.getId().equals(userId)) return u;
        }
        return null;
    }

    private String createSession(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        activeSessions.put(token, userId);
        return token;
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String saltStr) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltStr);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}