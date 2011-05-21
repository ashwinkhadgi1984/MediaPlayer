package com.musicplayer.media;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

public class Songlistdata {
	private static Songlistdata mSonglistdata;
	private static List<Songdata> songsList = null;
	// public static final String MEDIA_PATH = new String("/sdcard/");
	private static Context context = null;

	private Songlistdata() {
		songsList = new ArrayList<Songdata>();
		getAllSongsFromSDCARD();
		
	}

	public static void setApplicationcontext(Context ctx) {
		context = ctx;
	}

	public static List<Songdata> getsongList() {
		if (mSonglistdata == null)
			mSonglistdata = new Songlistdata();
		return songsList;
	}

	public static List<Songdata> getShuffledsongList() {
		if (mSonglistdata == null)
			mSonglistdata = new Songlistdata();
		Collections.shuffle(songsList);
		return songsList;
	}

	public static void setShuffledsongList(List<Songdata> sl) {
		if (mSonglistdata == null)
			mSonglistdata = new Songlistdata();
		songsList = sl;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		songsList = null;
	}

	private void updateSongList() {
		/*
		 * File home = new File(MEDIA_PATH); songsList.clear(); if
		 * (home.listFiles(new Mp3Filter()).length > 0) { for (File file :
		 * home.listFiles(new Mp3Filter())) { songsList.add(file.getName()); } }
		 */
	}

	public void getAllSongsFromSDCARD() {
		String[] STAR = { "*" };
		Uri allsongsuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

		Cursor cursor = context.getContentResolver().query(allsongsuri, STAR,
				selection, null, null);

		// require to calculate the width and height
		Bitmap bgBMP = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.album_art_bounder);

		songsList.clear();

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					Songdata data = new Songdata();
					data.song_name = cursor.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.TITLE));

					data.fullpath = cursor.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.DATA));

					data.album_name = cursor.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.ALBUM));

					data.artist_name = cursor.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.ARTIST));
					
					data.song_id = cursor.getInt(cursor
							.getColumnIndex(MediaStore.Audio.Media._ID));

					int album_id = cursor.getInt(cursor
							.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
					
					

					Uri sArtworkUri = Uri
							.parse("content://media/external/audio/albumart");
					Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
					ContentResolver res = context.getContentResolver();
					InputStream in = null;
					try {
						in = res.openInputStream(uri);
						data.albumArtBmp = getArtworkQuick(context, uri,
								bgBMP.getWidth() - 20, bgBMP.getHeight() - 10);

					} catch (FileNotFoundException e) {
						// Bitmap bitmapResult =
						// BitmapFactory.decodeResource(context.getResources(),
						// R.drawable.albumart_background);
						// data.albumArtBmp = bitmapResult;
						data.albumArtBmp = null;
					}
					songsList.add(data);

				} while (cursor.moveToNext());

			}
			cursor.close();
		}
		bgBMP.recycle();
	}

	// Get album art for specified album. This method will not try to
	// fall back to getting artwork directly from the file, nor will
	// it attempt to repair the database.
	private Bitmap getArtworkQuick(Context context, Uri uri, int w, int h) {

		final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
		sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
		sBitmapOptionsCache.inDither = false;

		w -= 2;
		h -= 2;
		ContentResolver res = context.getContentResolver();
		Bitmap b = null;
		if (uri != null) {
			ParcelFileDescriptor fd = null;
			try {
				fd = res.openFileDescriptor(uri, "r");
				int sampleSize = 1;

				// Compute the closest power-of-two scale factor
				// and pass that to sBitmapOptionsCache.inSampleSize, which will
				// result in faster decoding and better quality
				sBitmapOptionsCache.inJustDecodeBounds = true;
				BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(),
						null, sBitmapOptionsCache);
				int nextWidth = sBitmapOptionsCache.outWidth >> 1;
				int nextHeight = sBitmapOptionsCache.outHeight >> 1;
				while (nextWidth > w && nextHeight > h) {
					sampleSize <<= 1;
					nextWidth >>= 1;
					nextHeight >>= 1;
				}

				sBitmapOptionsCache.inSampleSize = sampleSize;
				sBitmapOptionsCache.inJustDecodeBounds = false;
				b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(),
						null, sBitmapOptionsCache);

				// Bitmap bitmapResult =
				// BitmapFactory.decodeResource(context.getResources(),
				// R.drawable.albumart_background);
				// Bitmap bg = Bitmap.createBitmap(bitmapResult.getWidth(),
				// bitmapResult.getHeight(), bitmapResult.getConfig());

				if (b != null) {

					// finally rescale to exactly the size we need
					if (sBitmapOptionsCache.outWidth != w
							|| sBitmapOptionsCache.outHeight != h) {
						Bitmap tmp = Bitmap.createScaledBitmap(b, w, h, true);

						// Canvas canvas = new Canvas();

						// canvas.setBitmap(bg);
						// canvas.drawBitmap(tmp, 10, 10, new Paint());
						// canvas.save();

						b.recycle();
						b = tmp;
					}
				}

				return b;
			} catch (FileNotFoundException e) {
				b = null;
			} finally {
				try {
					if (fd != null)
						fd.close();
				} catch (IOException e) {
				}
			}
		}
		return b;
	}

	public void getArtwork(Context context, int album_id) {
		final Uri sArtworkUri = Uri
				.parse("content://media/external/audio/albumart");
		final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
		final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();

		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
		if (uri != null) {
			InputStream in = null;

			try {
				in = res.openInputStream(uri);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Bitmap bmp = BitmapFactory.decodeStream(in, null, sBitmapOptions);

		}

	}

	
}
