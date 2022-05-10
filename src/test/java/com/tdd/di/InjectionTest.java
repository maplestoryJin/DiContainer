package com.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
class InjectionTest {

    private Dependency dependency = mock(Dependency.class);
    private Context context = mock(Context.class);
    ;

    @BeforeEach
    void setUp() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }


    @Nested
    class ConstructorInjectionTest {


        static class InjectConstructor implements Component {
            Dependency dependency;

            @Inject
            public InjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        @Nested
        class Injection {

            @Test
            void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor instance = new ConstructorInjectionProvider<>(DefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class DefaultConstructor implements Component {
                public DefaultConstructor() {
                }
            }

            @Test
            void should_inject_dependency_via_inject_constructor() {
                InjectConstructor component = new ConstructorInjectionProvider<>(InjectConstructor.class).get(context);
                assertNotNull(component);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_constructor_dependency_in_dependencies() {
                ConstructorInjectionProvider<InjectConstructor> provider = new ConstructorInjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));

            }

        }

        @Nested
        class IllegalInjectConstructor {


            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () ->
                        new ConstructorInjectionProvider<>(AbstractComponent.class));
            }

            static abstract class AbstractComponent implements Component {
                @Inject
                AbstractComponent() {
                }
            }


            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () ->
                        new ConstructorInjectionProvider<>(Component.class));
            }


            @Test
            void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MultiInjectConstructors.class));
            }


            static class MultiInjectConstructors implements Component {
                @Inject
                public MultiInjectConstructors(Dependency dependency) {
                }

                @Inject
                public MultiInjectConstructors(Component component) {

                }
            }


            @Test
            void should_throw_exception_if_no_default_constructor_nor_inject_constructor() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(NoDefaultConstructorNorInjectConstructor.class));
            }

            static class NoDefaultConstructorNorInjectConstructor implements Component {
                public NoDefaultConstructorNorInjectConstructor(Dependency dependency) {
                }
            }

        }

    }


    @Nested
    class FiledInjectionTest {

        @Nested
        class Injection {


            static class ComponentWithInjectField implements Component {
                @Inject
                Dependency dependency;
            }

            static class SuperClassWithInjectField extends ComponentWithInjectField {

            }

            @Test
            void should_inject_dependency_via_field() {

                ComponentWithInjectField component = new ConstructorInjectionProvider<>(ComponentWithInjectField.class).get(context);
                assertSame(dependency, component.dependency);
            }


            @Test
            void should_inject_dependency_via_superclass_inject_field() {
                SuperClassWithInjectField component = new ConstructorInjectionProvider<>(SuperClassWithInjectField.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithInjectField> provider = new ConstructorInjectionProvider<>(ComponentWithInjectField.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

        }

        @Nested
        class IllegalInjectField {
            @Test
            void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () ->
                        new ConstructorInjectionProvider<>(FinalFieldInject.class));

            }

            static class FinalFieldInject {
                @Inject
                final Dependency dependency = null;
            }
        }
    }


    @Nested
    class MethodInjectionTest {

        @Nested
        class Injection {


            static class InjectMethodWithNoArgs {
                private boolean installed = false;

                @Inject
                public void install() {
                    installed = true;
                }
            }


            static class ComponentWithInjectMethod {
                private Dependency dependency;

                @Inject
                public void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }


            @Test
            void should_class_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoArgs component = new ConstructorInjectionProvider<>(InjectMethodWithNoArgs.class).get(context);
                assertTrue(component.installed);
            }

            @Test
            void should_inject_dependency_via_method() {
                ComponentWithInjectMethod component = new ConstructorInjectionProvider<>(ComponentWithInjectMethod.class).get(context);
                assertSame(dependency, component.dependency);
            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                public void install() {
                    superCalled++;
                }
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                public void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            void should_inject_dependencies_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component = new ConstructorInjectionProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubclassWithOverrideSuperClassWithInjectMethod extends SuperClassWithInjectMethod {

                @Inject
                @Override
                public void install() {
                    super.install();
                }
            }

            @Test
            void should_only_call_once_if_subclass_override_inject_method() {
                SubclassWithOverrideSuperClassWithInjectMethod component = new ConstructorInjectionProvider<>(SubclassWithOverrideSuperClassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
            }

            static class SubclassWithOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {

                @Override
                public void install() {
                    super.install();
                }
            }


            @Test
            void should_not_call_inject_method_if_override_inject_method_not_inject_annotation() {
                SubclassWithOverrideSuperClassWithNoInject component = new ConstructorInjectionProvider<>(SubclassWithOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            @Test
            void should_include_method_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithInjectMethod> provider = new ConstructorInjectionProvider<>(ComponentWithInjectMethod.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

        }

        @Nested
        class IllegalInjectMethod {
            @Test
            void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () ->
                        new ConstructorInjectionProvider<>(TypeParameterInjectMethod.class));
            }

            static class TypeParameterInjectMethod {
                @Inject
                <T> void install(T t) {
                }
            }
        }


    }

}

