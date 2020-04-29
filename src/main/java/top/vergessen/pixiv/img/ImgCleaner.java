package top.vergessen.pixiv.img;

import org.apache.commons.io.IOUtils;
import top.vergessen.pixiv.propertie.PropertyMgr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// 图片整理器
// 整理后会把小图片，宽图，长图分类
public class ImgCleaner {

    // 图片保存路径
    private static final String imgPath =  PropertyMgr.getString("path");

    private static final ImgCleaner instance = new ImgCleaner();

    private ImgCleaner() {}

    public static ImgCleaner getInstance(){
        return instance;
    }

    public synchronized void startCleaner(String imgFile){
        System.out.println("开始整理图片...");
        ExecutorService executor = Executors.newFixedThreadPool(5);

        File file = new File(imgFile);
        File small = new File(imgPath + "/small");
        File widthFile = new File(imgPath + "/width");
        File heightFile = new File(imgPath + "/height");
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
            File newFile = new File(imgPath + "/small" + "/" + path);
            widthFile = new File(imgPath + "/width" + "/" + path);
            heightFile = new File(imgPath + "/height" + "/" + path);
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
                    executor.execute(new RemoveFile(oldFile, newFile));
                    continue;
                }
                if (Math.min(height, width) < 950){
                    executor.execute(new RemoveFile(oldFile, newFile));
                    continue;
                }else if (Math.max(height, width) < 1800){
                    executor.execute(new RemoveFile(oldFile, newFile));
                    continue;
                }
                if (width > height)
                    executor.execute(new RemoveFile(oldFile, widthFile));
                else if (width <= height)
                    executor.execute(new RemoveFile(oldFile, heightFile));
            }catch (IOException e) {
                oldFile.delete();
            }
        }
        executor.shutdown();
        while (!executor.isTerminated()){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("图片整理结束...");
    }

    public static void main(String[] args) {
        ImgCleaner.getInstance().startCleaner("H:/img/image");
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
            // 获取输出输入流
            try(BufferedInputStream recIN = new BufferedInputStream(
                    new FileInputStream(src));
                BufferedOutputStream targetOS = new BufferedOutputStream(
                        new FileOutputStream(target))){

                // 拷贝
                IOUtils.copy(recIN, targetOS);
                targetOS.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 删除原文件
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
