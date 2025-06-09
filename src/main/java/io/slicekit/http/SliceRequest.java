package io.slicekit.http;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an HTTP request to a slice handler.
 * 
 * Provides convenient access to request data including headers,
 * query parameters, path parameters, and request body.
 */
public final class SliceRequest {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceRequest.class);
    
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParameters;
    private final String body;
    private final JsonService jsonService;
    
    /**
     * Creates a slice request from an HTTP server exchange.
     * 
     * @param exchange HTTP server exchange, must not be null
     * @param jsonService JSON service for deserialization, must not be null
     * @throws IOException if reading request body fails
     * @throws IllegalArgumentException if exchange or jsonService is null
     */
    public SliceRequest(final HttpServerExchange exchange, final JsonService jsonService) throws IOException {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        this.jsonService = Objects.requireNonNull(jsonService, "JSON service cannot be null");
        
        this.method = exchange.getRequestMethod().toString();
        this.path = exchange.getRequestPath();
        this.headers = extractHeaders(exchange);
        this.queryParameters = extractQueryParameters(exchange);
        this.body = extractBody(exchange);
    }
    
    /**
     * Gets the HTTP method.
     * 
     * @return HTTP method (GET, POST, etc.)
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * Gets the request path.
     * 
     * @return request path (e.g., "/api/orders")
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Gets all request headers.
     * 
     * @return unmodifiable map of headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Gets a specific header value.
     * 
     * @param name header name, case-insensitive
     * @return header value if present, empty Optional otherwise
     */
    public Optional<String> getHeader(final String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(headers.get(name.toLowerCase()));
    }
    
    /**
     * Gets all query parameters.
     * 
     * @return unmodifiable map of query parameters
     */
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }
    
    /**
     * Gets a specific query parameter value.
     * 
     * @param name parameter name
     * @return parameter value if present, empty Optional otherwise
     */
    public Optional<String> getQueryParameter(final String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(queryParameters.get(name));
    }
    
    /**
     * Gets the raw request body.
     * 
     * @return request body as string, empty string if no body
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Checks if the request has a body.
     * 
     * @return true if request has non-empty body, false otherwise
     */
    public boolean hasBody() {
        return body != null && !body.trim().isEmpty();
    }
    
    /**
     * Deserializes the request body to the specified type.
     * 
     * @param targetType target class type, must not be null
     * @param <T> target type
     * @return deserialized object
     * @throws io.slicekit.core.SliceKitException.SliceExecutionException if deserialization fails
     * @throws IllegalArgumentException if targetType is null or request has no body
     */
    public <T> T getBodyAs(final Class<T> targetType) {
        Objects.requireNonNull(targetType, "Target type cannot be null");
        
        if (!hasBody()) {
            throw new IllegalArgumentException("Request has no body to deserialize");
        }
        
        return jsonService.fromJson(body, targetType);
    }
    
    /**
     * Gets the content type of the request.
     * 
     * @return content type if present, empty Optional otherwise
     */
    public Optional<String> getContentType() {
        return getHeader("content-type");
    }
    
    /**
     * Checks if the request content type is JSON.
     * 
     * @return true if content type is application/json, false otherwise
     */
    public boolean isJsonContent() {
        return getContentType()
            .map(ct -> ct.toLowerCase().contains("application/json"))
            .orElse(false);
    }
    
    /**
     * Extracts headers from the HTTP exchange.
     */
    private static Map<String, String> extractHeaders(final HttpServerExchange exchange) {
        final Map<String, String> headers = new HashMap<>();
        
        exchange.getRequestHeaders().forEach(header -> {
            final String name = header.getHeaderName().toString().toLowerCase();
            final String value = header.getFirst();
            if (value != null) {
                headers.put(name, value);
            }
        });
        
        return Collections.unmodifiableMap(headers);
    }
    
    /**
     * Extracts query parameters from the HTTP exchange.
     */
    private static Map<String, String> extractQueryParameters(final HttpServerExchange exchange) {
        final Map<String, String> params = new HashMap<>();
        
        exchange.getQueryParameters().forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
                params.put(name, values.getFirst());
            }
        });
        
        return Collections.unmodifiableMap(params);
    }
    
    /**
     * Extracts the request body from the HTTP exchange.
     * 
     * For GET requests and other methods without body, returns empty string.
     */
    private static String extractBody(final HttpServerExchange exchange) throws IOException {
        // For methods that typically don't have a body, return empty string
        final String method = exchange.getRequestMethod().toString();
        if ("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
            logger.debug("Method {} typically has no body, returning empty string", method);
            return "";
        }
        
        // For methods that can have a body, check content length
        final long contentLength = exchange.getRequestContentLength();
        if (contentLength == 0) {
            logger.debug("Content-Length is 0, returning empty string");
            return "";
        }
        
        if (!exchange.isBlocking()) {
            exchange.startBlocking();
        }
        
        try {
            final byte[] bodyBytes = exchange.getInputStream().readAllBytes();
            final String body = new String(bodyBytes, StandardCharsets.UTF_8);
            
            logger.debug("Extracted request body ({} bytes): {}", 
                bodyBytes.length, body.length() > 200 ? body.substring(0, 200) + "..." : body);
            
            return body;
            
        } catch (IOException e) {
            logger.error("Failed to read request body: {}", e.getMessage());
            throw e;
        }
    }
    
    @Override
    public String toString() {
        return "SliceRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers.size() +
                ", queryParams=" + queryParameters.size() +
                ", hasBody=" + hasBody() +
                '}';
    }
}