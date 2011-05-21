package com.musicplayer.media;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ThemeListView extends ListActivity{
	
	static final String[] THEMES = new String[]{
		"Black",
		"White"};
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	    setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, THEMES));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		ThemeMgr.setCurrentSelectedThemeId(position);
		Intent intent = new Intent(ThemeListView.this, Player.class);
		startActivity(intent);
		finish();
	}
	
}
