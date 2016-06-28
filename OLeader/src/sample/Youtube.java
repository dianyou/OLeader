package sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("youtube")
public class Youtube {
	/**
	 * YouTube的搜索流程：
	 * 1.根据关键词搜索，获取channelId
	 * 2.根据channelId获取用户信息，已经用户上传视频的playlistId
	 * 3.根据playlistId获取videoId，获取最近发布的10个video地址
	 */
	private static Properties prop = new Properties();
	
	static 
	{
		 InputStream in=null;
	      //读取属性文件
  		try {
  			in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/youtube.properties");
  	        prop.load(in);
  			} catch (IOException e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
	}
	
	@GET
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam("keyword") String keyword)
	{
		long start = System.currentTimeMillis();
		System.out.println("keyword::"+keyword);
	    //展示的信息
		JSONArray columnList = new JSONArray();
		try {
			columnList.put(new JSONObject("{\"labelZh\":\"账号信息\",\"labelEn\":\"userName\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"观看数量\",\"labelEn\":\"viewCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"评论数量\",\"labelEn\":\"commentCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"订阅者数量\",\"labelEn\":\"subscriberCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"视频数量\",\"labelEn\":\"videoCount\"}"));

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		JSONObject response = new JSONObject();
		try {
			
			response.put("media", "YouTube");
			response.put("keyword",keyword);
			response.put("column",columnList);
			JSONArray ja = searchByKeyword(keyword);
			if(ja == null)
			{
				response.put("success",false );
				response.put("message","YouTube访问受限！"); 
				response.put("results", "");
			}
			else
			{
				response.put("success",true );
				response.put("results", ja);
			}
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		System.out.println("Time::"+(end-start)/1000+"s");
		try {
			response.put("time",(end-start)/1000);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Response responseCn = Response.status(200).
                entity(response.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		return responseCn;
	}
	
	@GET
	@Path("/playlist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response playlist(@QueryParam("id") String playlistId,@QueryParam("keyword") String keyword)
	{
		long start = System.currentTimeMillis();
	    //展示的信息
		JSONArray response = getPlaylist(playlistId,keyword,10);
		long end = System.currentTimeMillis();
		System.out.println("Time::"+(end-start)/1000);
		
		Response responseCn = Response.status(200).
                entity(response.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		
		return responseCn;
	}
	
	/**
	 * 根据关键词搜索
	 * @param keyword
	 * @return
	 */
	private static JSONArray searchByKeyword(String keyword)
	{
		
		JSONArray profile_array = new JSONArray();
		int limits = Integer.parseInt(prop.getProperty("search.limits"));
		String videoId = "",channelId = "";
		String response = null;
		String maxResults = "20";
		//根据关键词搜索的URL
		String ACTIVITY_KEYWORD_URL = prop.getProperty("URL.ACTIVITY_KEYWORD");
		//添加关键词
		ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{query}", keyword);
		//设置Max Results
		ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{maxResults}", maxResults);
		//访问API，使用nextPageToken遍历所有页
		boolean nextPage = true;
		String nextPageToken = "";
		int count=0;
		do
		{
			count++;
			try {
				response = getAPIData(ACTIVITY_KEYWORD_URL.replace("{pageToken}", nextPageToken));//第一次是空
				if(response == null || response.length()==0)
					return null;
				//解析API返回的数据，转换成JSON object
				JSONObject response_json = null;

				response_json = new JSONObject(response);
				//获取nextPageToken，如果非空，就继续获取下一页的数据
				if(!response_json.has("nextPageToken") || response_json.getString("nextPageToken").length()==0)
				{
					nextPage = false;
				}
				else
				{
					nextPageToken = "pageToken=" + response_json.getString("nextPageToken");
				}
				//抽取items
				JSONArray items = null;
				//首先，并判断items是否为空
				if(!response_json.has("items") || response_json.getString("items").length()==0)
				{
					nextPage = false;
				}
				else
				{
					items = response_json.getJSONArray("items");
					JSONObject profile = new JSONObject();
					/**
					 * 开启多线程
					 * 根据解析的各item，采用多线程的方式，获取用户信息
					 */
					ExecutorService pool = Executors.newFixedThreadPool(5);
					//2.逐条解析items，生成用户列表的JSON
					for(int i=0;i<items.length();i++)
					{
						JSONObject json = items.getJSONObject(i);
						YoutubeThread t1 = new YoutubeThread(json,profile_array);
						pool.execute(t1);
					}
					
					try {
						pool.shutdown();
						if(pool.awaitTermination(60,TimeUnit.SECONDS))
							System.out.println("All threads finished!");
						else
							System.out.println("Not all threads finished!");
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//只抓取一页
				nextPage = false;
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}while(nextPage);
		
		//根据viewCount排序
		JSONArray sortedArray = new JSONArray();
		sortedArray = searchResultSort(profile_array,"viewCount","channelId");
		System.out.println("Profile array::"+profile_array);
		return sortedArray;
	}
	
	
//	/**
//	 * 根据关键词搜索
//	 * @param keyword
//	 * @return
//	 */
//	private static JSONArray searchByKeyword(String keyword)
//	{
//		
//		JSONArray profile_array = new JSONArray();
//		
//		String videoId = "",channelId = "";
//		String response = null;
//		String maxResults = "20";
//		//根据关键词搜索的URL
//		String ACTIVITY_KEYWORD_URL = prop.getProperty("URL.ACTIVITY_KEYWORD");
//		//添加关键词
//		ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{query}", keyword);
//		//设置Max Results
//		ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{maxResults}", maxResults);
//		//访问API，使用nextPageToken遍历所有页
//		boolean nextPage = true;
//		String nextPageToken = "";
//		int count=0;
//		do
//		{
//			count++;
//			try {
//				response = getAPIData(ACTIVITY_KEYWORD_URL.replace("{pageToken}", nextPageToken));//第一次是空
//				if(response == null || response.length()==0)
//					return null;
//				//解析API返回的数据，转换成JSON object
//				JSONObject response_json = null;
//
//				response_json = new JSONObject(response);
//				//获取nextPageToken，如果非空，就继续获取下一页的数据
//				if(!response_json.has("nextPageToken") || response_json.getString("nextPageToken").length()==0)
//				{
//					nextPage = false;
//				}
//				else
//				{
//					nextPageToken = "pageToken=" + response_json.getString("nextPageToken");
//				}
//				//抽取items
//				JSONArray items = null;
//				//首先，并判断items是否为空
//				if(!response_json.has("items") || response_json.getString("items").length()==0)
//				{
//					nextPage = false;
//				}
//				else
//				{
//					items = response_json.getJSONArray("items");
//					JSONObject profile = new JSONObject();
//					//2.逐条解析items，生成用户列表的JSON
//					for(int i=0;i<items.length();i++)
//					{
//						JSONObject item_json = items.getJSONObject(i);
//						
//						//获取video的id
//						if(item_json.has("id"))
//							if(item_json.getJSONObject("id").has("videoId"))
//								videoId = item_json.getJSONObject("id").getString("videoId");
//						
//						
//						//获取snippet
//						JSONObject snippet = item_json.getJSONObject("snippet");
//						channelId = (snippet.getString("channelId")!=null?snippet.getString("channelId"):"0");
//						
//						//获取channel信息
//						profile = channelInfo(channelId);
//						profile_array.put(profile);
//					}
//				}
//				nextPage = false;
//				
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				break;
//			}
//		}while(nextPage);
//		
//		//根据viewCount排序
//		JSONArray sortedArray = new JSONArray();
//		sortedArray = searchResultSort(profile_array,"viewCount","channelId");
//		
//		return sortedArray;
//		
//	}
	/**
	 * 
	 * @param JArray：待排序的JSON数组
	 * @param sortValue：排序的参照字段
	 * @param id：各项数据的id；例如"userId"
	 * @return
	 */
	
	public static JSONArray searchResultSort(JSONArray JArray,String sortValue,String id)
	{
		JSONArray sortedArray = new JSONArray();
		try {
			//json写入到List中
			List<Map<String,Object>> list = new ObjectMapper().readValue(JArray.toString(), List.class);
			Collections.sort(list,new Comparator<Map<String,Object>>()
					{
						public int compare(Map<String,Object> m1,Map<String,Object> m2)
						{
							//根据排序字段，由大到小
							int k =0;
							String m1Value = m1.get(sortValue).toString();
							String m2Value = m2.get(sortValue).toString();
							if(m1Value ==null || m1Value.length() == 0)
								m1Value = "0";
							if(m2Value == null || m2Value.length() == 0)
								m2Value = "0";
							if(Long.parseLong(m2Value) > Long.parseLong(m1Value))
								k =1;
							else if(Long.parseLong(m2Value) < Long.parseLong(m1Value))
								k = -1;
							else
								k=0;
							//如果排序字段的值相同，按照actorId排序
							if(k==0)
							{
								k = m1.get(id).toString().compareTo(m2.get(id).toString());
							}
							return k;
						}
					}
					
					);
			
			for(int i=0;i<list.size();i++)
			{
				String referenceId = list.get(i).get(id).toString();
				//判断actorId重复
				if(i>0 && list.get(i).equals(list.get(i-1)))
					continue;
				for(int j=0;j<JArray.length();j++)
				{
					JSONObject item;
					try
					{
						item = JArray.getJSONObject(j);
						if(item.getString(id).equals(referenceId))
						{
							sortedArray.put(item);
							break;
						}
					}
					catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
			
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sortedArray;
	}
	
	public static JSONObject channelInfo(String channelId)
	{
		String CHANNEL_GET_URL = prop.getProperty("URL.CHANNEL_GET").replace("{channelId}",channelId);
		String response = null;
		JSONObject channelInfo = new JSONObject();
		
		response = getAPIData(CHANNEL_GET_URL);
		String title = "",description="";
				
		if(response==null || response.length()==0)
			return null;
		
		JSONObject response_json = null;
		JSONObject response_items = null;
		String playlistId = "",imageURL = "";
	//	System.out.println("Channel response::"+response);
		try {
			channelInfo.put("channelId",channelId);
			response_json = new JSONObject(response);
			if(response_json.has("items"))
				if(response_json.getJSONArray("items").length()>0)
				{
					response_items = response_json.getJSONArray("items").getJSONObject(0);
					JSONObject snippet = new JSONObject();
					if(response_items.has("snippet"))
					{
						snippet = response_items.getJSONObject("snippet");
						//判断image的url信息是否存在
						if( snippet.has("thumbnails"))
							if(snippet.getJSONObject("thumbnails").has("default"))
								imageURL = snippet.getJSONObject("thumbnails").getJSONObject("default").getString("url");
						channelInfo.put("imageURL",imageURL);
						//获取个人信息：title和description
						title = snippet.getString("title");
						description = snippet.getString("description");
						channelInfo.put("userName",title);
						channelInfo.put("description",description);
					}
					
					//获取playlistId
					if(response_items.has("contentDetails") && 
							response_items.getJSONObject("contentDetails").has("relatedPlaylists") &&
							response_items.getJSONObject("contentDetails").getJSONObject("relatedPlaylists").has("uploads")
							)
						playlistId = response_items.getJSONObject("contentDetails").getJSONObject("relatedPlaylists").getString("uploads");
					channelInfo.put("playlistId",playlistId);
					
					//获取统计信息
					JSONObject statistics = response_items.getJSONObject("statistics");
					String viewCount = (statistics.getString("viewCount")!=null?statistics.getString("viewCount"):"0");
					channelInfo.put("viewCount",Long.parseLong(viewCount));
					String commentCount = (statistics.getString("commentCount")!=null?statistics.getString("commentCount"):"0");
					channelInfo.put("commentCount",Long.parseLong(commentCount));
					String subscriberCount = (statistics.getString("subscriberCount")!=null?statistics.getString("subscriberCount"):"0");
					channelInfo.put("subscriberCount",Long.parseLong(subscriberCount));
					String hiddenSubscriberCount = (statistics.getString("hiddenSubscriberCount")!=null?statistics.getString("hiddenSubscriberCount"):"false");
					channelInfo.put("hiddenSubscriberCount",Boolean.parseBoolean(hiddenSubscriberCount));
					String videoCount = (statistics.getString("videoCount")!=null? statistics.getString("videoCount"):"0");
					channelInfo.put("videoCount",Long.parseLong(videoCount));
					
				}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return channelInfo;
	}
	
	/**
	 * 获取playlistId的播放列表
	 * @param playlistId
	 * @param keyword
	 * @param limit
	 * @return
	 */
	private static JSONArray getPlaylist(String playlistId,String keyword,int limit)
	{		
		String maxResults = "10";
		String PLAY_LIST_URL = prop.getProperty("URL.PLAY_LIST").replace("{playlistId}",playlistId);
		PLAY_LIST_URL = PLAY_LIST_URL.replace("{maxResults}", maxResults);
		String response = null;
		response = getAPIData(PLAY_LIST_URL);
		JSONArray results = new JSONArray();
		
		if(response ==null || response.length()==0)
			return null;
		
		JSONObject response_json  = null;
		JSONArray itemlist = null;
		JSONObject item = null;
		
		try {
			response_json = new JSONObject(response);
			itemlist = response_json.getJSONArray("items");
			if(itemlist ==null || itemlist.length()==0)
				return null;
			for(int i=0;i<itemlist.length();i++)
			{
				JSONObject results_item = new JSONObject();
				item = itemlist.getJSONObject(i);
				JSONObject snippet = item.getJSONObject("snippet");
				String channelId = snippet.getString("channelId");
				String videoId = item.getJSONObject("contentDetails").getString("videoId");
				
				String videoURL = "https://www.youtube.com/watch?v="+videoId;
				
				String title = snippet.getString("title");
				String description = snippet.getString("description");
				String publishedAt = "";
				if(snippet.has("publishedAt"))
				{
					publishedAt = snippet.getString("publishedAt").replace("T"," ").replace("Z","");
					long publishedMsec = 0;
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
					Date date =null;
					try {
						date = format.parse(publishedAt);
						publishedMsec = date.getTime();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					results_item.put("publishedAt",publishedMsec);
				}
				
				results_item.put("channelId",channelId);
				results_item.put("videoId",videoId);
				results_item.put("videoURL",videoURL);
				results_item.put("title",title);
				results_item.put("description",description);
				//判断是否和搜索词相关
				boolean related = false;
				if(title.toLowerCase().contains(keyword.toLowerCase())||description.toLowerCase().contains(keyword.toLowerCase()))
					related = true;
				results_item.put("related",related);
				
				//YouTube中没有转发信息，默认为false
				results_item.put("forwarded",false);
				
				results.put(results_item);
				if(results.length()>=limit)
					break;
				
			}
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONArray sortResults = new JSONArray();
		sortResults = playListSort(results);
		return sortResults;
	}
	
	/**
	 * 根据related, publishedAt排序
	 * @param JArray
	 * @return
	 */
	public static JSONArray playListSort(JSONArray JArray)
	{
		JSONArray sortedArray = new JSONArray();
		try
		{
			//json写入到List中
			List<Map<String,Object>> list = new ObjectMapper().readValue(JArray.toString(), List.class);
			Collections.sort(list,new Comparator<Map<String,Object>>()
					{
						public int compare(Map<String,Object> m1,Map<String,Object> m2)
						{
							//circleByCount由大到小
							boolean related1 = Boolean.parseBoolean(m1.get("related").toString());
							boolean related2 = Boolean.parseBoolean(m2.get("related").toString());
							
							long publishedAt1 =0;
							if(m1.get("publishedAt") !=null)
								publishedAt1 = Long.parseLong(m1.get("publishedAt").toString());

							long publishedAt2 =0;
							if(m2.get("publishedAt") !=null)
								publishedAt2 = Long.parseLong(m2.get("publishedAt").toString());

							if(related1 ==true && related2 ==false)
								return -1;
							else if(related1 == false && related2 == true)
								return 1;
							
							//处理related值相同的情况
							if(publishedAt1 > publishedAt2)
								return -1;
							else
								return 1;
							
						}
					}
					
					);
			
			for(int i=0;i<list.size();i++)
			{
				String actorId = list.get(i).get("videoId").toString();
				//判断videoId重复
				if(i>0 && list.get(i).equals(list.get(i-1)))
					continue;
				for(int j=0;j<JArray.length();j++)
				{
					JSONObject item;
					try
					{
						item = JArray.getJSONObject(j);
						if(item.getString("videoId").equals(actorId))
						{
							sortedArray.put(item);
							break;
						}
					}
					catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sortedArray;
	}
	
	/**
	 * 根据URL获取数据
	 * @param APIUrl
	 * @return
	 */
	
	public static String getAPIData(String APIUrl)
	{
        String result = "";
        BufferedReader in = null;
        String url = APIUrl,token="";
        String proxyAddr = prop.getProperty("proxyAddr");
		int tokenCount = Integer.parseInt(prop.getProperty("youtube.keyNum"));
        int proxyPort = Integer.parseInt(prop.getProperty("proxyPort"));
        
		URL connectURL = null;
		URLConnection ucon = null;
		//设置代理
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddr, proxyPort));
		
		
		boolean flag = true;
		int index=0;
		
		while(flag)
		{
			token = prop.getProperty("youtube.accessToken"+index);
			url = url.replace("{YOUR_API_KEY}", token);
			try {
				// 建立连接 
				System.out.println(url);
				connectURL = new URL(url);
				ucon = connectURL.openConnection(proxy);
				ucon.connect();			
		        // 定义 BufferedReader输入流来读取URL的响应
		        in = new BufferedReader(new InputStreamReader(
		            ucon.getInputStream(),"utf-8"));
		            String line;
		            while ((line = in.readLine()) != null) {
		                result += line;
		            }
		        in.close();
		        flag = false;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Index::"+index);
				System.out.println("URL::"+url);
				flag = true;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				index++;
				//尝试三次
				if(index>2)
					break;
			}//第一次是空
			/**
			 * 判断是否获取到网络数据，如果获取成功，跳出循环,
			 * 否则使用新的token重新获取数据
			 */
			if(result!=null && result.length()>0)
				break;
		}
		
		
		return result;
	}
	
	public static void main(String args[])
	{
		JSONArray ja = searchByKeyword("中国");
		System.out.println("JSON Array length::"+ ja.length());
		
		for(int i = 0;i<ja.length();i++)
		{
			try {
				System.out.println(ja.get(i).toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}
	
}
