import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Iggie on 8/25/2016.
 */
public class Test {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://www.youtube.com/watch?v=aanO5yfinQ0");
            Document d = Jsoup.parse(url, 15000);
            Elements elements = d.select("a[href]");
            System.out.println(elements.size());
            Element e = elements.get(0);
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).toString().contains("/watch")) {
                    e = elements.get(i);
                    break;
                }
            }
            System.out.println(e);
//            System.out.println(elements);
            String part = e.toString();
            String watchid = part.substring(part.indexOf("href=") + 6, part.indexOf("class=") - 2);
            String youtubeurl = "https://www.youtube.com" + watchid;
            System.out.println(youtubeurl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
