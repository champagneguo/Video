/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ntian.videoplayer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

import tv.danmaku.ijk.media.widget.MediaController;
import tv.danmaku.ijk.media.widget.VideoView;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.VideoColumns;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ntian.videoplayer.greendao.DaoMaster;
import com.ntian.videoplayer.greendao.DaoMaster.DevOpenHelper;
import com.ntian.videoplayer.greendao.DaoSession;
import com.ntian.videoplayer.greendao.VideoScanRecords;
import com.ntian.videoplayer.greendao.VideoScanRecordsDao;

import de.greenrobot.dao.query.QueryBuilder;

public class VideoPlayerActivity extends Activity {

	private static String TAG = "VideoPlayerActivity_TAG";
	private static final Uri VIDEO_URI = Media.EXTERNAL_CONTENT_URI;
	private static final String[] PROJECTION = new String[] { BaseColumns._ID,
			MediaColumns.DISPLAY_NAME, VideoColumns.DATE_TAKEN,
			VideoColumns.DURATION, MediaColumns.MIME_TYPE, MediaColumns.DATA,
			MediaColumns.SIZE, Media.IS_DRM, MediaColumns.DATE_MODIFIED/*,
			Media.STEREO_TYPE*/ };
	private static final int INDEX_ID = 0;
	private static final int INDEX_DISPLAY_NAME = 1;
	private static final int INDEX_TAKEN_DATE = 2;
	private static final int INDEX_DRUATION = 3;
	private static final int INDEX_MIME_TYPE = 4;
	private static final int INDEX_DATA = 5;
	private static final int INDEX_FILE_SIZE = 6;
	private static final int INDEX_IS_DRM = 7;
	private static final int INDEX_DATE_MODIFIED = 8;
	private static final int INDEX_SUPPORT_3D = 9;
	private static final String VALUE_IS_DRM = "1";
	private VideoView mVideoView;
	private View mBufferingIndicator;
	private MediaController mMediaController;
	private String mVideoPath;
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private VideoScanRecordsDao videoScanRecordsDao;
	// 一整天毫秒数，
	private final static Long OneDayMillSeconds = 86400000L;
	private long mCurrentPosition = 0L;
	private long mBase_ID = -1L;
	private long _ID = -1L;
	private Date date;
	private VideoScanRecords mvideoScanRecords = new VideoScanRecords();
	private String mTitle;
	private long mDuration;
	private long mDateModified;
	private boolean mIsDrm;
	private boolean mSupport3D;
	private static final int sDefaultTimeout = 2500;
	private static final int FADE_OUT = 1;
	public static final int Activity_Back = 3;
	private AudioManager mAM;
	private RelativeLayout mRelativelayout_Sound, mRelativelayout_Bright;
	private TextView mTextView_Sound, mTextView_Bright;
	private int maxVolume, currentVolume;
	private float currentBright;
	private int temp = 0, temp1 = 0, BrightPercent = 0;
	private int mSystemBright = 0;
	private Window mWindow;
	private WindowManager.LayoutParams mLayoutParams;
	private Intent intent;
	private String intentAction;
	private String mIntentString;
	private StringBuffer stringbuilder = new StringBuffer();
	private String[] pathtemp;
	private boolean NoExitVideo = false;

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Activity_Back:
				finish();
				break;
			case FADE_OUT:
				mRelativelayout_Bright.setVisibility(View.GONE);
				mRelativelayout_Sound.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		Log.e(TAG, "onCreate()");

		DevOpenHelper myHelper = new DevOpenHelper(this, "records.db", null);
		db = myHelper.getWritableDatabase();
		daoMaster = new DaoMaster(db);
		daoSession = daoMaster.newSession();
		videoScanRecordsDao = daoSession.getVideoScanRecordsDao();
		// 音量控制,初始化定义
		mAM = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		// 最大音量
		maxVolume = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		// 当前音量
		currentVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
		//

		// if (TextUtils.isEmpty(mVideoPath)) {
		// mVideoPath = new File(Environment.getExternalStorageDirectory(),
		// "download/test.mp4").getAbsolutePath();
		// }

		mBufferingIndicator = findViewById(R.id.buffering_indicator);
		mRelativelayout_Sound = (RelativeLayout) findViewById(R.id.activity_playerlayout_sound);
		mRelativelayout_Bright = (RelativeLayout) findViewById(R.id.activity_playerlayout_bright);
		mTextView_Bright = (TextView) findViewById(R.id.activity_player_brightvalues);
		mTextView_Sound = (TextView) findViewById(R.id.activity_player_soundvalues);
		mMediaController = new MediaController(this, mHandler);
		mVideoView = (VideoView) findViewById(R.id.video_view);
		mVideoView.setMediaController(mMediaController);
		mVideoView.setMediaBufferingIndicator(mBufferingIndicator);

		mWindow = this.getWindow();
		mLayoutParams = mWindow.getAttributes();

		try {
			mSystemBright = Settings.System.getInt(getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS);
			BrightPercent = (mSystemBright * 100) / 255;
			Log.e(TAG, "BrightPercent------>>>>>" + BrightPercent);

		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 退出当前activity时，保存数据库
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (NoExitVideo == true) {

			Log.e(TAG, "onPause()----noExitVideo:" + NoExitVideo);

		} else {

			mCurrentPosition = mVideoView.getCurrentPosition();
			Log.e(TAG, "mVideoView.getCurrentPosition()------------>"
					+ mVideoView.getCurrentPosition());

			VideoScanRecords videoScanRecords = new VideoScanRecords();
			videoScanRecords.setBase_ID(mBase_ID);
			videoScanRecords.setPlayedPosition(mCurrentPosition);
			videoScanRecords.setIs_Scanned(true);
			videoScanRecords.setLast_Played(new java.util.Date());
			videoScanRecords.setVideo_path(mVideoPath);
			videoScanRecords.setTime_Flag(videoScanRecords.getLast_Played()
					.getTime() / OneDayMillSeconds);
			videoScanRecords.setTitle(mTitle);
			videoScanRecords.setDuration(mDuration);
			videoScanRecords.setDateModified(mDateModified);
			videoScanRecords.setIsDrm(mIsDrm);
			videoScanRecords.setSupport3D(mSupport3D);

			// 判断数据库中是否找到，如没有找到则直接插入，有则删除原有记录再插入；
			_ID = mFindRecordByID(mBase_ID);
			if (_ID > -1) {
				videoScanRecords.setId(_ID);
				videoScanRecordsDao.insertOrReplace(videoScanRecords);
			} else if (_ID == -1) {
				videoScanRecordsDao.insert(videoScanRecords);
			}
			date = videoScanRecords.getLast_Played();

			Log.e(TAG, "Records------------------date>>>>>>" + date);

		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		intent = this.getIntent();
		mIntentString = intent.getDataString();
		Log.e(TAG,
				"intent----intent.getDataString()>>>>" + intent.getDataString());

		intentAction = intent.getAction();
		if (mIntentString.contains("content:")) {

			Log.e(TAG, "content:------>>>>>");

			if (!TextUtils.isEmpty(intentAction)
					&& intentAction.equals(Intent.ACTION_VIEW)) {
				mVideoPath = intent.getExtras().getString("video_path");
				mBase_ID = intent.getExtras().getLong("mBase_ID", -1);
				mTitle = intent.getExtras().getString("mTitle");
				mDuration = intent.getExtras().getLong("mDuration");
				mDateModified = intent.getExtras().getLong("mDateModified");
				mIsDrm = intent.getBooleanExtra("mIsDrm", false);
				mSupport3D = intent.getBooleanExtra("mSupport3D", false);
				Log.e(TAG, "mVideoPath----------->" + mVideoPath);

			}
			_ID = mFindRecordByID(mBase_ID);
			Log.e(TAG, "_ID------->>>>" + _ID);
			if (_ID > -1) {
				mCurrentPosition = mvideoScanRecords.getPlayedPosition();
			} else if (_ID == -1) {
				mCurrentPosition = 0;
			}
			Cursor cursor = getContentResolver().query(VIDEO_URI, PROJECTION,
					BaseColumns._ID + " = ?",
					new String[] { String.valueOf(mBase_ID) }, null);
			cursor.moveToFirst();
			Log.e(TAG, "cursor------->>>>>" + cursor.getCount());
			if (cursor.getCount() > 0) {
				mVideoView.setVideoPath(mVideoPath);
				mVideoView.requestFocus();
				mVideoView.seekTo(mCurrentPosition);
				mVideoView.start();
			} else {

				Toast.makeText(VideoPlayerActivity.this, "数据库中尚未找到",
						Toast.LENGTH_SHORT).show();

				NoExitVideo = true;
				Builder builder = new Builder(VideoPlayerActivity.this);
				builder.setTitle("温馨提示");
				builder.setMessage("播放记录无效，是否删除？");
				builder.setPositiveButton("确定", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						videoScanRecordsDao.deleteByKey(_ID);
						Log.e(TAG, "deleteByKey:mBase_ID----->>>" + _ID);
						finish();

					}
				});
				builder.setNegativeButton("取消", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						finish();

					}
				});
				builder.create().show();

				Log.e(TAG, "after finish()----->>>>>content");

			}
			cursor.close();

		} else if (mIntentString.contains("file:")) {
			int i = 0;
			pathtemp = mIntentString.split("[////]");
			for (i = 3; i < pathtemp.length; i++) {
				stringbuilder.append("/").append(pathtemp[i]);
			}
			mVideoPath = stringbuilder.toString();
			try {
				mVideoPath = URLDecoder.decode(mVideoPath, "utf-8");
				Log.e(TAG, "mVideoPath-------->>>>>" + mVideoPath);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			stringbuilder.setLength(0);

			_ID = mFindRecordByPath(mVideoPath);
			if (_ID > -1) {
				Log.e(TAG, "_ID-------->>>>>>" + _ID);

				mCurrentPosition = mvideoScanRecords.getPlayedPosition();
				mBase_ID = mvideoScanRecords.getBase_ID();
				mTitle = mvideoScanRecords.getTitle();
				mDuration = mvideoScanRecords.getDuration();
				mDateModified = mvideoScanRecords.getDateModified();
				mIsDrm = mvideoScanRecords.getIsDrm();
				mSupport3D = mvideoScanRecords.getSupport3D();
				mVideoView.setVideoPath(mVideoPath);

			} else if (_ID == -1) {
				Log.e(TAG, "_ID------>>>>-1");

				mCurrentPosition = 0;
				Cursor cursor = getContentResolver().query(VIDEO_URI,
						PROJECTION,
						MediaStore.Video.Thumbnails.DATA + " like ?",
						new String[] { mVideoPath }, null);
				cursor.moveToFirst();
				Log.e(TAG, "cursor------->>>>>" + cursor.getCount());
				if (cursor.getCount() > 0) {
					mBase_ID = cursor.getLong(INDEX_ID);
					mTitle = cursor.getString(INDEX_DISPLAY_NAME);
					mDuration = cursor.getLong(INDEX_DRUATION);
					mIsDrm = VALUE_IS_DRM
							.equals(cursor.getString(INDEX_IS_DRM));
					mDateModified = cursor.getLong(INDEX_DATE_MODIFIED);
					mSupport3D = MtkUtils.isStereo3D(cursor
							.getInt(INDEX_SUPPORT_3D));
					mVideoView.setVideoPath(mVideoPath);
				} else {

					Builder builder = new Builder(VideoPlayerActivity.this);
					builder.setTitle("温馨提示");
					builder.setMessage("播放记录无效，是否删除？");
					builder.setPositiveButton("确定", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							videoScanRecordsDao.deleteByKey(_ID);
							Log.e(TAG, "deleteByKey:mBase_ID----->>>" + _ID);
							finish();

						}
					});
					builder.setNegativeButton("取消", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							finish();

						}
					});
					builder.create().show();
				}
				cursor.close();

			}

			mVideoView.requestFocus();
			mVideoView.seekTo(mCurrentPosition);
			mVideoView.start();

		}
		
		

	}

	// 根据mBase_ID值查询ID值；
	private long mFindRecordByID(Long mBase_ID) {
		Log.e(TAG, "mFindRecordByID()---------------->>" + mBase_ID);
		long _id = -1;
		QueryBuilder<VideoScanRecords> qb = videoScanRecordsDao.queryBuilder()
				.where(VideoScanRecordsDao.Properties.Base_ID.eq(mBase_ID));
		if (qb.buildCount().count() > 0) {
			mvideoScanRecords = qb.list().get(0);
			_id = mvideoScanRecords.getId();
		}
		return _id;

	}

	private long mFindRecordByPath(String videopath) {
		Log.e(TAG, "mFindRecordByPath()---------------->>" + videopath);
		long _id = -1;
		QueryBuilder<VideoScanRecords> qb = videoScanRecordsDao.queryBuilder()
				.where(VideoScanRecordsDao.Properties.Video_path.eq(videopath));
		Log.e(TAG, "VideoScanRecordsDao.Properties.Video_path>>>"
				+ VideoScanRecordsDao.Properties.Video_path);
		Log.e(TAG, "videopath---->>>" + videopath);
		Log.e(TAG, "qb.buildCount().count()---->>>" + qb.buildCount().count());
		if (qb.buildCount().count() > 0) {
			mvideoScanRecords = qb.list().get(0);
			_id = mvideoScanRecords.getId();
		}
		return _id;

	}

	// 控制音量大小变化
	public void setSound(float SoundValue) {

		mRelativelayout_Sound.setVisibility(View.VISIBLE);
		mRelativelayout_Bright.setVisibility(View.GONE);
		if (SoundValue > 0) {
			temp++;
			if (temp % 3 == 0) {
				mAM.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_RAISE, 0);
				currentVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
			}

		} else {
			temp1++;
			if (temp1 % 3 == 0) {
				mAM.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_LOWER, 0);
				currentVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
			}

		}
		mTextView_Sound.setText(String.valueOf((currentVolume * 100)
				/ maxVolume)
				+ "%");
		Log.e(TAG, "setSound(float SoundValue)--------->" + SoundValue);

	}

	public void setBright(float BrightValue) {

		mRelativelayout_Sound.setVisibility(View.GONE);
		mRelativelayout_Bright.setVisibility(View.VISIBLE);
		currentBright = (BrightPercent * 1.0f) / 100;
		if (BrightValue > 0) {
			currentBright += 0.02f;
			mLayoutParams.screenBrightness = currentBright;
			BrightPercent = (int) (currentBright * 100);
			if (currentBright >= 1.0f) {
				mLayoutParams.screenBrightness = currentBright;
				mLayoutParams.screenBrightness = 1.0f;
				BrightPercent = 100;
				currentBright = 1.0f;
			}
		} else {
			currentBright -= 0.02f;
			mLayoutParams.screenBrightness = currentBright;
			BrightPercent = (int) (currentBright * 100);
			if (currentBright < 0.001f) {
				currentBright = 0f;
				mLayoutParams.screenBrightness = 0.001f;
				BrightPercent = 0;
				currentBright = 0f;
			}
		}
		mWindow.setAttributes(mLayoutParams);
		mTextView_Bright.setText(String.valueOf(BrightPercent) + "%");
		Log.e(TAG, " setBright(float mLayoutParams)--------->" + mLayoutParams);

	}

	public void hide() {

		mHandler.sendEmptyMessageDelayed(FADE_OUT, sDefaultTimeout);

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (db != null) {
			db.close();
			db = null;
		}

		Log.e(TAG, "onDestroy()--------->>>>>>");
	}

}
