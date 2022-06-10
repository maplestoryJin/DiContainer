package com.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tdd.di.InjectionProvider.Injectable.of;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {
    private final Injectable<Constructor<T>> injectConstructor;
    private Collection<Class<?>> superClasses;
    private Map<Class<?>, List<Injectable<Method>>> injectMethods;
    private Map<Class<?>, List<Injectable<Field>>> injectFields;
    private List<ComponentRef<?>> dependencies;

    InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw ComponentError.abstractComponent(component);
        injectConstructor = getInjectConstructor(component);
        superClasses = allSuperClass(component);
        var injectFields = getInjectFields(component);
        var injectMethods = getInjectMethods(component);

        this.injectMethods = groupByClass(injectMethods);
        this.injectFields = groupByClass(injectFields);

        dependencies = concat(concat(Stream.of(injectConstructor), injectFields.stream()),
                injectMethods.stream())
                .flatMap(injectable -> stream(injectable.required())).toList();

    }

    private static <E extends AccessibleObject> Map<Class<?>, List<Injectable<E>>> groupByClass(List<Injectable<E>> injectMethods) {
        return injectMethods.stream().collect(Collectors.groupingBy(i -> ((Member) i.element()).getDeclaringClass()));
    }

    private static List<Class<?>> allSuperClass(Class<?> component) {
        List<Class<?>> result = new ArrayList<>();
        for (Class superClass = component;
             superClass != Object.class;
             superClass = superClass.getSuperclass())
            result.add(0, superClass);
        return result;
    }

    record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
        public Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }

        static <Element extends Executable> Injectable<Element> of(Element element) {
            element.setAccessible(true);
            return new Injectable<>(element, stream(element.getParameters()).map(Injectable::toComponentRef).toArray(ComponentRef<?>[]::new));
        }

        static Injectable<Field> of(Field field) {
            field.setAccessible(true);
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
            injectMembers(context, instance, false);
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void injectMembers(Context context, T instance, boolean statics) throws IllegalAccessException, InvocationTargetException {
        for (final Class<?> superClass : superClasses) {
            for (Injectable<Field> injectField : injectMembers(superClass, injectFields, f -> statics == isStatic(f))) {
                injectField.element().set(instance, injectField.toDependencies(context)[0]);
            }
            for (Injectable<Method> injectMethod : injectMembers(superClass, injectMethods, f -> statics == isStatic(f))) {
                injectMethod.element().invoke(instance, injectMethod.toDependencies(context));
            }
        }
    }

    private <E extends AccessibleObject> boolean isStatic(Injectable<E> f) {
        return Modifier.isStatic(((Member) f.element()).getModifiers());
    }

    private <E extends AccessibleObject> List<Injectable<E>> injectMembers(Class<?> superClass, Map<Class<?>, List<Injectable<E>>> members, Predicate<Injectable<E>> predicate) {
        return members.getOrDefault(superClass, List.of()).stream().filter(predicate).toList();
    }

    @Override
    public void statics(Context context) {
        try {
            injectMembers(context, null, true);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return dependencies;
    }


    private static List<Injectable<Method>> getInjectMethods(Class<?> component) {
        List<Injectable<Method>> injectables = InjectionProvider.<Method>traverse(component, (injectMethods1, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isOverrideByInjectMethod(injectMethods1, m))
                .filter(m -> isOverrideByNoInjectMethod(component, m))
                .toList()).stream().map(Injectable::of).toList();

        return check(component, injectables, InjectionProvider::hasTypeParameter, ComponentError::injectMethodsWithTypeParameter);
    }

    private static List<Injectable<Field>> getInjectFields(Class<?> component) {
        List<Injectable<Field>> injectables = InjectionProvider.<Field>traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList()).stream().map(Injectable::of).toList();
        return check(component, injectables, InjectionProvider::isFinal, ComponentError::finalInjectFields);
    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getDeclaredConstructors())
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


    private static <T extends AnnotatedElement> Stream<T> injectable(final T[] element) {
        return stream(element).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(final Method method, final Method other) {
        boolean visible;
        if (method.getDeclaringClass().getPackageName().equals(other.getDeclaringClass().getPackageName()))
            visible = !Modifier.isPrivate(other.getModifiers()) && !Modifier.isPrivate(method.getModifiers());
        else visible = (Modifier.isPublic(other.getModifiers()) || Modifier.isProtected(other.getModifiers()))
                && (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers()));
        return visible && other.getName().equals(method.getName()) && Arrays.equals(other.getParameterTypes(), method.getParameterTypes());
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

    private static <E extends AccessibleObject> List<Injectable<E>> check(Class<?> component, List<Injectable<E>> injectables, Predicate<E> predicate, BiFunction<Class<?>, Collection<E>, ComponentError> a) {
        Collection<E> elements = injectables.stream().map(Injectable::element).toList();
        if (elements.stream().anyMatch(predicate)) {
            throw a.apply(component, elements);
        }
        return injectables;
    }

    private static boolean hasTypeParameter(final Method m) {
        return m.getTypeParameters().length > 0;
    }


    private static boolean isFinal(Field f) {
        return Modifier.isFinal(f.getModifiers());
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
