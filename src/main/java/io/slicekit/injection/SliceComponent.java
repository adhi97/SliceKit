package io.slicekit.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a component that belongs to a specific slice.
 * 
 * Slice components are isolated and can only be injected into their
 * owning slice or explicitly shared components.
 * 
 * Example:
 * <pre>
 * &#64;SliceComponent("create-order")
 * public class CreateOrderDataAccess {
 *     // Implementation specific to create-order slice
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SliceComponent {
    
    /**
     * The name of the slice this component belongs to.
     * Must match a slice name defined in @Slice annotation.
     * 
     * @return slice name
     */
    String value();
    
    /**
     * Component scope - singleton or prototype.
     * 
     * @return component scope
     */
    ComponentScope scope() default ComponentScope.SINGLETON;
}