package io.slicekit.examples;

import io.slicekit.core.SliceKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example application demonstrating SliceKit MVP functionality.
 * 
 * This application starts a SliceKit server with example slices to
 * demonstrate the framework's capabilities.
 */
public final class SliceKitExampleApp {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceKitExampleApp.class);
    
    public static void main(String[] args) {
        logger.info("Starting SliceKit Example Application...");
        
        try {
            // Build and start SliceKit application
            final SliceKit app = SliceKit.builder()
                .scanPackages("io.slicekit.examples")  // Scan for example slices
                .port(8080)
                .host("localhost")
                .start();
            
            // Log startup information
            logStartupInfo(app);
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down SliceKit Example Application...");
                app.stop();
                logger.info("Application shutdown complete.");
            }));
            
            // Keep the application running
            logger.info("SliceKit Example Application is now running. Press Ctrl+C to stop.");
            
        } catch (Exception e) {
            logger.error("Failed to start SliceKit Example Application: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Logs startup information about the application.
     */
    private static void logStartupInfo(SliceKit app) {
        logger.info("=".repeat(60));
        logger.info("SliceKit Example Application Started Successfully!");
        logger.info("=".repeat(60));
        logger.info("Server: http://{}:{}", 
            app.getHttpServer().getHost(), 
            app.getHttpServer().getPort());
        logger.info("Registered Slices: {}", app.getSliceRegistry().size());
        
        app.getSliceRegistry().getAllSlices().forEach(slice -> {
            logger.info("  - {} ({})", slice.getRouteKey(), slice.getName());
        });
        
        logger.info("=".repeat(60));
        logger.info("Try these example requests:");
        logger.info("  GET  http://localhost:8080/hello");
        logger.info("  GET  http://localhost:8080/health");
        logger.info("  POST http://localhost:8080/echo");
        logger.info("       Body: {{\"message\":\"Hello SliceKit!\",\"clientId\":\"test-client\"}}");
        logger.info("=".repeat(60));
    }
}