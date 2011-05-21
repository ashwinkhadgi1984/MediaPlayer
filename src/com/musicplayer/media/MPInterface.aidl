package com.musicplayer.media;

interface MPInterface {
	void clearPlaylist();
	void addSongPlaylist( in String song ); 
	void addSongListToPlaylist( in List<String> songsList ); 
	void playFile( in int position );
	void setFile( in int position );

	void pause();
	void resume();
	void stop();
	void skipForward();
	void skipBack(); 
	
	String getCurrentSongName();
	int getTotalNoOfSongs();
	int getCurrentSongIndex();
	String getCurrentSongRemainingDurationinSec();
	String getCurrentSongDurationinSec();
	int getCurrentSongDuration();
	int getCurrentSongTotalDuration(); 
	int getMPState();
	String getAlbumName();
	Bitmap getCurrentAlbumArtBMP();
	void setRepeat(boolean bValue);
	void setShuffle(boolean bValue);
	Bitmap getAlbumArtBMP(int position);
	int getCurrentSongPosition();
	void seekTo(int msec);
	int getCurrentsongID();
	
} 