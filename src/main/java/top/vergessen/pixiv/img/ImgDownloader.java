package top.vergessen.pixiv.img;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.vergessen.pixiv.util.HttpsUrlValidator;
import top.vergessen.pixiv.util.ImgDownUtil;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 图片下载器
public class ImgDownloader {

    // 任务数
    private static AtomicInteger nums = new AtomicInteger(0);
    // 已下载图片页的缓存，防止重复下载
    private static HashSet<String> memory = new HashSet<>();

    // 设置v2ray代理
    private static final Proxy proxy = new Proxy(Proxy.Type.SOCKS,
            new InetSocketAddress("127.0.0.1", 9999));
    // https请求header设置
    private static HashMap<String,String> headers = new HashMap<>();
    static {
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36");
        headers.put("Accept","*/*");
        headers.put("Accept-Language","zh-CN,zh;q=0.9");
        headers.put("Accept-Encoding","");
        headers.put("Connection","keep-alive");
    }

    // 从此队列加载图片地址以下载
    private BlockingQueue<String> imgQueue;
    // 任务执行线程池
    private ExecutorService executor;
    // 信号量，参见BootStrap解释
    private Semaphore semaphore;

    public ImgDownloader(BlockingQueue<String> imgQueue, ExecutorService executor
                            , Semaphore semaphore){
        this.imgQueue = imgQueue;
        this.executor = executor;
        this.semaphore = semaphore;
    }

    public void startDownLoad(){
        // 允许所有https
        HttpsUrlValidator.trustEveryone();

        // 每次获取3000张图片，可以调整至4000左右，个人认为3000已经够了
        while (nums.get() < 3000){
            try {
                // 获取待执行的图片地址
                String uri = imgQueue.take();
                // 判断是否已经下载过
                if (!memory.contains(uri)) {
                    semaphore.acquire();
                    // 异步执行图片下载，及其耗时，多线程执行提高下载效率
                    executor.submit(new DownloadImg(uri));
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 具体的图片下载任务执行者
    class DownloadImg implements Runnable{

        String imgId;
        String imgUrl;

        DownloadImg(String imgUrl){
            this.imgUrl = imgUrl;
            String[] split = imgUrl.split("/");
            // 获取P站图片的id
            this.imgId = split[split.length - 1].split("_")[0];
        }

        @Override
        public void run() {
            long start = System.nanoTime();
            String fileName = "image/"+imgId+".";
            try {
                // 获取图片的收藏点赞信息
                Connection connection = Jsoup
                        .connect("https://www.pixiv.net/artworks/" + imgId)
                        .headers(headers)
                        .proxy(proxy)
                        // 超时时间一分钟，防止网络延迟的影响
                        .timeout(60000);
                Document document = connection.get();
                // 获取收藏数
                Pattern pattern = Pattern.compile(";bookmarkCount&quot;:(.*?),&quot;");
                // 获取点赞数
                Pattern pattern2 = Pattern.compile(";likeCount&quot;:(.*?),&quot;");
                Matcher matcher = pattern.matcher(document.html());
                Matcher matcher1 = pattern2.matcher(document.html());
                if (!matcher.find() || !matcher1.find()) {
                    System.out.println(imgId+" 爬取失败!");
                    return;
                }

                Integer markCount = Integer.valueOf(matcher.group(1));
                Integer likeCount = Integer.valueOf(matcher1.group(1));
                if (likeCount > 5000 && markCount > 5000){
                    // P站图片有的是jpg有的是png，jpg占多
                    // 如果以jpg格式下载失败则以png策略下载
                    try {
                        new ImgDownUtil(imgUrl, "jpg", fileName,
                                headers, proxy).download();
                    }catch (HttpStatusException e){
                        new ImgDownUtil(imgUrl, "png", fileName,
                                headers, proxy).download();
                    }
                    long end = System.nanoTime();
                    System.out.println(nums.incrementAndGet()+" : "+fileName+" 爬取完成！"+(end-start)/1000000000+" s");
                    memory.add(imgUrl);
                    // 进行图片清理
                    if (nums.get() % 500 == 0) {
                        ImgCleaner.getInstance().startCleaner("image");
                    }
                }
            } catch (Exception e) {
                try {
                    long end = System.nanoTime();
                    long time = (end-start)/1000000000;
                    if (time >= 10)
                        // 失败重试机制(但大部分经过重试也无法完成下载，原因未明，好在数量不多)
                        System.out.println(fileName + " 爬取失败 "+ time +" s");
                        imgQueue.offer(imgUrl,10,TimeUnit.SECONDS);
                } catch (InterruptedException e1) {}
            }finally {
                // 释放信号量
                semaphore.release();
            }
        }
    }
}
