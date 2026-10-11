package src;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class StudyPlannerServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            System.out.println("==================================================");
            System.out.println("  Smart Study Planner Server - Initializing...   ");
            System.out.println("==================================================");

            FileStorageService storageService = new FileStorageService();
            AuthService authService = new AuthService(storageService);
            StudyPlanService planService = new StudyPlanService(storageService);

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new ApiHandler(authService, storageService, planService));
            
            // Multi-threaded execution model
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("[SUCCESS] Server running locally at: http://localhost:" + PORT);
            System.out.println("[INFO] Open your web browser and navigate to http://localhost:" + PORT);
            System.out.println("[INFO] Press CTRL+C in this terminal to stop the server.");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}