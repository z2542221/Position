package com.vorpegy.position;

import android.content.Context;
import android.util.AttributeSet;

import com.qozix.tileview.TileView;

public class MyView extends TileView {

	public MyView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public MyView(Context context, AttributeSet attrs) {
		this(context);
	}

	public void init() {
		setSize(1080, 1716);
		setBackgroundResource(R.drawable.sample);
	}

}
