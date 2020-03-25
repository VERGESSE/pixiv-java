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

public class ImgDownloader {

    // 任务数
    private static AtomicInteger nums = new AtomicInteger(0);
    // 已下载图片页的缓存，防止重复下载
    private static HashSet<String> memory = new HashSet<>();

    // 设置v2ray代理
    private static final Proxy proxy = new Proxy(Proxy.Type.SOCKS,
            new InetSocketAddress("127.0.0.1", 9999));
    private static HashMap<String,String> headers = new HashMap<>();

    static {
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36");
        headers.put("Accept","*/*");
        headers.put("Accept-Language","zh-CN,zh;q=0.9");
        headers.put("Accept-Encoding","");
        headers.put("Connection","keep-alive");
    }

    private BlockingQueue<String> imgQueue;
    private ExecutorService executor;
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

        while (nums.get() < 3000){
            try {
                semaphore.acquire();
                String uri = imgQueue.take();
                if (!memory.contains(uri)) {
                    executor.submit(new DownloadImg(uri));
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class DownloadImg implements Runnable{

        String imgId;
        String imgUrl;

        DownloadImg(String imgUrl){
            this.imgUrl = imgUrl;
            String[] split = imgUrl.split("/");
            this.imgId = split[split.length - 1].split("_")[0];
        }

        @Override
        public void run() {
            long start = System.nanoTime();
            String fileName = "image/"+imgId+".";
            try {
                Connection connection = Jsoup
                        .connect("https://www.pixiv.net/artworks/" + imgId)
                        .headers(headers)
                        .proxy(proxy)
                        // 超时时间一分钟，防止网络延迟的影响
                        .timeout(60000);
                Document document = connection.get();
                // 获取收藏数
                Pattern pattern = Pattern.compile(";bookmarkCount&quot;:(.*?),&quot;");
                Matcher matcher = pattern.matcher(document.html());
                if (!matcher.find()) {
                    System.out.println(imgId+" 爬取失败!");
                    return;
                }
                Integer markCount = Integer.valueOf(matcher.group(1));
                if (markCount > 4000){
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
                        System.out.println(fileName + " 爬取失败 "+ time +" s");
                        imgQueue.offer(imgUrl,10,TimeUnit.SECONDS);
                } catch (InterruptedException e1) {}
            }finally {
                semaphore.release();
            }
        }
    }
}
