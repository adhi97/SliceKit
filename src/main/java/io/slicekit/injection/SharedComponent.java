package io.slicekit.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a shared component that can be injected into multiple slices.
 * 
 * Shared components should be stateless or carefully designed to avoid
 * coupling between slices.
 * 
 * Example:
 * <pre>
 * &#64;SharedComponent
 * public class DatabaseConnection {
 *     // Shared database connection pool
 * }
 * 
 * &#64;SharedComponent(sharedWith = {"create-order", "update-order"})
 * public class OrderValidator {
 *     // Shared only with specific slices
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SharedComponent {
    
    /**
     * Specific slice names that can access this shared component.
     * If empty, the component is shared with all slices.
     * 
     * @return array of slice names that can access this component
     */
    String[] sharedWith() default {};
    
    /**
     * Component scope - singleton or prototype.
     * 
     * @return component scope
     */
    ComponentScope scope() default ComponentScope.SINGLETON;
}