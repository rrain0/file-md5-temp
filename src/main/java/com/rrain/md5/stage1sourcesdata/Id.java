package com.rrain.md5.stage1sourcesdata;

import java.util.concurrent.atomic.AtomicInteger;

public class Id {
    private static final AtomicInteger nextId = new AtomicInteger();
    public final int id;
    public Id(){ id = nextId.getAndIncrement(); }
}
