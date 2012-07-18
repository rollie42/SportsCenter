package com.espn;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import android.util.Log;

// DataLoader will load data from one or more DataAccess objects, and cache that data.  When
// requested again, we will just re-use the cached version, unless the data has become stale (too old), or the requester
// demands a re-load.
public class DataLoader {
	private LinkedHashMap<String, Object> mCache = new LinkedHashMap<String, Object>(200, 0.75f, true);
	private DataAccess mDataAccess;
	
	DataLoader() {
		mDataAccess = new ESPNDataAccess();
	}
	
	public LinkedList<Player> LoadPlayersFromTeam(TeamGrouping team) {
		try {
			String key = "LoadPlayersFromTeam" + team.getSport().mName + team.getLeague().mName + team.mName;
			LinkedList<Player> ret = (LinkedList<Player>)get(key);
			if (ret == null) {
				ret = mDataAccess.LoadPlayersFromTeam(team);
	
				if (ret == null) {
					Log.e("DataLoader", "Unable to retreive players from team '" + team.mName + "'");
					return null;
				}
				set(key, ret);
			}
					
			return ret;
		}
		catch(Exception e) {
			Log.e("DataLoader", "Exception caught trying to load players");
			e.printStackTrace();
			return null;
		}
	}
	
	public LinkedList<TeamGrouping> LoadTeamsFromLeague(Grouping group) {
		try {
			String key = "LoadTeamsFromLeague" + group.getSport().mName + group.getLeague().mName;
			LinkedList<TeamGrouping> ret = (LinkedList<TeamGrouping>)get(key);
			if (ret == null) {
				ret = mDataAccess.LoadTeamsFromLeague(group);
	
				if (ret == null) {
					Log.e("DataLoader", "Unable to retreive teams from league '" + group.getLeague().mName + "'");
					return null;
				}
				set(key, ret);
			}
					
			return ret;
		}
		catch(Exception e) {
			Log.e("DataLoader", "Exception caught trying to load teams");
			e.printStackTrace();
			return null;
		}
	}
	
	public LinkedList<Grouping> LoadLeaguesFromSport(Grouping group) {
		try {
			String key = "LoadLeaguesFromSport" + group.getSport();
			LinkedList<Grouping> ret = (LinkedList<Grouping>)get(key);
			if (ret == null) {
				ret = mDataAccess.LoadLeaguesFromSport(group);
	
				if (ret == null) {
					Log.e("DataLoader", "Unable to retreive leagues from sport '" + group.getSport() + "'");
					return null;
				}
				set(key, ret);
			}
			return ret;
		}
		catch(Exception e) {
			Log.e("DataLoader", "Exception caught trying to load leagues");
			e.printStackTrace();
			return null;
		}						
	}
	
	public LinkedList<NewsItem> LoadNewsFromGroup(Grouping group) {
		String key = "LoadNewsFromSport" + group.getSport();
		LinkedList<NewsItem> ret = (LinkedList<NewsItem>)get(key);
		Log.d("LNFS", "Key: " + key);
		if (ret == null) {
			ret = mDataAccess.LoadNewsFromGroup(group);

			if (ret == null) {
				Log.e("DataLoader", "Unable to retreive news from sport '" + group.getSport() + "'");
				return null;
			}
			set(key, ret);
		}
		return ret;
	}
	
	private Object get(String key) {
		return mCache.get(key);
	}
	
	private void set(String key, Object value) {
		mCache.put(key, value);
	}
}
