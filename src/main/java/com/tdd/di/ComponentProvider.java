package com.tdd.di;

import java.util.List;

import static java.util.List.of;

interface ComponentProvider<T> {
    T get(Context context);

    default List<ComponentRef<?>> getDependencies() {
        return of();
    }
}
