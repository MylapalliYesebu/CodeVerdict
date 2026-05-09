package com.codeverdict.auth;

import com.codeverdict.database.SessionDao;
import com.codeverdict.database.UserDao;
import com.codeverdict.models.Session;
import com.codeverdict.models.User;
import com.codeverdict.utils.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;
    private StubUserDao userDao;
    private StubSessionDao sessionDao;

    @BeforeEach
    void setUp() {
        userDao = new StubUserDao();
        sessionDao = new StubSessionDao();
        authService = new AuthService(userDao, sessionDao);
    }

    @Test
    @DisplayName("signup with valid data -> success")
    void shouldReturnUser_whenSignupWithValidData() {
        User user = authService.signup("alice", "alice@example.com", "Password123");
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertTrue(user.getId() > 0);
    }

    @Test
    @DisplayName("signup with duplicate email -> exception")
    void shouldThrowException_whenSignupWithDuplicateEmail() {
        authService.signup("alice", "alice@example.com", "Password123");
        assertThrows(RuntimeException.class, () -> {
            authService.signup("bob", "alice@example.com", "Password456");
        });
    }

    @Test
    @DisplayName("signup with weak password -> throws ValidationException")
    void shouldThrowValidationException_whenSignupWithWeakPassword() {
        assertThrows(ValidationException.class, () -> {
            authService.signup("alice", "alice@example.com", "weak");
        });
    }

    @Test
    @DisplayName("login with correct credentials -> returns token string")
    void shouldReturnToken_whenLoginWithCorrectCredentials() {
        authService.signup("alice", "alice@example.com", "Password123");
        String token = authService.login("alice@example.com", "Password123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("login with wrong password -> throws AuthException")
    void shouldThrowAuthException_whenLoginWithWrongPassword() {
        authService.signup("alice", "alice@example.com", "Password123");
        assertThrows(AuthException.class, () -> {
            authService.login("alice@example.com", "WrongPassword");
        });
    }

    @Test
    @DisplayName("login with nonexistent email -> throws AuthException")
    void shouldThrowAuthException_whenLoginWithNonexistentEmail() {
        assertThrows(AuthException.class, () -> {
            authService.login("nobody@example.com", "Password123");
        });
    }

    // In-memory stubs
    private static class StubUserDao extends UserDao {
        private final Map<Long, User> byId = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();
        private final Map<String, User> byUsername = new HashMap<>();
        private long idCounter = 1;

        public StubUserDao() {
            super(null); // No DB connection needed
        }

        @Override
        public long createUser(User user) {
            if (byEmail.containsKey(user.getEmail())) throw new RuntimeException("duplicate email");
            if (byUsername.containsKey(user.getUsername())) throw new RuntimeException("duplicate username");
            
            user.setId(idCounter++);
            byId.put(user.getId(), user);
            byEmail.put(user.getEmail(), user);
            byUsername.put(user.getUsername(), user);
            return user.getId();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    private static class StubSessionDao extends SessionDao {
        public StubSessionDao() {
            super(null);
        }

        @Override
        public void createSession(long userId, String token, java.time.Instant expiresAt) {
        }

        @Override
        public Optional<Session> findSessionByToken(String token) {
            return Optional.empty();
        }

        @Override
        public void deleteSession(String token) {
        }
    }
}
