package com.codeverdict.routes;

import com.codeverdict.auth.AuthService;
import com.codeverdict.database.DatabaseManager;
import com.codeverdict.database.ProblemDao;
import com.codeverdict.database.SessionDao;
import com.codeverdict.database.SubmissionDao;
import com.codeverdict.database.TestCaseDao;
import com.codeverdict.database.UserDao;
import com.codeverdict.handlers.AuthHandler;
import com.codeverdict.handlers.HealthHandler;
import com.codeverdict.handlers.LeaderboardHandler;
import com.codeverdict.handlers.ProblemHandler;
import com.codeverdict.handlers.SubmissionHandler;
import com.codeverdict.server.CorsFilter;
import com.codeverdict.services.ProblemService;
import com.codeverdict.services.SubmissionService;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.util.logging.Logger;

public final class RouteRegistry {

    private static final Logger LOGGER = Logger.getLogger(RouteRegistry.class.getName());

    private RouteRegistry() {}

    public static void registerRoutes(HttpServer server, CorsFilter corsFilter) {
        DatabaseManager db = DatabaseManager.INSTANCE;
        UserDao userDao = new UserDao(db);
        SessionDao sessionDao = new SessionDao(db);
        ProblemDao problemDao = new ProblemDao(db);
        TestCaseDao testCaseDao = new TestCaseDao(db);
        SubmissionDao submissionDao = new SubmissionDao(db);

        AuthService authService = new AuthService(userDao, sessionDao);
        ProblemService problemService = new ProblemService(problemDao, testCaseDao);
        SubmissionService submissionService = new SubmissionService(submissionDao, problemDao);

        AuthHandler authHandler = new AuthHandler(); 
        ProblemHandler problemHandler = new ProblemHandler(problemService, authService);
        SubmissionHandler submissionHandler = new SubmissionHandler(submissionService, authService);
        LeaderboardHandler leaderboardHandler = new LeaderboardHandler(submissionService);

        String adminEmail = com.codeverdict.utils.EnvConfig.get("ADMIN_EMAIL");
        String adminPassword = com.codeverdict.utils.EnvConfig.get("ADMIN_PASSWORD");
        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
            String adminUser = com.codeverdict.utils.EnvConfig.get("ADMIN_USERNAME", "superadmin");
            authService.provisionAdminIfMissing(adminUser, adminEmail, adminPassword);
        }

        com.codeverdict.server.RequestLoggingFilter logFilter = new com.codeverdict.server.RequestLoggingFilter();

        register(server, corsFilter, logFilter, "/api/auth/signup",  authHandler);
        register(server, corsFilter, logFilter, "/api/auth/login",   authHandler);
        register(server, corsFilter, logFilter, "/api/auth/logout",  authHandler);

        register(server, corsFilter, logFilter, "/api/problems",     problemHandler);
        
        register(server, corsFilter, logFilter, "/api/submit",       submissionHandler);
        register(server, corsFilter, logFilter, "/api/submissions",  submissionHandler);

        register(server, corsFilter, logFilter, "/api/leaderboard",  leaderboardHandler);

        register(server, corsFilter, logFilter, "/api/health",       new HealthHandler());

        LOGGER.info("All routes registered successfully.");
    }

    private static void register(HttpServer server, CorsFilter corsFilter, com.codeverdict.server.RequestLoggingFilter logFilter, String path, HttpHandler handler) {
        HttpContext ctx = server.createContext(path, handler);
        ctx.getFilters().add(logFilter);
        ctx.getFilters().add(corsFilter);
        LOGGER.info(() -> "Registered route: " + path + " -> " + handler.getClass().getSimpleName());
    }
}
