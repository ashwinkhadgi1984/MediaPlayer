package com.musicplayer.media;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.musicplayer.media.HorizontalSlider.OnProgressChangeListener;

/**
 * @author ashwin_khadgi
 *
 */
public class Player extends Activity{

	private MPInterface mpInterface;

	private final String MY_PREF = "mypref";
	private final String SONG_INDEX = "song_index";
	private final String SONG_DURATION = "song_duration";
	private final String FILE_NAME = "tempfile.bin";

	private final int SELECTED_SONG = 1;

	// SeekBar seekbar = null;
	TextView currentPositionTV = null;
	TextView remainingDurationTV = null;
	private Handler mHandler = new Handler();
	private HorizontalSlider mProgressBar = null;
	TextView songCountTV = null;
	TextView songNameTV = null;
	TextView albumNameTV = null;
	ImageView imgView = null;
	ImageView play_pause_button = null;
	ToggleButton repeatButton = null;
	ToggleButton shuffleButton = null;
	CoverFlow coverflow = null;
	RatingBar song_rating = null;
	private boolean isCoverFlowFocoused = true;
	private MyBroadcastReceiver receiver = null;
	private Intent serviceIntent = null;
	private int nLastMediaState = -1;
	private SharedPreferences myPrefs = null;
	
	View controlBar = null;
	View infoBar = null;
	View ratingBar = null;
	
	HashMap<String, String> rating_hm = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(ThemeMgr.getCurrentSelectedThemeId() == 0)
			this.setTheme(R.style.Theme_Black);
		else
			this.setTheme(R.style.Theme_White);
		setContentView(R.layout.home_screen);
		Songlistdata.setApplicationcontext(getApplicationContext());
		
		getViewsFromXML();
		setListners();
		myPrefs = this.getSharedPreferences(MY_PREF, MODE_WORLD_READABLE);
		
		rating_hm = deSerializeMap();
		if(rating_hm == null)
			rating_hm = new HashMap<String, String>();
	}

	@Override
	protected void onStart() {
		super.onStart();
		// start the service and bind to it
		serviceIntent = new Intent(Player.this, MPService.class);
		startService(serviceIntent);
		this.bindService(serviceIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();

		IntentFilter nextSong_filter = new IntentFilter(
				MPService.MEDIA_NEXT_SONG);
		IntentFilter previousSong_filter = new IntentFilter(
				MPService.MEDIA_PREVIOUS_SONG);
		receiver = new MyBroadcastReceiver();
		registerReceiver(receiver, nextSong_filter);
		registerReceiver(receiver, previousSong_filter);

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (receiver != null)
			unregisterReceiver(receiver);
		serializeMap(rating_hm);
	}

	@Override
	protected void onStop() {
		super.onStop();
		try {
			nLastMediaState = mpInterface.getMPState();

			// set the preference
			SharedPreferences.Editor prefsEditor = myPrefs.edit();
			prefsEditor.putInt(SONG_INDEX, getCurrentsongIndex());
			prefsEditor.putInt(SONG_DURATION,
					mpInterface.getCurrentSongDuration());
			prefsEditor.commit();
			
		} catch (RemoteException e) {

		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mHandler.removeCallbacks(updateProgress);
		if (nLastMediaState != MPService.MEDIA_PLAY)
			stopService(serviceIntent);

		// android.os.Process.killProcess(android.os.Process.myPid());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECTED_SONG:
			if (resultCode == RESULT_OK) {
				int songIndex = data.getIntExtra("SongIndex", 0);
				coverflow.setSelection(songIndex, true);
				setGUI();

				break;
			}
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mpInterface = MPInterface.Stub.asInterface((IBinder) service);

			try {
				if (mpInterface.getMPState() == MPService.MEDIA_PLAY
						|| mpInterface.getMPState() == MPService.MEDIA_PAUSE
						|| mpInterface.getMPState() == MPService.MEDIA_PREPARE) {
					setGUI();
					setImageAdapter();
					isCoverFlowFocoused = false;
					coverflow.setSelection(
							mpInterface.getCurrentSongPosition(), true);
					try {
						if (mpInterface.getMPState() == MPService.MEDIA_PLAY) {
							play_pause_button
									.setImageResource(R.drawable.play_normal);

						} else if (mpInterface.getMPState() == MPService.MEDIA_PAUSE
								|| mpInterface.getMPState() == MPService.MEDIA_PREPARE) {
							play_pause_button
									.setImageResource(R.drawable.pause_normal);
						}

					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else {
					// play the first song by-default
					setImageAdapter();

					int song_index = myPrefs.getInt(SONG_INDEX, 0);
					int song_duration = myPrefs.getInt(SONG_DURATION, 0);
					mpInterface.setFile(song_index);
					mpInterface.seekTo(song_duration);
					setGUI();
					isCoverFlowFocoused = false;
					coverflow.setSelection(
							mpInterface.getCurrentSongPosition(), true);
				}
			} catch (RemoteException e) {

				e.printStackTrace();
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// stopService(new Intent(Player.this, MPService.class));
			mpInterface = null;
		}

	};

	private void playSong(int position) {
		try {
			mpInterface.playFile(position);
			setGUI();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void nextSong() {
		try {
			mpInterface.skipForward();
			isCoverFlowFocoused = false;
			coverflow.setSelection(mpInterface.getCurrentSongPosition(), true);
			setGUI();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void previousSong() {
		try {
			mpInterface.skipBack();
			isCoverFlowFocoused = false;
			coverflow.setSelection(mpInterface.getCurrentSongPosition(), true);
			setGUI();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private Runnable updateProgress = new Runnable() {
		public void run() {
			updateGUI();
			mHandler.postDelayed(this, 1000);
		}
	};

	private void updateGUI() {

		try {
			currentPositionTV
					.setText(mpInterface.getCurrentSongDurationinSec());
			remainingDurationTV.setText(mpInterface
					.getCurrentSongRemainingDurationinSec());
			mProgressBar.setProgress(mpInterface.getCurrentSongDuration());
			// seekbar.setProgress(mpInterface.getCurrentSongDuration());
		} catch (RemoteException e) {

			e.printStackTrace();
		}

	}

	private void setGUI() {

		try {
			mProgressBar.setProgress(0);
			mProgressBar.setMax(mpInterface.getCurrentSongTotalDuration());
			// seekbar.setProgress(0);
			// seekbar.setMax(mpInterface.getCurrentSongTotalDuration());
			songCountTV.setText((getCurrentsongIndex() + 1) + "/"
					+ mpInterface.getTotalNoOfSongs());

			songNameTV.setText(mpInterface.getCurrentSongName());
			albumNameTV.setText(mpInterface.getAlbumName());
			
			String str = rating_hm.get(""+getCurrentsongID());
			float ft = 0;
			if( str != null)
				ft = Float.valueOf(str);
			if(ft > 0)
				song_rating.setRating(ft);
			else
				song_rating.setRating(0);

			/*
			 * old album art code Drawable d = new
			 * BitmapDrawable(mpInterface.getAlbumArtBMP()); LayerDrawable
			 * myDrawable = (LayerDrawable)
			 * getResources().getDrawable(R.drawable.image_layer);
			 * myDrawable.setDrawableByLayerId(R.id.layer2, d);
			 * imgView.setImageDrawable(myDrawable);
			 */
			mHandler.removeCallbacks(updateProgress);
			mHandler.postDelayed(updateProgress, 100);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	private void setImageAdapter() {
		coverflow.setAdapter(new ImageAdapter(this));
		coverflow.setSpacing(-25);
		// coverflow.setSelection(0, true);
		coverflow.setAnimationDuration(1000);

	}

	private void getViewsFromXML() {
		currentPositionTV = (TextView) findViewById(R.id.PlayedTime);
		remainingDurationTV = (TextView) findViewById(R.id.RemainingTime);
		mProgressBar = (HorizontalSlider) findViewById(R.id.progress);
		// seekbar = (SeekBar) findViewById(R.id.seekBar);
		songCountTV = (TextView) findViewById(R.id.SongCount);
		songNameTV = (TextView) findViewById(R.id.SongName);
		albumNameTV = (TextView) findViewById(R.id.ArtistName);

		// imgView = (ImageView) findViewById(R.id.imageView1);

		coverflow = (CoverFlow) findViewById(R.id.coverflow);
		coverflow.setPressed(true);

		repeatButton = (ToggleButton) findViewById(R.id.repeatButton);
		shuffleButton = (ToggleButton) findViewById(R.id.shuffleButton);
		
		song_rating = (RatingBar)findViewById(R.id.song_rating);
	
		controlBar = findViewById(R.id.ControlBar);
		infoBar = findViewById(R.id.InfoBar);
		ratingBar = findViewById(R.id.ratingBar);
		

		// set progress bar color programatically
		/*
		 * final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5
		 * }; ShapeDrawable pgDrawable = new ShapeDrawable(new
		 * RoundRectShape(roundedCorners, null,null)); String MyColor =
		 * "#FF00FF"; pgDrawable.getPaint().setColor(Color.parseColor(MyColor));
		 * ClipDrawable progress = new ClipDrawable(pgDrawable, Gravity.LEFT,
		 * ClipDrawable.HORIZONTAL); mProgressBar.setProgressDrawable(progress);
		 * mProgressBar
		 * .setBackgroundDrawable(getResources().getDrawable(android.
		 * R.drawable.progress_horizontal));
		 */
	}
	
	

	private void setListners() {
		// set the playlist button click listener
		ImageView playListButton = (ImageView) findViewById(R.id.playlist_button);
		playListButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(Player.this, Playlist.class);
				intent.putExtra("SongIndex", getCurrentsongIndex());
				startActivityForResult(intent, SELECTED_SONG);

			}
		});

		// set the play button click listener
		play_pause_button = (ImageView) findViewById(R.id.play_button);
		play_pause_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (mpInterface.getMPState() == MPService.MEDIA_PLAY) {
						mpInterface.pause();
						play_pause_button
								.setImageResource(R.drawable.play_normal);

					} else if (mpInterface.getMPState() == MPService.MEDIA_PAUSE
							|| mpInterface.getMPState() == MPService.MEDIA_PREPARE) {
						mpInterface.resume();
						play_pause_button
								.setImageResource(R.drawable.pause_normal);
					}

				} catch (RemoteException e) {
					e.printStackTrace();
				}

			}
		});

		// set the browse button click listener
		ImageView browseButton = (ImageView) findViewById(R.id.browse_button);
		browseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		// set the reverse button click listener
		ImageView reverseButton = (ImageView) findViewById(R.id.reverse_button);
		reverseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				previousSong();
			}
		});

		// set the forward button click listener
		ImageView forwardButton = (ImageView) findViewById(R.id.forward_button);
		forwardButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				nextSong();
			}
		});

		// set repeat button listner
		repeatButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (repeatButton.isChecked())
						mpInterface.setRepeat(true);
					else
						mpInterface.setRepeat(false);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		// set shuffle button listner
		shuffleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (shuffleButton.isChecked())
						mpInterface.setShuffle(true);
					else
						mpInterface.setShuffle(false);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		// set gallery item change listener
		coverflow.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parentView,
					View childView, int position, long id) {

				try {
					if (mpInterface.getMPState() == MPService.MEDIA_PLAY) {

						if (isCoverFlowFocoused == true) {
							mpInterface.playFile(position);
							setGUI();
						}

					} else if (mpInterface.getMPState() == MPService.MEDIA_PAUSE
							|| mpInterface.getMPState() == MPService.MEDIA_PREPARE) {
						if (isCoverFlowFocoused == true) {
							mpInterface.setFile(position);
							setGUI();
						}

					}
					isCoverFlowFocoused = true;
				} catch (RemoteException e) {

				}

			}

			public void onNothingSelected(AdapterView<?> arg0) {

				// Do nothing

			}

		});
		
	/*	song_rating.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
		            // TODO perform your action here
		        }
		        return true;

			}
			
		});
	*/	
		song_rating.setOnRatingBarChangeListener(new OnRatingBarChangeListener(){

			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				rating_hm.put(""+getCurrentsongID(), Float.toString(rating));
			}
			
		});
		
	/*	ratingBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		*/
	

		
		
		coverflow.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if(infoBar.getVisibility() == View.VISIBLE)
				{
					infoBar.setVisibility(View.GONE);
					controlBar.setVisibility(View.GONE);
					ratingBar.setVisibility(View.VISIBLE);
				}
				else
				{
					infoBar.setVisibility(View.VISIBLE);
					controlBar.setVisibility(View.VISIBLE);
					ratingBar.setVisibility(View.GONE);
				}
				
			}
			
			
		});
		// set the progress bar listener
		mProgressBar.setOnProgressChangeListener(changeListener);

		/*
		 * seekbar.setOnSeekBarChangeListener(new
		 * SeekBar.OnSeekBarChangeListener() {
		 * 
		 * @Override public void onStopTrackingTouch(SeekBar arg0) {
		 * 
		 * }
		 * 
		 * @Override public void onStartTrackingTouch(SeekBar arg0) {
		 * 
		 * }
		 * 
		 * @Override public void onProgressChanged(SeekBar arg0, int arg1,
		 * boolean arg2) { try { if(arg2 == true) mpInterface.seekTo(arg1);
		 * //setProgress(arg1); } catch (RemoteException e) {
		 * 
		 * } } });
		 */

	}

	// start of broadcast receivers
	/**
	 * @author ashwin_khadgi
	 *
	 */
	public class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			try {
				isCoverFlowFocoused = false;
				coverflow.setSelection(mpInterface.getCurrentSongPosition(),
						true);
				setGUI();
			} catch (RemoteException e) {

			}

		}
	}// end of broadcast receivers

	// Start of Image Adapter Class for albumart CoverFlow
	public class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			try {
				return mpInterface.getTotalNoOfSongs();
			} catch (RemoteException e) {
				return 0;
			}

		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		/* (non-Javadoc)
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent) {

			ImageView view = (ImageView) convertView;

			if (view == null)
				view = new ImageView(mContext);

			Bitmap bmp = null;
			Bitmap bgBMP = null;
			try {
				bmp = mpInterface.getAlbumArtBMP(position);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			// if(position == coverflow.getSelectedItemPosition()){
			view.setBackgroundResource(R.drawable.album_art_bounder);
			bgBMP = BitmapFactory.decodeResource(getResources(),
					R.drawable.album_art_bounder);

			if (bmp != null) {
				view.setImageBitmap(bmp);
				view.setPadding(15, 5, 5, 5);
			} else {
				view.setImageResource(R.drawable.albumart_mp_unknown);
				view.setPadding(15, 5, 5, 5);
			}
			// }
			/*
			 * else{
			 * view.setBackgroundResource(R.drawable.albumart_background_small);
			 * bgBMP = BitmapFactory.decodeResource(getResources(),
			 * R.drawable.albumart_background_small);
			 * 
			 * if (bmp != null) { view.setImageBitmap(bmp); view.setPadding(16,
			 * 10, 4, 10); } }
			 */

			view.setLayoutParams(new CoverFlow.LayoutParams(bgBMP.getWidth(),
					bgBMP.getHeight()));
			view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			bgBMP.recycle();

			// Make sure we set anti-aliasing otherwise we get jaggies
			BitmapDrawable drawable = (BitmapDrawable) view.getDrawable();
			if (drawable != null)
				drawable.setAntiAlias(true);

			return view;

		}

		/**
		 * Returns the size (0.0f to 1.0f) of the views depending on the
		 * 'offset' to the center.
		 */
		public float getScale(boolean focused, int offset) {
			/* Formula: 1 / (2 ^ offset) */
			return Math.max(0, 1.0f / (float) Math.pow(2, Math.abs(offset)));
		}

	} // End of Image Adapter Class

	private OnProgressChangeListener changeListener = new OnProgressChangeListener() {
		public void onProgressChanged(View v, int progress) {

			try {
				mpInterface.seekTo(progress);
				// setProgress(progress);
			} catch (RemoteException e) {

			}
		}
	};

	int getCurrentsongIndex() {
		try {
			return mpInterface.getCurrentSongIndex();
		} catch (RemoteException e) {

		}
		return 0;
	}
	
	int getCurrentsongID() {
		try {
			return mpInterface.getCurrentsongID();
		} catch (RemoteException e) {

		}
		return -1;
	}
	
	public void serializeMap(HashMap<String, String> hm) {
		try {
			if( hm == null)
				return;
			FileOutputStream fStream = openFileOutput(FILE_NAME, MODE_PRIVATE);
			ObjectOutputStream oStream = new ObjectOutputStream(fStream);
			oStream.writeObject(hm);

			oStream.flush();
			oStream.close();

			Log.v("Serialization success", "Success");
		} catch (Exception e) {

			Log.v("IO Exception", e.getMessage());
		}
	}

	public HashMap<String, String> deSerializeMap() {
		HashMap<String, String> hm = null;
		try {
				FileInputStream in = openFileInput(FILE_NAME);
				ObjectInputStream oStream = new ObjectInputStream(in);
				hm = (HashMap<String, String>) oStream.readObject();
				oStream.close();

		} catch (Exception e) {

			Log.v("IO Exception", e.getMessage());
		}
		return hm;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu1, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.playlist:
	        
	        return true;
	    case R.id.theme:
	    	Intent intent = new Intent(Player.this, ThemeListView.class);
			startActivity(intent);
			finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

}

// End of Player Activity
