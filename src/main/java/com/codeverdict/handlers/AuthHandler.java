package com.codeverdict.handlers;

import com.codeverdict.auth.AuthException;
import com.codeverdict.auth.AuthService;
import com.codeverdict.database.DatabaseManager;
import com.codeverdict.database.SessionDao;
import com.codeverdict.database.UserDao;
import com.codeverdict.models.User;
import com.codeverdict.utils.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Handles authentication-related API routes.
 *
 * <ul>
 *   <li>{@code POST /api/auth/signup}  — register a new user</li>
 *   <li>{@code POST /api/auth/login}   — authenticate and issue a session token</li>
 *   <li>{@code POST /api/auth/logout}  — invalidate the current session</li>
 * </ul>
 */
public class AuthHandler extends BaseHandler {

    private final AuthService authService;

    public AuthHandler() {
        UserDao userDao = new UserDao(DatabaseManager.INSTANCE);
        SessionDao sessionDao = new SessionDao(DatabaseManager.INSTANCE);
        this.authService = new AuthService(userDao, sessionDao);
    }

    private static class SignupRequest {
        String username;
        String email;
        String password;
    }

    private static class LoginRequest {
        String email;
        String password;
    }

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        String path   = ex.getRequestURI().getPath();
        String method = getMethod(ex);

        if (path.endsWith("/signup") && "POST".equals(method)) {
            handleSignup(ex);
        } else if (path.endsWith("/login") && "POST".equals(method)) {
            handleLogin(ex);
        } else if (path.endsWith("/logout") && "POST".equals(method)) {
            handleLogout(ex);
        } else {
            sendError(ex, 405, "Method not allowed");
        }
    }

    private void handleSignup(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        SignupRequest req;
        try {
            req = JsonUtil.fromJson(body, SignupRequest.class);
            if (req == null || req.username == null || req.email == null || req.password == null) {
                throw new IllegalArgumentException("Malformed JSON or missing fields");
            }
        } catch (Exception e) {
            sendError(ex, 400, "Malformed JSON");
            return;
        }

        User user = authService.signup(req.username, req.email, req.password);
        String response = String.format("{\"message\":\"User created\",\"userId\":%d}", user.getId());
        sendResponse(ex, 201, response);
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        LoginRequest req;
        try {
            req = JsonUtil.fromJson(body, LoginRequest.class);
            if (req == null || req.email == null || req.password == null) {
                throw new IllegalArgumentException("Malformed JSON or missing fields");
            }
        } catch (Exception e) {
            sendError(ex, 400, "Malformed JSON");
            return;
        }

        String token = authService.login(req.email, req.password);
        User user = authService.validateToken(token)
                .orElseThrow(() -> new AuthException("Session creation failed"));

        String response = String.format("{\"token\":\"%s\",\"userId\":%d,\"username\":\"%s\"}",
                token, user.getId(), user.getUsername());
        sendResponse(ex, 200, response);
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(ex, 401, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7).trim();
        authService.logout(token);
        sendResponse(ex, 200, "{\"message\":\"Logged out successfully\"}");
    }
}
