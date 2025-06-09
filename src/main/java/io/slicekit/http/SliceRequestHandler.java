package io.slicekit.http;

import io.slicekit.core.SliceMetadata;
import io.slicekit.discovery.SliceRegistry;
import io.slicekit.injection.SliceInjector;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles HTTP requests by routing them to appropriate slice handlers.
 * 
 * This class is responsible for:
 * - Matching incoming requests to registered slices
 * - Handling 404 responses for unmatched routes
 * - Basic error handling and logging
 */
public final class SliceRequestHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceRequestHandler.class);
    
    private final SliceRegistry sliceRegistry;
    private final JsonService jsonService;
    private final SliceInjector sliceInjector;
    
    /**
     * Creates a new slice request handler.
     * 
     * @param sliceRegistry slice registry for route resolution, must not be null
     * @param sliceInjector dependency injector for slices, must not be null
     * @throws IllegalArgumentException if parameters are null
     */
    public SliceRequestHandler(final SliceRegistry sliceRegistry, final SliceInjector sliceInjector) {
        this.sliceRegistry = Objects.requireNonNull(sliceRegistry, "Slice registry cannot be null");
        this.sliceInjector = Objects.requireNonNull(sliceInjector, "Slice injector cannot be null");
        this.jsonService = JsonService.getDefault();
    }
    
    /**
     * Handles an incoming HTTP request.
     * 
     * @param exchange HTTP server exchange containing request/response, must not be null
     * @throws Exception if request handling fails
     */
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
        
        final String method = exchange.getRequestMethod().toString();
        final String path = exchange.getRequestPath();
        
        logger.debug("Handling request: {} {}", method, path);
        
        try {
            // Find matching slice
            final Optional<SliceMetadata> sliceMetadata = sliceRegistry.findSlice(method, path);
            
            if (sliceMetadata.isPresent()) {
                handleSliceRequest(exchange, sliceMetadata.get());
            } else {
                handleNotFound(exchange, method, path);
            }
            
        } catch (Exception e) {
            logger.error("Error handling request {} {}: {}", method, path, e.getMessage(), e);
            handleInternalError(exchange, e);
        }
    }
    
    /**
     * Handles a request that matches a registered slice.
     */
    private void handleSliceRequest(final HttpServerExchange exchange, final SliceMetadata sliceMetadata) 
            throws Exception {
        
        logger.debug("Routing request to slice: {}", sliceMetadata.getName());
        
        try {
            // Create slice request wrapper
            final SliceRequest request = new SliceRequest(exchange, jsonService);
            
            // Create slice instance using dependency injection
            final Object sliceInstance = sliceInjector.createSliceInstance(sliceMetadata);
            
            // Prepare method arguments
            final Object[] args = prepareMethodArguments(sliceMetadata.getHandlerMethod(), request);
            
            // Invoke handler method
            final Object result = sliceMetadata.getHandlerMethod().invoke(sliceInstance, args);
            
            // Handle response
            handleSliceResponse(exchange, result);
            
        } catch (Exception e) {
            logger.error("Error executing slice {}: {}", sliceMetadata.getName(), e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Handles the response from a slice handler.
     */
    private void handleSliceResponse(final HttpServerExchange exchange, final Object result) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        if (result == null) {
            // No content response
            exchange.setStatusCode(StatusCodes.NO_CONTENT);
            exchange.endExchange();
        } else {
            // Serialize result to JSON
            try {
                final String jsonResponse = jsonService.toJson(result);
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send(jsonResponse);
                
                logger.debug("Sent JSON response: {}", 
                    jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
                
            } catch (Exception e) {
                logger.error("Failed to serialize response to JSON: {}", e.getMessage(), e);
                handleInternalError(exchange, e);
            }
        }
    }
    
    /**
     * Handles requests that don't match any registered slice.
     */
    private void handleNotFound(final HttpServerExchange exchange, final String method, final String path) {
        logger.debug("No slice found for: {} {}", method, path);
        
        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        final String errorResponse = String.format(
            "{\"error\":\"Not Found\",\"message\":\"No slice registered for %s %s\",\"availableRoutes\":%s}",
            method, path, formatAvailableRoutes()
        );
        
        exchange.getResponseSender().send(errorResponse);
    }
    
    /**
     * Handles internal server errors.
     */
    private void handleInternalError(final HttpServerExchange exchange, final Exception error) {
        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        
        final String errorResponse = String.format(
            "{\"error\":\"Internal Server Error\",\"message\":\"%s\"}",
            error.getMessage().replace("\"", "\\\"")
        );
        
        exchange.getResponseSender().send(errorResponse);
    }
    
    /**
     * Formats available routes for error responses.
     */
    private String formatAvailableRoutes() {
        final StringBuilder routes = new StringBuilder("[");
        boolean first = true;
        
        for (final String route : sliceRegistry.getAllRouteKeys()) {
            if (!first) {
                routes.append(",");
            }
            routes.append("\"").append(route).append("\"");
            first = false;
        }
        
        routes.append("]");
        return routes.toString();
    }
    
    /**
     * Prepares method arguments for slice handler invocation.
     * 
     * Supports zero arguments or single parameter (request body deserialization).
     */
    private Object[] prepareMethodArguments(final Method handlerMethod, final SliceRequest request) {
        final Parameter[] parameters = handlerMethod.getParameters();
        
        if (parameters.length == 0) {
            // No parameters - return empty array
            return new Object[0];
        }
        
        if (parameters.length == 1) {
            // Single parameter - attempt to deserialize request body
            final Parameter parameter = parameters[0];
            final Class<?> parameterType = parameter.getType();
            
            // Special handling for SliceRequest type
            if (parameterType.equals(SliceRequest.class)) {
                return new Object[]{request};
            }
            
            // For other types, try to deserialize from request body
            if (request.hasBody() && request.isJsonContent()) {
                try {
                    final Object arg = request.getBodyAs(parameterType);
                    return new Object[]{arg};
                } catch (Exception e) {
                    logger.error("Failed to deserialize request body to {}: {}", 
                        parameterType.getSimpleName(), e.getMessage());
                    throw new IllegalArgumentException(
                        "Failed to deserialize request body to " + parameterType.getSimpleName(), e);
                }
            } else {
                // No body or not JSON - pass null for MVP
                logger.warn("Handler method expects parameter of type {} but request has no JSON body", 
                    parameterType.getSimpleName());
                return new Object[]{null};
            }
        }
        
        // Multiple parameters not supported in MVP
        throw new IllegalArgumentException(
            "Handler methods with multiple parameters are not supported in MVP. Found " + 
            parameters.length + " parameters in method " + handlerMethod.getName());
    }
}