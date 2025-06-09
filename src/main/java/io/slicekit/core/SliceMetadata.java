package io.slicekit.core;

import io.slicekit.annotations.HttpMethod;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Metadata about a discovered slice.
 * 
 * Contains all information needed to route requests to the slice
 * and invoke the handler method.
 */
public class SliceMetadata {
    
    private final String name;
    private final String route;
    private final HttpMethod method;
    private final String description;
    private final String version;
    private final Class<?> sliceClass;
    private final Method handlerMethod;
    
    public SliceMetadata(String name, String route, HttpMethod method, 
                        String description, String version, 
                        Class<?> sliceClass, Method handlerMethod) {
        this.name = Objects.requireNonNull(name, "Slice name cannot be null");
        this.route = Objects.requireNonNull(route, "Slice route cannot be null");
        this.method = Objects.requireNonNull(method, "HTTP method cannot be null");
        this.description = description != null ? description : "";
        this.version = version != null ? version : "1.0";
        this.sliceClass = Objects.requireNonNull(sliceClass, "Slice class cannot be null");
        this.handlerMethod = Objects.requireNonNull(handlerMethod, "Handler method cannot be null");
    }
    
    public String getName() {
        return name;
    }
    
    public String getRoute() {
        return route;
    }
    
    public HttpMethod getMethod() {
        return method;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public Class<?> getSliceClass() {
        return sliceClass;
    }
    
    public Method getHandlerMethod() {
        return handlerMethod;
    }
    
    /**
     * Creates a route key for matching requests.
     * Format: "METHOD /route" (e.g., "GET /api/orders")
     */
    public String getRouteKey() {
        return method.name() + " " + route;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SliceMetadata that = (SliceMetadata) o;
        return Objects.equals(name, that.name) && 
               Objects.equals(route, that.route) && 
               method == that.method;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, route, method);
    }
    
    @Override
    public String toString() {
        return "SliceMetadata{" +
                "name='" + name + '\'' +
                ", route='" + route + '\'' +
                ", method=" + method +
                ", class=" + sliceClass.getSimpleName() +
                '}';
    }
}