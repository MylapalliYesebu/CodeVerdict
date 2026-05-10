package com.codeverdict.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.codeverdict.database.SessionDao;
import com.codeverdict.database.UserDao;
import com.codeverdict.models.Session;
import com.codeverdict.models.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for handling user authentication and session management.
 * 
 * THREAD-SAFETY: Accessed by multiple request threads.
 * Using stateless service design and UUID.randomUUID() — UUID uses SecureRandom internally which is thread-safe.
 */
public class AuthService {

    private final UserDao userDao;
    private final SessionDao sessionDao;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,20}$");

    public AuthService(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    /**
     * Registers a new user.
     *
     * @param username    the desired username
     * @param email       the user's email address
     * @param rawPassword the user's raw password
     * @return the created User object (with passwordHash removed for safety)
     * @throws IllegalArgumentException if validation fails or username/email is taken
     */
    public User signup(String rawUsername, String rawEmail, String rawPassword) {
        String username = rawUsername != null ? rawUsername.trim() : null;
        String email = rawEmail != null ? rawEmail.trim().toLowerCase() : null;

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new com.codeverdict.utils.ValidationException("Username must be 3-20 alphanumeric characters");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new com.codeverdict.utils.ValidationException("Invalid email format");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new com.codeverdict.utils.ValidationException("Password must be at least 8 characters");
        }

        if (userDao.findByUsername(username).isPresent() || userDao.findByEmail(email).isPresent()) {
            throw new RuntimeException("Duplicate username or email");
        }

        String passwordHash = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole("USER"); // Default role

        long userId = userDao.createUser(user);

        return userDao.findById(userId).map(u -> {
            User safeUser = new User();
            safeUser.setId(u.getId());
            safeUser.setUsername(u.getUsername());
            safeUser.setEmail(u.getEmail());
            safeUser.setRole(u.getRole());
            safeUser.setCreatedAt(u.getCreatedAt());
            return safeUser;
        }).orElseThrow(() -> new RuntimeException("Failed to retrieve created user"));
    }

    /**
     * Authenticates a user and creates a session.
     *
     * @param email       the user's email
     * @param rawPassword the user's raw password
     * @return the generated session token
     * @throws AuthException if authentication fails
     */
    public String login(String rawEmail, String rawPassword) {
        String email = rawEmail != null ? rawEmail.trim().toLowerCase() : null;

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), user.getPasswordHash());
        if (!result.verified) {
            throw new AuthException("Invalid email or password");
        }

        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

        sessionDao.createSession(user.getId(), token, expiresAt);

        return token;
    }

    /**
     * Invalidate a session token.
     *
     * @param token the session token to delete
     */
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessionDao.deleteSession(token);
        }
    }

    /**
     * Validates a session token and returns the authenticated user.
     *
     * @param token the session token
     * @return an Optional containing the User if the token is valid, empty otherwise
     */
    public Optional<User> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<Session> sessionOpt = sessionDao.findSessionByToken(token);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }

        Session session = sessionOpt.get();
        Instant expiresAt = Instant.parse(session.getExpiresAt());

        if (Instant.now().isAfter(expiresAt)) {
            sessionDao.deleteSession(token);
            return Optional.empty();
        }

        return userDao.findById(session.getUserId());
    }

    /**
     * Seeds a default admin user if one with the given email does not already exist.
     * 
     * @param username    the admin username
     * @param email       the admin email
     * @param rawPassword the admin raw password
     */
    public void provisionAdminIfMissing(String rawUsername, String rawEmail, String rawPassword) {
        String username = rawUsername != null ? rawUsername.trim() : null;
        String email = rawEmail != null ? rawEmail.trim().toLowerCase() : null;

        if (userDao.findByEmail(email).isEmpty()) {
            String passwordHash = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
            User admin = new User();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPasswordHash(passwordHash);
            admin.setRole("ADMIN");
            userDao.createUser(admin);
            System.out.println("[INFO] Provisioned default ADMIN user: " + email);
        } else {
            // Optional: If you want to ensure the existing user definitely has the ADMIN role,
            // you would do an update here. For now, we assume if the email exists, it's fine.
            System.out.println("[INFO] Default ADMIN user already exists: " + email);
        }
    }
}
