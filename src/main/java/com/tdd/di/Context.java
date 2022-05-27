package com.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public interface Context {

     Optional get(Ref ref);

     class Ref {
         private Class<?> component;
         private Type container;

         static Ref of(Type type) {
             if (type instanceof ParameterizedType container) return new Ref(container);
             return new Ref((Class<?>) type);
         }

         public Ref(ParameterizedType type) {
             component = (Class<?>) type.getActualTypeArguments()[0];
             container = type.getRawType();
         }

         public Ref(Class<?> type) {
             component = type;
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
