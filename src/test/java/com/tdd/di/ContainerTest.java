package com.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;

public class ContainerTest {


    @Nested
    public class DependenciesSelection {
    }

    @Nested
    public class LifecycleManagement {
    }

    interface AnotherDependency {

    }


    static class DependencyDependsOnComponent implements Dependency {
        private TestComponent dependency;

        @Inject
        public DependencyDependsOnComponent(TestComponent dependency) {
            this.dependency = dependency;
        }
    }


    static class DependencyDependsOnAnotherDependency implements Dependency {
        private AnotherDependency dependency;

        @Inject
        public DependencyDependsOnAnotherDependency(AnotherDependency dependency) {
            this.dependency = dependency;
        }
    }

    static class AnotherDependencyDependsOnComponent implements AnotherDependency {
        private TestComponent component;

        @Inject
        public AnotherDependencyDependsOnComponent(TestComponent component) {
            this.component = component;
        }
    }


    static class ComponentInjectWithInjectConstructor implements TestComponent {
        private Dependency dependency;

        @Inject
        public ComponentInjectWithInjectConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

    }

    public interface TestComponent {
        default Dependency dependency() {
            return null;
        }
    }

    public interface Dependency {

    }
}
