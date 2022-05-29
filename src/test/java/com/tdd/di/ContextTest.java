package com.tdd.di;

import com.tdd.di.ContainerTest.*;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
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
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            assertSame(instance, config.getContext().get(Ref.of(TestComponent.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        void should_bind_type_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);
            Optional<TestComponent> component = config.getContext().get(Ref.of(TestComponent.class));
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructionInjection.class)),
                    Arguments.of(Named.of("Field Injection", FiledInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructionInjection implements TestComponent {
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

        static class FiledInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
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
            assertTrue(config.getContext().get(Ref.of(TestComponent.class)).isEmpty());
        }

        @Test
        void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Provider<TestComponent> provider = config.getContext().get(new Ref<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);


            assertFalse(config.getContext().get(new Ref<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        class WithQualifier {
            @Test
            void should_bind_instance_with_multi_qualifiers() {
                TestComponent instance = new TestComponent() {
                };

                config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new NamedLiteral("Skywalker"));
                TestComponent chosenOne = config.getContext().get(Ref.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = config.getContext().get(Ref.of(TestComponent.class, new NamedLiteral("Skywalker"))).get();
                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }


            @Test
            void should_bind_component_with_multi_qualifiers() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(TestComponent.class, ConstructionInjection.class, new NamedLiteral("ChosenOne"), new NamedLiteral("Skywalker"));

                TestComponent chosenOne = config.getContext().get(Ref.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = config.getContext().get(Ref.of(TestComponent.class, new NamedLiteral("Skywalker"))).get();
                assertSame(dependency, chosenOne.dependency());
                assertSame(dependency, skywalker.dependency());
            }


            // TODO throw illegal component if illegal qualifier
        }
    }

    @Nested
    class DependencyCheckTest {

        @ParameterizedTest
        @MethodSource
        void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class,
                    () -> config.getContext());
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(TestComponent.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)));
        }

        static class MissingDependencyConstructor implements TestComponent {
            private Dependency dependency;

            @Inject
            public MissingDependencyConstructor(final Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            private Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            private Dependency dependency;

            @Inject
            public void install(final Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependencyProvider) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependencyProvider;
        }


        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependencyProvider) {

            }
        }


        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        void should_throw_exception_if_cycle_dependencies_found(Class<? extends TestComponent> component, Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> config.getContext());
            List<Class<?>> components = List.of(exception.getComponents());
            assertEquals(2, components.size());
            assertTrue(components.contains(TestComponent.class));
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


        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }


        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }


        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            void CyclicDependencyInjectConstructor(TestComponent component) {

            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent component) {
            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        void should_throw_exception_if_transitive_cyclic_dependency(Class<? extends TestComponent> component,
                                                                    Class<? extends Dependency> dependency,
                                                                    Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, ComponentInjectWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependsOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependsOnComponent.class);
            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class,
                    () -> config.getContext());

            List<Class<?>> components = List.of(exception.getComponents());
            assertEquals(3, components.size());
            assertTrue(components.contains(TestComponent.class));
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
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(TestComponent component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> componentProvider) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider_constructor() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);
            assertTrue(config.getContext().get(Ref.of(Dependency.class)).isPresent());
        }

        @Nested
        class WithQualifier {
            // TODO dependency missing if qualifier not match
            // TODO check cyclic dependencies with qualifier
        }
    }


    record NamedLiteral(String value) implements Annotation {

        @Override
        public Class<? extends Annotation> annotationType() {
            return jakarta.inject.Named.class;
        }
    }
}
