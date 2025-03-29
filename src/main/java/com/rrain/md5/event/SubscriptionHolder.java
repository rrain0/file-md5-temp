package com.rrain.md5.event;

public record SubscriptionHolder(EventManager eventManager, Callback callback) {
    public void unsubscribe(){
        eventManager.unsubscribe(callback);
    }
}
