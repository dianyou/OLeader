package sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("twitter")
public class TwitterGather {
	
	private static Properties prop = new Properties();

	static 
	{
		 InputStream in=null;
	      //读取属性文件
  		try {
  			in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/twitter.properties");
  	        prop.load(in);
  			}
  		catch (IOException e) {
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
			columnList.put(new JSONObject("{\"labelZh\":\"位置\",\"labelEn\":\"location\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"鲜花数量\",\"labelEn\":\"followersCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"好友数量\",\"labelEn\":\"friendsCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"收藏数量\",\"labelEn\":\"favouritesCount\"}"));
			columnList.put(new JSONObject("{\"labelZh\":\"状态数量\",\"labelEn\":\"statusesCount\"}"));

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		JSONObject response = new JSONObject();
		try {
			response.put("media", "Twitter");
			response.put("keyword",keyword);
			response.put("column",columnList);
			JSONArray ja = searchByKeyword(keyword);
			if(ja == null)
			{
				response.put("success",false );
				response.put("message","twitter访问受限！"); 
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
	@Path("/timeline")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response userTimeline(@QueryParam("id")long userId,@QueryParam("keyword") String keyword)
	{
		long start = System.currentTimeMillis();
	    //展示的信息
		JSONArray response = getUserTimelie(userId,keyword);
		long end = System.currentTimeMillis();
		System.out.println("Time::"+(end-start)/1000);
		
		Response responseCn = Response.status(200).
                entity(response.toString()).
                header("Content-Type", "application/json; charset=utf-8").build();
		
		return responseCn;
	}
	
	/**
	 * 根据搜索词获取查询结果
	 * @param keyword
	 * @param limit
	 * @return
	 */
	public static JSONArray searchByKeyword(String keyword)
	{
		int limit = Integer.parseInt(prop.getProperty("searchByKeyword.limits"));
        Query query = new Query(keyword);
		ConfigurationBuilder cb = new ConfigurationBuilder();
	    QueryResult queryResult= null; 
	    TwitterFactory tf = null;
	    Twitter twitter = null;
        int accountNum = Integer.parseInt(prop.getProperty("twitter.accountNum").toString());
        //添加token，访问网络获得twitter实例
        for(int i=0;i<accountNum;i++)
        {
        	String consumerKey = prop.getProperty("twitter.consumerKey"+i);
        	String consumerSecret = prop.getProperty("twitter.consumerSecret"+i);
  		   	String accessToken = prop.getProperty("twitter.accessToken"+i);
  		   	String accessTokenSecret = prop.getProperty("twitter.accessTokenSecret"+i);
  		   	//proxy参数
  	        String proxyAddr = prop.getProperty("proxyAddr");
  		   	int proxyPort = Integer.parseInt(prop.getProperty("proxyPort"));
 	        try {
  		   	cb.setDebugEnabled(true)
  	          .setOAuthConsumerKey(consumerKey)
  	          .setOAuthConsumerSecret(consumerSecret)
  	          .setOAuthAccessToken(accessToken)
  	          .setOAuthAccessTokenSecret(accessTokenSecret)
  	          .setHttpProxyHost(proxyAddr)
  	          .setHttpProxyPort(proxyPort);
  	        tf = new TwitterFactory(cb.build());
  	        twitter = tf.getInstance();

 				queryResult = twitter.search(query);
 			} catch (Exception e) {
		    	System.out.println(query.getQuery());
		    	System.out.println(e.getMessage());
		    //	System.out.println(((TwitterException) e).getExceptionCode());
 			} 
 	        if(queryResult !=null)
 	        	break;
        }
        
        if(queryResult==null)
        	return null;
        
        if(queryResult.toString().length() == 0 )
        	return null;
        
        JSONArray userArray = new JSONArray();
        JSONObject user = null;
        boolean flag = true;
        List<Status> list = null;
        int count = 0;
        //处理query的结果，迭代查询，获取user信息
        while(flag)
        {
        	list =  queryResult.getTweets();
        	count = count + list.size();
        	//获取用户信息
        	for(int i=0;i<list.size();i++)
        	{
        		User userInfo = list.get(i).getUser();
        		try
        		{
            		user = new JSONObject();
            	 	Long id = userInfo.getId();
   					user.put("userId", id);
            	 	String userName = userInfo.getName();
            	 	user.put("userName",userName);
            	 	String screenName = userInfo.getScreenName();
            	 	user.put("screenName",screenName);
            	 	String location  = userInfo.getLocation();
            	 	user.put("location",location);
            	 	String description = userInfo.getDescription();
            	 	user.put("description",description);
            	 	String imageURL = userInfo.getProfileImageURL();
            	 	user.put("imageURL",imageURL);
            	 	//count参数
            	 	int favouritesCount =  userInfo.getFavouritesCount();
            	 	user.put("favouritesCount",favouritesCount);
            	 	int followersCount = userInfo.getFollowersCount();
            	 	user.put("followersCount",followersCount);
            	 	int friendsCount = userInfo.getFriendsCount();
            	 	user.put("friendsCount",friendsCount);
            	 	
            	 	int statusesCount = userInfo.getStatusesCount();
            	 	user.put("statusesCount",statusesCount);
            	 	
            	 	
        		}
        	 	catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		userArray.put(user);
        	}
        	//翻页，翻到下一页的搜索结果
        	if(queryResult.hasNext())
        	{
        		try
        		{
        			queryResult = twitter.search(queryResult.nextQuery());
        		} catch (TwitterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					flag = false;
				}
        		
        	}
        	
        	if(count > limit)
        		flag = false;
        }
	        
	        JSONArray sortedArray = jsonArraySort(userArray,"followersCount","userId");
	        
	        return sortedArray;
	}
	
	/**
	 * 
	 * @param JArray：待排序的JSON数组
	 * @param sortValue：排序的参照字段
	 * @param id：各项数据的id；例如"userId"
	 * @return
	 */
	public static JSONArray jsonArraySort(JSONArray JArray,String sortValue,String id)
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
	
	
	
	/**
	 * 获取用户发布的10条状态（twitter默认返回20条）
	 * @param id 用户ID
	 */
	public static JSONArray getUserTimelie(long id,String keyword)
	{
		int limit = Integer.parseInt(prop.getProperty("timeline.limits"));
		ConfigurationBuilder cb = new ConfigurationBuilder();
		List<Status> statuses= null; 
	    TwitterFactory tf = null;
	    Twitter twitter = null;
	    JSONArray results = new JSONArray();
        int accountNum = Integer.parseInt(prop.getProperty("twitter.accountNum").toString());
        //添加token，访问网络获得twitter实例
        for(int i=0;i<accountNum;i++)
        {
        	String consumerKey = prop.getProperty("twitter.consumerKey"+i);
        	String consumerSecret = prop.getProperty("twitter.consumerSecret"+i);
  		   	String accessToken = prop.getProperty("twitter.accessToken"+i);
  		   	String accessTokenSecret = prop.getProperty("twitter.accessTokenSecret"+i);
  		   	//proxy参数
  	        String proxyAddr = prop.getProperty("proxyAddr");
  		   	int proxyPort = Integer.parseInt(prop.getProperty("proxyPort"));
  		   	 
  		   	cb.setDebugEnabled(true)
  	          .setOAuthConsumerKey(consumerKey)
  	          .setOAuthConsumerSecret(consumerSecret)
  	          .setOAuthAccessToken(accessToken)
  	          .setOAuthAccessTokenSecret(accessTokenSecret)
  	          .setHttpProxyHost(proxyAddr)
  	          .setHttpProxyPort(proxyPort);
  	        tf = new TwitterFactory(cb.build());
  	        twitter = tf.getInstance();
  	        
 	        try {
 	        	statuses= twitter.getUserTimeline(id);
 			} catch (TwitterException e) {
		    	System.out.println(e.getErrorMessage());
		    	System.out.println(e.getExceptionCode());
 			} 
 	        
 	        //解析每条状态
 	        for(int j=0;j<statuses.size();j++)
 	        {
 	        	try {
 	    	        JSONObject item = new JSONObject();
 	 	        	Status status = statuses.get(j);
 	 	        	Long publishedAt = status.getCreatedAt().getTime();
					item.put("publishedAt",publishedAt);
	 	        	String description = status.getText();
	 	        	System.out.println("description::"+description);
	 	        	item.put("description",description);
	 	        	boolean related = false;
	 	        	if(description.toLowerCase().contains(keyword.toLowerCase()))
	 	        		related = true;
	 	        	item.put("related",related);
	 	        	boolean forwarded = false;
	 	        	//没有forward信息，默认为false
	 	        	item.put("forwarded",forwarded);
	 	        	//添加至结果数组
	 	        	results.put(item);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
 	        	if(results.length()>=limit)
 	        		break;
 	        }
 	        if(statuses !=null)
 	        	break;
        }
		JSONArray sortResults = new JSONArray();
		sortResults = timelineSort(results);
		return sortResults;
	}
	
	/**
	 * 根据related, publishedAt排序
	 * @param JArray
	 * @return
	 */
	public static JSONArray timelineSort(JSONArray JArray)
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
			
			//twitter使用publishedAt编码
			for(int i=0;i<list.size();i++)
			{
				String actorId = list.get(i).get("publishedAt").toString();
				//判断videoId重复
				if(i>0 && list.get(i).equals(list.get(i-1)))
					continue;
				for(int j=0;j<JArray.length();j++)
				{
					JSONObject item;
					try
					{
						item = JArray.getJSONObject(j);
						if(item.getString("publishedAt").equals(actorId))
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
	
	public static void main(String args[])
	{
	 //   searchByKeyword("bangkok",10);
		getUserTimelie(143276440,"bangkok");
		System.out.println("haha");
	}
}
