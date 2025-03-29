package com.rrain.md5.stage1sourcesdata;

import lombok.ToString;
import com.rrain.md5.event.Event;

import java.util.List;

@ToString(callSuper = true)
public class SourceEv extends Event<SourceEvType> {
    public final List<Source> sources;

    public SourceEv(SourceEvType type, List<Source> sources) {
        super(type);
        this.sources = sources;
    }
}
