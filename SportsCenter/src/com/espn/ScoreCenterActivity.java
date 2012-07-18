package com.espn;

import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.espn.ScoreCenter.R;

public class ScoreCenterActivity extends Activity {		
	// TODO: handle onPause, in case of incoming phone call, etc
	abstract private class LoadDataTask<T1> extends AsyncTask<Void, Void, T1>	{
		protected Object mParam;
		protected ScoreCenterActivity mContext;
		
		LoadDataTask(Object param, ScoreCenterActivity context) {
			mParam = param;
			mContext = context;
		}
	}
	
	abstract private class LoadNewsTask extends LoadDataTask<LinkedList<NewsItem>> {
		public final int mScrollViewID;
		
		LoadNewsTask(Object param, ScoreCenterActivity context, int scrollViewID) {
			super(param, context);
			mScrollViewID = scrollViewID;
		}
				
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			ScrollView sv = (ScrollView)findViewById(mScrollViewID);
			sv.removeAllViews();
			
			sv.addView(getProgressBar(mContext));
		}
		
		protected void onPostExecute(LinkedList<NewsItem> results) {
			if (results == null) {
				return;
			}    		

			ScrollView sv = (ScrollView)findViewById(mScrollViewID);
			sv.removeAllViews();
			
			LinearLayout ll = new LinearLayout(mContext);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			for (NewsItem news : results) {
				TextView tv = new TextView(mContext);
				tv.setLineSpacing(0,.8f);
				StringBuilder text = new StringBuilder("");
				text.append("<p><font color=0x111111><b>" + news.mHeadline + "</b></font><br/>");
				text.append("<font color=0x007b25><small>" + news.mPublishDate.toLocaleString() + "</small></font></p>");
				text.append("<p><font color=0x333333>" + getTwizlerHTML() + "</font></p>"); 
				tv.setText(Html.fromHtml((text.toString())));
				// TODO: onClick handler to get full stories
				ll.addView(tv);
				if (news != results.getLast()) {
					// Horizontal separator between stories
					View seperator = new View(mContext);
					seperator.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
					seperator.setBackgroundColor(0xff333333);
					ll.addView(seperator);
				}    						
			}
			sv.addView(ll);
		}
	}
	
	public String getTwizlerHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("Twizzlers are a type of candy manufactured by the Hershey Foods Corporation.");
		sb.append("Twizzlers are similar to twisted strands of licorice candy, but come in a variety of flavors. The most popular flavors of Twizzlers are:<br/><br/>");

		sb.append(" 1) Strawberry<br/> 2) Cherry<br/> 3) Licorice<br/> 4) Choclate<br/><br/>");
		sb.append("Less popular flavors include red raspberry tropical, watermelon cherry, and watermelon and wild berry.");
		sb.append("Twizzlers are made out of corn syrup, flour, sugar, cornstarch, shortening, molasses, and the ubiquitous \"flavorings.\" The ingredients are cooked, then extruded into ropes, strings, nibs, and bites.<br/><br/>");

		sb.append("Compared to other types of candies, Twizzlers do not have a strong flavor. They are also quite chewy.");

		sb.append("Eating a Twizzler is like consuming a scented candle, but that hardly conveys the strange pleasure of the Twizzlers' smell and the satisfaction of masticating each strand into submission. A package of Twizzlers is more of a small hobby than a dining experience. Their durability and lack of surprise make them perfect for consumption during movies. They are a sort of gustatory pastime—the snack food equivalent of working on one's knitting.<br/>");
		return sb.toString();
	}
	
	private class Data {
		
	}
	
	private class PlayerData extends Data {
		
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        
        try {
        	setContentView(R.layout.main);
        }
        catch (Exception e)
        {
        	Log.e("Main", "Error: " + e.toString());
        	e.printStackTrace();
        }       
        
        // Header
        mSportHeader = (ImageButton)findViewById(R.id.headeritem1);
        mLeagueHeader = findViewById(R.id.headerlayout2);
        mTeamHeader = findViewById(R.id.headerlayout3);
        mPlayerHeader = findViewById(R.id.headerlayout4);

        mLeagueHeaderText = (TextView)findViewById(R.id.headeritem2);
        mTeamHeaderText = (TextView)findViewById(R.id.headeritem3);
        mPlayerHeaderText = (TextView)findViewById(R.id.headeritem4);
        
        // Body
        mHandle1 = (TextView) findViewById(R.id.handle1);
        mHandle2 = (TextView) findViewById(R.id.handle2);
        mHandle3 = (TextView) findViewById(R.id.handle3);
        mHandle4 = (TextView) findViewById(R.id.handle4);
        
        /*mContent1 = (TextView) findViewById(R.id.content1);
        mContent2 = (TextView) findViewById(R.id.content2);
        mContent3 = (TextView) findViewById(R.id.content3);
        mContent4 = (TextView) findViewById(R.id.content4);*/
        mContentContainer3 = (LinearLayout) findViewById(R.id.contentContainer3);
        
        // Load data
        // Baseball we give special treatment because mlb is assumed to be our 'favorite', so loads immediately
        Grouping favoriteSport = new Grouping(Grouping.GroupingType.SPORT, "Baseball", "Baseball", mSportsData);
        favoriteSport.mImageID = R.drawable.baseball;
        mSportsData.add(favoriteSport);
        LinkedList<Grouping> results = mDataLoader.LoadLeaguesFromSport(favoriteSport);
		if (results != null) {
			for (Grouping grp : results) {
				Log.v("ScoreCenterActivity", "Got league " + grp.mName);
				favoriteSport.add(grp);
			}
		}
					
        
        for (String sport : new String[]{"Football", "Basketball", "Soccer"}) {
        	final Grouping sportGroup = new Grouping(Grouping.GroupingType.SPORT, sport, sport, mSportsData);
        	if (!mSportsData.add(sportGroup)) {
        		continue;
        	}
            LoadDataTask<LinkedList<Grouping>> task = new LoadDataTask<LinkedList<Grouping>>(sportGroup, this) {
    			protected LinkedList<Grouping> doInBackground(Void... params) {
    				return mDataLoader.LoadLeaguesFromSport((Grouping)mParam);
    			}
            	
    			protected void onPostExecute(LinkedList<Grouping> results) {
    				if (results == null) {
    					return;
    				}
    				
    				if (sportGroup.mContents.size() == 0) {	// make doubly sure we don't accidentally load extra values    					
	    				for (Grouping grp : results) {
	    					Log.v("ScoreCenterActivity", "Got league " + grp.mName);
	    					sportGroup.add(grp);
	    				}
    				}
    			}
            };
            task.execute();
        }
        
        mSportsData.get("Football").mImageID = R.drawable.football;
        mSportsData.get("Basketball").mImageID = R.drawable.basketball;
        mSportsData.get("Soccer").mImageID = R.drawable.soccerball;
        
        setActiveGroup(favoriteSport.getContentGroup("MLB"));      
        
        //mGLView = new MyGLSurfaceView(this);
    }

    public void setActiveGroup(final Grouping group) {
    	if (group == null) {
    		Log.e("SCA", "setActiveGroup: group is null");
    		return;
    	}
    	
    	// Just so happens that news is tab1 for everything that has news
    	mSelectedGroup = group;
    	LoadNewsTask newsTask = new LoadNewsTask(group, this, R.id.scrollview0) {
			protected LinkedList<NewsItem> doInBackground(Void... params) {
				return mDataLoader.LoadNewsFromGroup((Grouping)mParam);
			}
    	};
    	
    	mTeamHeader.setVisibility(View.GONE);
    	mPlayerHeader.setVisibility(View.GONE);
    	
    	Log.v("SCA", "Activity type = " + group.mCategoryType);
    	mContentContainer3.removeAllViews();
    	
    	switch(group.mCategoryType) {
    	case NONE:// TODO: return ? error?
    		mHandle1.setText(R.string.News);
    		mHandle2.setText(R.string.Standings);
    		newsTask.execute();       
    		break;
    	case SPORT:
    		mHandle1.setText(R.string.News);
    		mHandle2.setText(R.string.Standings);
    		mHandle3.setText(R.string.Teams);
    		mHandle4.setText(R.string.Schedule);
    		mSportHeader.setImageResource(group.mImageID);
    		mLeagueHeaderText.setText(Html.fromHtml("<i><small>&lt;select league&gt;</small></i>"));
    		newsTask.execute();       
    		break;
    	case LEAGUE:
    		mHandle1.setText(R.string.News);
    		mHandle2.setText(R.string.Standings);
    		mHandle3.setText(R.string.Teams);
    		mHandle4.setText(R.string.Schedule);
    		mTeamHeader.setVisibility(View.VISIBLE);
    		mLeagueHeaderText.setText(group.getAbbrev().toUpperCase());
    		mTeamHeaderText.setText(Html.fromHtml("<i><small>&lt;select team&gt;</small></i>"));
    		
    		// Load teams for this league, in background thread
    		Log.v("SCA", "contents size = " + group.mContents.size());    
    		mContentContainer3.removeAllViews();
    		if (group.mContents.size() == 0) {
	    		LoadDataTask<LinkedList<TeamGrouping>> loadTeamsTask = new LoadDataTask<LinkedList<TeamGrouping>>(group, this) {
	    			@Override
	    			protected void onPreExecute() {
	    				super.onPreExecute();
	    				
	    				mContentContainer3.addView(getProgressBar(mContext));
	    			}
	    			
	    			protected LinkedList<TeamGrouping> doInBackground(Void... params) {
	    				return mDataLoader.LoadTeamsFromLeague((Grouping)mParam);
	    			}
	            	
	    			protected void onPostExecute(LinkedList<TeamGrouping> results) {
	    				if (mContext.mSelectedGroup != group) {
	    					return;
	    				}
	    				
	    				mContentContainer3.removeAllViews();
	    				
	    				if (results == null) {
	    					return;
	    				}
	    				
	    				GridView gv = ScoreCenterActivity.getTeamGridView(mContext, results);
	    				EditText et = getEditTextFilter(mContext, (ImgTextAdapter)gv.getAdapter(), "Team name");
	    				
	    				if (group.mContents.size() == 0) {	    					
		    				for (Grouping grp : results) {
		    					Log.v("ScoreCenterActivity", "Got team " + grp.mName);
		    					group.add(grp);
		    				}
	    				}
	    				
	    				mContentContainer3.addView(et);
	    				mContentContainer3.addView(gv);
	    			}
	            };
	            loadTeamsTask.execute();
    		}
    		else {
				GridView gv = ScoreCenterActivity.getTeamGridView(this, group.mContents);
				EditText et = getEditTextFilter(this, (ImgTextAdapter)gv.getAdapter(), "Team name");
				mContentContainer3.addView(et);
				mContentContainer3.addView(gv);
    		}
            
    		newsTask.execute();      				     
    		break;
    	case TEAM:
    		mHandle1.setText(R.string.News);
    		mHandle2.setText(R.string.Schedule);
    		mHandle3.setText(R.string.Roster);
    		mHandle4.setText(R.string.GameRecap);
    		mTeamHeader.setVisibility(View.VISIBLE);
    		mPlayerHeader.setVisibility(View.VISIBLE);
    		mTeamHeaderText.setText(group.mName);
    		mPlayerHeaderText.setText(Html.fromHtml("<i><small>&lt;select player&gt;</small></i>"));
    		
    		// Load players for this team, in background thread if needed
    		mContentContainer3.removeAllViews();
    		if (group.getTeam().size() == 0) {    			
	    		LoadDataTask<LinkedList<Player>> loadPlayersTask = new LoadDataTask<LinkedList<Player>>(group, this) {
	    			@Override
	    			protected void onPreExecute() {
	    				super.onPreExecute();
	    				
	    				mContentContainer3.addView(getProgressBar(mContext));
	    			}
	    			
	    			protected LinkedList<Player> doInBackground(Void... params) {
	    				return mDataLoader.LoadPlayersFromTeam((TeamGrouping)mParam);
	    			}
	            	
	    			protected void onPostExecute(LinkedList<Player> results) {
	    				if (mContext.mSelectedGroup != group) { // We changed groups before doInBackground returned
	    					return;
	    				}
	    				
	    				mContentContainer3.removeAllViews();
	    				
	    				if (results == null) { 
	    					return;
	    				}	    					    				
	    				
	    				if (group.mContents.size() == 0) {	  
		    				for (Player player : results) { 
		    					group.getTeam().add(player);
		    				}
	    				}
	    				
	    				final ListView lv = ScoreCenterActivity.getPlayerListView(mContext, group.getTeam().players());
	        			final EditText et = getEditTextFilter(mContext, (ImgTextAdapter)lv.getAdapter(), "Player, team, position"); 			    			
	        			
	        			mContentContainer3.addView(et);
	        			mContentContainer3.addView(lv);
	    			}
	            };
	            loadPlayersTask.execute();
    		}
    		else {
    			final ListView lv = ScoreCenterActivity.getPlayerListView(this, group.getTeam().players());
    			final EditText et = getEditTextFilter(this, (ImgTextAdapter)lv.getAdapter(), "Player, team, position"); 			    			
    			
    			mContentContainer3.addView(et);
    			mContentContainer3.addView(lv);
    		}
    			
            
    		break;
    	}    	
    }
    
    // Unlike other selectable types, 'player' is not a group, so has to be handled separately
    void setActivePlayer(Player player) {
    	mHandle1.setText(R.string.Stats);
		mHandle2.setText(R.string.GameRecap);
		mHandle3.setText(R.string.Images);
		
		// TODO: invisible
		mHandle4.setText(R.string.Roster);
		
    	mTeamHeader.setVisibility(View.VISIBLE);
    	mPlayerHeader.setVisibility(View.VISIBLE);
    	mTeamHeaderText.setText(player.mTeam.mName);
		mPlayerHeaderText.setText(player.mShortName);
		
		mContentContainer3.removeAllViews();
    }
    
    // Header click handlers
    public void onHeaderSportClick(View v) {
    	if (mActiveDialog == true) {
    		return;
    	}
    	
    	mActiveDialog = true;
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Sports");

    	ListView dList = new ListView(this);    	
    	ImgTextAdapter adapter = new ImgTextAdapter(this);
    	adapter.mColor = Color.rgb(210, 210, 210);
    	adapter.mHeight = 100;
    	for (Grouping group : mSportsData.mContents) {
    		adapter.add(group.mImageID, Html.fromHtml("<b>" + group.mName + " </b>"));
    	}
    	dList.setAdapter(adapter);
    	
    	builder.setView(dList);
    	final Dialog dialog = builder.create();
    	dialog.setOwnerActivity(this);

    	dList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {		
				dialog.dismiss();
				setActiveGroup(mSportsData.mContents.get(position));				
			}
		});
    
    	dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {				
				mActiveDialog = false;
			}
		});

    	dialog.show();
    }
    
    public void onHeaderLeagueClick(View v) {
    	if (mActiveDialog == true) {
    		return;
    	}
    	
    	mActiveDialog = true;
    	
    	final Grouping sportGroup = mSelectedGroup.getSport();
    	if (sportGroup.mContents.size() == 0) {
    		// Hopefully we won't get here - but if we do, in order to not have difficult race conditions, we 
    		// will get the data in the UI thread
    		// TODO: convert to async task
    		Log.v("ScoreCenterActivity", "onHeaderLeagueClick, loading leagues");
			LinkedList<Grouping> results = mDataLoader.LoadLeaguesFromSport(sportGroup); 					
			for (Grouping grp : results) {
				Log.v("ScoreCenterActivity", "Got league " + grp.mName);
				sportGroup.add(grp);
			}
    	}    	
    	
    	ArrayList<String> leagues = new ArrayList<String>();
    	for (Grouping grp : sportGroup.mContents) {
    		leagues.add(grp.mName);    	
    	}    	    	

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Leagues");
    	ListView dList = new ListView(this);
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, leagues);
    	dList.setAdapter(adapter);
    	
    	builder.setView(dList);
    	final Dialog dialog = builder.create();
    	
    	dList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {				
				dialog.dismiss();
				setActiveGroup(sportGroup.mContents.get(position));				
			}
		});

    	dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {				
				mActiveDialog = false;
			}
		});

    	dialog.show();
    }    
    
    public void onHeaderTeamClick(View v) {
    	if (mActiveDialog == true) {
    		return;
    	}
    	
    	mActiveDialog = true;
    	
    	final Grouping leagueGroup = mSelectedGroup.getLeague();
    	final ProgressDialog pd; 
    	if (leagueGroup.mContents.size() == 0) {
    		pd = new ProgressDialog(this);
    		pd.setTitle("Loading teams...");
    	}
    	else {
    		pd = null;
    	}
    	
    	LoadDataTask<LinkedList<TeamGrouping>> task = new LoadDataTask<LinkedList<TeamGrouping>>(null, this) {
    		@Override
    		protected void onPreExecute() {
    			super.onPreExecute();
    			if (pd != null) {
    				pd.show();
    			}
    		}    		
    			
			@Override
			protected LinkedList<TeamGrouping> doInBackground(Void... params) {
				if (leagueGroup.mContents.size() == 0) {
		    		// Hopefully we won't get here - but if we do, in order to not have difficult race conditions, we 
		    		// will get the data in the UI thread
		    		Log.v("ScoreCenterActivity", "onHeaderTeamClick, loading teams");
					return mDataLoader.LoadTeamsFromLeague(leagueGroup);			
		    	}
				
				return null;
			}
			
    		protected void onPostExecute(LinkedList<TeamGrouping> results) {
    			if (leagueGroup.mContents.size() == 0 && results != null) {
					for (TeamGrouping grp : results) {
						Log.v("ScoreCenterActivity", "Got team " + grp.mName);
						leagueGroup.add(grp);
					}										
    			}
    			
    			if (leagueGroup.mContents.size() == 0) {
    				// Still no teams for league - abort!
    				mActiveDialog = false;
    				if (pd != null) {
        				pd.dismiss();
        			}
    				
    				return;
				}
    			
    			ArrayList<String> teams = new ArrayList<String>();
    	    	for (Grouping grp : leagueGroup.mContents) {
    	    		teams.add(((TeamGrouping)grp).mLocation + " " + grp.mName);    	
    	    	}    	    	

    	    	// TODO: image/text view with team logos, if possible
    	    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	    	builder.setTitle("Teams");
    	    	ListView dList = new ListView(mContext);
    	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, teams);
    	    	dList.setAdapter(adapter);
    	    	
    	    	builder.setView(dList);
    	    	final Dialog dialog = builder.create();
    	    	
    	    	dList.setOnItemClickListener(new OnItemClickListener() {
    				@Override
    				public void onItemClick(AdapterView<?> parent, View view, int position,
    						long id) {				
    					dialog.dismiss();
    					setActiveGroup(leagueGroup.mContents.get(position));				
    				}
    			});
    	    	
    	    	dialog.setOnDismissListener(new OnDismissListener() {
    				
    				@Override
    				public void onDismiss(DialogInterface dialog) {				
    					mActiveDialog = false;
    				}
    			});

    			if (pd != null) {
    				pd.dismiss();
    			}
    			
    	    	dialog.show();
    		}
    	};
    	
    	task.execute();
    }
    
    public void onHeaderPlayerClick(View v) {
    	if (mActiveDialog == true) {
    		return;
    	}
    	
    	mActiveDialog = true;
    	final TeamGrouping teamGroup = mSelectedGroup.getTeam();
    	if (teamGroup == null) {
    		Log.e("ScoreCenterActivity", "Couldn't get team");
    	}
    	
    	final ProgressDialog pd; 
    	if (teamGroup.size() == 0) {
    		pd = new ProgressDialog(this);
    		pd.setTitle("Loading players...");
    	}
    	else {
    		pd = null;
    	}
    	
    	LoadDataTask<LinkedList<Player>> task = new LoadDataTask<LinkedList<Player>>(null, this) {
    		@Override
    		protected void onPreExecute() {
    			super.onPreExecute();
    			if (pd != null) {
    				pd.show();
    			}
    		}
    		
    		@Override
			protected LinkedList<Player> doInBackground(Void... params) {
				if (teamGroup.size() == 0) {
		    		// Hopefully we won't get here - but if we do, in order to not have difficult race conditions, we 
		    		// will get the data in the UI thread
		    		Log.v("ScoreCenterActivity", "onHeaderPlayerClick, loading players");
					return mDataLoader.LoadPlayersFromTeam(teamGroup);										
		    	}
				
				return null;
			}
    		
    		@Override
    		protected void onPostExecute(LinkedList<Player> results) {
    			if (teamGroup.size() == 0 && results != null) {
	    			for (Player grp : results) {
						teamGroup.add(grp);
					}
    			}
    			
    			if (teamGroup.size() == 0) {
    				// Still no players for team - abort!
    				mActiveDialog = false;
    				if (pd != null) {
        				pd.dismiss();
        			}
    				
    				return;
    			}
    			
    			ArrayList<String> players = new ArrayList<String>();
    	    	for (Player player : teamGroup.players()) {
    	    		players.add(player.mDisplayName);    	
    	    	}    	    	

    	    	// TODO: add headshot, if possible
    	    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	    	builder.setTitle("Players");
    	    	ListView dList = new ListView(mContext);
    	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, players);
    	    	dList.setAdapter(adapter);
    	    	
    	    	builder.setView(dList);
    	    	final Dialog dialog = builder.create();
    	    	
    	    	dList.setOnItemClickListener(new OnItemClickListener() {
    				@Override
    				public void onItemClick(AdapterView<?> parent, View view, int position,
    						long id) {				
    					dialog.dismiss();
    					setActivePlayer(teamGroup.get(position));				
    				}			
    			});
    	    	
    	    	dialog.setOnDismissListener(new OnDismissListener() {
    				
    				@Override
    				public void onDismiss(DialogInterface dialog) {				
    					mActiveDialog = false;
    				}
    			});
    	    	
    	    	if (pd != null) {
    				pd.dismiss();
    			}
    	    	
    	    	dialog.show();
    		}
    	};
    	
    	task.execute();    	    	
    }
    
    public static EditText getEditTextFilter(final ScoreCenterActivity context, final ImgTextAdapter adapter, final String hint) {
    	final EditText et = new EditText(context);
		et.setHint(hint);
		et.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
		et.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		et.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {						
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {						
			}

			@Override
			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
				adapter.getFilter().filter(s);
			}    				
		});
		
		return et;
    }
    
    public static ListView getPlayerListView(final ScoreCenterActivity context, final LinkedList<Player> players) {
    	ListView lv = new ListView(context);
    	lv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		lv.setCacheColorHint(Color.TRANSPARENT);
		ImgTextAdapter adapter = new ImgTextAdapter(context);
		adapter.mHeight = 100;
		
		for (Player player : players) {
			adapter.add(player.mHeadshot, Html.fromHtml("<b>" + player.mDisplayName + "</b><br/><i>" + player.mTeam.mLocation + " " + player.mTeam.mName + ", " + player.mPosition + "</i>"));
		}
		
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {		
				context.setActivePlayer(players.get(position));				
			}
		});

		return lv;
    }
    
    public static <T> GridView getTeamGridView(final ScoreCenterActivity context, final LinkedList<T> groups) {
    	GridView gv = new GridView(context);
		gv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		gv.setNumColumns(3);
		gv.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
		gv.setGravity(Gravity.TOP);
		final int width = (int)(context.getWindowManager().getDefaultDisplay().getWidth()/3.2);
		gv.setColumnWidth(width);
		gv.setVerticalSpacing(5);
		
		
		ImgTextAdapter adapter = new ImgTextAdapter(context);
		adapter.mWidth = width;
		//adapter.mHeight = GridView.LayoutParams.WRAP_CONTENT;
		adapter.mHeight = (int) (width * 1.4);	// what a hack this is...
		adapter.mVertical = true;

		
		for (T grp : groups) {
			adapter.add(((Grouping)grp).mImageID, ((TeamGrouping)grp).mLocation + " " + ((TeamGrouping)grp).mName);
		}
		
		gv.setAdapter(adapter);
		gv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {		
				context.setActiveGroup((Grouping)groups.get(position));				
			}
		});
		return gv;
    }
    
    public static LinearLayout getProgressBar(ScoreCenterActivity context) {
    	ProgressBar pb = new ProgressBar(context, null, android.R.attr.progressBarStyleSmallInverse);
		pb.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		LinearLayout ll = new LinearLayout(context);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		ll.setGravity(Gravity.CENTER);
		ll.addView(pb);
		return ll;
    }
    
    private Grouping mSportsData = new Grouping(Grouping.GroupingType.NONE, "", "", null);
    private Grouping mSelectedGroup = null;
    private DataLoader mDataLoader = new DataLoader();
    
    // Header controls
    private ImageButton mSportHeader;
    private View mLeagueHeader;
    private View mTeamHeader;
    private View mPlayerHeader;
    private TextView mLeagueHeaderText;
    private TextView mTeamHeaderText;
    private TextView mPlayerHeaderText;
    
    // Body
    private TextView mHandle1;
    private TextView mHandle2;
    private TextView mHandle3;
    private TextView mHandle4;
    private TextView mContentContainer1;
    private TextView mContentContainer2;
    private LinearLayout mContentContainer3;
    private TextView mContentContainer4;
    
    private boolean mActiveDialog = false;
    
	//private GLSurfaceView mGLView;
}