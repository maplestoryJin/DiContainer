package com.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {

    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

     class Ref<ComponentType> {
         private Class<ComponentType> component;
         private Type container;

         public static <ComponentType> Ref<ComponentType> of(Class<ComponentType> component) {
             return new Ref<>(component);
         }

         public static Ref of(Type type) {
             return new Ref(type);
         }

         Ref(Type type) {
             init(type);
         }

         public Ref(Class<ComponentType> component) {
             init(component);
         }

         protected Ref() {
             Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
             init(type);
         }

         private void init(Type type) {
             if (type instanceof ParameterizedType container) {
                 component = (Class<ComponentType>) container.getActualTypeArguments()[0];
                 this.container = container.getRawType();
             } else {
                 component = (Class<ComponentType>) type;
             }
         }

         public boolean isContainer() {
             return container != null;
         }

         public Class<?> getComponent() {
             return component;
         }

         public Type getContainer() {
             return container;
         }

         @Override
         public boolean equals(Object o) {
             if (this == o) return true;
             if (o == null || getClass() != o.getClass()) return false;
             Ref ref = (Ref) o;
             return component.equals(ref.component) && Objects.equals(container, ref.container);
         }

         @Override
         public int hashCode() {
             return Objects.hash(component, container);
         }
     }
}
