package com.rrain.md5.stage2estimate;

import lombok.ToString;
import com.rrain.md5.event.Event;

@ToString(callSuper = true)
public class EstimateEv extends Event<EstimateEvType> {
    public final FileInfo fileInfo;

    public EstimateEv(EstimateEvType type, FileInfo fileInfo) {
        super(type);
        this.fileInfo = fileInfo;
    }
}
