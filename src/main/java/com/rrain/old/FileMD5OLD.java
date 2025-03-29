package com.rrain.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

// НЕ РАБОТАЕТ

/* сделать:
    файлы с одинаковыми путями не совпадают
    найти файлы с одинаковым хэшем

*/

public class FileMD5OLD {
    //private static final String PATH_1 = "O:\\DISTRIBUTIV\\ОБРАЗЫ\\Запись загрузочного образа на флешку\\BootInst";
    //private static final String PATH_2 = "L:\\БЭКАПЫ\\DISTRIBUTIV\\ОБРАЗЫ\\Запись загрузочного образа на флешку\\BootInst";

    //private static final String PATH_1 = "O:\\DISTRIBUTIV\\ПРОГРАММЫ\\---Медиа & Графика & Обработка данных\\AutoCAD";
    //private static final String PATH_2 = "L:\\БЭКАПЫ\\DISTRIBUTIV\\ПРОГРАММЫ\\---Медиа & Графика & Обработка данных\\AutoCAD";

    // todo есть файл в первой папке но нет во второй и НАОБОРОТ

    private static final String PATH_2 =
      "I:\\GAMES\\Grand Theft Auto V by xatab";
    private static final String PATH_1 =
      "K:\\GAMES\\GAMES 3\\Grand Theft Auto V by xatab";

    //private static final String PATH_1 = "O:\\DISTRIBUTIV\\ПРОГРАММЫ\\---Медиа & Графика & Обработка данных\\Matlab R2020a [x64] [PC] [Windows]";
    //private static final String PATH_2 = "L:\\БЭКАПЫ\\DISTRIBUTIV\\ПРОГРАММЫ\\---Медиа & Графика & Обработка данных\\Matlab R2020a [x64] [PC] [Windows]";

    private static final boolean PARALLEL = true;

    private static final int filePartLen = 100 * 1024 * 1024; // 100 Megabytes
    private static final long ramConsumption = 2048L * 1024 * 1024; // 2 Gigabytes


    private static int falses = 0;
    private static ArrayList<String> falsesList = new ArrayList<>();

    public static void main(String[] args) {

        //start = System.currentTimeMillis();

        readAndGetMd5();
    }


    //private static volatile long start;


    private static void readAndGetMd5(){
        queue = new Queue(ramConsumption);
        pool = new ExecutorService[2];
        Arrays.fill(pool, Executors.newSingleThreadExecutor());

        var router = new MD5Router();
        Thread t = new Thread(router);
        t.setDaemon(false);
        t.start();

        recursive(Path.of(""));

        Arrays.stream(pool).forEach(ExecutorService::shutdown);

        while (!queue.isEmpty()){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        t.interrupt();

    }


    private static final Path root1 = Path.of(PATH_1);
    private static final Path root2 = Path.of(PATH_2);

    private static void recursive(Path p) {
        File f1 = root1.resolve(p).toFile();
        if (f1.isFile()){
            latch = new CountDownLatch(2);
            readFile(f1, 0);
            File f2 = root2.resolve(p).toFile();
            readFile(f2, 1);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (f1.isDirectory()){
            for(File ff : f1.listFiles()){
                Path pp = p.resolve(ff.getName());
                recursive(pp);
            }
        }
    }


    private static ExecutorService[] pool;
    //private static final ExecutorService pool = Executors.newFixedThreadPool(2);
    private static CountDownLatch latch;

    private static void readFile(File f, int threadNumber){
        if (PARALLEL) pool[threadNumber].execute(()->fileReadingTask(f,threadNumber));
        else pool[0].execute(()->fileReadingTask(f,threadNumber));
    }

    private static void fileReadingTask(File f, int threadNumber) {
        try {
            int pieces = (int)(f.length()/filePartLen);
            if (f.length()%filePartLen > 0) pieces++;
            FileInfo info = new FileInfo(pieces, threadNumber, f.getPath());
            try (FileInputStream fis = new FileInputStream(f);){
                for (int i = 1; i <= pieces; i++) {
                    int len = i<pieces ? filePartLen : (int)(f.length()%filePartLen);
                    byte[] buf = new byte[len];
                    fis.read(buf);
                    queue.put(new FilePart(buf, info));
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
            FileInfo info = new FileInfo(1, threadNumber, f.getPath());
            try {
                queue.put(new FilePart(new byte[0], info)); // если файл не нашёлся, то отправляем пустоту и перед этим стэк трэй
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
            //System.out.println("file readed in " + (System.currentTimeMillis()-start));
        }
    }



    private static volatile Queue queue;

    private static class MD5Router implements Runnable {
        private final ResultsCombiner resultsCombiner = new ResultsCombiner();
        private final List<MD5Calculator> list = new ArrayList<>();

        @Override
        public void run() {
            while (true){
                try {
                    calc(queue.take());
                } catch (InterruptedException e) {
                    System.out.println("FALSES: " + falses);
                    falsesList.forEach(System.out::println);
                    //System.out.println("MD5 calculated in " + (System.currentTimeMillis()-start));
                    return;
                }
            }
        }

        private void calc(FilePart part){
            createMD5Calculator(part);
            var md5Calc = list.get(part.info.threadNumber);
            md5Calc.nextPart(part.part);
            if (--part.info.piecesCnt == 0) {
                part.info.MD5 = md5Calc.getMD5();
                resultsCombiner.push(part.info);
            }
        }

        private void createMD5Calculator(FilePart part){
            while (list.size() <= part.info.threadNumber) list.add(new MD5Calculator());
        }
    }

    private static class ResultsCombiner{
        private final List<FileInfo> list = new ArrayList<>();

        //// TODO: 03.03.2021 не синхронизироваться после каждого файла.... наверное..., тогда сделать мапу с относительными путями и там что-то делать
        public void push(FileInfo info){
            if (info.threadNumber==0) list.add(0, info); else list.add(info);

            if (list.size()==2){
                String md5f1 = list.get(0).MD5;
                String md5f2 = list.get(1).MD5;
                boolean equality = md5f1.equals(md5f2);
                if (!equality) falses++;
                String eq = equality ? "true" : "FALSE";
                String md5 = equality ? " MD5: "+md5f1 : "";
                String out = eq + " file: " + list.get(0).path + md5;
                System.out.println(out);
                if (!equality) falsesList.add(out);
                list.clear();
            }
        }
    }


    private static class FilePart {
        final byte[] part;
        final FileInfo info; // один info на несколько частей

        public FilePart(byte[] part, FileInfo info) {
            this.part = part;
            this.info = info;
        }
    }

    private static class FileInfo {
        int piecesCnt;
        final int threadNumber;
        final String path;
        String MD5 = null;

        public FileInfo(int piecesCnt, int threadNumber, String path) {
            this.piecesCnt = piecesCnt;
            this.threadNumber = threadNumber;
            this.path = path;
        }
    }

    private static class MD5Calculator {
        private final MessageDigest mdEnc;

        public MD5Calculator() {
            try {
                mdEnc = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("NoSuchAlgorithmException (MD5)");
                e.printStackTrace();
                throw new RuntimeException();
            }
        }

        public void nextPart(byte[] bytes){
            mdEnc.update(bytes, 0, bytes.length);
        }

        // get and reset
        public String getMD5(){
            String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
            StringBuilder sb = new StringBuilder(md5);
            while (sb.length()<32) sb.insert(0, "0");
            sb.insert(24, " ");
            sb.insert(16, " ");
            sb.insert(8, " ");
            return sb.toString().toUpperCase();
        }

        public void reset(){
            mdEnc.reset();
        }
    }

    private static class Queue {
        private final LinkedBlockingQueue<FilePart> queue = new LinkedBlockingQueue<>();
        private volatile long size = 0L;
        private final long maxLen;

        public Queue(long maxLen) {
            this.maxLen = maxLen;
        }

        public FilePart take() throws InterruptedException {
            synchronized (queue){
                while (queue.size()==0) queue.wait();
                var part = queue.take();
                size-=part.part.length;
                queue.notifyAll();
                return part;
            }
        }

        public void put(FilePart part) throws InterruptedException {
            synchronized (queue){
                while (size+part.part.length > maxLen) queue.wait();
                queue.put(part);
                size+=part.part.length;
                queue.notifyAll();
            }
        }

        public boolean isEmpty(){
            synchronized (queue){
                return queue.isEmpty();
            }
        }

    }
}
