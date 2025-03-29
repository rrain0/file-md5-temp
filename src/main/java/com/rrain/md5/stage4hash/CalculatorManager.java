package com.rrain.md5.stage4hash;

import com.rrain.md5.event.Event;
import com.rrain.md5.event.EventManager;
import com.rrain.md5.event.SubscriptionHolder;
import com.rrain.md5.stage3read.ReadEv;
import com.rrain.md5.stage3read.ReadEvType;

import java.nio.file.Path;
import java.util.concurrent.*;

public class CalculatorManager implements Runnable {

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    private final int nSimultaneousThreads;



    private final ConcurrentHashMap<Path, Md5Hasher> hasherMap = new ConcurrentHashMap<>(); // Map<absolutePath, Md5Hasher>
    private final BlockingQueue<Event<ReadEv>> fileParts = new LinkedBlockingQueue<>();
    private long MAX_FILE_SIZE = 200 * 1024L * 1024;
    private long totalFileSize = 0;

    private final ExecutorService threads;


    public CalculatorManager(int nSimultaneousThreads, EventManager eventManager) {
        this.nSimultaneousThreads = nSimultaneousThreads;
        threads = Executors.newFixedThreadPool(nSimultaneousThreads);
        this.eventManager = eventManager;
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
        new Thread(this::work).start();

        loop: while (true){
            var event = incomeEvents.take();
            switch (event){
                case ReadEv ev when ev.type==ReadEvType.ALL_READY -> { synchronized (this) {
                    unsubscribe();
                    threads.shutdown();
                    break loop;
                }}
                case ReadEv ev -> { synchronized (this) {
                    if (ev.type==ReadEvType.PART) {
                        totalFileSize += ev.part.part().length;
                        if (totalFileSize>=MAX_FILE_SIZE) eventManager.addEvent(new CalcEv(CalcEvType.OVERLOADED, null));
                    }
                    threads.execute(()->executeTask(ev));
                }}
                default -> {}
            }
        }

    }

    private void work() {
        try {
            while (!threads.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS));
            eventManager.addEvent(new CalcEv(CalcEvType.ALL_READY, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void executeTask(ReadEv ev){
        switch (ev.type){
            case NEW_FILE -> {
                hasherMap.computeIfAbsent(
                    ev.part.getFullPath(),
                    path -> new Md5Hasher()
                );
            }
            case PART -> {
                var hasher = hasherMap.get(ev.part.getFullPath());
                hasher.addNextPart(ev.part.part());
                synchronized (this){
                    totalFileSize -= ev.part.part().length;
                    if (totalFileSize<MAX_FILE_SIZE) eventManager.addEvent(new CalcEv(CalcEvType.READY_TO_WORK, null));
                }
            }
            case FILE_END -> {
                var hasher = hasherMap.remove(ev.part.getFullPath());
                var hash = hasher.getMd5();

                var result = new CalcResult(ev.part.source(), ev.part.srcPath(), ev.part.relPath(), hash);
                eventManager.addEvent(new CalcEv(CalcEvType.FILE_CALCULATED, result));
            }
            case NOT_FOUND -> {
                // файл должен быть, но не найден
                // возможно, что пока читали файл, его удалили
                hasherMap.remove(ev.part.getFullPath());
            }
            case READ_ERROR -> {
                hasherMap.remove(ev.part.getFullPath());
            }
            default -> {}
        }
    }


}
