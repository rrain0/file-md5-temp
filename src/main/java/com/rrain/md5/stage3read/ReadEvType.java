package com.rrain.md5.stage3read;

public enum ReadEvType {
    NEW_FILE, PART, FILE_END,
    NOT_FOUND, READ_ERROR, SOURCE_FINISHED,
    ALL_READY
}
