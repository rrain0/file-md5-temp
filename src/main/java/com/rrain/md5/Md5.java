package com.rrain.md5;

import com.rrain.md5.event.EventManager;
import com.rrain.md5.print.PrintManager;
import com.rrain.md5.readutils.ReadThreadManager;
import com.rrain.md5.stage1sourcesdata.Source;
import com.rrain.md5.stage1sourcesdata.SourceManager;
import com.rrain.md5.stage2estimate.EstimateManager;
import com.rrain.md5.stage3read.ReadManager;
import com.rrain.md5.stage4hash.CalculatorManager;
import com.rrain.md5.stage5results.ResultManager;

import java.util.List;

/*
    todo
     задавать конфиг в json рядом с исполняемым или jar файлом
     <имя проекта>.<профиль>.json
     file-md5.config.json
     file-md5.default.config.json
     file-md5.profile1.config.json
 */

// todo made a parameter sequential with <thread>

// todo надо сделать 1 диск - 1 одновременный поток, но перед этим проверить на ссд как он читает, есть ли разница, если 2 потока или 1

// todo в одном источнике несколько путей

// todo отображать source list в правильном порядке

public class Md5 {

    public static void main(String[] args) {
        // Если расположения находятся на одном физическом диске, то лучше их указать в одном потоке чтения,
        // чтобы программа их читала последовательно, а не параллельно



        /*final List<Source> sources = List.of(
            Source.builder().path("L:\\ВИДЕО\\Dr. Stone - Доктор Стоун\\Доктор Стоун S01 1080p HEVC AniPlague")
                .tag(1).readThreadId("L").build(),
            Source.builder().path("E:\\ТОРРЕНТЫ\\Доктор Стоун S01 1080p HEVC")
                .tag(2).readThreadId("E").build()
        );*/

        /*final List<Source> sources = List.of(
            Source.builder().path("I:\\test\\1").tag(1).readThreadId("I").build(),
            Source.builder().path("I:\\test\\2").tag(2).readThreadId("I").build(),
            Source.builder().path("F:\\test\\3").tag(3).readThreadId("F").build()
        );*/

        /*final List<Source> sources = List.of(
            Source.builder().path("K:\\GAMES\\GAMES 2\\Streets of Rage\\Street Of Rage Collection").
                tag("Seagate").readThreadId("K").build(),
            Source.builder().path("G:\\[удалить]\\Street Of Rage Collection").
                tag("DATA_TWO").readThreadId("G").build(),
            Source.builder().path("H:\\[удалить]\\Street Of Rage Collection").
                tag("Ts64").readThreadId("H").build()
        );*/

        /*final List<Source> sources = List.of(
            Source.builder().path("H:\\").
                tag("H").readThreadId("H").build(),
            Source.builder().path("E:\\ТОРРЕНТЫ").
                tag("G").readThreadId("G").build()
        );*/

        /*final List<Source> sources = List.of(
            Source.builder().path("L:\\d\\[ ] DISK D\\ЗАГРУЗКИ\\скобочные последовательности - Задача - E - Codeforces.html").
                tag(1).readThreadId(1).build(),
            Source.builder().path("F:\\скобочные последовательности - Задача - E - Codeforces.html").
                tag(2).readThreadId(2).build()
        );*/


        // todo THERE IS BUG
        final List<Source> sources = List.of(
            //Source.builder().path("H:\\SQL ДОРОФЕЕВ\\материалы\\DataBase 0").
            //    tag(0).readThreadId(1).build(),
            Source.builder().path("H:\\SQL ДОРОФЕЕВ\\материалы\\DataBase 1 ноут").
                tag(1).readThreadId(1).build(),
            Source.builder().path("H:\\SQL ДОРОФЕЕВ\\материалы\\DataBase 2 телефон").
                tag(2).readThreadId(1).build()
        );


        /*final List<Source> sources = List.of(
            Source.builder().path("F:\\test1").path("F:\\test2")
                .tag(1).readThreadId("F").build(),
            Source.builder().path("I:\\test1").path("I:\\test2")
                .tag(2).readThreadId("I").build()
        );*/





        final int maxCalculationThreads = 4;

        var eventManager = new EventManager();
        var readThreadManager = new ReadThreadManager();

        var sourcesManager = new SourceManager(sources, eventManager);
        var estimateManager = new EstimateManager(eventManager, readThreadManager);
        var readManager = new ReadManager(eventManager, readThreadManager);
        var calcManager = new CalculatorManager(maxCalculationThreads, eventManager);
        var resultManager = new ResultManager(eventManager);
        var printManager = new PrintManager(eventManager);


        new Thread(sourcesManager).start();
        new Thread(estimateManager).start();
        new Thread(readManager).start();
        new Thread(calcManager).start();
        new Thread(resultManager).start();
        new Thread(printManager).start();
    }




}
