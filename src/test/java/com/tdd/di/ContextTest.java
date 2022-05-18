package com.tdd.di;

import com.tdd.di.ContainerTest.*;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Nested
class ContextTest {

    private ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }


    @Nested
    class TypeBindingTest {
        @Test
        void should_bind_type_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Component.class).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        void should_bind_type_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructionInjection.class)),
                    Arguments.of(Named.of("Field Injection", FiledInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructionInjection implements Component {
            private Dependency dependency;

            @Inject
            public ConstructionInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FiledInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            private Dependency dependency;

            @Inject
            public void install(Dependency dependency) {
                this.dependency = dependency;
            }


            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        void should_return_empty_if_component_does_not_exist() {
            assertTrue(config.getContext().get(Component.class).isEmpty());
        }
    }

    @Nested
    class DependencyCheckTest {

        @ParameterizedTest
        @MethodSource
        void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)));
        }

        static class MissingDependencyConstructor implements Component {
            private Dependency dependency;

            @Inject
            public MissingDependencyConstructor(final Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            private Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            private Dependency dependency;

            @Inject
            public void install(final Dependency dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        void should_throw_exception_if_cycle_dependencies_found(Class<? extends Component> component, Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> config.getContext());
            List<Class<?>> components = List.of(exception.getComponents());
            assertEquals(2, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cycle_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (final Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (final Named dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }


        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }


        static class CyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }


        static class CyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            void CyclicDependencyInjectConstructor(Component component) {

            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        void should_throw_exception_if_transitive_cyclic_dependency(Class<? extends Component> component,
                                                                    Class<? extends Dependency> dependency,
                                                                    Class<? extends AnotherDependency> anotherDependency) {
            config.bind(Component.class, ComponentInjectWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependsOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependsOnComponent.class);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> config.getContext());

            List<Class<?>> components = List.of(exception.getComponents());
            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependency() {
            List<Arguments> arguments = new ArrayList<>();

            for (final Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (final Named dependency : List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class))) {
                    for (final Named anotherDependency : List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class))) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }


        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            Component component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(Component component) {
            }
        }

    }
}
