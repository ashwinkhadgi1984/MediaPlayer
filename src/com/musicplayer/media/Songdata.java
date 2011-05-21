package com.musicplayer.media;

import android.graphics.Bitmap;
import android.net.Uri;

public class Songdata{
	String song_name = "";
	String fullpath = "";
	String album_name = "";
	String artist_name = "";
	int song_id = -1;
	Uri albumArtUri;
	Bitmap albumArtBmp ;
	
	
	@Override
	public String toString() {
		super.toString();
		return song_name;
	}
	
}