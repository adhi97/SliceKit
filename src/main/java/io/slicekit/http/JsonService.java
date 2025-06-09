package io.slicekit.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.slicekit.core.SliceKitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Service for JSON serialization and deserialization using Jackson.
 * 
 * Provides a centralized way to handle JSON operations with proper
 * configuration for Java 8+ time types and consistent formatting.
 */
public final class JsonService {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonService.class);
    
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new JSON service with default configuration.
     */
    public JsonService() {
        this.objectMapper = createDefaultObjectMapper();
    }
    
    /**
     * Creates a new JSON service with custom ObjectMapper.
     * 
     * @param objectMapper custom ObjectMapper, must not be null
     * @throws IllegalArgumentException if objectMapper is null
     */
    public JsonService(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    }
    
    /**
     * Serializes an object to JSON string.
     * 
     * @param object object to serialize, can be null
     * @return JSON string representation
     * @throws SliceKitException.SliceExecutionException if serialization fails
     */
    public String toJson(final Object object) {
        if (object == null) {
            return "null";
        }
        
        try {
            final String json = objectMapper.writeValueAsString(object);
            logger.debug("Serialized object of type {} to JSON: {}", 
                object.getClass().getSimpleName(), json);
            return json;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object of type {} to JSON: {}", 
                object.getClass().getSimpleName(), e.getMessage());
            throw new SliceKitException.SliceExecutionException(
                "Failed to serialize object to JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deserializes JSON string to an object of the specified type.
     * 
     * @param json JSON string, must not be null
     * @param targetType target class type, must not be null
     * @param <T> target type
     * @return deserialized object
     * @throws SliceKitException.SliceExecutionException if deserialization fails
     * @throws IllegalArgumentException if json or targetType is null
     */
    public <T> T fromJson(final String json, final Class<T> targetType) {
        Objects.requireNonNull(json, "JSON string cannot be null");
        Objects.requireNonNull(targetType, "Target type cannot be null");
        
        try {
            final T object = objectMapper.readValue(json, targetType);
            logger.debug("Deserialized JSON to object of type {}: {}", 
                targetType.getSimpleName(), json);
            return object;
            
        } catch (IOException e) {
            logger.error("Failed to deserialize JSON to type {}: {}", 
                targetType.getSimpleName(), e.getMessage());
            throw new SliceKitException.SliceExecutionException(
                "Failed to deserialize JSON to " + targetType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a string is valid JSON.
     * 
     * @param json string to check, must not be null
     * @return true if valid JSON, false otherwise
     * @throws IllegalArgumentException if json is null
     */
    public boolean isValidJson(final String json) {
        Objects.requireNonNull(json, "JSON string cannot be null");
        
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Gets the underlying ObjectMapper instance.
     * 
     * @return ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * Creates a default ObjectMapper with sensible configuration.
     */
    private static ObjectMapper createDefaultObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        
        // Register Java 8+ time module for LocalDateTime, Instant, etc.
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Pretty print JSON for better readability in development
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Don't fail on unknown properties
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        logger.debug("Created default ObjectMapper with Java time support");
        return mapper;
    }
    
    /**
     * Default singleton instance for convenience.
     */
    private static volatile JsonService defaultInstance;
    
    /**
     * Gets the default JSON service instance.
     * 
     * @return default JsonService instance
     */
    public static JsonService getDefault() {
        if (defaultInstance == null) {
            synchronized (JsonService.class) {
                if (defaultInstance == null) {
                    defaultInstance = new JsonService();
                }
            }
        }
        return defaultInstance;
    }
}