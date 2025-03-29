package com.rrain.md5.stage3read;

import com.rrain.md5.event.Event;
import com.rrain.md5.event.EventManager;
import com.rrain.md5.event.SubscriptionHolder;
import com.rrain.md5.readutils.ReadThreadBox;
import com.rrain.md5.readutils.ReadThreadManager;
import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage1sourcesdata.SourceEv;
import com.rrain.md5.stage1sourcesdata.SourceEvType;
import com.rrain.md5.stage2estimate.EstimateEv;
import com.rrain.md5.stage2estimate.EstimateEvType;
import com.rrain.md5.stage4hash.CalcEv;
import com.rrain.md5.stage4hash.CalcEvType;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class ReadManager implements Runnable{


    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    private final ReadThreadManager threadManager;


    private Map<Source,SourceFiles> srcToSrcFilesMap;


    private Map<Object, ReadThreadBox<SourceFiles>> threadMap; // Map<readThreadId, SourceInfoBox>
    private Set<SourceFiles> paused;


    private boolean ramOverload = false;

    public ReadManager(EventManager eventManager, ReadThreadManager threadManager) {
        this.eventManager = eventManager;
        this.threadManager = threadManager;
        subscribe();
    }

    synchronized private void subscribe(){
        holder = eventManager.subscribe(incomeEvents::put);
    }
    synchronized private void unsubscribe(){
        holder.unsubscribe();
        incomeEvents.clear();
    }


    public void run(){
        try {
            start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void start() throws InterruptedException {

        loop: while (true){
            var event = incomeEvents.take();
            switch (event){
                case SourceEv ev when ev.type==SourceEvType.ALL_READY -> { synchronized (this) {
                    List<SourceFiles> sourceFilesList = ev.sources.stream().map(SourceFiles::new).toList();
                    srcToSrcFilesMap = sourceFilesList.stream().collect(HashMap::new, (map,sf)->map.put(sf.src, sf), Map::putAll);
                    threadMap = sourceFilesList.stream().collect(Collectors.groupingBy(
                        sf->sf.src.readThreadId(), Collectors.collectingAndThen(Collectors.toList(), ReadThreadBox::new)
                    ));
                    paused = new HashSet<>(sourceFilesList);
                    new Thread(this::work).start();
                }}
                case EstimateEv ev when ev.type==EstimateEvType.FILE_FOUND -> { synchronized (this) {
                        srcToSrcFilesMap.get(ev.fileInfo.src()).files.add(ev.fileInfo);
                }}
                case EstimateEv ev when ev.type==EstimateEvType.READ_THREAD_VIEWED -> { synchronized (this) {
                    var box = threadMap.get(ev.fileInfo.src().readThreadId());
                    box.getList().forEach(srcFiles->new Thread(new ReadTask(srcFiles, this, eventManager)).start());
                    paused.remove(box.get());
                    notifyAll();
                }}

                case CalcEv ev when ev.type==CalcEvType.READY_TO_WORK -> { synchronized (this) {
                    ramOverload = false;
                    notifyAll();
                }}
                case CalcEv ev when ev.type==CalcEvType.OVERLOADED -> { synchronized (this) {
                    ramOverload = true;
                    notifyAll();
                }}
                case CalcEv ev when ev.type==CalcEvType.ALL_READY -> {
                    unsubscribe();
                    break loop;
                }

                default -> {}
            }
        }

    }

    synchronized private void work() {
        try {
            while (!threadMap.isEmpty()){
                threadMap.forEach((id,box)->{
                    if (paused.containsAll(box.getList())) {
                        paused.remove(box.next());
                    }
                });
                this.notifyAll();
                this.wait();
            }
            eventManager.addEvent(new ReadEv(ReadEvType.ALL_READY, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void awaitForWork(SourceFiles source) throws InterruptedException {
        while (paused.contains(source) && ramOverload) this.wait();
        threadManager.acquire(source.src.readThreadId());
    }

    synchronized public void oneFileWasRead(SourceFiles source){
        paused.add(source);
        threadManager.release(source.src.readThreadId());
        this.notifyAll();
    }

    synchronized public void workFinished(SourceFiles source){
        paused.remove(source);
        threadMap.compute(source.src.readThreadId(),(tId, box)->{
            box.remove(source);
            if (box.getList().isEmpty()){
                return null;
            }
            return box;
        });
        FilePart fp = FilePart.builder()
            .source(source.src)
            .build();
        eventManager.addEvent(new ReadEv(ReadEvType.SOURCE_FINISHED, fp));
        this.notifyAll();
    }


}
