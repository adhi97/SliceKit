package io.slicekit.injection;

/**
 * Defines the lifecycle scope of dependency injection components.
 */
public enum ComponentScope {
    
    /**
     * Single instance shared across the application or slice.
     * This is the default and recommended scope for most components.
     */
    SINGLETON,
    
    /**
     * New instance created each time the component is requested.
     * Use this for stateful components or when you need fresh instances.
     */
    PROTOTYPE
}