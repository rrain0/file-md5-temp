package com.rrain.md5.stage5results;

import com.rrain.md5.event.Event;
import com.rrain.md5.event.EventManager;
import com.rrain.md5.event.SubscriptionHolder;
import com.rrain.md5.print.TotalInSource;
import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage1sourcesdata.SourceEv;
import com.rrain.md5.stage1sourcesdata.SourceEvType;
import com.rrain.md5.stage2estimate.EstimateEv;
import com.rrain.md5.stage2estimate.EstimateEvType;
import com.rrain.md5.stage3read.ReadEv;
import com.rrain.md5.stage3read.ReadEvType;
import com.rrain.md5.stage4hash.CalcEv;
import com.rrain.md5.stage4hash.CalcEvType;
import com.rrain.md5.stage4hash.CalcResult;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// todo поиск одинаковых файлов по:
//  относительный путь - уже сделано
//  просто название файла
//  по одинаковому хэшу (можно даже 2 хэша вычислять: MD5 и  CRC32 или SHA-256, можно ещё размер учитывать)

public class ResultManager implements Runnable {

    public ResultManager(EventManager eventManager) {
        this.eventManager = eventManager;
        subscribe();
    }

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;


    // Map<relativePath, Map<Source, ResultInfo with md5>>
    private final Map<Path, Map<Source, ResultEv>> compareResults = new HashMap<>();



    private List<Source> sources;
    private Map<Source, TotalInSource> sourceTotalsMap;





    // Map<relativePath, Map<sourcePath, ResultInfo with md5>>
    private final Map<Path, Map<Path, CalcResult>> results = new HashMap<>();
    // Map<srcPath, Map<type, count>>
    //private Map<Path, Map<CalcResult.Info, Integer>> filesCnt;



    synchronized private void subscribe(){
        holder = eventManager.subscribe(incomeEvents::put);
    }
    synchronized private void unsubscribe(){
        holder.unsubscribe();
        incomeEvents.clear();
    }


    @Override
    public void run() {
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
                case SourceEv ev when ev.type==SourceEvType.ALL_READY -> { synchronized (this){
                    sources = ev.sources;
                    sourceTotalsMap = sources.stream().collect(HashMap::new, (map,s)->map.put(s,new TotalInSource(s)), Map::putAll);
                    /*filesCnt = sources.stream().collect(Collectors.toUnmodifiableMap(Source::path, src->
                        Arrays.stream(CalcResult.Info.values())
                            .filter(info->info!= CalcResult.Info.SOURCE_READY)
                            .collect(Collectors.toMap(info->info,info->0))
                    ));*/
                }}
                case EstimateEv ev when ev.type==EstimateEvType.FILE_FOUND -> {
                    var sourceMap = compareResults.compute(ev.fileInfo.relPath(), (rel,srcMap)->{
                        if (srcMap==null) srcMap = sources.stream().collect(
                            HashMap::new,
                            (map,s)->map.put(s,new ResultEv(ResultEvType.NO_FILE, new Result(s, ev.fileInfo.relPath(), null))),
                            Map::putAll
                        );
                        return srcMap;
                    });
                    sourceMap.put(ev.fileInfo.src(), new ResultEv(ResultEvType.FILE_EXISTS, new Result(ev.fileInfo.src(), ev.fileInfo.relPath(), null)));
                }

                case CalcEv ev when ev.type==CalcEvType.FILE_CALCULATED -> {
                    compareResults.get(ev.result.relPath()).put(ev.result.source(),
                        new ResultEv(ResultEvType.FILE_READY, new Result(ev.result.source(), ev.result.relPath(), ev.result.md5()))
                    );
                }
                case ReadEv ev when ev.type==ReadEvType.READ_ERROR -> {
                    compareResults.get(ev.part.relPath()).put(ev.part.source(),
                        new ResultEv(ResultEvType.READ_ERROR, new Result(ev.part.source(), ev.part.relPath(), null))
                    );
                }
                case ReadEv ev when ev.type==ReadEvType.NOT_FOUND -> {
                    compareResults.get(ev.part.relPath()).put(ev.part.source(),
                        new ResultEv(ResultEvType.NOT_FOUND, new Result(ev.part.source(), ev.part.relPath(), null))
                    );
                }

                case CalcEv ev when ev.type==CalcEvType.ALL_READY -> {
                    unsubscribe();
                    break loop;
                }
                default -> {}
            }
        }


        finishAll();
        eventManager.addEvent(new ResultEv(ResultEvType.ALL_READY, null));
    }



    private void finishAll(){
        var srcs = new ArrayList<>(this.sources); // копируем для того, чтобы можно было отсортировать если надо
        printResultsList(srcs);
        printFalsesResults(srcs);
        //printFilesCnt(srcs);
    }

    // result может быть null если файла нет и не предполагалось
    private void printResultsList(List<Source> srcs){

        compareResults.forEach((relPath,srcMap)->{
            System.out.println(relPath);

            String md5 = null;
            boolean equals = true;
            for (int i = 0; i < srcs.size(); i++) {
                var src = srcs.get(i);
                var result = srcMap.get(src);
                if (i==0) {
                    md5 = Optional.ofNullable(result).map(r->r.result.md5()).orElse(null);
                    equals &= md5!=null;
                }
                else equals &= Objects.equals(md5,Optional.ofNullable(result).map(r->r.result.md5()).orElse(null));
                System.out.println(printOne(result));
            }

            System.out.println("\t"+"EQUALS: "+equals);
        });
    }

    private void printFalsesResults(List<Source> srcs){
        System.out.println();
        System.out.println("FALSES:");
        compareResults.forEach((relPath,srcMap)->{
            StringBuilder sb = new StringBuilder();
            sb.append(relPath).append('\n');

            String md5 = null;
            boolean equals = true;
            for (int i = 0; i < srcs.size(); i++) {
                var src = srcs.get(i);
                var result = srcMap.get(src);
                if (i==0) {
                    md5 = Optional.ofNullable(result).map(r->r.result.md5()).orElse(null);
                    equals &= md5!=null;
                }
                else equals &= Objects.equals(md5,Optional.ofNullable(result).map(r->r.result.md5()).orElse(null));
                sb.append(printOne(result)).append('\n');
            }

            sb.append("\t"+"EQUALS: "+equals);

            if (!equals) System.out.println(sb);
        });
    }

    /*private void printFilesCnt(List<Source> srcs){
        var infos = List.of(CalcResult.Info.FILE_READY, CalcResult.Info.READ_ERROR, CalcResult.Info.NOT_FOUND);
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append("FILES COUNT:").append('\n');
        for (var src : srcs) {
            sb.append('\t').append("src: [").append(src.readThreadId()).append(", ").append(src.path()).append("]").append(" ");
            for (var info : infos){
                sb.append(info).append(": ").append(filesCnt.get(src.path()).get(info)).append(" ");
            }
            sb.append('\n');
        }
        System.out.println(sb);
    }*/

    private String printOne(ResultEv result){
        var src = result.result.source();
        return (
            "\t"+
            "src: ["+src.readThreadId()+", #"+src.tag()+/*", "+src.path()+*/"] "+
            switch (result.type){
                case FILE_READY -> "MD5: "+result.result.md5();
                case NOT_FOUND -> "FILE NOT FOUND";
                case READ_ERROR -> "READ ERROR";
                case NO_FILE -> "NO_FILE";
                case FILE_EXISTS -> "FILE_EXISTS ???";
                case null -> "null ???";
                default -> "default ???";
            }+" "
            //+ "full path: "+src.path().resolve(result.result.relPath())
        );
    }

}
