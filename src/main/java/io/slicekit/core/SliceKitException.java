package io.slicekit.core;

/**
 * Base exception for SliceKit framework errors.
 */
public class SliceKitException extends RuntimeException {
    
    public SliceKitException(String message) {
        super(message);
    }
    
    public SliceKitException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Exception thrown during slice discovery phase.
     */
    public static class SliceDiscoveryException extends SliceKitException {
        public SliceDiscoveryException(String message) {
            super(message);
        }
        
        public SliceDiscoveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when slice configuration is invalid.
     */
    public static class SliceConfigurationException extends SliceKitException {
        public SliceConfigurationException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when there are route conflicts.
     */
    public static class RouteConflictException extends SliceKitException {
        public RouteConflictException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown during slice execution.
     */
    public static class SliceExecutionException extends SliceKitException {
        public SliceExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}