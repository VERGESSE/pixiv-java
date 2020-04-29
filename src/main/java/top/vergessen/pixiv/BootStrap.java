package top.vergessen.pixiv;

import top.vergessen.pixiv.img.ImgCleaner;
import top.vergessen.pixiv.img.ImgDownloader;
import top.vergessen.pixiv.page.PageLoader;
import top.vergessen.pixiv.propertie.PropertyMgr;

import java.util.Scanner;
import java.util.concurrent.*;

public class BootStrap {

    public static void main(String[] args) throws InterruptedException {

        // 保存图片的根路径
        String imgPath =  PropertyMgr.getString("path");
        Integer threadPath = PropertyMgr.getInt("threadPath");
        // 存储图片路径的阻塞队列
        BlockingQueue<String> imageQueue = new LinkedBlockingDeque<>(threadPath*2);
        // 图片下载任务执行线程池
        ExecutorService executor = new ThreadPoolExecutor(threadPath, Integer.MAX_VALUE,
                threadPath*2, TimeUnit.SECONDS, new SynchronousQueue<>());
        // 保证线程池在接收一定数量任务后程序阻塞等待任务执行
        Semaphore semaphore = new Semaphore(threadPath*2);

        // 获取用户输入初始路径
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入P站初始页，本爬虫将爬取所有相关图片，并过滤清晰度(size > 1M, pixel > 1200, 收藏、点赞 > 5000)");
        System.out.println("请输入你要爬取的初始页:");
        String path = scanner.next();

        // 页面加载器，获取输入页全部的相关页地址
        PageLoader pageLoader = new PageLoader(path, imageQueue);
        pageLoader.initLoad();

        // 图片下载器，解析页面加载器获取的全部相关页的相关图片并加入图片下载队列执行下载任务
        // 理论上爬取181*180张图片，但会由于收藏点赞不达标，下载失败，
        // 图片重复等原因下载量远少于181*180
        new ImgDownloader(imageQueue,executor,semaphore).startDownLoad();

        // 确保当前提交任务执行完成
        executor.shutdown();
        while (!executor.isTerminated()){
            TimeUnit.SECONDS.sleep(3);
        }
        // 最后再对图片执行一次整理
        ImgCleaner.getInstance().startCleaner(imgPath + "/image");
    }
}
