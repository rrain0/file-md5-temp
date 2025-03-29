package com.rrain.md5.stage4hash;

import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage3read.FilePart;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

// todo ограничить размер задавая суммарный максимальный размер данных файлов (т.е. учитывать только byte[] файла)

public class Cache {
    //final long maxBytes;


    // todo:
    // Map<SourceInfo,Queue<FilePart>>
    // this is Map.of(source, new queue)

    // todo возможность зарезервировать место
    // наверное лучше запросить у кэша часть файла


    // блокирует поток
    /*public void reserve(long bytes){

    }
    public void free(long bytes){

    }*/

    private final long maxSize = 500*1024L*1024;
    private final Map<Source, BlockingQueue<FilePart>> map;
    //private volatile long size = 0;

    public Cache(Collection<Source> sources) {
        map = sources.stream().collect(
            Collectors.toUnmodifiableMap(info->info, info->new LinkedBlockingQueue<>())
        );
    }

    public void add(FilePart filePart) throws InterruptedException {
        //long partSz = filePart.part()==null ? 0 : filePart.part().length;
        //while (partSz+size > maxSize) this.wait();
        //size += filePart.len();
        map.get(filePart.source()).put(filePart);
    }

    public FilePart take(Source source) throws InterruptedException {
        //System.out.println("source");
        var elem = map.get(source).take();
        //size -= elem.len();
        //this.notifyAll();
        return elem;
    }
}
