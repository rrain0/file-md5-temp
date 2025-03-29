package com.rrain.md5.stage4hash;

import com.rrain.md5.event.Event;

public class CalcEv extends Event<CalcEvType> {
    public final CalcResult result;

    public CalcEv(CalcEvType type, CalcResult result) {
        super(type);
        this.result = result;
    }
}
