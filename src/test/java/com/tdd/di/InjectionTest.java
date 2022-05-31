package com.tdd.di;

import com.tdd.di.ContainerTest.Dependency;
import com.tdd.di.ContainerTest.TestComponent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
class InjectionTest {

    private Dependency dependency = mock(Dependency.class);
    private Context context = mock(Context.class);
    private Provider<Dependency> dependencyProvider = mock(Provider.class);
    private ParameterizedType providerDependencyType;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        providerDependencyType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));
        when(context.get(eq(ComponentRef.of(providerDependencyType)))).thenReturn(Optional.of(dependencyProvider));
    }


    @Nested
    class ConstructorInjectionTest {


        static class InjectConstructor implements TestComponent {
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
                DefaultConstructor instance = new InjectionProvider<>(DefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class DefaultConstructor implements TestComponent {
                public DefaultConstructor() {
                }
            }

            @Test
            void should_inject_dependency_via_inject_constructor() {
                InjectConstructor component = new InjectionProvider<>(InjectConstructor.class).get(context);
                assertNotNull(component);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_constructor_dependency_in_dependencies() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));

            }


            @Test
            void should_inject_provider_via_inject_constructor() {
                ProviderInjectConstructor constructor = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);
                assertSame(dependencyProvider, constructor.dependency);
            }


            @Test
            void should_include_dependency_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(providerDependencyType)}, provider.getDependencies().toArray(ComponentRef[]::new));

            }

            static class ProviderInjectConstructor {
                private Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Nested
            class WithQualifier {

                @BeforeEach
                void before() {
                    Mockito.reset(context);
                    when(context.get(ComponentRef.of(Dependency.class, new ContextTest.NamedLiteral("ChosenOne")))).thenReturn(Optional.of(dependency));
                }

                static class InjectConstructor {
                    Dependency dependency;

                    @Inject
                    public InjectConstructor(@Named("ChosenOne") Dependency dependency) {
                        this.dependency = dependency;
                    }
                }

                @Test
                void should_inject_dependency_with_qualifier_via_constructor() {
                    InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                    assertSame(dependency, provider.get(context).dependency);

                }

                static class MultiQualifierInjectConstructor {

                    @Inject
                    public MultiQualifierInjectConstructor(@Named("ChosenOne")
                                                           @ContextTest.Skywalker Dependency dependency) {
                    }
                }

                @Test
                void should_throw_exception_if_multi_qualifiers_given() {
                    assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectConstructor.class));
                }

                @Test
                void should_include_dependencies_with_qualifier() {
                    InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                    assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new ContextTest.NamedLiteral("ChosenOne"))},
                            provider.getDependencies().toArray());
                }

            }
        }

        @Nested
        class IllegalInjectConstructor {


            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(AbstractComponent.class));
            }

            static abstract class AbstractComponent implements TestComponent {
                @Inject
                AbstractComponent() {
                }
            }


            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(TestComponent.class));
            }


            @Test
            void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiInjectConstructors.class));
            }


            static class MultiInjectConstructors implements TestComponent {
                @Inject
                public MultiInjectConstructors(Dependency dependency) {
                }

                @Inject
                public MultiInjectConstructors(TestComponent component) {

                }
            }


            @Test
            void should_throw_exception_if_no_default_constructor_nor_inject_constructor() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(NoDefaultConstructorNorInjectConstructor.class));
            }

            static class NoDefaultConstructorNorInjectConstructor implements TestComponent {
                public NoDefaultConstructorNorInjectConstructor(Dependency dependency) {
                }
            }

        }

    }


    @Nested
    class FiledInjectionTest {

        @Nested
        class Injection {


            static class ComponentWithInjectField implements TestComponent {
                @Inject
                Dependency dependency;
            }

            static class SuperClassWithInjectField extends ComponentWithInjectField {

            }

            @Test
            void should_inject_dependency_via_field() {

                ComponentWithInjectField component = new InjectionProvider<>(ComponentWithInjectField.class).get(context);
                assertSame(dependency, component.dependency);
            }


            @Test
            void should_inject_dependency_via_superclass_inject_field() {
                SuperClassWithInjectField component = new InjectionProvider<>(SuperClassWithInjectField.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_field_dependency_in_dependencies() {
                InjectionProvider<ComponentWithInjectField> provider = new InjectionProvider<>(ComponentWithInjectField.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


            @Test
            void should_inject_provider_via_inject_field() {
                ProviderInjectField constructor = new InjectionProvider<>(ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, constructor.dependency);
            }

            @Test
            void should_include_dependency_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(providerDependencyType)}, provider.getDependencies().toArray(ComponentRef[]::new));

            }

            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;

            }

            @Nested
            class WithQualifier {

                @BeforeEach
                void before() {
                    Mockito.reset(context);
                    when(context.get(ComponentRef.of(Dependency.class, new ContextTest.NamedLiteral("ChosenOne")))).thenReturn(Optional.of(dependency));
                }

                static class InjectField {

                    @Inject
                    @Named("ChosenOne")
                    Dependency dependency;
                }


                @Test
                void should_inject_dependency_with_qualifier_via_field() {
                    InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);
                    assertSame(dependency, provider.get(context).dependency);

                }

                @Test
                void should_include_dependencies_with_qualifier() {
                    InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);
                    assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new ContextTest.NamedLiteral("ChosenOne"))},
                            provider.getDependencies().toArray());
                }

                static class MultiQualifierInjectField {
                    @Named("ChosenOne")
                    @ContextTest.Skywalker
                    @Inject
                    Dependency dependency;
                }

                @Test
                void should_throw_exception_if_multi_qualifiers_given() {
                    assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectField.class));
                }

            }

        }

        @Nested
        class IllegalInjectField {
            @Test
            void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(FinalFieldInject.class));

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
                InjectMethodWithNoArgs component = new InjectionProvider<>(InjectMethodWithNoArgs.class).get(context);
                assertTrue(component.installed);
            }

            @Test
            void should_inject_dependency_via_method() {
                ComponentWithInjectMethod component = new InjectionProvider<>(ComponentWithInjectMethod.class).get(context);
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
                SubclassWithInjectMethod component = new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);
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
                SubclassWithOverrideSuperClassWithInjectMethod component = new InjectionProvider<>(SubclassWithOverrideSuperClassWithInjectMethod.class).get(context);
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
                SubclassWithOverrideSuperClassWithNoInject component = new InjectionProvider<>(SubclassWithOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            @Test
            void should_include_method_dependency_in_dependencies() {
                InjectionProvider<ComponentWithInjectMethod> provider = new InjectionProvider<>(ComponentWithInjectMethod.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


            @Test
            void should_inject_provider_via_inject_method() {
                ProviderInjectMethod constructor = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, constructor.dependency);
            }


            @Test
            void should_include_dependency_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(providerDependencyType)}, provider.getDependencies().toArray(ComponentRef[]::new));

            }

            static class ProviderInjectMethod {
                private Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Nested
            class WithQualifier {
                @BeforeEach
                void before() {
                    Mockito.reset(context);
                    when(context.get(ComponentRef.of(Dependency.class, new ContextTest.NamedLiteral("ChosenOne")))).thenReturn(Optional.of(dependency));
                }

                static class InjectMethod {
                    Dependency dependency;

                    @Inject
                    void install(@Named("ChosenOne") Dependency dependency) {
                        this.dependency = dependency;
                    }
                }

                @Test
                void should_inject_dependency_with_qualifier_via_method() {
                    InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                    assertSame(dependency, provider.get(context).dependency);

                }

                @Test
                void should_include_dependencies_with_qualifier() {
                    InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                    assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new ContextTest.NamedLiteral("ChosenOne"))},
                            provider.getDependencies().toArray());
                }

                static class MultiQualifierInjectMethod {

                    @Inject
                    void install(@Named("ChosenOne")
                                 @ContextTest.Skywalker Dependency dependency) {
                    }
                }

                @Test
                void should_throw_exception_if_multi_qualifiers_given() {
                    assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectMethod.class));
                }

            }
        }

        @Nested
        class IllegalInjectMethod {
            @Test
            void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () ->
                        new InjectionProvider<>(TypeParameterInjectMethod.class));
            }

            static class TypeParameterInjectMethod {
                @Inject
                <T> void install(T t) {
                }
            }
        }


    }

}

