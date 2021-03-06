package sample;
/**
 * Jersey
 */
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
/**
 * API data
 */
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;



@Path("googleplus")
public class GooglePlus {
	
	
    private static Properties prop = new Properties();
	
	static 
	{
		 InputStream in=null;
	      //读取属性文件
  		try {
  			in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/googleplus.properties");
  	        prop.load(in);
  			}
  		catch (IOException e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
	}
	
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam("keyword") String keyword)
	{
		long start = System.currentTimeMillis();
		System.out.println("keyword::"+keyword);
	    //展示的信息
		JSONArray columnList = new JSONArray();
		try {
			columnList.put(new JSONObject("{\"labelZh\":\"账号信息\",\"labelEn\":\"userName\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"帖子位置\",\"labelEn\":\"location\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"账户圈子个数\",\"labelEn\":\"circleByCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"相关帖子回复数量\",\"labelEn\":\"replies\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"相关帖子支持个数\",\"labelEn\":\"plusoners\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"相关帖子分享次数\",\"labelEn\":\"reshares\"}"));
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject response = new JSONObject();
		try {
			response.put("media", "Google+");
			response.put("keyword",keyword);
			response.put("column",columnList);
			JSONArray results = searchByKeyword(keyword);
			if(results == null)
			{
				response.put("success",false );
				response.put("message","GooglePlus访问受限！"); 
				response.put("results", "");
			}
			else
			{
				response.put("success",true );
				response.put("results", results);
			}
			JSONArray sortedArray = searchResultSort(results);
			response.put("results", sortedArray);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		System.out.println((end-start)/1000+" s");
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
	@Path("/activityList")
	@Produces(MediaType.APPLICATION_JSON)
	public Response activitylist(@QueryParam("id") String actorId,@QueryParam("keyword") String keyword)
	{
		long start = System.currentTimeMillis();
	    //展示的信息
		JSONArray activityList = peopleActivityList(actorId,keyword);
		long end = System.currentTimeMillis();
		System.out.println((end-start)/1000);
		
		Response responseCn = Response.status(200).
                entity(activityList.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		return responseCn;
	}	
	/**
	 * 根据URL获取数据
	 * @param APIUrl
	 * @return
	 */
	public static String getAPIData(String APIUrl)
	{
        String result = "";
        BufferedReader br = null;
        String url = APIUrl,token="";
        String proxyAddr = prop.getProperty("proxyAddr");
		int tokenCount = Integer.parseInt(prop.getProperty("gplus.keyNum"));
        int proxyPort = Integer.parseInt(prop.getProperty("proxyPort"));
        
		URL connectURL = null;
		URLConnection ucon = null;
		//设置代理
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddr, proxyPort));
        
		boolean flag = true;
		int index =0;
	//	for(int i=0;i<tokenCount;i++)
		while(flag)
		{
			result = "";
			token = prop.getProperty("gplus.accessToken"+index);
			url = url.replace("{YOUR_API_KEY}", token);
			System.out.println(url);
			flag = false;
			try {
				// 建立连接 
				connectURL = new URL(url);

				ucon = connectURL.openConnection(proxy);
				ucon.connect();			
		        // 定义 BufferedReader输入流来读取URL的响应
		        br = new BufferedReader(new InputStreamReader(
		            ucon.getInputStream(),"utf-8"));
		        String line;
		        while ((line = br.readLine()) != null) {
		                result += line;
		            }
		        br.close();
				
			} catch (IOException e) {
				System.out.println("Here"+index);
//				System.out.println("result::"+result);
//				System.out.println("Length::"+result.length());
	//			e.printStackTrace();
				flag = true;
			}//第一次是空
			/**
			 * 判断是否获取到网络数据，如果获取成功，跳出循环,
			 * 否则使用新的token重新获取数据
			 */
			index = index +1;
		}

		return result;
	}
	
	//判断字符串中是否包含中文字符
	public static boolean CNCheck(String keyword)
	{
		if(keyword == null || keyword.length()==0)
		{
			return false;
		}
		boolean flag = false;
		Pattern p=Pattern.compile("[\u4e00-\u9fa5]");
	    Matcher m=p.matcher(keyword);
	   if(m.find())
	   {
		   flag =  true;
	   }
	       return flag;
	}
	
	/**
	 * 根据关键词搜索 
	 * @param keyword
	 * @return
	 */
		public static JSONArray searchByKeyword(String keyword)
		{
			JSONArray profile_array = new JSONArray();
			String query = null;
			String response = null;
			String actor_id = null;
			//根据关键词搜索的URL
			String ACTIVITY_KEYWORD_URL = prop.getProperty("URL.ACTIVITY_KEYWORD");
			//抽取有效的fields
			String fields = "fields=nextPageToken,items(id,title,url,published,updated,actor,object)";
			ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{fields}", fields);
			//判断输入的关键词中是否包含中文
			boolean flag = false;
			flag = CNCheck(keyword);
			if(flag)
			{
				query = "query="+keyword+"&language=CN";
			}
			else
			{  
				query = "query="+keyword;
			}
			ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{query}", query);
			//设置Max Results
			String maxResults = "maxResults="+prop.getProperty("searchBykeyword.maxResults");
			ACTIVITY_KEYWORD_URL = ACTIVITY_KEYWORD_URL.replace("{maxResults}", maxResults);
			
			//访问API，使用nextPageToken遍历所有页
			boolean nextPage = true;
			String nextPageToken = "";
			int count=0;
			do
			{
				count++;
				try {
					response = getAPIData(ACTIVITY_KEYWORD_URL.replace("{pageToken}", nextPageToken));
					if(response==null || response.length()<=0)
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
					
					if(response_json.has("items") && response_json.getJSONArray("items").length()>0)
					{
						items = response_json.getJSONArray("items");
						/**
						 * 开启多线程
						 * 根据解析的各item，采用多线程的方式，获取用户信息
						 */
						ExecutorService pool = Executors.newFixedThreadPool(5);
						//2.逐条解析items，生成用户列表的JSON
						for(int i=0;i<items.length();i++)
						{
							JSONObject json = items.getJSONObject(i);
							GooglePlusThread t1 = new GooglePlusThread(json,profile_array);
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
					else
					{
						nextPage = false;
					}

					nextPage = false;
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}while(nextPage);
			
			System.out.println("Results::"+profile_array);
			return profile_array;
		}
	
	/**
	 * 获取用户信息
	 * @param actorId
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject peopleInfo(String actorId) throws IOException, JSONException
	{
		JSONObject profile = new JSONObject();
		String response = null;
		String PEOPLE_GET_URL = prop.getProperty("URL.PEOPLE_GET");
		//替换userID
		PEOPLE_GET_URL = PEOPLE_GET_URL.replace("{USERID}", actorId);
		//获取用户信息
			response = getAPIData(PEOPLE_GET_URL);
			if(response==null && response.length()>0)
				return null;
		JSONObject response_json = new JSONObject(response);
		
		//提取response里面的各项信息，拼成结果的JSON对象
			profile.put("actorId",actorId);
		if(response_json.has("displayName"))
			profile.put("userName",response_json.getString("displayName"));
		else
			profile.put("userName","");
		if(response_json.has("gender"))
			profile.put("gender", response_json.getString("gender"));
		else
			profile.put("gender","");
		if(response_json.has("url"))
			profile.put("profileURL",response_json.getString("url"));
		else
			profile.put("profile url","");
		if(response_json.has("birthday"))
			profile.put("birthday",response_json.getString("birthday"));
		else
			profile.put("birthday","");
		if(response_json.has("verified"))
			profile.put("verified",response_json.getString("verified"));
		else
			profile.put("verified","");
		if(response_json.has("circledByCount"))
			profile.put("circleByCount",response_json.getString("circledByCount"));		
		else
			profile.put("circleByCount","0");
		if(response_json.has("image"))
		{
			JSONObject image = response_json.getJSONObject("image");
			if(image.has("url"))
				profile.put("imageURL",image.getString("url"));
			else
				profile.put("imageURL","");
			
		}
		else
			profile.put("imageURL","");
		
		return profile;
	}
	
	/**
	 * 
	 * @param actorId,limit(限制返回的activity的个数)
	 * @return 用户发表的所有帖子的信息
	 * @throws IOException
	 * @throws JSONException 
	 */
	public static JSONArray peopleActivityList(String actorId,String keyword)
	{
		int limit = Integer.parseInt(prop.getProperty("activitylist.maxResults"));
		JSONArray activities = new JSONArray();
		//读取属性文件
		String response = null;
		String nextPageToken="";
		boolean nextPage = true;
		
		try {
			String PEOPLE_ACTIVITY_LIST_URL = prop.getProperty("URL.PEOPLE_ACTIVITY_LIST");
			//替换userID和collections
			PEOPLE_ACTIVITY_LIST_URL = PEOPLE_ACTIVITY_LIST_URL.replace("{USERID}", actorId).replace("{COLLECTIONS}", "public");
			PEOPLE_ACTIVITY_LIST_URL = PEOPLE_ACTIVITY_LIST_URL.replace("{maxResults}", "maxResults="+limit);
			//获取用户 Activity List
			do
			{
				//获取activity的list信息
				response = getAPIData(PEOPLE_ACTIVITY_LIST_URL);
				if(response==null && response.length()>0)
						return null;
				JSONObject response_json = new JSONObject(response);
				nextPage = false;
				//解析items
				
				//目前只爬取用户最近发布的10条帖子
				//所以不需要继续访问下一页
				nextPage = false;				
//				//获取nextPageToken，如果非空，就继续获取下一页的数据
//				if(!response_json.has("nextPageToken") || response_json.getString("nextPageToken").length()==0)
//				{
//					nextPage = false;
//				}
//				else
//				{
//					nextPageToken = "pageToken=" + response_json.getString("nextPageToken");
//				}
				//抽取items
				JSONArray items_jsonArray = null;
				//首先，并判断items是否为空
				if(!response_json.has("items") || response_json.getString("items").length()==0)
				{
					nextPage = false;
				}
				else
				{

					items_jsonArray = response_json.getJSONArray("items");
					//2.逐条解析items，生成用户列表的JSON
					for(int i=0;i<items_jsonArray.length();i++)
					{
						//多个activity的json对象组成array
						JSONObject activity_json = new JSONObject();
						//获取activity的信息	
						JSONObject item_json = items_jsonArray.getJSONObject(i);
						if(item_json.has("id"))
							activity_json.put("activityID",item_json.getString("id"));
						else 
							activity_json.put("activityID","");
						if(item_json.has("title"))
							activity_json.put("title",item_json.getString("title"));
						else
							activity_json.put("title","");
						if(item_json.has("url"))
							activity_json.put("activityURL", item_json.getString("url"));
						if(item_json.has("published"))
						{
							String publishedAt = item_json.getString("published").replace("T"," ").replace("Z","");
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
							activity_json.put("publishedAt",publishedMsec);
							
						}
						if(item_json.has("updated"))
						{
							String updatedAt = item_json.getString("updated").replace("T"," ").replace("Z","");
							long updatedMsec = 0;
							SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
							Date date =null;
							try
							{
								date = format.parse(updatedAt);
								updatedMsec = date.getTime();
							}
							catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							activity_json.put("updatedAt",updatedMsec);
						}				
						
						boolean forwarded = false,related = false;
						String content="";
						//回复次数，点赞数，分享次数
						String replies="",plusoners = "", reshares = "";
						//获取object信息
						if(item_json.has("object") && item_json.getString("object").length()>0)
						{
							JSONObject object = item_json.getJSONObject("object");
							//若object中有actor的id信息，说明这是一个转发的activity
							if(object.has("actor") && object.getJSONObject("actor").has("id"));
							{
								forwarded =  object.getJSONObject("actor").has("id");
							}	
							
							if(object.has("content"))
								content = object.getString("content");
							else
								content = "";
							//判断是否包含相关的关键字，全转换成小写字母进行匹配
							if(content.toLowerCase().contains(keyword.toLowerCase()))
								related = true;
							//回复次数
							if(object.has("replies"))
								if(object.getJSONObject("replies").has("totalItems"))
									replies = object.getJSONObject("replies").getString("totalItems");
							else
								replies = "";
							//点赞数
							if(object.has("plusoners"))
								if(object.getJSONObject("plusoners").has("totalItems"))
									plusoners = object.getJSONObject("plusoners").getString("totalItems");
							else
								plusoners = "";						
							//分享数
							if(object.has("reshares"))
								if(object.getJSONObject("reshares").has("totalItems"))
									reshares = object.getJSONObject("reshares").getString("totalItems");
							else
								reshares = "";	
						}
						
						activity_json.put("forwarded", forwarded);
						activity_json.put("related", related);
						activity_json.put("description",content);
						activity_json.put("replies",replies);
						activity_json.put("plusoners",plusoners);
						activity_json.put("reshares",reshares);			
					//	System.out.println(activity_json);
						activities.put(activity_json);
					}

				}
//				if(activities.length()>=limit)
//					break;
			}while(nextPage);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		JSONArray sortActivities = new JSONArray();
		
		sortActivities = activityListSort(activities);
		return sortActivities;
	}
	
	/**
	 * 根据related, publishedAt排序
	 * @param JArray
	 * @return
	 */
	public static JSONArray activityListSort(JSONArray JArray)
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
				String actorId = list.get(i).get("activityID").toString();
				//判断activityID重复
				if(i>0 && list.get(i).equals(list.get(i-1)))
					continue;
				for(int j=0;j<JArray.length();j++)
				{
					JSONObject item;
					try
					{
						item = JArray.getJSONObject(j);
						if(item.getString("activityID").equals(actorId))
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
	 * 对GooglePlus获取的用户列表按照circleByCount排序，并去除重复的用户信息
	 * @param JArray
	 * @return
	 */
	public static JSONArray searchResultSort(JSONArray JArray)
	{
		JSONArray sortedArray = new JSONArray();
		try {
			//json写入到List中
			List<Map<String,Object>> list = new ObjectMapper().readValue(JArray.toString(), List.class);
			Collections.sort(list,new Comparator<Map<String,Object>>()
					{
						public int compare(Map<String,Object> m1,Map<String,Object> m2)
						{
							//circleByCount由大到小
							String m1Value = m1.get("circleByCount").toString();
							String m2Value = m2.get("circleByCount").toString();
							if(m1Value ==null || m1Value.length() == 0)
								m1Value = "0";
							if(m2Value == null || m2Value.length() == 0)
								m2Value = "0";
							int k = Integer.parseInt(m2Value) - Integer.parseInt(m1Value);
							//如果circleByCount相同，按照actorId排序
							if(k==0)
							{
								k = m1.get("actorId").toString().compareTo(m2.get("actorId").toString());
							}
							return k;
						}
					}
					
					);
			
			for(int i=0;i<list.size();i++)
			{
				String actorId = list.get(i).get("actorId").toString();
				//判断actorId重复
				if(i>0 && list.get(i).equals(list.get(i-1)))
					continue;
				for(int j=0;j<JArray.length();j++)
				{
					JSONObject item;
					try
					{
						item = JArray.getJSONObject(j);
						if(item.getString("actorId").equals(actorId))
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
	
	public static void main(String args[])
	{
	    //展示的信息
//	    String column = "User ID,User Name,Gender,Birthday,Profile URL,CircleByCount,Verfied";
//		JSONObject response = new JSONObject();
//		try {
//			response.put("media", "Google+");
//			response.put("keyword","天津爆炸");
//			response.put("column",column);
//		//	response.put("results", searchBykeyword("Bangkok"));
//			System.out.println("Results length::"+response.length());
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		peopleActivityList("107222473366265501216","Bangkok",10);
		
		JSONArray results = searchByKeyword("bangkok");
		System.out.println("Array Length::"+results.length());
		System.out.println("Results::"+results);
		
	

		
	}
	
}
