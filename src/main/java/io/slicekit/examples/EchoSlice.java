package io.slicekit.examples;

import io.slicekit.annotations.HttpMethod;
import io.slicekit.annotations.Slice;
import io.slicekit.annotations.SliceHandler;

import java.time.LocalDateTime;

/**
 * Echo slice that accepts JSON input and returns it with additional metadata.
 * 
 * Demonstrates request body deserialization and complex object response.
 */
@Slice(
    name = "echo", 
    route = "/echo", 
    method = HttpMethod.POST,
    description = "Echoes back the received JSON with metadata"
)
public final class EchoSlice {
    
    @SliceHandler
    public EchoResponse handle(EchoRequest request) {
        if (request == null) {
            return new EchoResponse(
                null,
                "No request body received",
                LocalDateTime.now(),
                0
            );
        }
        
        return new EchoResponse(
            request,
            "Request received and processed successfully",
            LocalDateTime.now(),
            calculateResponseSize(request)
        );
    }
    
    private int calculateResponseSize(EchoRequest request) {
        // Simple size calculation for demonstration
        int size = 0;
        if (request.getMessage() != null) {
            size += request.getMessage().length();
        }
        if (request.getData() != null) {
            size += request.getData().toString().length();
        }
        return size;
    }
    
    /**
     * Request object for echo slice.
     */
    public static final class EchoRequest {
        private String message;
        private Object data;
        private String clientId;
        
        // Default constructor for Jackson
        public EchoRequest() {}
        
        public EchoRequest(String message, Object data, String clientId) {
            this.message = message;
            this.data = data;
            this.clientId = clientId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
    
    /**
     * Response record for echo slice.
     */
    public record EchoResponse(
        EchoRequest originalRequest,
        String processingMessage,
        LocalDateTime processedAt,
        int estimatedSize
    ) {
        public String getSliceInfo() {
            return "Processed by SliceKit Echo Slice v1.0";
        }
    }
}