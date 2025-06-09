package io.slicekit.core;

import io.slicekit.discovery.SliceDiscovery;
import io.slicekit.discovery.SliceRegistry;
import io.slicekit.http.SliceHttpServer;
import io.slicekit.injection.SliceInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Main entry point for the SliceKit framework.
 * 
 * Provides a fluent builder API for configuring and starting a SliceKit application.
 * 
 * Example usage:
 * <pre>
 * SliceKit.builder()
 *     .scanPackages("com.example.slices")
 *     .port(8080)
 *     .start();
 * </pre>
 */
public final class SliceKit {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceKit.class);
    
    private final SliceHttpServer httpServer;
    private final SliceRegistry sliceRegistry;
    private final SliceInjector sliceInjector;
    
    private SliceKit(final SliceHttpServer httpServer, final SliceRegistry sliceRegistry, final SliceInjector sliceInjector) {
        this.httpServer = Objects.requireNonNull(httpServer, "HTTP server cannot be null");
        this.sliceRegistry = Objects.requireNonNull(sliceRegistry, "Slice registry cannot be null");
        this.sliceInjector = Objects.requireNonNull(sliceInjector, "Slice injector cannot be null");
    }
    
    /**
     * Starts the SliceKit application.
     * 
     * @throws SliceKitException if startup fails
     */
    public void start() {
        logger.info("Starting SliceKit application...");
        httpServer.start();
        logger.info("SliceKit application started successfully");
    }
    
    /**
     * Stops the SliceKit application.
     */
    public void stop() {
        logger.info("Stopping SliceKit application...");
        httpServer.stop();
        logger.info("SliceKit application stopped");
    }
    
    /**
     * Gets the HTTP server instance.
     * 
     * @return HTTP server
     */
    public SliceHttpServer getHttpServer() {
        return httpServer;
    }
    
    /**
     * Gets the slice registry.
     * 
     * @return slice registry
     */
    public SliceRegistry getSliceRegistry() {
        return sliceRegistry;
    }
    
    /**
     * Gets the slice injector.
     * 
     * @return slice injector
     */
    public SliceInjector getSliceInjector() {
        return sliceInjector;
    }
    
    /**
     * Creates a new SliceKit builder.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for configuring SliceKit applications.
     */
    public static final class Builder {
        
        private final Set<String> packagesToScan = new HashSet<>();
        private int port = 8080;
        private String host = "localhost";
        
        private Builder() {
            // Private constructor - use SliceKit.builder()
        }
        
        /**
         * Adds packages to scan for slice classes.
         * 
         * @param packages package names to scan, must not be null or empty
         * @return this builder
         * @throws IllegalArgumentException if packages is null or contains null/empty elements
         */
        public Builder scanPackages(final String... packages) {
            Objects.requireNonNull(packages, "Packages cannot be null");
            
            for (final String pkg : packages) {
                if (pkg == null || pkg.trim().isEmpty()) {
                    throw new IllegalArgumentException("Package name cannot be null or empty");
                }
                packagesToScan.add(pkg.trim());
            }
            
            return this;
        }
        
        /**
         * Sets the HTTP server port.
         * 
         * @param port server port, must be between 1 and 65535
         * @return this builder
         * @throws IllegalArgumentException if port is invalid
         */
        public Builder port(final int port) {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
            }
            this.port = port;
            return this;
        }
        
        /**
         * Sets the HTTP server host.
         * 
         * @param host server host, must not be null or empty
         * @return this builder
         * @throws IllegalArgumentException if host is null or empty
         */
        public Builder host(final String host) {
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Host cannot be null or empty");
            }
            this.host = host.trim();
            return this;
        }
        
        /**
         * Builds and returns a configured SliceKit instance.
         * 
         * @return configured SliceKit instance
         * @throws SliceKitException if configuration is invalid or slice discovery fails
         */
        public SliceKit build() {
            logger.info("Building SliceKit application...");
            
            // Validate configuration
            if (packagesToScan.isEmpty()) {
                throw new SliceKitException.SliceConfigurationException(
                    "At least one package must be specified for scanning");
            }
            
            // Create dependency injection container
            logger.info("Initializing dependency injection for packages: {}", packagesToScan);
            final SliceInjector sliceInjector = new SliceInjector(packagesToScan);
            
            // Discover slices
            logger.info("Discovering slices in packages: {}", packagesToScan);
            final SliceDiscovery discovery = new SliceDiscovery(
                packagesToScan.toArray(new String[0])
            );
            
            final Map<String, SliceMetadata> discoveredSlices = discovery.discoverSlices();
            
            if (discoveredSlices.isEmpty()) {
                logger.warn("No slices found in packages: {}", packagesToScan);
            }
            
            // Create slice registry
            final SliceRegistry sliceRegistry = new SliceRegistry();
            sliceRegistry.registerSlices(discoveredSlices);
            
            // Create HTTP server with dependency injection
            final SliceHttpServer httpServer = new SliceHttpServer(port, host, sliceRegistry, sliceInjector);
            
            logger.info("SliceKit application built successfully with {} slices and dependency injection", 
                discoveredSlices.size());
            
            return new SliceKit(httpServer, sliceRegistry, sliceInjector);
        }
        
        /**
         * Builds and starts a SliceKit application in one step.
         * 
         * @return started SliceKit instance
         * @throws SliceKitException if build or startup fails
         */
        public SliceKit start() {
            final SliceKit sliceKit = build();
            sliceKit.start();
            return sliceKit;
        }
    }
}