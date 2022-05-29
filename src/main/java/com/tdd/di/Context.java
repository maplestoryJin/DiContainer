package com.tdd.di;

import java.util.Optional;

public interface Context {

    <ComponentType> Optional<ComponentType> get(Ref<ComponentType> ref);

}
