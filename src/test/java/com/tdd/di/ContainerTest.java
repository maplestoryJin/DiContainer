package com.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class ComponentConstructionTest {

        @Test
        void should_bind_type_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Component.class).get());
        }

        @Test
        void should_return_empty_if_component_does_not_exist() {
            assertTrue(config.getContext().get(Component.class).isEmpty());
        }

        @Nested
        class DependencyCheckTest {

            @Test
            void should_throw_exception_if_dependencies_not_exist() {
                config.bind(Component.class, ComponentInjectWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                        () -> config.getContext());
                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }

            @Test
            void should_throw_exception_if_cycle_dependency() {
                config.bind(Component.class, ComponentInjectWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependsOnComponent.class);
                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                        () -> config.getContext());
                List<Class<?>> components = List.of(exception.getComponents());
                assertEquals(2, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            void should_throw_exception_if_transitive_cycle_dependency() {
                config.bind(Component.class, ComponentInjectWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependsOnAnotherDependency.class);
                config.bind(DependencyAnother.class, AnotherDependencyDependsOnComponent.class);
                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                        () -> config.getContext());

                List<Class<?>> components = List.of(exception.getComponents());
                assertEquals(3, components.size());
                assertTrue(components.contains(Component.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(DependencyAnother.class));
            }

        }
    }

    interface DependencyAnother {

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


    static class ComponentInjectWithInjectConstructor implements Component {
        private Dependency dependency;

        @Inject
        public ComponentInjectWithInjectConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

    }
}
