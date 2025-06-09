package io.slicekit.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import io.slicekit.core.SliceKitException;
import io.slicekit.core.SliceMetadata;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guice-based dependency injection for SliceKit with slice isolation.
 * 
 * Creates a parent injector for shared components and child injectors
 * for each slice to enforce isolation while allowing controlled sharing.
 */
public final class SliceInjector {
    
    private static final Logger logger = LoggerFactory.getLogger(SliceInjector.class);
    
    private final Set<String> packagesToScan;
    private final Injector sharedInjector;
    private final Map<String, Injector> sliceInjectors = new ConcurrentHashMap<>();
    
    /**
     * Creates a slice injector that scans the specified packages.
     * 
     * @param packagesToScan packages to scan for components
     */
    public SliceInjector(final Set<String> packagesToScan) {
        this.packagesToScan = Objects.requireNonNull(packagesToScan, "Packages to scan cannot be null");
        this.sharedInjector = createSharedInjector();
        logger.info("SliceInjector initialized with shared components");
    }
    
    /**
     * Creates or gets an injector for a specific slice.
     * 
     * @param sliceName slice name
     * @param sliceMetadata slice metadata for context
     * @return slice-specific injector
     */
    public Injector getSliceInjector(final String sliceName, final SliceMetadata sliceMetadata) {
        Objects.requireNonNull(sliceName, "Slice name cannot be null");
        Objects.requireNonNull(sliceMetadata, "Slice metadata cannot be null");
        
        return sliceInjectors.computeIfAbsent(sliceName, name -> createSliceInjector(name, sliceMetadata));
    }
    
    /**
     * Creates an instance of a slice using its dedicated injector.
     * 
     * @param sliceMetadata slice metadata
     * @return slice instance with dependencies injected
     */
    public Object createSliceInstance(final SliceMetadata sliceMetadata) {
        Objects.requireNonNull(sliceMetadata, "Slice metadata cannot be null");
        
        try {
            // Get the slice injector
            final Injector sliceInjector = getSliceInjector(sliceMetadata.getName(), sliceMetadata);
            
            // Use manual constructor resolution as fallback
            return createInstanceManually(sliceMetadata.getSliceClass(), sliceInjector);
            
        } catch (Exception e) {
            throw new SliceKitException.SliceExecutionException(
                "Failed to create slice instance for " + sliceMetadata.getName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Manually creates an instance using constructor injection.
     */
    private Object createInstanceManually(final Class<?> sliceClass, final Injector injector) throws Exception {
        // Find constructor with @Inject annotation
        final java.lang.reflect.Constructor<?>[] constructors = sliceClass.getDeclaredConstructors();
        
        for (final java.lang.reflect.Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(javax.inject.Inject.class) || constructor.getParameterCount() == 0) {
                constructor.setAccessible(true);
                
                if (constructor.getParameterCount() == 0) {
                    // No-arg constructor
                    final Object instance = constructor.newInstance();
                    logger.debug("Created slice instance using no-arg constructor: {}", sliceClass.getSimpleName());
                    return instance;
                } else {
                    // Constructor with dependencies
                    final Object[] args = new Object[constructor.getParameterCount()];
                    final Class<?>[] paramTypes = constructor.getParameterTypes();
                    
                    for (int i = 0; i < paramTypes.length; i++) {
                        args[i] = injector.getInstance(paramTypes[i]);
                    }
                    
                    final Object instance = constructor.newInstance(args);
                    logger.debug("Created slice instance with {} dependencies: {}", 
                        args.length, sliceClass.getSimpleName());
                    return instance;
                }
            }
        }
        
        throw new IllegalStateException("No suitable constructor found for " + sliceClass.getName());
    }
    
    /**
     * Gets the shared injector for accessing shared components.
     * 
     * @return shared injector
     */
    public Injector getSharedInjector() {
        return sharedInjector;
    }
    
    /**
     * Creates the shared injector for components available to all slices.
     */
    private Injector createSharedInjector() {
        logger.debug("Creating shared injector...");
        
        final Set<Class<?>> sharedComponents = findSharedComponents();
        logger.info("Found {} shared components", sharedComponents.size());
        
        return Guice.createInjector(new SharedComponentModule(sharedComponents));
    }
    
    /**
     * Creates a slice-specific injector as a child of the shared injector.
     */
    private Injector createSliceInjector(final String sliceName, final SliceMetadata sliceMetadata) {
        logger.debug("Creating slice injector for: {}", sliceName);
        
        final Set<Class<?>> sliceComponents = findSliceComponents(sliceName);
        final Set<Class<?>> accessibleSharedComponents = findAccessibleSharedComponents(sliceName);
        
        logger.debug("Slice '{}' has {} components and access to {} shared components", 
            sliceName, sliceComponents.size(), accessibleSharedComponents.size());
        
        return sharedInjector.createChildInjector(
            new SliceComponentModule(sliceName, sliceComponents, sliceMetadata)
        );
    }
    
    /**
     * Finds all shared components in the configured packages.
     */
    private Set<Class<?>> findSharedComponents() {
        return scanForAnnotatedClasses(SharedComponent.class);
    }
    
    /**
     * Finds all components belonging to a specific slice.
     */
    private Set<Class<?>> findSliceComponents(final String sliceName) {
        final Set<Class<?>> allSliceComponents = scanForAnnotatedClasses(SliceComponent.class);
        
        return allSliceComponents.stream()
            .filter(clazz -> {
                final SliceComponent annotation = clazz.getAnnotation(SliceComponent.class);
                return annotation != null && sliceName.equals(annotation.value());
            })
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Finds shared components that a specific slice can access.
     */
    private Set<Class<?>> findAccessibleSharedComponents(final String sliceName) {
        final Set<Class<?>> allSharedComponents = findSharedComponents();
        
        return allSharedComponents.stream()
            .filter(clazz -> {
                final SharedComponent annotation = clazz.getAnnotation(SharedComponent.class);
                if (annotation == null) return false;
                
                final String[] sharedWith = annotation.sharedWith();
                // If sharedWith is empty, it's shared with all slices
                return sharedWith.length == 0 || Arrays.asList(sharedWith).contains(sliceName);
            })
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Scans for classes annotated with a specific annotation.
     */
    private Set<Class<?>> scanForAnnotatedClasses(final Class<? extends java.lang.annotation.Annotation> annotation) {
        final Set<Class<?>> allClasses = new java.util.HashSet<>();
        
        for (final String packageName : packagesToScan) {
            try {
                final Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
                final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotation);
                allClasses.addAll(classes);
                
            } catch (Exception e) {
                logger.warn("Failed to scan package '{}' for {}: {}", 
                    packageName, annotation.getSimpleName(), e.getMessage());
            }
        }
        
        return allClasses;
    }
    
    /**
     * Guice module for shared components.
     */
    private static final class SharedComponentModule extends AbstractModule {
        
        private final Set<Class<?>> sharedComponents;
        
        public SharedComponentModule(final Set<Class<?>> sharedComponents) {
            this.sharedComponents = sharedComponents;
        }
        
        @Override
        protected void configure() {
            for (final Class<?> componentClass : sharedComponents) {
                final SharedComponent annotation = componentClass.getAnnotation(SharedComponent.class);
                if (annotation == null) continue;
                
                if (annotation.scope() == ComponentScope.SINGLETON) {
                    bind(componentClass).in(Scopes.SINGLETON);
                } else {
                    bind(componentClass).in(Scopes.NO_SCOPE); // Prototype
                }
                
                logger.debug("Configured shared component: {} ({})", 
                    componentClass.getName(), annotation.scope());
            }
        }
    }
    
    /**
     * Guice module for slice-specific components.
     */
    private static final class SliceComponentModule extends AbstractModule {
        
        private final String sliceName;
        private final Set<Class<?>> sliceComponents;
        private final SliceMetadata sliceMetadata;
        
        public SliceComponentModule(final String sliceName, final Set<Class<?>> sliceComponents, 
                                   final SliceMetadata sliceMetadata) {
            this.sliceName = sliceName;
            this.sliceComponents = sliceComponents;
            this.sliceMetadata = sliceMetadata;
        }
        
        @Override
        protected void configure() {
            // Bind slice components (the slice class will be discovered automatically)
            for (final Class<?> componentClass : sliceComponents) {
                final SliceComponent annotation = componentClass.getAnnotation(SliceComponent.class);
                if (annotation == null) continue;
                
                if (annotation.scope() == ComponentScope.SINGLETON) {
                    bind(componentClass).in(Scopes.SINGLETON);
                } else {
                    bind(componentClass).in(Scopes.NO_SCOPE); // Prototype
                }
                
                logger.debug("Configured slice component for '{}': {} ({})", 
                    sliceName, componentClass.getName(), annotation.scope());
            }
        }
        
        /**
         * Provides the slice metadata for injection.
         */
        @Provides
        @Singleton
        public SliceMetadata provideSliceMetadata() {
            return sliceMetadata;
        }
    }
}