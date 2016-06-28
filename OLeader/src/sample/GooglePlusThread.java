package sample;

import java.io.IOException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public class GooglePlusThread  implements Runnable{
	
	private JSONObject json;
	private JSONArray ja;
    private static GooglePlus GP = new GooglePlus();
    
	
    public GooglePlusThread(JSONObject json,JSONArray ja)
    {
    	this.json = json;
    	this.ja = ja;
    }

	 /**
	  * 用于解析item，并根据用户的URL获得用户信息
	  */
	public void run()
	{
		JSONObject item = json;
		String actorId ="";
		
		//获取actor的ID
		JSONObject actor;
		try {
			actor = item.getJSONObject("actor");
			actorId = actor.getString("id");
			//如果object中有actor的id信息，使用这个原创的用户
			if(item.has("object"))
			{
				JSONObject object = item.getJSONObject("object");
				if(object.has("actor"))
				{
					actor = object.getJSONObject("actor");
					if(actor.has("id"))
					{	
						actorId = actor.getString("id");
					}
				}
			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//获取用户信息
		JSONObject profile = new JSONObject();
		try {
			profile = GP.peopleInfo(actorId);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//获取当前activity的回复数，支持数和分享数
		try
		{
			if(item.has("replies"))
				profile.put("replies",item.getJSONObject("replies").getString("totalItems"));
			else
				profile.put("replies","0");
			
			if(item.has("plusoners"))
				profile.put("plusoners",item.getJSONObject("plusoners").getString("totalItems"));
			else
				profile.put("plusoners","0");
			
			if(item.has("reshares"))
				profile.put("reshares",item.getJSONObject("reshares").getString("totalItems"));
			else
				profile.put("reshares","0");
			
			if(item.has("location"))
			{
				if(item.getJSONObject("location").has("displayName"))
					profile.put("location",item.getJSONObject("location").get("displayName"));
				else
					profile.put("location","");
			}
			else
				profile.put("location","");
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(actorId+"'s profile"+profile);
		ja.put(profile);
	}
}
