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

public class PageLoader {

    private static final String PIVIX = "https://www.pixiv.net";

    private String originPath;
    private List<String> pathList;
    private WebDriver driver;
    private BlockingQueue<String> imgQueue;

    public PageLoader(String originPath, BlockingQueue<String> imgQueue){
        this.originPath = originPath;
        this.pathList = new ArrayList<>();
        this.driver = MyDriver.getInstance().getWebDriver();
        this.imgQueue = imgQueue;
    }

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
        new Thread(new ImgLoader()).start();
    }

    class ImgLoader implements Runnable{

        @Override
        public void run() {
            int num = 0;
            try {
                for (String s : pathList) {
                    Document doc = load(PIVIX + s);
                    List<JXNode> imgUrls = new XpathUtil(doc)
                            .xpath("//li/div/div/div/a/div/img/@src");
                    int i = 0;
                    for (JXNode imgUrl : imgUrls) {
                        Pattern pattern = Pattern.compile(".*?img/(.*?_p0).*?");
                        Matcher matcher = pattern.matcher(imgUrl.toString());
                        try {
                            matcher.find();
                            i++;
                            String uri = matcher.group(1);
                            imgQueue.put(uri);
                        }catch (Exception e){}
                    }
                    System.out.println("页面: " + ++num + " 爬取完成 "+i);
                }
            }finally {
                driver.quit();
            }
        }
    }

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
        return Jsoup.parse(driver.getPageSource());
    }

}
