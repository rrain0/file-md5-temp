package com.rrain.md5.event;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public class Event<T> {
    public final T type;
}
