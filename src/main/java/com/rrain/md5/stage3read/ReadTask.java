package com.rrain.md5.stage3read;

import com.rrain.md5.event.EventManager;

import java.io.*;
import java.nio.file.Path;


// todo set max filepart in ram

public class ReadTask implements Runnable {
    private final SourceFiles source;
    private final ReadManager readManager;
    private final EventManager eventManager;




    public ReadTask(SourceFiles source, ReadManager readManager, EventManager eventManager) {
        this.source = source;
        this.readManager = readManager;
        this.eventManager = eventManager;
    }


    @Override
    public void run() {
        try {
            for (var info : source.files){
                readManager.awaitForWork(source);
                File f = info.srcPath().resolve(info.relPath()).toFile();
                readFile(f,info.relPath());
                readManager.oneFileWasRead(source);
            }
            readManager.workFinished(source);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void readFile(File f, Path relativePath) throws InterruptedException {
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))){
            final long len = f.length();
            final int chunkSz = 10*1024*1024; // размер считываемого за раз куска в байтах

            {
                FilePart fp = FilePart.builder()
                    .source(source.src)
                    .relPath(relativePath)
                    .len(len)
                    .build();
                eventManager.addEvent(new ReadEv(ReadEvType.NEW_FILE, fp));
            }

            for (long from = 0, to = 0; to<len; from=to){
                to = Long.min(from+chunkSz,len);
                byte[] buf = new byte[(int) (to-from)];
                bis.read(buf);

                FilePart fp = FilePart.builder()
                    .source(source.src)
                    .relPath(relativePath)
                    .from(from)
                    .to(to)
                    .len(len)
                    .part(buf)
                    .build();
                eventManager.addEvent(new ReadEv(ReadEvType.PART, fp));
            }

            {
                FilePart fp = FilePart.builder()
                    .source(source.src)
                    .relPath(relativePath)
                    .len(len)
                    .build();
                eventManager.addEvent(new ReadEv(ReadEvType.FILE_END, fp));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            FilePart fp = FilePart.builder()
                .source(source.src)
                .relPath(relativePath)
                .build();
            eventManager.addEvent(new ReadEv(ReadEvType.NOT_FOUND, fp));
        } catch (IOException e) {
            e.printStackTrace();

            FilePart fp = FilePart.builder()
                .source(source.src)
                .relPath(relativePath)
                .build();
            eventManager.addEvent(new ReadEv(ReadEvType.READ_ERROR, fp));
        }
    }

}
