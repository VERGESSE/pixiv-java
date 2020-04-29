package top.vergessen.pixiv.util;

import org.apache.commons.io.IOUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.Proxy;
import java.util.HashMap;

// 图片下载工具类
public class ImgDownUtil {

    private String imgUrl;
    private String fileUrl;
    private HashMap<String,String> headers;
    private Proxy proxy;
    private String type;

    public ImgDownUtil(String imgUrl, String type ,String fileUrl,
                       HashMap<String,String> headers, Proxy proxy){
        this.imgUrl = imgUrl;
        this.fileUrl = fileUrl;
        this.headers = headers;
        this.proxy = proxy;
        this.type = type;
    }

    public void download() throws IOException {
        HttpsUrlValidator.trustEveryone();

        String[] split = imgUrl.split("/");
        String imgId = split[split.length - 1].split("_")[0];
        Connection imgConnection = Jsoup
                .connect("https://i.pximg.net/img-original/img/"+imgUrl+"."+type)
                .headers(headers)
                // 此处不设置referrer无法下载
                .referrer("https://www.pixiv.net/member_illust.php?mode=medium&illust_id="+imgId)
                .proxy(proxy)
                .ignoreContentType(true)
                .maxBodySize(20971520)
                // 超时时间一刻钟，图片过大或网络不好超时无法下载
                .timeout(900000);
        byte[] imgBytes = imgConnection.execute().bodyAsBytes();
        BufferedInputStream img = new BufferedInputStream(
                new ByteArrayInputStream(imgBytes));
        try(BufferedOutputStream imgFileOS = new BufferedOutputStream(
                new FileOutputStream(fileUrl+type))) {
            IOUtils.copy(img, imgFileOS);
            img.close();
        }
    }
}