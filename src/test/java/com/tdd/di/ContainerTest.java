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
        private Component dependency;

        @Inject
        public DependencyDependsOnComponent(Component dependency) {
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

    public interface Component {
        default Dependency dependency() {
            return null;
        }
    }

    public interface Dependency {

    }
}
