package com.espn;

public class Player {
	Player(String displayName, String shortName, TeamGrouping team) {
		mDisplayName = displayName;
		mShortName = shortName;
		mTeam = team;
	}
	
	public String mDisplayName;
	public String mShortName;
	public String mPosition;
	public TeamGrouping mTeam;
	public int mHeadshot = 0;	// resource handle to headshot
}
