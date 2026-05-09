package com.codeverdict.server;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * {@link Filter} implementation that adds CORS (Cross-Origin Resource Sharing)
 * headers to every HTTP response served by the CodeVerdict backend.
 *
 * <p>This filter must be added to each {@link com.sun.net.httpserver.HttpContext}
 * registered on the server.  Example:
 * <pre>
 *   HttpContext ctx = server.createContext("/api/problems", handler);
 *   ctx.getFilters().add(new CorsFilter());
 * </pre>
 *
 * <p><strong>Preflight requests</strong> (HTTP {@code OPTIONS}) are short-circuited:
 * a {@code 204 No Content} response is sent immediately and the handler chain
 * is <em>not</em> invoked, avoiding unnecessary processing.
 */
public class CorsFilter extends Filter {

    private static final Logger LOGGER = Logger.getLogger(CorsFilter.class.getName());

    // ------------------------------------------------------------------ //
    //  CORS header names and values                                        //
    // ------------------------------------------------------------------ //

    private static final String HEADER_ALLOW_ORIGIN  = "Access-Control-Allow-Origin";
    private static final String HEADER_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String HEADER_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static final String VALUE_ALLOW_ORIGIN  = "*";
    private static final String VALUE_ALLOW_METHODS = "GET,POST,PUT,DELETE,OPTIONS";
    private static final String VALUE_ALLOW_HEADERS = "Content-Type,Authorization";

    /**
     * Applies CORS headers to the response and, for {@code OPTIONS} preflight
     * requests, terminates the filter chain with a {@code 204 No Content}.
     *
     * @param exchange the current HTTP exchange
     * @param chain    the remaining filter chain to invoke for non-preflight requests
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        // Add CORS headers to every response
        exchange.getResponseHeaders().set(HEADER_ALLOW_ORIGIN,  VALUE_ALLOW_ORIGIN);
        exchange.getResponseHeaders().set(HEADER_ALLOW_METHODS, VALUE_ALLOW_METHODS);
        exchange.getResponseHeaders().set(HEADER_ALLOW_HEADERS, VALUE_ALLOW_HEADERS);

        // Short-circuit OPTIONS preflight — respond 204 and stop chain
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            LOGGER.fine("CORS preflight intercepted — responding 204.");
            exchange.sendResponseHeaders(204, -1); // -1 = no body
            exchange.close();
            return; // do NOT call chain.doFilter()
        }

        // Pass non-preflight requests through to the actual handler
        chain.doFilter(exchange);
    }

    /**
     * Returns a human-readable name for this filter, used in server logs.
     *
     * @return {@code "CORS Filter"}
     */
    @Override
    public String description() {
        return "CORS Filter";
    }
}
