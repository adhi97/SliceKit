package io.slicekit.examples;

import io.slicekit.annotations.HttpMethod;
import io.slicekit.annotations.Slice;
import io.slicekit.annotations.SliceHandler;

import java.time.LocalDateTime;

/**
 * Health check slice that returns application status information.
 * 
 * Demonstrates object response serialization with JSON.
 */
@Slice(
    name = "health-check", 
    route = "/health", 
    method = HttpMethod.GET,
    description = "Returns application health status"
)
public final class HealthCheckSlice {
    
    @SliceHandler
    public HealthStatus handle() {
        return new HealthStatus(
            "UP",
            "SliceKit MVP is running successfully",
            LocalDateTime.now(),
            System.currentTimeMillis() - startTime
        );
    }
    
    private static final long startTime = System.currentTimeMillis();
    
    /**
     * Health status response object.
     */
    public static final class HealthStatus {
        private final String status;
        private final String message;
        private final LocalDateTime timestamp;
        private final long uptimeMs;
        
        public HealthStatus(String status, String message, LocalDateTime timestamp, long uptimeMs) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.uptimeMs = uptimeMs;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public long getUptimeMs() {
            return uptimeMs;
        }
        
        public String getUptimeFormatted() {
            long seconds = uptimeMs / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            
            return String.format("%d hours, %d minutes, %d seconds", 
                hours, minutes % 60, seconds % 60);
        }
    }
}