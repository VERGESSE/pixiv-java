package top.vergessen.pixiv;

import top.vergessen.pixiv.img.ImgCleaner;
import top.vergessen.pixiv.img.ImgDownloader;
import top.vergessen.pixiv.page.PageLoader;

import java.util.Scanner;
import java.util.concurrent.*;

public class BootStrap {

    public static void main(String[] args) {

        BlockingQueue<String> imageQueue = new LinkedBlockingDeque<>(100);
        ExecutorService executor = new ThreadPoolExecutor(100, Integer.MAX_VALUE,
                150, TimeUnit.SECONDS, new SynchronousQueue<>());
        Semaphore semaphore = new Semaphore(200);
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入P站初始页，本爬虫将爬取所有相关图片，并过滤清晰度(size > 1M, pixel > 1200, 收藏 > 3000)");
        System.out.println("请输入你要爬取的初始页:");
        String path = scanner.next();
        PageLoader pageLoader = new PageLoader(path, imageQueue);
        pageLoader.initLoad();

        new ImgDownloader(imageQueue,executor,semaphore).startDownLoad();
        executor.shutdown();
        while (executor.isShutdown());

        ImgCleaner.getInstance().startCleaner("image");
    }
}
