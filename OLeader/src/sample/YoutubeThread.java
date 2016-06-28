package sample;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class YoutubeThread implements Runnable{
	
	private JSONObject json;
	private JSONArray ja;
	private static Youtube YTB;
	public YoutubeThread(JSONObject json,JSONArray ja)
	{
		this.json = json;
		this.ja = ja;
	}

	/**
	 * 解析channel的信息
	 * YouTube中，一个用户当做一个channel
	 */
	public void run()
	{
		JSONObject item = json;
		String videoId ="",channelId = "";
		JSONObject profile = new JSONObject();
		
		//获取video的id
		try
		{
			if(item.has("id"))
				if(item.getJSONObject("id").has("videoId"))
					videoId = item.getJSONObject("id").getString("videoId");
			
			//获取snippet
			JSONObject snippet = item.getJSONObject("snippet");
			channelId = (snippet.getString("channelId")!=null?snippet.getString("channelId"):"0");
			
			//获取channel信息
			profile = YTB.channelInfo(channelId);
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ja.put(profile);
		System.out.println(channelId +"'s profle::"+profile);
	}
}
