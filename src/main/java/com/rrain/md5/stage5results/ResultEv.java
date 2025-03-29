package com.rrain.md5.stage5results;

import com.rrain.md5.event.Event;

public class ResultEv extends Event<ResultEvType> {
    public final Result result;

    public ResultEv(ResultEvType type, Result result) {
        super(type);
        this.result = result;
    }
}
