package top.vergessen.pixiv.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import top.vergessen.pixiv.propertie.PropertyMgr;

// 封装selenium的执行器，方便使用
public class MyDriver {

    private static final MyDriver driver = new MyDriver();
    private static WebDriver webDriver;

    private MyDriver(){}

    static {
        System.setProperty("webdriver.chrome.driver", PropertyMgr.getString("chromedriver"));
        ChromeOptions options = new ChromeOptions();
        String chromePath = PropertyMgr.getString("userPath");
        options.addArguments("--user-data-dir="+chromePath);
        options.addArguments("--disable-gpu");
        options.addArguments("start-maximized");
        options.addArguments("enable-automation");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-browser-side-navigation");
        options.addArguments("blink-settings=imagesEnabled=false");

        webDriver = new ChromeDriver(options);
    }

    public static MyDriver getInstance(){
        return driver;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public void quit(){
        webDriver.quit();
    }
}
