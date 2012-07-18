package com.espn;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class Grouping {
	public enum GroupingType {
		NONE, SPORT, LEAGUE, TEAM
	}
	
	public GroupingType mCategoryType;	// 'Sport', 'League', 'Team', 'Player', or 'None'
	public String mName; // 'Baseball', 'Blue Jays', 'NFL', etc
	public String mAbbreviation;
	
	public Grouping(GroupingType gt, String name, String abbrev, Grouping parent) {
		mCategoryType = gt;
		mName = name;
		mParent = parent;
		mAbbreviation = abbrev;
	}
	
	public Grouping get(String name) {
		for (Grouping group : mContents) {
			if (group.mName.compareToIgnoreCase(name) == 0) {
				return group;
			}
		}
		
		return null;
	}
	
	public boolean add(Grouping grp) {
		for (Grouping group : mContents) {
			if (group.mName == grp.mName) {
				return false;
			}
		}
		mContents.add(grp);
		return true;
	}
	
	public Grouping getSport() {
		if (mCategoryType == GroupingType.NONE) {
			return null;
		}
		else if (mCategoryType == GroupingType.SPORT) {
			return this;
		}
		else {
			return mParent.getSport();
		}			
	}
	
	public Grouping getLeague() {
		if (mCategoryType == GroupingType.NONE) {
			return null;
		}
		else if (mCategoryType == GroupingType.LEAGUE) {
			return this;
		}
		else {
			return mParent.getLeague();
		}			
	}
		
	public Grouping getContentGroup(String name) {
		for (Grouping grp : mContents) {
			if (grp.mName.compareToIgnoreCase(name) == 0 || grp.mAbbreviation.compareToIgnoreCase(name) == 0) {
				return grp;
			}
		}
		
		return null;
	}
	
	public TeamGrouping getTeam() {
		if (mCategoryType == GroupingType.TEAM && this instanceof TeamGrouping) {
			
			return (TeamGrouping)this;
		}
		
		return null;
	}
	
	public String getAbbrev() {
		String ret = mAbbreviation;
		if (mAbbrevDisplayMap.containsKey(mAbbreviation.toLowerCase())) {
			ret = mAbbrevDisplayMap.get(mAbbreviation);
		}
		
		return ret;
	}
	
	private static final Map<String, String> mAbbrevDisplayMap = Collections.unmodifiableMap(new HashMap<String, String>() {{ 
        put("college-football", "NCAA");
        put("mens-college-basketball", "NCAA");
        put("womens-college-basketball", "NCAA (W)");
    }});
	
	public LinkedList<Grouping> mContents = new LinkedList<Grouping>();
	private Grouping mParent;
	public int mImageID = 0;	// optional, represents an image associated with this group (team logo, baseball image, etc)
}
