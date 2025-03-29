package com.rrain.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileMD5OLDOLD {
    //private static final String PATH_1
    // = "O:\\DISTRIBUTIV\\ПРОГРАММЫ\\---Медиа & Графика & Обработка данных\\Matlab R2020a [x64] [PC] [Windows]";
    //private static final String PATH_2
    // = "L:\\БЭКАПЫ\\DISTRIBUTIV\\ПРОГРАММЫ\\---Медиа & Графика & Обработка данных\\Matlab R2020a [x64] [PC] [Windows]";
    private static final String PATH_2 =
      "L:\\[удалить]\\Fumetsu no Anata e  Для тебя бессмертный (JAM S01 20 eps)";
    private static final String PATH_1 =
      "M:\\Anime\\Fumetsu no Anata e  Для тебя бессмертный\\[01] Fumetsu no Anata e (JAM S1 20 eps)  Для тебя бессмертный";


    private static int falses = 0;

    public static void main(String[] args) {
        calcOld();
    }


    public static void calcOld(){
        try {
            recursive(new File(PATH_1), new File(PATH_2));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println();
            System.out.println("FALSES: " + falses);
        }
    }

    private static void recursive(File f1, File f2) throws Exception{
        if (f1.isDirectory()){
            for(File ff : f1.listFiles()){
                recursive(ff, new File(f2, ff.getName()));
            }
        } else if (f1.isFile()){
            String md5f1 = md5(getFileContent(f1));
            String md5f2 = md5(getFileContent(f2));
            boolean equality = md5f1.equals(md5f2);
            if (!equality) falses++;
            String eq = equality ? "true" : "FALSE";
            String md5 = equality ? " MD5: "+md5f1 : "";
            System.out.println(eq + " file: " + f1.getAbsolutePath() + md5);
        }
    }

    private static byte[] getFileContent(File f) throws IOException {
        InputStream is = new FileInputStream(f);
        byte[] bytes = new byte[(int)f.length()];
        is.read(bytes);
        is.close();
        return bytes;
    }

    private static final MD5Calculator MD5 = new MD5Calculator();
    private static String md5(byte[] bytes){
        MD5.nextPart(bytes);
        return MD5.getMD5();
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
}
