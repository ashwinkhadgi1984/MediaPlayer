package com.musicplayer.media;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class MPService extends Service {

	private MediaPlayer mp = new MediaPlayer();
	private List<Songdata> songsList = null;
	private int currentPosition;
	NotificationManager nm = null;
	private static final int NOTIFY_ID = R.layout.songlist;

	public static final int MEDIA_NOT_READY = 1;
	public static final int MEDIA_PLAY = 2;
	public static final int MEDIA_PAUSE = 3;
	public static final int MEDIA_STOP = 4;
	public static final int MEDIA_PREPARE = 5;

	private int media_state = MEDIA_NOT_READY;

	// broadcast events
	public static final String MEDIA_NEXT_SONG = "com.ashwin.media.action.NEXT_SONG";
	public static final String MEDIA_PREVIOUS_SONG = "com.ashwin.media.action.PREVIOUS_SONG";

	// repeat button
	private boolean isRepeatOn = false;

	// shuffle button
	private boolean isShuffleOn = false;

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//android.os.Debug.waitForDebugger();
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Songlistdata.setApplicationcontext(this);
		songsList = Songlistdata.getsongList();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mp.stop();
		mp.release();
		nm.cancel(NOTIFY_ID);
		songsList = null;
		Log.d("ashwin","destroy called");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	private void playSong(String songPath) {
		try {
			// android.os.Debug.waitForDebugger();
			mp.reset();
			mp.setDataSource(songPath);
			mp.prepare();
			mp.start();

			Songdata data = (Songdata) songsList.get(currentPosition);
			setNotification(R.drawable.playbackstart, data.song_name,
					data.fullpath);

			media_state = MEDIA_PLAY;

			mp.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer arg0) {
					nextSong();
				}
			});

		} catch (IOException e) {
			Log.e(getString(R.string.app_name), e.getMessage());
		}
	}

	private void setSong(String songPath) {
		try {

			mp.reset();
			mp.setDataSource(songPath);
			mp.prepare();

			media_state = MEDIA_PREPARE;

			mp.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer arg0) {
					nextSong();
				}
			});

		} catch (IOException e) {
			Log.e(getString(R.string.app_name), e.getMessage());
		}
	}

	private void nextSong() {
		// Check if last song or not
		if (++currentPosition >= songsList.size()) {
			if (isRepeatOn == false)
				currentPosition = songsList.size()-1;
			else
				currentPosition = 0;
			//nm.cancel(NOTIFY_ID);
		} 
		if (media_state != MEDIA_PLAY) {
			try {
				mp.reset();
				mp.setDataSource(songsList.get(currentPosition).fullpath);
				mp.prepare();
			} catch (IOException e) {

				e.printStackTrace();
			}
		} else {
			
			playSong(songsList.get(currentPosition).fullpath);
		}
		Intent intent = new Intent(MEDIA_NEXT_SONG);
		sendBroadcast(intent);
	}

	private void prevSong() {
		if (--currentPosition <= 0) {
			if (isRepeatOn == false)
				currentPosition = 0;
			else
				currentPosition = songsList.size() - 1;
		}
		if (media_state != MEDIA_PLAY) {
			try {
				mp.reset();
				mp.setDataSource(songsList.get(currentPosition).fullpath);
				mp.prepare();
			} catch (IOException e) {

				e.printStackTrace();
			}
		} else {
			playSong(songsList.get(currentPosition).fullpath);
		}
		Intent intent = new Intent(MEDIA_PREVIOUS_SONG);
		sendBroadcast(intent);
	}

	public void setNotification(int icon, String sTitle, String sText) {
		Notification notification = new Notification(icon, sTitle,
				System.currentTimeMillis());

		Context context = getApplicationContext();
		CharSequence contentTitle = sTitle;
		CharSequence contentText = sText;
		Intent notificationIntent = new Intent(this, Player.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,	contentIntent);
		

		nm.notify(NOTIFY_ID, notification);
	}

	// implement the AIDL interface
	private final MPInterface.Stub mBinder = new MPInterface.Stub() {

		@Override
		public void clearPlaylist() throws DeadObjectException {
			songsList.clear();

		}

		@Override
		public void addSongPlaylist(String songPath) throws DeadObjectException {
			// if(songsList == null)
			// songsList = new ArrayList<String>();
			// songsList.add(songPath);

		}

		@Override
		public void addSongListToPlaylist(List<String> list)
				throws DeadObjectException {
			// songsList = list;

		}

		@Override
		public void playFile(int position) throws DeadObjectException {
			currentPosition = position;
			playSong(songsList.get(position).fullpath);
			
		}

		@Override
		public void setFile(int position) throws DeadObjectException {
			currentPosition = position;
			setSong(songsList.get(position).fullpath);
		}

		@Override
		public void pause() throws DeadObjectException {
			mp.pause();
			nm.cancel(NOTIFY_ID);
			media_state = MEDIA_PAUSE;
		}

		@Override
		public void resume() throws DeadObjectException {
			mp.start();
			media_state = MEDIA_PLAY;
			Songdata data = (Songdata) songsList.get(currentPosition);
			setNotification(R.drawable.playbackstart, data.song_name,
					data.fullpath);
		}

		@Override
		public void stop() throws DeadObjectException {
			mp.stop();
			nm.cancel(NOTIFY_ID);
			media_state = MEDIA_STOP;
		}

		@Override
		public void skipForward() throws DeadObjectException {
			// media_state = MEDIA_NOT_READY;
			nextSong();
		}

		@Override
		public void skipBack() throws DeadObjectException {
			// media_state = MEDIA_NOT_READY;
			prevSong();
		}

		@Override
		public String getCurrentSongName() {
			Songdata data = (Songdata) songsList.get(currentPosition);
			return data.song_name;
		}

		@Override
		public int getCurrentSongIndex() {
			return currentPosition;
		}

		@Override
		public int getTotalNoOfSongs() {
			return songsList.size();
		}

		@Override
		public int getCurrentSongPosition() {
			return currentPosition;
		}

		@Override
		public int getCurrentSongDuration() {
			if (media_state == MEDIA_PLAY || media_state == MEDIA_PAUSE
					|| media_state == MEDIA_PREPARE)
				return mp.getCurrentPosition();
			else
				return 0;
		}

		@Override
		public int getCurrentSongTotalDuration() {
			if (media_state == MEDIA_PLAY || media_state == MEDIA_PAUSE
					|| media_state == MEDIA_PREPARE)
				return mp.getDuration();
			else
				return 0;
		}

		@Override
		public String getCurrentSongRemainingDurationinSec()
				throws RemoteException {
			if (media_state == MEDIA_PLAY || media_state == MEDIA_PAUSE
					|| media_state == MEDIA_PREPARE) {
				int nRemainingDuartionSec = (int) (mp.getDuration() - mp

				.getCurrentPosition()) / 1000;
				int nRemainingDuartionMin = nRemainingDuartionSec / 60;
				nRemainingDuartionSec = nRemainingDuartionSec % 60;

				return String.valueOf(nRemainingDuartionMin) + "."
						+ String.valueOf(nRemainingDuartionSec);
			} else
				return "";

		}

		@Override
		public String getCurrentSongDurationinSec() throws RemoteException {
			if (media_state == MEDIA_PLAY || media_state == MEDIA_PAUSE
					|| media_state == MEDIA_PREPARE) {
				int nCurrentPosSec = (int) (mp.getCurrentPosition() / 1000);
				int nCurrentMinutes = nCurrentPosSec / 60;
				nCurrentPosSec = nCurrentPosSec % 60;

				return String.valueOf(nCurrentMinutes) + "."
						+ String.valueOf(nCurrentPosSec);
			} else
				return "";
		}

		@Override
		public int getMPState() {
			return media_state;
		}

		@Override
		public String getAlbumName() {
			return songsList.get(currentPosition).album_name;
		}

		@Override
		public Bitmap getCurrentAlbumArtBMP() {
			return songsList.get(currentPosition).albumArtBmp;
		}

		@Override
		public Bitmap getAlbumArtBMP(int position) {
			if (position <= songsList.size())
				return songsList.get(position).albumArtBmp;
			else
				return null;
		}

		@Override
		public void setRepeat(boolean bValue) {
			isRepeatOn = bValue;
		}

		@Override
		public void setShuffle(boolean bValue) {
			isShuffleOn = bValue;
			if (isShuffleOn == true)
			{
				Collections.shuffle(songsList);
			//	Songlistdata.setShuffledsongList(songsList);
				//need to implement syncup logic
			}
		}

		@Override
		public void seekTo(int msec) {
			mp.seekTo(msec);
		}
		
		@Override
		public int getCurrentsongID() {
			return songsList.get(currentPosition).song_id;
		}

	};
	// end of AIDL interface implementation
}
