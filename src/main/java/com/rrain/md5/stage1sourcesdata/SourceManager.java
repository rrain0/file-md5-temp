package com.rrain.md5.stage1sourcesdata;

import com.rrain.md5.event.EventManager;

import java.util.List;

public class SourceManager implements Runnable {

    private final List<Source> sources;
    private final EventManager eventManager;

    public SourceManager(List<Source> sources, EventManager eventManager) {
        this.sources = sources;
        this.eventManager = eventManager;
    }

    @Override
    public void run() {
        eventManager.addEvent(new SourceEv(SourceEvType.ALL_READY, sources));
    }
}
