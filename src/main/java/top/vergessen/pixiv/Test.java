package top.vergessen.pixiv;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.vergessen.pixiv.util.HttpsUrlValidator;
import top.vergessen.pixiv.util.ImgDownUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

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

    public static void main(String[] args) throws IOException, InterruptedException {
        // 允许所有https
        HttpsUrlValidator.trustEveryone();
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(2);

        System.out.println("-----");
        queue.put(1);
        queue.put(2);
        queue.put(3);
        System.out.println("-----");
        for (int i = 0; i < 10; i++)
            System.out.println(queue.take());
        System.out.println(1111);
        while (true) {
            Connection connection = Jsoup
                    .connect("https://www.pixiv.net/artworks/" + 31287042)
                    .headers(headers)
                    .proxy(proxy)
                    .timeout(20000);
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 获取收藏数
            Pattern pattern = Pattern.compile(";bookmarkCount&quot;:(.*?),&quot;");
            Matcher matcher = pattern.matcher(document.html());
            if (!matcher.find()) {
                System.out.println(31287042 + " 爬取失败!");
                return;
            }
            Integer markCount = Integer.valueOf(matcher.group(1));
            if (markCount > 4000){
                try {
                    new ImgDownUtil("2012/11/04/21/03/50/31287042_p0", "jpg", "image/1.jpg",
                            headers, proxy).download();
                }catch (HttpStatusException e){
                    new ImgDownUtil("2012/11/04/21/03/50/31287042_p0", "png", "image/1.jpg",
                            headers, proxy).download();
                }
            }
            System.out.println("---");
        }
    }
}
