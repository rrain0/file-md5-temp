package com.rrain.md5.stage3read;

import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage2estimate.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class SourceFiles {
    public final Source src;
    public final List<FileInfo> files = new ArrayList<>();

    public SourceFiles(Source src) {
        this.src = src;
    }
}
