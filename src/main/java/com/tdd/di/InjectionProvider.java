package com.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;
    private final List<ComponentRef> dependencies;

    InjectionProvider(Class<? extends T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(component);
        injectFields = getInjectFields(component);
        injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(InjectionProvider::hasTypeParameter)) {
            throw new IllegalComponentException();
        }
        dependencies = getDependencies();

    }

    @Override
    public T get(Context context) {
        try {
            T instance = this.injectConstructor.newInstance(toDependencies(context, this.injectConstructor));
            for (Field injectField : injectFields) {
                injectField.set(instance, toDependency(context, injectField));
            }
            for (Method injectMethod : injectMethods) {
                injectMethod.invoke(instance, toDependencies(context, injectMethod));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return concat(injectMethods.stream().flatMap(m -> stream(m.getParameters())).map(InjectionProvider::toComponentRef),
                concat(injectFields.stream().map(InjectionProvider::toComponentRef),
                        stream(injectConstructor.getParameters()).map(InjectionProvider::toComponentRef))).toList();
    }

    private static ComponentRef toComponentRef(Parameter parameter) {
        return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter.getAnnotations()));
    }

    private static ComponentRef toComponentRef(Field field) {
        return ComponentRef.of(field.getGenericType(), getQualifier(field.getAnnotations()));
    }

    private static Annotation getQualifier(final Annotation[] annotations) {
        List<Annotation> qualifiers = stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        if (qualifiers.size() > 1) throw new IllegalComponentException();
        return qualifiers.stream()
                .findFirst().orElse(null);
    }

    private static List<Method> getInjectMethods(Class<?> component) {
        return traverse(component, (injectMethods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(injectMethods, m))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList());
    }

    private static List<Field> getInjectFields(Class<?> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<? extends Type> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getConstructors())
                .toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        return defaultConstructor(component, injectConstructors);
    }

    private static <Type> Constructor<Type> defaultConstructor(final Class<?> component, final List<Constructor<?>> injectConstructors) {
        return (Constructor<Type>) injectConstructors
                .stream().findFirst().orElseGet(() -> {
                    try {
                        return component.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new IllegalComponentException();
                    }
                });
    }


    private static <T extends AnnotatedElement> Stream<T> injectable(final T[] fields) {
        return stream(fields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(final Method method, final Method other) {
        return method.getName().equals(other.getName())
                && Arrays.equals(method.getParameters(), other.getParameters());
    }

    private static boolean isOverrideByNoInjectMethod(final Class<?> component, final Method method) {
        return stream(component.getDeclaredMethods()).filter(m -> !m.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(o, method));
    }

    private static boolean isOverrideByInjectMethod(final ArrayList<Method> injectMethods, final Method method) {
        return injectMethods.stream().noneMatch(o -> isOverride(o, method));
    }

    private static Object toDependency(final Context context, final Field field) {
        return toDependency(context, field.getGenericType(), getQualifier(field.getAnnotations()));
    }

    private static Object[] toDependencies(final Context context, final Executable executable) {
        return stream(executable.getParameters())
                .map(p -> toDependency(context, p.getParameterizedType(), getQualifier(p.getAnnotations()))).toArray(Object[]::new);
    }

    private static Object toDependency(Context context, Type type, final Annotation qualifier) {
        return context.get(ComponentRef.of(type, qualifier)).get();
    }


    private static <T> ArrayList<T> traverse(final Class<?> component, final BiFunction<ArrayList<T>, Class<?>, List<T>> function) {
        ArrayList<T> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(function.apply(injectMethods, current));
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static boolean hasTypeParameter(final Method m) {
        return m.getTypeParameters().length > 0;
    }


}
