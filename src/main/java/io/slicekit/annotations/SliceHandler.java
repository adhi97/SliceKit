package io.slicekit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the handler for a slice.
 * 
 * The annotated method will be invoked when the slice's route is matched.
 * Each slice class should have exactly one method annotated with @SliceHandler.
 * 
 * Method signature requirements:
 * - Can have zero or more parameters (request body, path params, query params)
 * - Can return any type (will be serialized to JSON)
 * - Can return void (results in 204 No Content)
 * 
 * Examples:
 * <pre>
 * &#64;SliceHandler
 * public String handle() {
 *     return "Hello World";
 * }
 * 
 * &#64;SliceHandler
 * public CreateOrderResult handle(CreateOrderCommand command) {
 *     // Process command and return result
 * }
 * 
 * &#64;SliceHandler
 * public void handle(DeleteOrderCommand command) {
 *     // Process deletion, no return value
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SliceHandler {
}