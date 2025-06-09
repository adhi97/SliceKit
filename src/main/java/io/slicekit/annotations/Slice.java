package io.slicekit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Vertical Slice in the SliceKit framework.
 * 
 * A slice represents a complete, self-contained feature that handles
 * a specific business capability from HTTP request to response.
 * 
 * Example:
 * <pre>
 * &#64;Slice(name = "create-order", route = "/api/orders", method = "POST")
 * public class CreateOrderSlice {
 *     &#64;SliceHandler
 *     public CreateOrderResult handle(CreateOrderCommand command) {
 *         // Implementation
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Slice {
    
    /**
     * Unique name for this slice.
     * Used for identification, logging, and metrics.
     * 
     * @return the slice name
     */
    String name();
    
    /**
     * HTTP route pattern for this slice.
     * Supports exact paths only in MVP (path parameters in future versions).
     * 
     * Examples:
     * - "/api/orders"
     * - "/health"
     * - "/api/users/profile"
     * 
     * @return the HTTP route
     */
    String route();
    
    /**
     * HTTP method for this slice.
     * 
     * @return the HTTP method
     */
    HttpMethod method() default HttpMethod.GET;
    
    /**
     * Optional description of what this slice does.
     * Used for documentation and introspection.
     * 
     * @return the slice description
     */
    String description() default "";
    
    /**
     * Slice version for future compatibility.
     * 
     * @return the slice version
     */
    String version() default "1.0";
}