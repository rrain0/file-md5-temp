package com.rrain.md5.stage1sourcesdata;

import lombok.Builder;
import lombok.Singular;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Source {
    private final List<Path> paths;
    private final Object readThreadId;
    private final Object tag;


    @Builder
    public Source(@Singular List<String> paths, Object readThreadId, Object tag) {
        this.paths = paths.stream().map(Path::of).toList();
        this.readThreadId = Optional.ofNullable(readThreadId).orElse(new Id());
        this.tag = tag;
    }

    public List<Path> paths() { return paths; }
    public Object readThreadId() { return readThreadId; }
    public Object tag() { return tag; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Source) obj;
        return Objects.equals(this.paths, that.paths) &&
            Objects.equals(this.readThreadId, that.readThreadId) &&
            Objects.equals(this.tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, readThreadId, tag);
    }

    @Override
    public String toString() {
        return "Source[" +
            "paths=" + paths + ", " +
            "readThreadId=" + readThreadId + ", " +
            "tag=" + tag + ']';
    }

}
