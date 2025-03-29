package com.rrain.md5.stage3read;

import lombok.Builder;
import com.rrain.md5.stage1sourcesdata.Source;

import java.nio.file.Path;


public record FilePart(
    byte[] part,

    //Info info,

    long from,
    long to,
    long len,

    Source source,
    Path srcPath,
    Path relPath
){
    /*public enum Info{
        NEW_FILE, PART, FILE_END,
        NOT_FOUND, READ_ERROR, SOURCE_FINISHED
    }*/

    public Path getFullPath(){
        return srcPath.resolve(relPath);
    }

    @Builder
    public FilePart {}
}
