package top.vergessen.pixiv.page;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.seimicrawler.xpath.JXNode;
import top.vergessen.pixiv.driver.MyDriver;
import top.vergessen.pixiv.util.XpathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 页面加载解析器
public class PageLoader {

    private static final String PIVIX = "https://www.pixiv.net";

    // 停止信号
    private static final String EXIT_SINGLE = "EXIT";

    // 源地址
    private String originPath;
    // 从源地址获取的目的地址列表
    private List<String> pathList;
    // selenium的driver（执行器）
    private WebDriver driver;
    // 图片地址队列
    private BlockingQueue<String> imgQueue;

    public PageLoader(String originPath, BlockingQueue<String> imgQueue){
        this.originPath = originPath;
        this.pathList = new ArrayList<>();
        this.driver = MyDriver.getInstance().getWebDriver();
        this.imgQueue = imgQueue;
    }

    // 解析初始页
    public void initLoad() {

        System.out.println("正在初始化页面...");
        Document doc = load(originPath);
        List<JXNode> paths = new XpathUtil(doc)
                .xpath("//aside//li/div/div[1]/div/a/@href");
        pathList.add(originPath.substring(21));
        for (JXNode path : paths) {
            pathList.add(path.asString());
        }
        System.out.println("页面初始化完成，开始爬取...");

        // 启动异步加载图片地址任务
        Thread imgLoader = new Thread(new ImgLoader());
        imgLoader.setPriority(Thread.MAX_PRIORITY);
        imgLoader.start();
    }

    // 图片地址加载任务
    class ImgLoader implements Runnable{

        @Override
        public void run() {
            int num = 0;
            try {
                for (String s : pathList) {
                    // 获取页面dom
                    Document doc = load(PIVIX + s);
                    // 解析dom，获取图片地址
                    List<JXNode> imgUrls = new XpathUtil(doc)
                            .xpath("//li/div/div/div/a/div/img/@src");
                    // 记录获取到的地址数
                    int i = 0;
                    // 根据图片地址获取对应P站图片的ID
                    for (JXNode imgUrl : imgUrls) {
                        Pattern pattern = Pattern.compile(".*?img/(.*?_p0).*?");
                        Matcher matcher = pattern.matcher(imgUrl.toString());
                        try {
                            matcher.find();
                            String uri = matcher.group(1);
                            // 加入执行队列
                            imgQueue.put(uri);
                            i++;
                        }catch (Exception e){}
                    }
                    System.out.println("页面: " + ++num + " 爬取完成 "+i);
                }
            }finally {
                // 发送停止信号
                try {
                    imgQueue.put(EXIT_SINGLE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 最后停止driver
                driver.quit();
            }
        }
    }

    // 加载单一页面，获取整个页面的dom
    private Document load(String originPath){
        driver.get(originPath);
        String js = "window.scrollTo(0,document.body.scrollHeight)";
        for (int i = 0; i < 10; i++){
            try {
                TimeUnit.SECONDS.sleep(3);
                ((JavascriptExecutor) driver).executeScript(js);
                try {
                    WebElement element = driver.findElement(By.xpath("//*[@id='root']/div[3]/div/aside[2]/div/div[2]/button"));
                    element.click();
                }catch (Exception e){}
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        // 用Jsoup把selenium获取的页面(String)转成Document
        // 以便于用Xpath解析
        return Jsoup.parse(driver.getPageSource());
    }

}
