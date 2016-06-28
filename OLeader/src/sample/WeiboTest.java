package sample;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class WeiboTest {
	
	public static void main(String[] args)
	{
		System.setProperty("http.proxyHost", "proxy.pek.sap.corp");
		System.setProperty("http.proxyPort", "8080");
		String weiboUrl = "http://s.weibo.com/weibo/%E5%A4%A9%E6%B4%A5%E7%88%86%E7%82%B8";
		Document doc = null;
		Map<String, String> map = new HashMap<>();
		try {
			doc = Jsoup.connect(weiboUrl).userAgent("Mozilla/5.0 (Windows NT 6.1; rv:22.0) Gecko/20100101 Firefox/22.0").ignoreContentType(true).get();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		Elements eles = doc.getElementsByClass("S_content_l");
		try {
			PrintWriter pw = new PrintWriter( new OutputStreamWriter(  
			        new FileOutputStream("C:/Users/i301431/Documents/SAP/weibo.txt"),"UTF8"));
			pw.write(doc.toString());
			pw.flush();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
