package io.slicekit.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Handles HTTP response serialization and sending.
 * 
 * Responsible for:
 * - Serializing slice handler results to JSON
 * - Setting appropriate HTTP headers and status codes
 * - Handling null responses (204 No Content)
 * - Logging response details
 */
public final class ResponseSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseSerializer.class);
    
    private final JsonService jsonService;
    
    /**
     * Creates a new response serializer.
     * 
     * @param jsonService JSON serialization service, must not be null
     * @throws IllegalArgumentException if jsonService is null
     */
    public ResponseSerializer(final JsonService jsonService) {
        this.jsonService = Objects.requireNonNull(jsonService, "JSON service cannot be null");
    }
    
    /**
     * Handles the response from a slice handler.
     * 
     * @param exchange HTTP server exchange, must not be null
     * @param result result object from slice handler (may be null)
     * @throws Exception if response serialization fails
     */
    public void handleSliceResponse(final HttpServerExchange exchange, final Object result) throws Exception {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        if (result == null) {
            handleNullResponse(exchange);
        } else {
            handleObjectResponse(exchange, result);
        }
    }
    
    /**
     * Handles null response with HTTP 204 No Content.
     */
    private void handleNullResponse(final HttpServerExchange exchange) {
        logger.debug("Sending No Content response");
        exchange.setStatusCode(StatusCodes.NO_CONTENT);
        exchange.endExchange();
    }
    
    /**
     * Handles object response by serializing to JSON and sending with HTTP 200.
     */
    private void handleObjectResponse(final HttpServerExchange exchange, final Object result) throws Exception {
        try {
            final String jsonResponse = jsonService.toJson(result);
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().send(jsonResponse);
            
            logger.debug("Sent JSON response: {}", 
                jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
            
        } catch (Exception e) {
            logger.error("Failed to serialize response to JSON: {}", e.getMessage(), e);
            throw e; // Let the caller handle this error
        }
    }
}