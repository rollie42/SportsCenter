package com.espn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.espn.ScoreCenter.R;

import android.util.Log;

public class ESPNDataAccess implements DataAccess {
	
	private static long sLastAPICall = 0;	// This represents the last time we used the ESPN api (/NOT/ the proxy API)
	private final static int sAPIWWaitTime = 1000;	// The time we wait after an API call to the ESPN api
	
	private String mApiUrlBase = "http://api.espn.com/v1/sports";
	private String mApiKey = "suacvy5bey6s6crfz6j9ttjg";
	private String mURLPrefix = "http://rolnicki.net/restcache.php?url=";
	private HashMap<Grouping, String> mNewsUrls = new HashMap<Grouping, String>(); 
	
	public LinkedList<Player> LoadPlayersFromTeam(TeamGrouping team) {
		// TODO: update this url if we get access to athletes by team api
		String url = mApiUrlBase + "/" + team.getSport().mAbbreviation.toLowerCase() + "/" + team.getLeague().mAbbreviation.toLowerCase() + "/athletes?apikey=" + mApiKey;
		String result = getData(url);
		
		if (result == null) {
			return null;
		}
		
		try {
			Log.v("ESPNDataAccess", "JSON message: " + result);
			JSONObject json = new JSONObject(result);
			JSONArray jsonLeagues = json.getJSONArray("sports").getJSONObject(0).getJSONArray("leagues").getJSONObject(0).getJSONArray("athletes");
			LinkedList<Player> ret = new LinkedList<Player>();
			
			for (int i = 0; i < jsonLeagues.length(); i++) {
				JSONObject obj = jsonLeagues.getJSONObject(i);
				String displayName = "";
				String shortName = "";
				
				if (obj.has("displayName")) {
					displayName = obj.getString("displayName");
				}
				
				if (obj.has("shortName")) {
					shortName = obj.getString("shortName");
				}
				else {
					shortName = displayName;
				}
				
				Player newPlayer = new Player(displayName, shortName, team);
				newPlayer.mHeadshot = R.drawable.fsm;
				newPlayer.mPosition = new String[]{"Shortstop", "Pitcher", "Quarterback", "Fullback", "Center", "Goalie"}[(int) Math.floor(Math.random() * 6)];
				
				ret.add(newPlayer);
				// TODO: add URLs to APIs hash
			}
			return ret;
		}
		catch(Exception e) {
			Log.e("ESPNDataAccess", "Error: " + e.toString());
			e.printStackTrace();
		}
		return null;		
	}
	
	public LinkedList<TeamGrouping> LoadTeamsFromLeague(Grouping group) {		
		// TODO: verify group does not already contain teams, OR handle the case
		if (group.mCategoryType != Grouping.GroupingType.LEAGUE) {
			Log.e("ESPNDataAccess", "Invalid group type: " + group.mCategoryType.toString());
			return null;
		}
		
		String url = mApiUrlBase + "/" + group.getSport().mAbbreviation.toLowerCase() + "/" + group.mAbbreviation.toLowerCase() + "/teams?apikey=" + mApiKey;
		String result = getData(url);
		
		if (result == null) {
			return null;
		}
		
		try {
			Log.v("ESPNDataAccess", "JSON message: " + result);
			JSONObject json = new JSONObject(result);
			JSONArray jsonLeagues = json.getJSONArray("sports").getJSONObject(0).getJSONArray("leagues").getJSONObject(0).getJSONArray("teams");
			LinkedList<TeamGrouping> ret = new LinkedList<TeamGrouping>();
			
			for (int i = 0; i < jsonLeagues.length(); i++) {
				JSONObject obj = jsonLeagues.getJSONObject(i);
				String name = "";
				String abbrev = "";
				
				if (obj.has("name")) {
					name = obj.getString("name");
				}
				
				if (obj.has("abbreviation")) {
					abbrev = obj.getString("abbreviation");
				}
				else {
					abbrev = name;
				}
				
				TeamGrouping newGroup = new TeamGrouping(name, abbrev, group);				
				newGroup.mImageID = R.drawable.fsm;
				if (obj.has("location")) {
					newGroup.mLocation = obj.getString("location");
				}
				
				ret.add(newGroup);
				// TODO: add URLs to APIs hash
			}
			return ret;
		}
		catch(Exception e) {
			Log.e("ESPNDataAccess", "Error: " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	public LinkedList<Grouping> LoadLeaguesFromSport(Grouping group) {		
		// TODO: verify group does not already contain leagues, OR handle the case
		if (group.mCategoryType != Grouping.GroupingType.SPORT) {
			Log.e("ESPNDataAccess", "Invalid group type: " + group.mCategoryType.toString());
			return null;
		}
		
		String url = mApiUrlBase + "/" + group.mAbbreviation.toLowerCase() + "?apikey=" + mApiKey;
		String result = getData(url);
		
		if (result == null) {
			return null;
		}
		
		try {
			Log.v("ESPNDataAccess", "JSON message: " + result);
			JSONObject json = new JSONObject(result);
			JSONArray jsonLeagues = json.getJSONArray("sports").getJSONObject(0).getJSONArray("leagues");
			LinkedList<Grouping> ret = new LinkedList<Grouping>();
			
			for (int i = 0; i < jsonLeagues.length(); i++) {
				JSONObject obj = jsonLeagues.getJSONObject(i);
				String name = "";
				String abbrev = "";
				
				if (obj.has("name")) {
					name = obj.getString("name");
				}
				
				if (obj.has("abbreviation")) {
					abbrev = obj.getString("abbreviation");
				}
				else {
					abbrev = name;
				}
				
				Grouping newGroup = new Grouping(Grouping.GroupingType.LEAGUE, name, abbrev, group);				
				
				ret.add(newGroup);
				// TODO: add URLs to APIs hash
			}
			return ret;
		}
		catch(Exception e) {
			Log.e("ESPNDataAccess", "Error: " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	public LinkedList<NewsItem> LoadNewsFromGroup(Grouping group) {
		Grouping sport = group.getSport();
		Grouping league = group.getLeague();
		
		// TODO: rearchitect. maybe have a static map in ESPNDataAccess to map 'Group' (the object)
		// to 'news url'.  Should be easy enough...		
		String strURL;
		if (mNewsUrls.containsKey(group)) {
			strURL = mNewsUrls.get(group);
		}
		else {
			StringBuilder sb = new StringBuilder(mApiUrlBase);
			if (sport != null) {
				sb.append("/" + sport.mAbbreviation.toLowerCase());
			}
			
			if (league != null) {
				sb.append("/" + league.mAbbreviation.toLowerCase());	// TODO: epsn api wants /sports/football/college-football, not NCAA
			}
			
			// Can't get news for sub-groups (AL, NL, etc), so just get it up to league level
			
			sb.append("/news?apikey=" + mApiKey);
			strURL = sb.toString();
		}
		
		try {
			String result = getData(strURL);
			Log.v("ESPNDataAccess", "JSON message: " + result);
			JSONObject json = new JSONObject(result);
			JSONArray jsonHeadlines = json.getJSONArray("headlines");
			LinkedList<NewsItem> ret = new LinkedList<NewsItem>();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); 
			for (int i = 0; i < jsonHeadlines.length(); i++) {
				JSONObject obj = jsonHeadlines.getJSONObject(i);
				NewsItem newsItem = new NewsItem();
				newsItem.mHeadline = obj.getString("headline");
				newsItem.mID = obj.getString("id");				
				newsItem.mPublishDate = new Date(2012, 12, 25);// TODO:df.parse(obj.getString("published"));
				ret.add(newsItem);
			}
			return ret;
		}
		catch(Exception e) {
			Log.e("ESPNDataAccess", "Error: " + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	private String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            try {
                is.close();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
 
    public String getData(String url) { 
    	url = mURLPrefix + url; 
        HttpClient httpclient = new DefaultHttpClient();
 
        // Prepare a request object
        HttpGet httpget = new HttpGet(url);
        Log.v("getData", "httpget url: " + url);

        try {
	        synchronized(this) {
	        	long curTime = System.currentTimeMillis();
	        	if (sLastAPICall + sAPIWWaitTime > curTime) {
	        		Thread.sleep(sLastAPICall + sAPIWWaitTime - curTime);
	        	}
	        	HttpResponse response = httpclient.execute(httpget);
	
	            Log.i("ESPNDataAccess",response.getStatusLine().toString());
	            HttpEntity entity = response.getEntity();
	 
	            if (entity != null) {
	            	// TODO: return null if http code > 300, make sure proxy is passing along http code
	                InputStream instream = entity.getContent();
	                String result= convertStreamToString(instream);
	                if (result.charAt(0) == '1') {
	                	// Hack to signify we used the actual ESPN API to get this value.
	                	sLastAPICall = System.currentTimeMillis();
	                	result = result.substring(1); // Not so efficient, but simple
	                	Log.v("ESPNDataAccess", "ESPN API used");
	                }

	                instream.close();
	                return result;
	            }
	        }	        
        } 
        catch (Exception e) {
        	Log.e("ESPNDataAcces", "Error: " + e.toString());
        	e.printStackTrace();
        }
        
        return null;
    }
}
