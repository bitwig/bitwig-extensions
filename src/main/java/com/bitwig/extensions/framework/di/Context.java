package com.bitwig.extensions.framework.di;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

/**
 * A Dependency Inject context to be used within a Bitwig Extension content.
 */
public class Context {
    
    private static ViewTracker viewTrackerGlobal = null;
    private final Map<Class<?>, Class<?>> serviceTypes = new HashMap<>();
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final List<ComponentClosure<?>> incompleteClosures = new ArrayList<>();
    private final List<ComponentClosure<?>> incompleteSetterClosures = new ArrayList<>();
    private static int counter = 0;
    
    private static class ComponentClosure<T> {
        final Class<?> clazz;
        final Set<Class<?>> missingComponents = new HashSet<>();
        final Set<Class<?>> missingSetters = new HashSet<>();
        T instance;
        
        ComponentClosure(final Class<?> clazz) {
            this.clazz = clazz;
        }
    }
    
    /**
     * Currently experimental. For linked controls.
     *
     * @param host currently for timely output
     */
    public static void registerCounter(final ControllerHost host) {
        host.println(" REGISTER " + counter++);
    }
    
    /**
     * Creates the dependency injection context from the controller extension. Creates and registers the
     * following bitwig elements as services:
     * <ul>
     *     <li>{@link Transport}</li>
     *     <li>{@link ControllerHost}</li>
     *     <li>{@link Application}</li>
     *     <li>{@link HardwareSurface}</li>
     *     <li>a layers object {@link Layers}</li>
     * </ul>
     * The package of the extension and all sub-packages are scanned for other classes annotated with the
     * {@link Component} annotation.
     * Another way to create services is to create them via the {@link Context#create(Class)} method.
     * This instantiates the component and registers it as Service in the context. The service is access by
     * the class of the component.
     * Further more objects can be registered as services via {@link Context#create(Class)} or
     * {@link Context#registerService(Class, Object)}
     *
     * @param extension the controller extension.
     */
    public Context(final ControllerExtension extension, final Package... packages) {
        initializeExtension(extension);
        scanPackage(extension.getClass(), packages);
    }
    
    public void activate() {
        final HashSet<Object> serviced = new HashSet<>();
        final Collection<Object> components = services.values();
        for (final Object component : components) {
            if (!serviced.contains(component) && invokeActivation(component)) {
                serviced.add(component);
            }
        }
    }
    
    private boolean invokeActivation(final Object comp) {
        final Class<?> clazz = comp.getClass();
        final Method[] methods = clazz.getMethods();
        for (final Method method : methods) {
            final Activate activateAnnotation = method.getAnnotation(Activate.class);
            if (activateAnnotation != null && method.getParameterCount() == 0) {
                try {
                    method.invoke(comp);
                    return true;
                }
                catch (final IllegalAccessException | InvocationTargetException exception) {
                    throw new DiException("failed to activate component " + clazz.getName(), exception);
                }
            }
        }
        return false;
    }
    
    private record ComponentClassBind(Class<?> clazz, Component component) implements Comparable<ComponentClassBind> {
        //
        static Optional<ComponentClassBind> create(final Class<?> clazz) {
            final Component serviceAnnotation = clazz.getAnnotation(Component.class);
            if (serviceAnnotation != null && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                return Optional.of(new ComponentClassBind(clazz, serviceAnnotation));
            }
            return Optional.empty();
        }
        
        @Override
        public int compareTo(final ComponentClassBind o) {
            return o.component.priority() - component.priority();
        }
    }
    
    private void scanPackage(final Class<?> baseClass, final Package... packages) {
        try {
            final List<Class<?>> classes = PackageHelper.getClasses(baseClass, packages);
            final List<ComponentClassBind> components = classes.stream()//
                .map(ComponentClassBind::create) //
                .flatMap(o -> o.stream()).sorted() //
                .toList();
            for (final ComponentClassBind bind : components) {
                createAnnotatedComponent(bind.clazz());
                registerInterfacesToService(bind.clazz());
            }
        }
        catch (final IOException | ClassNotFoundException exception) {
            throw new DiException("Failed to create annotated components", exception);
        }
    }
    
    private void registerInterfacesToService(final Class<?> clazz) {
        final List<Class<?>> classInterfaces = getInterfaces(clazz);
        for (final Class<?> interfaceClass : classInterfaces) {
            serviceTypes.put(interfaceClass, clazz);
        }
    }
    
    private void initializeExtension(final ControllerExtension extension) {
        final ControllerHost host = extension.getHost();
        registerService(ControllerHost.class, host);
        registerService(Application.class, host.createApplication());
        registerService(HardwareSurface.class, host.createHardwareSurface());
        registerService(Layers.class, new Layers(extension));
        registerService(Transport.class, host.createTransport());
        registerService(Project.class, host.getProject());
    }
    
    public ViewTracker getViewTracker() {
        if (viewTrackerGlobal == null) {
            viewTrackerGlobal = new ViewTrackerImpl();
        }
        return viewTrackerGlobal;
    }
    
    /**
     * Retrieves an existing service/component. If the component isn't registered null is returned
     *
     * @param type the type the component/service is registered under.
     * @param <T>  the overall type of the service
     * @return the component/service.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(final Class<T> type) {
        return (T) services.get(type);
    }
    
    /**
     * Simply registers an object  under a given service type.
     *
     * @param type   the service type (needs to be an interface)
     * @param object the object to be registered
     * @param <T>    the type
     */
    public <T> void registerService(final Class<? extends T> type, final T object) {
        services.put(type, object);
        tryToCompleteIncompleteClosures(type);
        tryToCompleteIncompleteSetter(type);
    }
    
    /**
     * Creates a standard Layer given context
     *
     * @param name name of the layer.
     * @return the newly created layer.
     */
    public Layer createLayer(final String name) {
        return new Layer(getService(Layers.class), name);
    }
    
    /**
     * Creates a component and registers it under its class in the context.
     *
     * @param clazzToCreate the component to create.
     * @param <T>           the overall service type
     * @return the created component.
     */
    public <T> T create(final Class<T> clazzToCreate) {
        final ComponentClosure<T> closure = new ComponentClosure<>(clazzToCreate);
        create(closure);
        if (closure.instance != null) {
            registerService(closure.instance);
            tryToCompleteIncompleteClosures(clazzToCreate);
            tryToCompleteIncompleteSetter(clazzToCreate);
        }
        return closure.instance;
    }
    
    @SuppressWarnings("unchecked")
    private <T> void create(final ComponentClosure<T> closure) {
        final Class<?> clazzToCreate = closure.clazz;
        final Component componentAnnotation = clazzToCreate.getAnnotation(Component.class);
        try {
            final Constructor<?>[] constructors = clazzToCreate.getConstructors();
            T object = null;
            
            Constructor<?> lastConstructor = null;
            for (final Constructor<?> constructor : constructors) {
                lastConstructor = constructor;
                final Object[] args = getConstructableArguments(constructor, componentAnnotation);
                if (args != null) {
                    object = (T) constructor.newInstance(args);
                    break;
                }
            }
            if (lastConstructor == null) {
                throw new DiException(
                    String.format("Class %s has not available constructor", closure.clazz.getSimpleName()));
            }
            closure.missingComponents.addAll(findMissingFieldInjection(closure));
            
            if (object == null) {
                closure.missingComponents.addAll(getMissingConstructorTypes(lastConstructor));
                return;
            }
            
            if (!closure.missingComponents.isEmpty()) {
                return;
            }
            
            final Field[] fields = clazzToCreate.getDeclaredFields();
            for (final Field field : fields) {
                final Inject injectAnnotation = field.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                    final boolean success = inject(object, field);
                    if (!success) {
                        closure.missingComponents.add(field.getType());
                    }
                }
            }
            
            injectBySetter(object, closure);
            
            final Method[] methods = clazzToCreate.getDeclaredMethods();
            for (final Method method : methods) {
                final PostConstruct postConstruct = method.getAnnotation(PostConstruct.class);
                if (postConstruct != null && !Modifier.isPrivate(method.getModifiers())) {
                    invokePostConstruct(object, method);
                }
            }
            closure.instance = object;
        }
        catch (final SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new DiException("failed to create component object", e);
        }
    }
    
    private <T> void registerService(final T object) {
        final Class<?> mainType = object.getClass();
        final List<Class<?>> interfaces = getInterfaces(mainType);
        final Component annotation = mainType.getAnnotation(Component.class);
        services.put(mainType, object);
        if (annotation != null) {
            for (final Class<?> interfaceType : interfaces) {
                services.put(interfaceType, object);
            }
        }
    }
    
    private <T> void createAnnotatedComponent(final Class<T> clazzToCreate) {
        final ComponentClosure<T> closure = new ComponentClosure<>(clazzToCreate);
        create(closure);
        verifyIncompleteSetters(closure);
        if (closure.instance != null) {
            registerService(closure.instance);
            tryToCompleteIncompleteClosures(clazzToCreate);
            tryToCompleteIncompleteSetter(clazzToCreate);
        } else {
            incompleteClosures.add(closure);
        }
    }
    
    private <T> void verifyIncompleteSetters(final ComponentClosure<T> closure) {
        if (!closure.missingSetters.isEmpty() && !incompleteSetterClosures.contains(closure)) {
            incompleteSetterClosures.add(closure);
        }
    }
    
    private <T> List<Class<?>> findMissingFieldInjection(final ComponentClosure<T> closure) {
        final List<Class<?>> missingComponents = new ArrayList<>();
        final Field[] fields = closure.clazz.getDeclaredFields();
        for (final Field field : fields) {
            final Inject injectAnnotation = field.getAnnotation(Inject.class);
            if (injectAnnotation != null) {
                final Object serviceObject = getServiceImpl(field.getType());
                if (serviceObject == null) {
                    missingComponents.add(field.getType());
                }
            }
        }
        return missingComponents;
    }
    
    private void tryToCompleteIncompleteClosures(final Class<?> newType) {
        if (incompleteClosures.isEmpty()) {
            return;
        }
        final Iterator<ComponentClosure<?>> it = incompleteClosures.iterator();
        final List<ComponentClosure<?>> completedClosures = new ArrayList<>();
        while (it.hasNext()) {
            final ComponentClosure<?> closure = it.next();
            if (closure.missingComponents.remove(newType) && closure.missingComponents.isEmpty()) {
                create(closure);
                registerService(closure.instance);
                completedClosures.add(closure);
                verifyIncompleteSetters(closure);
                it.remove();
            }
        }
        for (final ComponentClosure<?> completed : completedClosures) {
            tryToCompleteIncompleteClosures(completed.clazz);
            tryToCompleteIncompleteSetter(completed.clazz);
        }
    }
    
    private void tryToCompleteIncompleteSetter(final Class<?> newType) {
        if (incompleteSetterClosures.isEmpty()) {
            return;
        }
        final Iterator<ComponentClosure<?>> it = incompleteSetterClosures.iterator();
        while (it.hasNext()) {
            final ComponentClosure<?> closure = it.next();
            if (closure.instance != null && closure.missingSetters.remove(newType)
                && closure.missingSetters.isEmpty()) {
                try {
                    injectBySetter(closure.instance, closure);
                    it.remove();
                }
                catch (final IllegalAccessException | InvocationTargetException exception) {
                    throw new DiException("Failed setter injection ", exception);
                }
            }
        }
    }
    
    private void injectBySetter(final Object object, final ComponentClosure<?> closure) throws
        IllegalAccessException,
        InvocationTargetException {
        final Method[] methods = closure.clazz.getMethods();
        
        for (final Method method : methods) {
            final Inject injectNotation = method.getAnnotation(Inject.class);
            if (injectNotation != null && !Modifier.isPrivate(method.getModifiers())
                && method.getParameterCount() == 1) {
                final Object[] args = getMethodServiceArguments(method);
                if (args != null) {
                    method.invoke(object, args);
                } else {
                    closure.missingSetters.addAll(getMissingTypes(method));
                }
            }
        }
    }
    
    private <T> void invokePostConstruct(final T object, final Method method) throws
        IllegalAccessException,
        InvocationTargetException {
        if (method.getParameterCount() == 0) {
            method.setAccessible(true);
            method.invoke(object);
            method.setAccessible(false);
        } else {
            // TODO PostConstruct is not covered when incomplete, maybe then it will not be called
            final Object[] args = getMethodServiceArguments(method);
            if (args != null) {
                method.setAccessible(true);
                method.invoke(object, args);
                method.setAccessible(false);
            }
        }
    }
    
    private <T> List<Class<?>> getMissingConstructorTypes(final Constructor<T> constructor) {
        final List<Class<?>> missingTypes = new ArrayList<>();
        final Parameter[] parameterTypes = constructor.getParameters();
        for (final Parameter parameterType : parameterTypes) {
            final Class<?> paramType = parameterType.getType();
            final Object constructorObject = getServiceImpl(paramType);
            if (constructorObject == null && paramType != String.class) {
                missingTypes.add(paramType);
            }
        }
        
        return missingTypes;
    }
    
    private List<Class<?>> getInterfaces(final Class<?> clazz) {
        final List<Class<?>> interfaces = new ArrayList<>();
        final Type[] classInterfaces = clazz.getGenericInterfaces();
        for (final Type interfaceClass : classInterfaces) {
            if (interfaceClass instanceof Class<?>) {
                interfaces.add((Class<?>) interfaceClass);
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            interfaces.addAll(getInterfaces(clazz.getSuperclass()));
        }
        return interfaces;
    }
    
    private <T> Object[] getConstructableArguments(final Constructor<T> constructor, final Component compAnnotation) {
        final Parameter[] parameterTypes = constructor.getParameters();
        final Object[] args = new Object[constructor.getParameterCount()];
        boolean foundName = false;
        for (int i = 0; i < parameterTypes.length; i++) {
            final Class<?> paramType = parameterTypes[i].getType();
            Object constructorObject = getServiceImpl(paramType);
            if (constructorObject == null && paramType == String.class && !foundName && compAnnotation != null
                && !compAnnotation.name().isBlank()) {
                constructorObject = compAnnotation.name();
                foundName = true;
            } else if (constructorObject == null && paramType != String.class) {
                return null;
            }
            args[i] = constructorObject;
        }
        
        return args;
    }
    
    private List<Class<?>> getMissingTypes(final Method method) {
        final List<Class<?>> missingComponents = new ArrayList<>();
        for (final Parameter parameterType : method.getParameters()) {
            final Class<?> paramType = parameterType.getType();
            final Object toSetService = getServiceImpl(paramType);
            if (toSetService == null) {
                missingComponents.add(paramType);
            }
        }
        
        return missingComponents;
    }
    
    private <T> Object[] getMethodServiceArguments(final Method method) {
        final Parameter[] parameterTypes = method.getParameters();
        final Object[] args = new Object[method.getParameterCount()];
        for (int i = 0; i < parameterTypes.length; i++) {
            final Class<?> paramType = parameterTypes[i].getType();
            final Object toSetService = getServiceImpl(paramType);
            if (toSetService == null && paramType != String.class) {
                return null;
            }
            args[i] = toSetService;
        }
        
        return args;
    }
    
    private <T> boolean inject(final T object, final Field field) throws
        IllegalArgumentException,
        IllegalAccessException {
        @SuppressWarnings("unchecked") final T serviceObject = (T) getServiceImpl(field.getType());
        //TODO make injection possibly Optional
        if (serviceObject != null) {
            field.setAccessible(true);
            field.set(object, serviceObject);
            field.setAccessible(false);
            return true;
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getServiceImpl(final Class<T> serviceType) {
        final T serviceObject = (T) services.get(serviceType);
        if (serviceObject != null) {
            return serviceObject;
        }
        final Class<?> serviceClass = serviceTypes.get(serviceType);
        if (serviceClass != null) {
            final ComponentClosure<T> closure = new ComponentClosure<>(serviceClass);
            create(closure);
            if (closure.instance != null) {
                services.put(serviceType, closure.instance);
                verifyIncompleteSetters(closure);
                return closure.instance;
            } else {
                incompleteClosures.add(closure);
            }
        }
        return null;
    }
    
    
}
