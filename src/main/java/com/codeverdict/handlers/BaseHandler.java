package com.codeverdict.handlers;

import com.codeverdict.server.ServerConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Abstract base class for all CodeVerdict HTTP handlers.
 *
 * <p>Provides a set of protected helper utilities for reading request bodies,
 * writing JSON responses, and extracting path parameters so concrete handlers
 * can focus on business logic rather than HTTP boilerplate.
 *
 * <p>All responses are written as {@code application/json} and the
 * {@link HttpExchange} is always closed in a {@code finally} block, even if
 * an exception is thrown while building the response.
 *
 * <p>Subclasses must implement {@link #handle(HttpExchange)} from
 * {@link HttpHandler} and dispatch on {@link #getMethod(HttpExchange)} as
 * appropriate.
 */
public abstract class BaseHandler implements HttpHandler {

    /**
     * Shared logger, available to every concrete handler subclass.
     */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            doHandle(ex);
        } catch (Throwable t) {
            com.codeverdict.utils.GlobalExceptionMapper.handle(ex, t);
        }
    }

    /**
     * Concrete handlers implement this method instead of handle() directly.
     */
    protected abstract void doHandle(HttpExchange ex) throws Exception;

    // ---------------------------------------------------------------------- //
    //  Response helpers                                                        //
    // ---------------------------------------------------------------------- //

    /**
     * Writes a JSON response to the client.
     *
     * <p>The exchange is closed in a {@code finally} block regardless of
     * whether an I/O error occurs while writing.
     *
     * @param ex       the current HTTP exchange
     * @param status   HTTP status code (e.g. 200, 201, 400)
     * @param jsonBody response body; {@code null} is treated as an empty string
     * @throws IOException if the response headers or body cannot be written
     */
    protected void sendResponse(HttpExchange ex, int status, String jsonBody) throws IOException {
        String body = (jsonBody != null) ? jsonBody : "";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        } finally {
            ex.close();
        }
    }

    /**
     * Writes a standardised JSON error response.
     *
     * <p>The body format is: {@code {"error":"<message>","status":<code>}}
     *
     * @param ex      the current HTTP exchange
     * @param status  HTTP status code (e.g. 400, 401, 404, 500)
     * @param message human-readable error description
     * @throws IOException if the response cannot be written
     */
    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        // Escape any double-quotes inside the message to keep JSON valid
        String safe = (message != null) ? message.replace("\"", "'") : "Unknown error";
        String body = String.format("{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}", 
                safe, status, java.time.Instant.now().toString());
        sendResponse(ex, status, body);
    }

    // ---------------------------------------------------------------------- //
    //  Request helpers                                                         //
    // ---------------------------------------------------------------------- //

    /**
     * Reads the entire request body as a UTF-8 string.
     *
     * <p>Returns an empty string if the body is absent or has zero length —
     * this method never throws a {@link NullPointerException} for empty bodies.
     *
     * @param ex the current HTTP exchange
     * @return the request body as a string, never {@code null}
     * @throws IOException if the body exceeds {@link ServerConfig#MAX_REQUEST_BODY}
     *                     or an I/O error occurs while reading
     */
    protected String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            if (is == null) {
                return "";
            }
            byte[] buffer = new byte[ServerConfig.MAX_REQUEST_BODY + 1];
            int totalRead = 0;
            int bytesRead;

            while ((bytesRead = is.read(buffer, totalRead, buffer.length - totalRead)) != -1) {
                totalRead += bytesRead;
                if (totalRead > ServerConfig.MAX_REQUEST_BODY) {
                    throw new IOException("Request body exceeds maximum allowed size of "
                            + ServerConfig.MAX_REQUEST_BODY + " bytes.");
                }
            }
            return new String(buffer, 0, totalRead, StandardCharsets.UTF_8);
        }
    }

    /**
     * Extracts the trailing path segment after {@code basePath}.
     *
     * <p>Example: for {@code basePath="/api/problems"} and a request URI of
     * {@code /api/problems/42}, this returns {@code "42"}.
     *
     * @param ex       the current HTTP exchange
     * @param basePath the registered context path (e.g. {@code "/api/problems"})
     * @return the trailing segment, or {@code null} if there is no trailing
     *         segment or the trailing part is blank
     */
    protected String getPathParam(HttpExchange ex, String basePath) {
        String rawPath = ex.getRequestURI().getPath();
        if (rawPath == null || !rawPath.startsWith(basePath)) {
            return null;
        }
        String suffix = rawPath.substring(basePath.length());
        // Strip leading slash, e.g. "/42" -> "42"
        if (suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }
        return suffix.isBlank() ? null : suffix;
    }

    /**
     * Returns the HTTP method of the current request in upper-case.
     *
     * @param ex the current HTTP exchange
     * @return HTTP method string, e.g. {@code "GET"}, {@code "POST"}
     */
    protected String getMethod(HttpExchange ex) {
        return ex.getRequestMethod().toUpperCase();
    }

    // Method removed: isAuthenticated was a stub. Authorization is handled specifically inside concrete handlers.

    /**
     * Checks if the client has exceeded the rate limit.
     * If so, sends a 429 response and returns false.
     * Otherwise returns true.
     */
    protected boolean checkRateLimit(HttpExchange ex) throws IOException {
        String ip = com.codeverdict.utils.RateLimiter.getInstance().getClientIp(ex);
        if (!com.codeverdict.utils.RateLimiter.getInstance().tryAcquire(ip)) {
            sendError(ex, 429, "Too many requests. Please slow down.");
            return false;
        }
        return true;
    }

    /**
     * Checks if POST/PUT requests have the correct Content-Type.
     * Returns true if valid or not required. Returns false and sends 415 error if invalid.
     */
    protected boolean validateContentType(HttpExchange ex) throws IOException {
        String method = getMethod(ex);
        if ("POST".equals(method) || "PUT".equals(method)) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("application/json")) {
                sendError(ex, 415, "Content-Type must be application/json");
                return false;
            }
        }
        return true;
    }
}
