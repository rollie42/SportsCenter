package com.espn;

import java.util.Collection;
import java.util.LinkedList;

public class TeamGrouping extends Grouping {
	TeamGrouping(String name, String abbrev, Grouping parent) {
		super(GroupingType.TEAM, name, abbrev, parent);
	}
	
	public void add(Player player) {
		String key = player.mDisplayName;
		for (Player pl : mPlayers) {
			if (player.mDisplayName == pl.mDisplayName) {
				return;
			}
		}
		
		mPlayers.add(player);
	}	
	
	public Player get(int position) {
		return mPlayers.get(position);
	}
	
	public int size() {
		return mPlayers.size();
	}
	
	public LinkedList<Player> players() {
		return mPlayers;
	}
	
	private LinkedList<Player> mPlayers = new LinkedList<Player>();
	public String mLocation = "";
}
