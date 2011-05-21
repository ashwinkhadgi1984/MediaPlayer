package com.musicplayer.media;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;


public class HorizontalSlider extends SeekBar {

	private OnProgressChangeListener listener;

	private static int padding = 5;

	public interface OnProgressChangeListener {
		void onProgressChanged(View v, int progress);
	}
	
	
	public HorizontalSlider(Context context) {
		super(context);

	}

	public HorizontalSlider(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
	}

	public HorizontalSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	/*	int attrsWanted[] = new int[]{R.attr.pressed_maxHeight, R.attr.pressed_minHeight, R.attr.pressed_progressDrawable,R.attr.pressed_thumb,R.attr.pressed_thumbOffset};
		
		TypedArray a = getContext().obtainStyledAttributes(attrs, attrsWanted);
		
		Drawable d = a.getDrawable(R.attr.pressed_thumb);
        //Don't forget this
        a.recycle();
		*/
	}
	

	public void setOnProgressChangeListener(OnProgressChangeListener l) {
		listener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int action = event.getAction();

		if (action == MotionEvent.ACTION_DOWN
				|| action == MotionEvent.ACTION_MOVE) {
			float x_mouse = event.getX() - padding;
			float width = getWidth() - 2*padding;
			int progress = Math.round((float) getMax() * (x_mouse / width));

			if (progress < 0)
				progress = 0;

			this.setProgress(progress);
			
			//setProgressDrawable(R.attr.pressed_progressDrawable);
			
			if (listener != null)
				listener.onProgressChanged(this, progress);
		}

		return true;
	}
}
