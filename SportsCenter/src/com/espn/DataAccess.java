package com.espn;

import java.util.LinkedList;

public interface DataAccess {
	public LinkedList<Player> LoadPlayersFromTeam(TeamGrouping team);
	public LinkedList<Grouping> LoadLeaguesFromSport(Grouping group);
	public LinkedList<TeamGrouping> LoadTeamsFromLeague(Grouping group);
	public LinkedList<NewsItem> LoadNewsFromGroup(Grouping group);
}
