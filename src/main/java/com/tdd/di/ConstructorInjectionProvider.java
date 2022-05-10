package com.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private final Constructor<Type> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    ConstructorInjectionProvider(Class<? extends Type> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(component);
        injectFields = getInjectFields(component);
        injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length > 0)) {
            throw new IllegalComponentException();
        }

    }

    private List<Method> getInjectMethods(Class<? extends Type> implementation) {
        ArrayList<Method> injectMethods = new ArrayList<>();
        Class<?> current = implementation;
        while (current != Object.class) {
            injectMethods.addAll(stream(current.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Inject.class))
                    .filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName())
                            && Arrays.equals(o.getParameters(), m.getParameters())))
                    .filter(m -> stream(implementation.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                            .noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameters(), m.getParameters())))
                    .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private List<Field> getInjectFields(Class<? extends Type> implementation) {
        ArrayList<Field> injectFields = new ArrayList<>();
        Class<?> current = implementation;
        while (current != Object.class) {
            injectFields.addAll(stream(current.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<? extends Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        return (Constructor<Type>) injectConstructors
                .stream().findFirst().orElseGet(() -> {
                    try {
                        return implementation.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new IllegalComponentException();
                    }
                });
    }

    @Override
    public Type get(Context context) {
        try {
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType()).get()).toArray(Object[]::new);
            Type instance = injectConstructor.newInstance(dependencies);
            for (Field injectField : injectFields) {
                injectField.set(instance, context.get(injectField.getType()).get());
            }
            for (Method injectMethod : injectMethods) {
                injectMethod.invoke(instance, stream(injectMethod.getParameterTypes())
                        .map(t -> context.get(t).get()).toArray(Object[]::new));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(concat(injectFields.stream().map(Field::getType), stream(injectConstructor.getParameters()).map(Parameter::getType)),
                injectMethods.stream().flatMap(m -> stream(m.getParameterTypes()))).toList();
    }
}
