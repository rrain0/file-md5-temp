package com.rrain.md5.stage5results;

import com.rrain.md5.stage1sourcesdata.Source;

import java.nio.file.Path;

public record Result(
    Source source, Path relPath, String md5
) { }
