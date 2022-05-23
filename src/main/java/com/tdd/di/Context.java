package com.tdd.di;

import java.lang.reflect.Type;
import java.util.Optional;

public interface Context {

     Optional get(Type type);
}
