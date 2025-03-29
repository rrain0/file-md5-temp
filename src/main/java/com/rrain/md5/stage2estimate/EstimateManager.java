package com.rrain.md5.stage2estimate;

import com.rrain.md5.event.Event;
import com.rrain.md5.event.EventManager;
import com.rrain.md5.event.SubscriptionHolder;
import com.rrain.md5.readutils.ReadThreadBox;
import com.rrain.md5.readutils.ReadThreadManager;
import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage1sourcesdata.SourceEv;
import com.rrain.md5.stage1sourcesdata.SourceEvType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


public class EstimateManager implements Runnable{

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    private final ReadThreadManager threadManager;

    private Map<Object, ReadThreadBox<Source>> sourceMap; // Map<readThreadId, SourceInfoBox>
    private Set<Source> paused;

    public EstimateManager(EventManager eventManager, ReadThreadManager threadManager) {
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


    @Override
    public void run(){
        try {
            start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void start() throws InterruptedException {

        while (true){
            Event<?> event = incomeEvents.take();
            if (event instanceof SourceEv ev && ev.type==SourceEvType.ALL_READY){ synchronized (this){
                unsubscribe();

                var sources = ev.sources;

                sourceMap = sources.stream().collect(Collectors.groupingBy(
                    Source::readThreadId,
                    Collectors.collectingAndThen(Collectors.toList(), ReadThreadBox::new)
                ));
                paused =new HashSet<>(sources);

                sourceMap.forEach((tId,box)->{
                    box.getList().forEach(src->new Thread(new EstimateTask(src, this, eventManager)).start());
                    paused.remove(box.get());
                });

                new Thread(this::work).start();

                break;
            }}

        }
    }

    synchronized private void work() {
        try {
            while (!sourceMap.isEmpty()){
                sourceMap.forEach((id,box)->{
                    if (paused.containsAll(box.getList())) {
                        paused.remove(box.next());
                    }
                });
                this.notifyAll();
                this.wait();
            }
            eventManager.addEvent(new EstimateEv(EstimateEvType.ALL_READY, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void awaitForWork(Source source) throws InterruptedException {
        while (paused.contains(source)) this.wait();
        threadManager.acquire(source.readThreadId());
    }


    synchronized public void yield(Source source){
        paused.add(source);
        threadManager.release(source.readThreadId());
        this.notifyAll();
    }

    synchronized public void workFinished(Source source){
        paused.remove(source);
        sourceMap.compute(source.readThreadId(),(tId, box)->{
            box.remove(source);
            if (box.getList().isEmpty()){
                eventManager.addEvent(new EstimateEv(EstimateEvType.READ_THREAD_VIEWED, new FileInfo(source, null, null, null)));
                return null;
            }
            return box;
        });
        eventManager.addEvent(new EstimateEv(EstimateEvType.SOURCE_VIEWED, new FileInfo(source, null, null, null)));
        this.notifyAll();
    }



}
