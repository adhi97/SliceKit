package io.slicekit.examples;

import io.slicekit.annotations.HttpMethod;
import io.slicekit.annotations.Slice;
import io.slicekit.annotations.SliceHandler;

/**
 * Simple example slice that returns a greeting message.
 * 
 * Demonstrates basic slice functionality with no parameters
 * and string response serialization.
 */
@Slice(
    name = "hello-world", 
    route = "/hello", 
    method = HttpMethod.GET,
    description = "Returns a simple greeting message"
)
public final class HelloWorldSlice {
    
    @SliceHandler
    public String handle() {
        return "Hello from SliceKit! The framework is working correctly.";
    }
}