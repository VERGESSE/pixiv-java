package top.vergessen.pixiv.img;

import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImgCleaner {

    private static final ImgCleaner instance = new ImgCleaner();
    private static final ExecutorService executor =
            Executors.newCachedThreadPool();

    private ImgCleaner() {}

    public static ImgCleaner getInstance(){
        return instance;
    }

    public void startCleaner(String imgFile){
        System.out.println("开始整理图片...");
        File file = new File(imgFile);
        File small = new File("small");
        File widthFile = new File("width");
        File heightFile = new File("height");
        if(!file.exists())
            file.mkdirs();
        if(!small.exists())
            small.mkdirs();
        if(!widthFile.exists())
            widthFile.mkdirs();
        if(!heightFile.exists())
            heightFile.mkdirs();
        String[] list = file.list();
        for (String path : list) {
            File oldFile = new File(imgFile + "/" + path);
            File newFile = new File("small" + "/" + path);
            widthFile = new File("width" + "/" + path);
            heightFile = new File("height" + "/" + path);
            BufferedImage image;
            try {
                image = ImageIO.read(oldFile);
                int height = image.getHeight();
                int width = image.getWidth();
                long size = oldFile.length();
                if (size < 200000) {
                    oldFile.delete();
                    continue;
                }
                if (size < 500000) {
                    executor.submit(new RemoveFile(oldFile, newFile));
                    continue;
                }
                if (Math.min(height, width) < 950){
                    executor.submit(new RemoveFile(oldFile, newFile));
                    continue;
                }else if (Math.max(height, width) < 1800){
                    executor.submit(new RemoveFile(oldFile, newFile));
                    continue;
                }
                if (width > height)
                    executor.submit(new RemoveFile(oldFile, widthFile));
                else if (width <= height)
                    executor.submit(new RemoveFile(oldFile, heightFile));
            }catch (IOException e) {
                oldFile.delete();
            }
        }
        System.out.println("图片整理结束...");
    }

    public static void main(String[] args) {
        ImgCleaner.getInstance().startCleaner("image");
    }

    class RemoveFile implements Runnable{

        private File src;
        private File target;

        RemoveFile(File src, File target){
            this.src = src;
            this.target = target;
        }

        @Override
        public void run() {
            try(BufferedInputStream recIN = new BufferedInputStream(
                    new FileInputStream(src));
                BufferedOutputStream targetOS = new BufferedOutputStream(
                        new FileOutputStream(target))){

                IOUtils.copy(recIN, targetOS);
                targetOS.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            src.delete();
        }
    }

    private void remove(String fileName) {
        HashSet<String> temp = new HashSet<>();
        File file = new File(fileName);
        String[] list = file.list();
        for (String path : list) {
            String[] split = path.split("\\.");
            if (split[1].equals("png"))
                temp.add(split[0]);
        }
        for (String path : list) {
            String[] split = path.split("\\.");
            if (temp.contains(split[0]) && split[1].equals("jpg")){
                File del = new File(fileName + "/" + path);
                del.delete();
            }
        }
    }

}
