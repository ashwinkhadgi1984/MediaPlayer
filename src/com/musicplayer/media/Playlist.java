package com.musicplayer.media;

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Playlist extends ListActivity {

	private static List<Songdata> songsList = null;
	Bitmap default_albumart = null;
	private final int ALBUM_ART_WIDTH = 72;
	private final int ALBUM_ART_HEIGHT = 72;
	private int ncurrentPlayingSongIndex = -1;
	 
	@Override
	public void onCreate(Bundle icicle) {
		try {
			super.onCreate(icicle);
			
			if(ThemeMgr.getCurrentSelectedThemeId() == 0)
				this.setTheme(R.style.Theme_Black);
			else
				this.setTheme(R.style.Theme_White);
			//create default album art image
			 Bitmap bgBMP = BitmapFactory.decodeResource(getResources(),
	  					R.drawable.albumart_mp_unknown);
			 default_albumart = Bitmap.createScaledBitmap(bgBMP, ALBUM_ART_WIDTH, ALBUM_ART_HEIGHT, true);
	         bgBMP.recycle();
	         	 
			songsList = Songlistdata.getsongList();
			SongListAdapter adp = new SongListAdapter(this);
			setListAdapter(adp);	
			
			Intent intent = getIntent();
			ncurrentPlayingSongIndex = intent.getIntExtra("SongIndex", 0);
			
			
			
		} catch (NullPointerException e) {
			Log.v(getString(R.string.app_name), e.getMessage());
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra("SongIndex", position);
		setResult(RESULT_OK, returnIntent);
		finish();
		
	}

	
	 public class SongListAdapter extends BaseAdapter {
	      
	        private final Context mContext;

	      
	          public SongListAdapter(Context c) {
	            mContext = c;
	          }

	        public int getCount() {
	            return songsList.size();
	        }

	        public Object getItem(int position) {
	            return position;
	        }

	        public long getItemId(int position) {
	            return position;
	        }

	        public View getView(int position, View convertView, ViewGroup parent) {
	        	 View view = null;//convertView;
	             if (view == null) {
	                 view = LayoutInflater.from(mContext).inflate(R.layout.song_item, null);
	             }

	             final TextView name = (TextView)view.findViewById(R.id.song_name);
	                       name.setText(songsList.get(position).song_name);
	           

	             final TextView number = (TextView)view.findViewById(R.id.song_album);
	             number.setText(songsList.get(position).album_name);

	             final ImageView photo = (ImageView)view.findViewById(R.id.song_albumart);
	             Bitmap bmp =  songsList.get(position).albumArtBmp;
	             Bitmap tempBMP = null;
	             if(bmp != null)
	             {
	            	tempBMP = Bitmap.createScaledBitmap(bmp, ALBUM_ART_WIDTH, ALBUM_ART_HEIGHT, true);
	             }
	             else
	             {
	            	 tempBMP = default_albumart;
	            	
	             }
	             
	             photo.setImageBitmap(tempBMP);
	             
	             ImageView play_icon = (ImageView)view.findViewById(R.id.song_playing);
	             if(position == ncurrentPlayingSongIndex){
	            	play_icon.setVisibility(0);
	             }
	                         
	             return view;
	    }
	 }
}
