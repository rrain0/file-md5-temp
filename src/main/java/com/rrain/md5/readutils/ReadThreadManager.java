package com.rrain.md5.readutils;


import java.util.HashSet;
import java.util.Set;

public class ReadThreadManager {

    private final Set<Object> workingThreads = new HashSet<>();

    synchronized public void acquire(Object threadId) {
        try {
            while (workingThreads.contains(threadId)) this.wait();
            workingThreads.add(threadId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void release(Object threadId){
        workingThreads.remove(threadId);
        this.notifyAll();
    }

}
