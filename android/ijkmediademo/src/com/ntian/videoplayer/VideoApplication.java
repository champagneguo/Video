package com.ntian.videoplayer;

import java.util.HashMap;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

public class VideoApplication extends Application {
	
	public static HashMap<Long,String> mThumbnailPath = new HashMap<Long,String>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Fresco.initialize(this);
	}
}
