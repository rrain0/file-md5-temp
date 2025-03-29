package com.rrain.md5.stage2estimate;

import com.rrain.md5.event.EventManager;
import com.rrain.md5.stage1sourcesdata.Source;

import java.io.File;
import java.nio.file.Path;


public class EstimateTask implements Runnable {
    private final Source src;
    private final EstimateManager manager;
    private final EventManager eventManager;


    public EstimateTask(Source src, EstimateManager manager, EventManager eventManager) {
        this.src = src;
        this.manager = manager;
        this.eventManager = eventManager;
    }


    @Override
    public void run() {
        try {
            for (var src : src.paths()) walkFileTree(src,Path.of(""));
            manager.workFinished(src);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void walkFileTree(Path s, Path p) throws InterruptedException {
        manager.awaitForWork(src);
        File f = s.resolve(p).toFile();
        if (f.isFile()){
            manager.yield(src);
            eventManager.addEvent(new EstimateEv(EstimateEvType.FILE_FOUND, new FileInfo(src,s,p,f.length())));
        } else if (f.isDirectory()){
            for (File ff : f.listFiles()){
                Path pp = p.resolve(ff.getName());
                walkFileTree(s,pp);
            }
            eventManager.addEvent(new EstimateEv(EstimateEvType.DIRECTORY_VIEWED, new FileInfo(src, s,p, null)));
        }
    }



}
