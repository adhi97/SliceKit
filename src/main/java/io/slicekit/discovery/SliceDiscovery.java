package io.slicekit.discovery;

import io.slicekit.annotations.Slice;
import io.slicekit.annotations.SliceHandler;
import io.slicekit.core.SliceKitException;
import io.slicekit.core.SliceMetadata;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers and validates slice classes in the classpath.
 * 
 * Scans specified packages for @Slice annotated classes,
 * validates their structure, and creates SliceMetadata objects.
 */
public class SliceDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceDiscovery.class);
    
    private final Set<String> packagesToScan;
    
    public SliceDiscovery(String... packagesToScan) {
        this.packagesToScan = packagesToScan != null && packagesToScan.length > 0 
            ? Set.of(packagesToScan) 
            : Set.of(""); // Scan all packages if none specified
    }
    
    /**
     * Discovers all slice classes in the configured packages.
     * 
     * @return Map of route keys to slice metadata
     * @throws SliceKitException.SliceDiscoveryException if discovery fails
     */
    public Map<String, SliceMetadata> discoverSlices() {
        logger.info("Starting slice discovery in packages: {}", packagesToScan);
        
        Set<Class<?>> sliceClasses = findSliceClasses();
        logger.info("Found {} potential slice classes", sliceClasses.size());
        
        Map<String, SliceMetadata> slices = new HashMap<>();
        
        for (Class<?> sliceClass : sliceClasses) {
            try {
                SliceMetadata metadata = createSliceMetadata(sliceClass);
                String routeKey = metadata.getRouteKey();
                
                // Check for route conflicts
                if (slices.containsKey(routeKey)) {
                    SliceMetadata existing = slices.get(routeKey);
                    throw new SliceKitException.RouteConflictException(
                        String.format("Route conflict detected: %s is used by both %s and %s",
                            routeKey, existing.getSliceClass().getName(), sliceClass.getName())
                    );
                }
                
                slices.put(routeKey, metadata);
                logger.debug("Registered slice: {}", metadata);
                
            } catch (Exception e) {
                throw new SliceKitException.SliceDiscoveryException(
                    "Failed to process slice class: " + sliceClass.getName(), e);
            }
        }
        
        logger.info("Successfully discovered {} slices", slices.size());
        return slices;
    }
    
    /**
     * Finds all classes annotated with @Slice in the configured packages.
     */
    private Set<Class<?>> findSliceClasses() {
        Set<Class<?>> allSliceClasses = new HashSet<>();
        
        for (String packageToScan : packagesToScan) {
            try {
                Reflections reflections = new Reflections(
                    packageToScan,
                    Scanners.TypesAnnotated
                );
                
                Set<Class<?>> sliceClasses = reflections.getTypesAnnotatedWith(Slice.class);
                allSliceClasses.addAll(sliceClasses);
                
            } catch (Exception e) {
                logger.warn("Failed to scan package '{}': {}", packageToScan, e.getMessage());
            }
        }
        
        return allSliceClasses;
    }
    
    /**
     * Creates SliceMetadata from a slice class.
     */
    private SliceMetadata createSliceMetadata(Class<?> sliceClass) {
        // Extract @Slice annotation
        Slice sliceAnnotation = sliceClass.getAnnotation(Slice.class);
        if (sliceAnnotation == null) {
            throw new SliceKitException.SliceConfigurationException(
                "Class " + sliceClass.getName() + " is not annotated with @Slice");
        }
        
        // Find handler method
        Method handlerMethod = findHandlerMethod(sliceClass);
        
        // Validate slice configuration
        validateSliceConfiguration(sliceAnnotation, sliceClass, handlerMethod);
        
        return new SliceMetadata(
            sliceAnnotation.name(),
            sliceAnnotation.route(),
            sliceAnnotation.method(),
            sliceAnnotation.description(),
            sliceAnnotation.version(),
            sliceClass,
            handlerMethod
        );
    }
    
    /**
     * Finds the method annotated with @SliceHandler.
     */
    private Method findHandlerMethod(Class<?> sliceClass) {
        List<Method> handlerMethods = Arrays.stream(sliceClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(SliceHandler.class))
            .collect(Collectors.toList());
        
        if (handlerMethods.isEmpty()) {
            throw new SliceKitException.SliceConfigurationException(
                "Slice class " + sliceClass.getName() + " must have exactly one method annotated with @SliceHandler");
        }
        
        if (handlerMethods.size() > 1) {
            throw new SliceKitException.SliceConfigurationException(
                "Slice class " + sliceClass.getName() + " has multiple @SliceHandler methods. Only one is allowed.");
        }
        
        Method handlerMethod = handlerMethods.get(0);
        handlerMethod.setAccessible(true); // Allow calling private methods
        return handlerMethod;
    }
    
    /**
     * Validates slice configuration for common issues.
     */
    private void validateSliceConfiguration(Slice annotation, Class<?> sliceClass, Method handlerMethod) {
        // Validate slice name
        if (annotation.name().trim().isEmpty()) {
            throw new SliceKitException.SliceConfigurationException(
                "Slice name cannot be empty in class " + sliceClass.getName());
        }
        
        // Validate route
        String route = annotation.route().trim();
        if (route.isEmpty()) {
            throw new SliceKitException.SliceConfigurationException(
                "Slice route cannot be empty in class " + sliceClass.getName());
        }
        
        if (!route.startsWith("/")) {
            throw new SliceKitException.SliceConfigurationException(
                "Slice route must start with '/' in class " + sliceClass.getName() + ". Found: " + route);
        }
        
        // Validate handler method accessibility
        if (handlerMethod.getParameterCount() > 1) {
            logger.warn("Handler method in {} has {} parameters. Only 0-1 parameters supported in MVP", 
                sliceClass.getName(), handlerMethod.getParameterCount());
        }
    }
}