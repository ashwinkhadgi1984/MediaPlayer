package com.musicplayer.media;

public class ThemeMgr{
	
	private static final int BLACK_THEME = 0;
	private static final int WHITE_THEME = 1;
	//private ThemeMgr obj = null;
	private static int nThemeId = BLACK_THEME;
	private ThemeMgr()
	{
		
	}
	
	static int getCurrentSelectedThemeId(){
		return nThemeId;
	}
	
	static void setCurrentSelectedThemeId(int id){
		nThemeId = id;
	}
	
}
