package com.rrain.md5.event;

import java.util.ArrayList;
import java.util.List;


public class EventManager {

    private final List<Callback> callbacks = new ArrayList<>();

    public synchronized SubscriptionHolder subscribe(Callback callback){
        var holder = new SubscriptionHolder(this, callback);
        callbacks.add(callback);
        return holder;
    }

    synchronized public void addEvent(Event<?> ev) {
        callbacks.forEach(cb -> {
            try { cb.accept(ev); }
            catch (InterruptedException e) {  e.printStackTrace(); }
        });
    }

    synchronized public void unsubscribe(Callback callback) {
        callbacks.remove(callback);
    }
}
