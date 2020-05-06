package com.cgd.esjd.utils;

import com.cgd.esjd.entity.Content;
import com.sun.org.apache.bcel.internal.generic.DCONST;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.print.Doc;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author CaoGongde
 * @date 2020/4/29 14:05
 */
@Component
public class HtmlParseUtil {
    public static void main(String[] args) throws IOException {
        paseJD("java");

    }

    public static List<Content> paseJD(String keyword) throws IOException {
        //获取请求
        String url = "https://search.jd.com/Search?keyword="+keyword+"&enc=utf-8";
        //解析网页 返回的是浏览器的document对象
        Document document = Jsoup.parse(new URL(url), 30000);
        //所有你在js中可以使用的方法  这里都可以使用
        Element element = document.getElementById("J_goodsList");
        System.out.println(element.html());
        //获取所有的li元素
        Elements elements = element.getElementsByTag("li");
        List<Content> goodList = new ArrayList<>();
        for (Element e1 : elements) {
            String img = e1.getElementsByTag("img").eq(0).attr("source-data-lazy-img");
            String price = e1.getElementsByClass("p-price").eq(0).text();
            String title = element.getElementsByClass("p-name").eq(0).text();
            Content content = new Content();
            content.setImg(img);
            content.setPrice(price);
            content.setTitle(title);
            goodList.add(content);

        }
        return goodList;
    }
}
