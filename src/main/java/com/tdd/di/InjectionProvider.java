package com.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.tdd.di.InjectionProvider.Injectable.of;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {
    private final Injectable<Constructor<T>> injectConstructor;
    private final List<Injectable<Field>> injectFields;
    private final List<Injectable<Method>> injectMethods;

    InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw ComponentError.abstractComponent(component);
        injectConstructor = getInjectConstructor(component);
        injectFields = getInjectFields(component);
        injectMethods = getInjectMethods(component);

        if (accessibleObject(injectFields).anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw ComponentError.finalInjectFields(component, accessibleObject(injectFields).toList());
        }
        if (accessibleObject(injectMethods).anyMatch(InjectionProvider::hasTypeParameter)) {
            throw ComponentError.injectMethodsWithTypeParameter(component, accessibleObject(injectMethods).toList());
        }

    }

    private <Element extends AccessibleObject> Stream<Element> accessibleObject(final List<Injectable<Element>> injectables) {
        return injectables.stream().map(Injectable::element);
    }

    record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
        public Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }

        static <Element extends Executable> Injectable<Element> of(Element element) {
            return new Injectable<>(element, stream(element.getParameters()).map(Injectable::toComponentRef).toArray(ComponentRef<?>[]::new));
        }

        static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef<?>[]{toComponentRef(field)});
        }


        private static ComponentRef<?> toComponentRef(Parameter parameter) {
            return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
        }

        private static ComponentRef<?> toComponentRef(Field field) {
            return ComponentRef.of(field.getGenericType(), getQualifier(field));
        }

        private static Annotation getQualifier(final AnnotatedElement element) {
            List<Annotation> qualifiers = stream(element.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
            if (qualifiers.size() > 1) throw ComponentError.ambiguousQualifiers(element, qualifiers);
            return qualifiers.stream()
                    .findFirst().orElse(null);
        }
    }

    @Override
    public T get(Context context) {
        try {
            T instance = this.injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Injectable<Field> injectField : injectFields) {
                Field field = injectField.element();
                field.set(instance, injectField.toDependencies(context)[0]);
            }
            for (Injectable<Method> injectMethod : injectMethods) {
                Method method = injectMethod.element();
                method.invoke(instance, injectMethod.toDependencies(context));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(concat(Stream.of(injectConstructor), injectFields.stream()),
                injectMethods.stream())
                .flatMap(injectable -> stream(injectable.required())).toList();
    }


    private static List<Injectable<Method>> getInjectMethods(Class<?> component) {
        return InjectionProvider.<Method>traverse(component, (injectMethods1, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(injectMethods1, m))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList()).stream().map(Injectable::of).toList();
    }

    private static List<Injectable<Field>> getInjectFields(Class<?> component) {
        return InjectionProvider.<Field>traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList()).stream().map(Injectable::of).toList();
    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getConstructors())
                .toList();
        if (injectConstructors.size() > 1) throw ComponentError.ambiguousInjectableConstructors(component);
        return of(defaultConstructor(component, injectConstructors));
    }


    private static <Type> Constructor<Type> defaultConstructor(final Class<?> component, final List<Constructor<?>> injectConstructors) {
        return (Constructor<Type>) injectConstructors
                .stream().findFirst().orElseGet(() -> {
                    try {
                        return component.getDeclaredConstructor();
                    } catch (NoSuchMethodException e) {
                        throw ComponentError.noDefaultConstructor(component);
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


    public static class ComponentError extends Error {
        public static ComponentError abstractComponent(Class<?> component) {
            return new ComponentError(MessageFormat.format("Can not be abstract: {0}", component));
        }

        public static ComponentError finalInjectFields(Class<?> component, Collection<Field> fields) {
            return new ComponentError(MessageFormat.format("Injectable field can not be final: {0} in {1}",
                    String.join(" , ", fields.stream().map(Field::getName).toList()), component));
        }

        public static ComponentError injectMethodsWithTypeParameter(Class<?> component, Collection<Method> fields) {
            return new ComponentError(MessageFormat.format("Injectable method can not have type parameter: {0} in {1}",
                    String.join(" , ", fields.stream().map(Method::getName).toList()), component));
        }

        public static ComponentError ambiguousInjectableConstructors(Class<?> component) {
            return new ComponentError(MessageFormat.format("Ambiguous injectable constructors: {0}", component));
        }

        public static ComponentError noDefaultConstructor(Class<?> component) {
            return new ComponentError(MessageFormat.format("No default constructors: {0}", component));
        }

        public static ComponentError ambiguousQualifiers(AnnotatedElement element, List<Annotation> qualifiers) {
            Class<?> component;
            if (element instanceof Parameter p) component = p.getDeclaringExecutable().getDeclaringClass();
            else component = ((Field) element).getDeclaringClass();
            return new ComponentError(MessageFormat.format("Ambiguous qualifiers: {0} on {1} of {2}",
                    String.join(" , ", qualifiers.stream().map(Object::toString).toList()), element, component));
        }

        ComponentError(String message) {
            super(message);
        }
    }
}
