package com.rrain.md5.print;

import com.rrain.md5.event.Event;
import com.rrain.md5.event.EventManager;
import com.rrain.md5.event.SubscriptionHolder;
import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage1sourcesdata.SourceEv;
import com.rrain.md5.stage1sourcesdata.SourceEvType;
import com.rrain.md5.stage2estimate.EstimateEv;
import com.rrain.md5.stage2estimate.EstimateEvType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PrintManager implements Runnable {

    private final EventManager manager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    public PrintManager(EventManager manager) {
        this.manager = manager;
        subscribe();
    }

    private void subscribe(){
        holder = manager.subscribe(incomeEvents::put);
    }
    private void unsubscribe(){
        holder.unsubscribe();
        incomeEvents.clear();
    }




    private Map<Source,TotalInSource> sourceMap;


    @Override
    public void run() {
        try {
            loop: while (true){
                Event<?> event = incomeEvents.take();
                switch (event){
                    case SourceEv ev when ev.type==SourceEvType.ALL_READY -> {
                        sourceMap = ev.sources.stream().collect(HashMap::new, (map,s)->map.put(s,new TotalInSource(s)), Map::putAll);

                        int sz = ev.sources.size();
                        System.out.println(String.format("Sources: %s pcs%s", sz, sz>0?":":""));
                        ev.sources.forEach(s->System.out.println("\t"+s));
                        System.out.println();
                    }



                    case EstimateEv ev when ev.type==EstimateEvType.FILE_FOUND -> {
                        sourceMap.compute(ev.fileInfo.src(), (s,ts)->{
                            ts.totalFiles++;
                            ts.totalSize+=ev.fileInfo.sz();
                            return ts;
                        });
                    }
                    case EstimateEv ev when ev.type==EstimateEvType.DIRECTORY_VIEWED -> {
                        sourceMap.get(ev.fileInfo.src()).totalFolders++;
                    }
                    case EstimateEv ev when ev.type==EstimateEvType.SOURCE_VIEWED -> {
                        var src = ev.fileInfo.src();
                        var total = sourceMap.get(src);
                        System.out.println(String.format(
                            "Source #%s readId=%s viewed:\n" +
                                "\tpaths: %s\n" +
                                "\ttotals: folders=%s files=%s size=%s",
                            src.tag(), src.readThreadId(), src.paths(), total.totalFolders, total.totalFiles, total.totalSize
                        ));
                        System.out.println();
                    }

                    case EstimateEv ev when ev.type==EstimateEvType.ALL_READY -> {
                        // todo move futrher
                        unsubscribe();
                        break loop;
                    }



                    /*case ReadEv ev && ev.type== ReadEvType.NEW_FILE -> {
                        System.out.println(String.format(
                            "Начинаю читать файл: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relPath(), ev.part.source().tag()
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.PART -> {
                        System.out.println(String.format(
                            "Читаю файл: [%s]/%s #%s %s%%",
                            ev.part.source().path(), ev.part.relPath(), ev.part.source().tag(),
                            Math.round(1d*ev.part.to()/ev.part.len()*100d)
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.FILE_END -> {
                        System.out.println(String.format(
                            "Файл прочитан: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relPath(), ev.part.source().tag()
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.NOT_FOUND -> {
                        System.out.println(String.format(
                            "Файл не найден: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relPath(), ev.part.source().tag()
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.READ_ERROR -> {
                        System.out.println(String.format(
                            "Ошибка чтения файла: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relPath(), ev.part.source().tag()
                        ));
                    }*/


                    // todo uncomment
                    /*case ResultEv ev && ev.type==ResultEvType.ALL_READY -> {
                        unsubscribe();
                        break loop;
                    }*/

                    default -> {}
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
