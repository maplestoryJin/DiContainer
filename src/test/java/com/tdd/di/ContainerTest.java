package com.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private ContextConfig contextConfig;

    @BeforeEach
    void setUp() {
        contextConfig = new ContextConfig();
    }

    @Nested
    class ComponentTest {
        // TODO: instance

        @Test
        void should_bind_type_a_specific_instance() {
            Component instance = new Component() {
            };
            contextConfig.bind(Component.class, instance);
            assertSame(instance, contextConfig.getContext().get(Component.class).get());
        }

        @Test
        void should_return_empty_if_component_does_not_exist() {
            contextConfig.getContext().get(Component.class).ifPresent(instance -> fail("should not be present"));
        }

        // TODO: abstract class
        // TODO: interface

    }

    @Nested
    class ConstructorInjectionTest {
        // TODO: default constructor

        @Test
        void should_bind_type_to_a_class_with_default_constructor() {
            contextConfig.bind(Component.class, ComponentInjectWithDefaultConstructor.class);
            assertNotNull(contextConfig.getContext().get(Component.class).get());
            assertTrue(contextConfig.getContext().get(Component.class).get() instanceof ComponentInjectWithDefaultConstructor);
        }

        // TODO: with dependencies

        @Test
        void should_bind_type_to_a_class_with_inject_constructor() {
            Dependency dependency = new Dependency() {
            };
            contextConfig.bind(Dependency.class, dependency);
            contextConfig.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            Component component = contextConfig.getContext().get(Component.class).get();
            assertNotNull(component);
            assertSame(dependency, ((ComponentInjectWithInjectConstructor) component).getDependency());

        }

        // TODO: A -> B -> C

        @Test
        void should_bind_type_to_a_class_with_transitive_dependencies() {
            contextConfig.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
            contextConfig.bind(String.class, "indirect dependency");

            Component instance = contextConfig.getContext().get(Component.class).get();
            Dependency dependency = ((ComponentInjectWithInjectConstructor) instance).getDependency();
            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());

        }

        // TODO: multi inject constructors

        @Test
        void should_throw_exception_if_multi_inject_constructors_provided() {
            assertThrows(IllegalComponentException.class, () ->
                    contextConfig.bind(Component.class, ComponentInjectWithMultiInjectConstructors.class));
        }

        // TODO: no default constructor nor inject constructor

        @Test
        void should_throw_exception_if_no_default_constructor_nor_inject_constructor() {
            assertThrows(IllegalComponentException.class, () ->
                    contextConfig.bind(Component.class, ComponentNoDefaultConstructorNorInjectConstructor.class));
        }

        // TODO: dependencies not exist
        @Test
        void should_throw_exception_if_dependencies_not_exist() {
            contextConfig.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> contextConfig.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        @Test
        void should_throw_exception_if_transitive_dependencies_not_found() {
            contextConfig.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> contextConfig.getContext());
            assertEquals(String.class, exception.getDependency());
            assertEquals(Dependency.class, exception.getComponent());
        }

        // TODO: cyclic dependency
        @Test
        void should_throw_exception_if_cycle_dependency() {
            contextConfig.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            contextConfig.bind(Dependency.class, DependencyDependsOnComponent.class);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> contextConfig.getContext());
            List<Class<?>> components = List.of(exception.getComponents());
            assertEquals(2, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        // TODO: transitive cyclic dependency
        @Test
        void should_throw_exception_if_transitive_cycle_dependency() {
            contextConfig.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            contextConfig.bind(Dependency.class, DependencyDependsOnAnotherDependency.class);
            contextConfig.bind(DependencyAnother.class, AnotherDependencyDependsOnComponent.class);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> contextConfig.getContext());

            List<Class<?>> components = List.of(exception.getComponents());
            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(DependencyAnother.class));
        }
    }

    @Nested
    class FiledInjectionTest {

    }

    @Nested
    class MethodInjectionTest {

    }

    interface Component {

    }

    interface Dependency {

    }

    interface DependencyAnother {

    }

    static class ComponentInjectWithMultiInjectConstructors implements Component {
        @Inject
        public ComponentInjectWithMultiInjectConstructors(Dependency dependency) {
        }

        @Inject
        public ComponentInjectWithMultiInjectConstructors(Component component) {

        }
    }

    static class ComponentNoDefaultConstructorNorInjectConstructor implements Component {
        public ComponentNoDefaultConstructorNorInjectConstructor(Dependency dependency) {
        }
    }

    static class ComponentInjectWithDefaultConstructor implements Component {
        public ComponentInjectWithDefaultConstructor() {
        }
    }

    static class ComponentInjectWithInjectConstructor implements Component {
        private Dependency dependency;

        @Inject
        public ComponentInjectWithInjectConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency() {
            return dependency;
        }
    }

    static class DependencyWithInjectConstructor implements Dependency {
        private String dependency;

        @Inject
        public DependencyWithInjectConstructor(String dependency) {
            this.dependency = dependency;
        }

        public String getDependency() {
            return dependency;
        }
    }


    static class DependencyDependsOnComponent implements Dependency {
        private Component dependency;

        @Inject
        public DependencyDependsOnComponent(Component dependency) {
            this.dependency = dependency;
        }
    }


    static class DependencyDependsOnAnotherDependency implements Dependency {
        private DependencyAnother dependency;

        @Inject
        public DependencyDependsOnAnotherDependency(DependencyAnother dependency) {
            this.dependency = dependency;
        }
    }

    static class AnotherDependencyDependsOnComponent implements DependencyAnother {
        private Component component;

        @Inject
        public AnotherDependencyDependsOnComponent(Component component) {
            this.component = component;
        }
    }


}
