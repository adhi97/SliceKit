package io.slicekit.http;

import io.slicekit.core.SliceKitException;
import io.slicekit.discovery.SliceRegistry;
import io.slicekit.injection.SliceInjector;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * HTTP server implementation using Undertow.
 * 
 * Handles incoming HTTP requests and routes them to appropriate slice handlers
 * through the SliceRequestHandler.
 */
public final class SliceHttpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceHttpServer.class);
    
    private final int port;
    private final String host;
    private final SliceRegistry sliceRegistry;
    private final SliceRequestHandler requestHandler;
    
    private Undertow server;
    private boolean started = false;
    
    /**
     * Creates a new HTTP server instance.
     * 
     * @param port server port, must be between 1 and 65535
     * @param host server host, must not be null or empty
     * @param sliceRegistry slice registry for route resolution, must not be null
     * @param sliceInjector dependency injector for slices, must not be null
     * @throws IllegalArgumentException if parameters are invalid
     */
    public SliceHttpServer(final int port, final String host, final SliceRegistry sliceRegistry, final SliceInjector sliceInjector) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        
        this.port = port;
        this.host = host.trim();
        this.sliceRegistry = Objects.requireNonNull(sliceRegistry, "Slice registry cannot be null");
        this.requestHandler = new SliceRequestHandler(sliceRegistry, sliceInjector);
    }
    
    /**
     * Convenience constructor for localhost.
     * 
     * @param port server port
     * @param sliceRegistry slice registry
     * @param sliceInjector dependency injector
     */
    public SliceHttpServer(final int port, final SliceRegistry sliceRegistry, final SliceInjector sliceInjector) {
        this(port, "localhost", sliceRegistry, sliceInjector);
    }
    
    /**
     * Starts the HTTP server.
     * 
     * @throws SliceKitException.SliceExecutionException if server fails to start
     * @throws IllegalStateException if server is already started
     */
    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Server is already started");
        }
        
        try {
            logger.info("Starting SliceKit HTTP server on {}:{}", host, port);
            
            server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(new RootHttpHandler())
                .build();
            
            server.start();
            started = true;
            
            logger.info("SliceKit HTTP server started successfully on {}:{}", host, port);
            logger.info("Registered {} slices: {}", 
                sliceRegistry.size(), sliceRegistry.getAllRouteKeys());
            
        } catch (Exception e) {
            throw new SliceKitException.SliceExecutionException(
                "Failed to start HTTP server on " + host + ":" + port, e);
        }
    }
    
    /**
     * Stops the HTTP server.
     * 
     * @throws IllegalStateException if server is not started
     */
    public synchronized void stop() {
        if (!started) {
            throw new IllegalStateException("Server is not started");
        }
        
        logger.info("Stopping SliceKit HTTP server");
        
        if (server != null) {
            server.stop();
            server = null;
        }
        
        started = false;
        logger.info("SliceKit HTTP server stopped");
    }
    
    /**
     * Checks if the server is currently running.
     * 
     * @return true if server is started, false otherwise
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * Gets the server port.
     * 
     * @return server port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Gets the server host.
     * 
     * @return server host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Root HTTP handler that delegates all requests to the slice request handler.
     */
    private final class RootHttpHandler implements HttpHandler {
        
        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {
            // Set CORS headers for development (should be configurable in production)
            exchange.getResponseHeaders().put(io.undertow.util.HttpString.tryFromString("Access-Control-Allow-Origin"), "*");
            exchange.getResponseHeaders().put(io.undertow.util.HttpString.tryFromString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().put(io.undertow.util.HttpString.tryFromString("Access-Control-Allow-Headers"), "Content-Type, Authorization");
            
            // Handle preflight OPTIONS requests
            if ("OPTIONS".equals(exchange.getRequestMethod().toString())) {
                exchange.setStatusCode(204);
                exchange.endExchange();
                return;
            }
            
            // Move request handling to worker thread to avoid blocking I/O on I/O thread
            if (exchange.isInIoThread()) {
                exchange.dispatch(this);
                return;
            }
            
            // Delegate to slice request handler
            requestHandler.handleRequest(exchange);
        }
    }
}