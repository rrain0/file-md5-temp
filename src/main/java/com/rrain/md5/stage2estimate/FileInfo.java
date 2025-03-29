package com.rrain.md5.stage2estimate;

import com.rrain.md5.stage1sourcesdata.Source;

import java.nio.file.Path;

public record FileInfo(Source src, Path srcPath, Path relPath, Long sz){}
