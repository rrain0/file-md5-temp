package com.rrain.md5.readutils;

import java.util.List;

public class ReadThreadBox<T> {

    public ReadThreadBox(List<T> srcFiles) {
        this.srcFiles = srcFiles;
        adjustIndex();
    }

    private Integer curr = 0;
    private final List<T> srcFiles;


    private void adjustIndex(){
        if (srcFiles.isEmpty()) curr = null;
        else {
            if (curr==null) curr=0;
            curr = curr%srcFiles.size();
        }
    }

    public List<T> getList(){
        return srcFiles.stream().toList();
    }


    public void add(T elem){
        srcFiles.add(elem);
        adjustIndex();
    }
    public void remove(T elem){
        srcFiles.remove(elem);
        adjustIndex();
    }


    public T next(){
        if (curr==null) return null;
        curr++;
        adjustIndex();
        return srcFiles.get(curr);
    }

    public T get(){ return curr==null ? null : srcFiles.get(curr); }

}
