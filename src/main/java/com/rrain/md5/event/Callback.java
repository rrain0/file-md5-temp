package com.rrain.md5.event;

@FunctionalInterface
public interface Callback {
    void accept(Event<?> ev) throws InterruptedException;
}
