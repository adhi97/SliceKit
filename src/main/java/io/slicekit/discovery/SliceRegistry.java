package io.slicekit.discovery;

import io.slicekit.core.SliceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing discovered slices.
 * 
 * Provides thread-safe access to slice metadata and
 * utilities for route matching and slice lookup.
 * 
 * This class is thread-safe and immutable after registration.
 */
public final class SliceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceRegistry.class);
    
    private final Map<String, SliceMetadata> slicesByRouteKey = new ConcurrentHashMap<>();
    private final Map<String, SliceMetadata> slicesByName = new ConcurrentHashMap<>();
    
    /**
     * Registers discovered slices in the registry.
     * 
     * @param discoveredSlices map of route keys to slice metadata, must not be null
     * @throws IllegalArgumentException if discoveredSlices is null
     */
    public void registerSlices(final Map<String, SliceMetadata> discoveredSlices) {
        Objects.requireNonNull(discoveredSlices, "Discovered slices cannot be null");
        
        logger.info("Registering {} slices", discoveredSlices.size());
        
        for (final Map.Entry<String, SliceMetadata> entry : discoveredSlices.entrySet()) {
            final String routeKey = entry.getKey();
            final SliceMetadata metadata = entry.getValue();
            
            slicesByRouteKey.put(routeKey, metadata);
            slicesByName.put(metadata.getName(), metadata);
            
            logger.debug("Registered slice '{}' with route '{}'", 
                metadata.getName(), routeKey);
        }
        
        logger.info("Slice registration complete. Total slices: {}", slicesByRouteKey.size());
    }
    
    /**
     * Finds a slice that matches the given HTTP method and route.
     * 
     * @param method HTTP method (e.g., "GET", "POST"), must not be null or empty
     * @param route Request route (e.g., "/api/orders"), must not be null or empty
     * @return SliceMetadata if found, empty Optional otherwise
     * @throws IllegalArgumentException if method or route is null or empty
     */
    public Optional<SliceMetadata> findSlice(final String method, final String route) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method cannot be null or empty");
        }
        if (route == null || route.trim().isEmpty()) {
            throw new IllegalArgumentException("Route cannot be null or empty");
        }
        
        final String routeKey = method.trim().toUpperCase() + " " + route.trim();
        return Optional.ofNullable(slicesByRouteKey.get(routeKey));
    }
    
    /**
     * Gets a slice by its name.
     * 
     * @param name slice name, must not be null or empty
     * @return SliceMetadata if found, empty Optional otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    public Optional<SliceMetadata> getSliceByName(final String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Slice name cannot be null or empty");
        }
        
        return Optional.ofNullable(slicesByName.get(name.trim()));
    }
    
    /**
     * Gets all registered slices.
     * 
     * @return unmodifiable collection of all registered slice metadata
     */
    public Collection<SliceMetadata> getAllSlices() {
        return Collections.unmodifiableCollection(slicesByRouteKey.values());
    }
    
    /**
     * Gets all route keys (for debugging/introspection).
     * 
     * @return unmodifiable set of all route keys
     */
    public Set<String> getAllRouteKeys() {
        return Collections.unmodifiableSet(slicesByRouteKey.keySet());
    }
    
    /**
     * Checks if any slices are registered.
     * 
     * @return true if no slices are registered, false otherwise
     */
    public boolean isEmpty() {
        return slicesByRouteKey.isEmpty();
    }
    
    /**
     * Gets the number of registered slices.
     * 
     * @return number of registered slices
     */
    public int size() {
        return slicesByRouteKey.size();
    }
    
    /**
     * Clears all registered slices.
     * 
     * Note: This method is primarily intended for testing purposes.
     * In production, the registry should be immutable after initial registration.
     */
    void clear() {
        slicesByRouteKey.clear();
        slicesByName.clear();
        logger.debug("Slice registry cleared");
    }
    
    @Override
    public String toString() {
        return "SliceRegistry{" +
                "sliceCount=" + slicesByRouteKey.size() +
                ", routes=" + slicesByRouteKey.keySet() +
                '}';
    }
}