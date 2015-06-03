package com.ntian.videoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ntian.videoplayer.greendao.DaoMaster;
import com.ntian.videoplayer.greendao.DaoMaster.DevOpenHelper;
import com.ntian.videoplayer.greendao.DaoSession;
import com.ntian.videoplayer.greendao.VideoScanRecords;
import com.ntian.videoplayer.greendao.VideoScanRecordsDao;

import de.greenrobot.dao.query.QueryBuilder;

@SuppressLint("InlinedApi")
public class MainMenuActivity extends Activity implements OnClickListener {

	private String TAG = "MainMenuActivity";

	private LinearLayout localvideos, recentvideos;
	private TextView Name, Tv_LocalVideos, Tv_RecentVideos;
	private static final Uri VIDEO_URI = Media.EXTERNAL_CONTENT_URI;
	private static final String[] PROJECTION = new String[] { BaseColumns._ID,
			MediaColumns.DISPLAY_NAME, VideoColumns.DATE_TAKEN,
			VideoColumns.DURATION, MediaColumns.MIME_TYPE, MediaColumns.DATA,
			MediaColumns.SIZE, Media.IS_DRM, MediaColumns.DATE_MODIFIED/*,
			Media.STEREO_TYPE*/ };

	private ProgressDialog mProgressDialog;
	private static String[] sExternalStoragePaths;

	private QueryHandler mQueryHandler = null;

	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private VideoScanRecordsDao videoScanRecordsDao;
	private long count = 0;
	private VideoObserver mObserver;

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_mainmenu);
		Log.e(TAG, "onCreate()");
		final StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
		sExternalStoragePaths = storageManager.getVolumePaths();
		localvideos = (LinearLayout) findViewById(R.id.menuactivity_localvideos);
		recentvideos = (LinearLayout) findViewById(R.id.menuactivity_recentvideos);
		Name = (TextView) findViewById(R.id.menuactivity_tv_name);
		Tv_LocalVideos = (TextView) findViewById(R.id.menuactivity_tv_localvideos);
		Tv_RecentVideos = (TextView) findViewById(R.id.menuactivity_tv_recentvideos);
		mQueryHandler = new QueryHandler(getContentResolver());

		DevOpenHelper myHelper = new DevOpenHelper(this, "records.db", null);
		db = myHelper.getWritableDatabase();
		daoMaster = new DaoMaster(db);
		daoSession = daoMaster.newSession();
		videoScanRecordsDao = daoSession.getVideoScanRecordsDao();

		localvideos.setOnClickListener(this);
		recentvideos.setOnClickListener(this);

		registerStorageListener();
		refreshSdStatus(MtkUtils.isMediaMounted(MainMenuActivity.this));
		this.getContentResolver().registerContentObserver(VIDEO_URI, true,
				new VideoObserver(new Handler()));

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.menuactivity_localvideos:
			Intent intent = new Intent(MainMenuActivity.this,
					MoviesContentAcitivity.class);
			startActivity(intent);
			break;
		case R.id.menuactivity_recentvideos:
			Intent intent1 = new Intent(MainMenuActivity.this,
					RecentRecordActivity.class);
			startActivity(intent1);
			break;
		default:
			break;
		}
	}

	private void registerStorageListener() {
		final IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		iFilter.addDataScheme("file");
		registerReceiver(mStorageListener, iFilter);
	}

	private void refreshSdStatus(final boolean mounted) {
		MtkLog.v(TAG, "refreshSdStatus(" + mounted + ")");
		if (mounted) {
			if (MtkUtils.isMediaScanning(this)) {
				MtkLog.v(TAG, "refreshSdStatus() isMediaScanning true");
				showScanningProgress();
				MtkUtils.disableSpinnerState(this);
			} else {
				MtkLog.v(TAG, "refreshSdStatus() isMediaScanning false");
				// 异步收索媒体数据库
				mQueryHandler.startQuery(0, null, VIDEO_URI, PROJECTION, null,
						null, null);

				MtkUtils.enableSpinnerState(this);
			}
		} else {
			// 提示关闭当前窗口；
			closeActivity();
			MtkUtils.disableSpinnerState(this);
		}
	}

	private void closeActivity() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				MainMenuActivity.this);
		builder.setTitle("提示信息");
		builder.setMessage("SD尚未加载，请退出。");
		builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				finish();

			}
		});
		builder.show();
	}

	private void showScanningProgress() {
		showProgress(getString(R.string.scanning), new OnCancelListener() {

			@Override
			public void onCancel(final DialogInterface dialog) {
				MtkLog.v(TAG, "mProgressDialog.onCancel()");
				hideScanningProgress();
				finish();
			}

		});
	}

	private void showProgress(final String message,
			final OnCancelListener cancelListener) {
		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setCancelable(cancelListener != null);
			mProgressDialog.setOnCancelListener(cancelListener);
			mProgressDialog.setMessage(message);
		}
		mProgressDialog.show();
	}

	private void hideScanningProgress() {
		hideProgress();
	}

	private void hideProgress() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	private final BroadcastReceiver mStorageListener = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			MtkLog.v(TAG, "mStorageListener.onReceive(" + intent + ")");
			final String action = intent.getAction();
			if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
				refreshSdStatus(MtkUtils.isMediaMounted(MainMenuActivity.this));
			} else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
				refreshSdStatus(MtkUtils.isMediaMounted(MainMenuActivity.this));
			} else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
					|| Intent.ACTION_MEDIA_EJECT.equals(action)) {
				if (intent.hasExtra(StorageVolume.EXTRA_STORAGE_VOLUME)) {
					final StorageVolume storage = (StorageVolume) intent
							.getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
					if (storage != null
							&& storage.getPath().equals(
									sExternalStoragePaths[0])) {
						refreshSdStatus(false);
						// mAdapter.changeCursor(null);
					} // else contentObserver will listen it.
					MtkLog.v(TAG, "mStorageListener.onReceive() eject storage="
							+ (storage == null ? "null" : storage.getPath()));
				}
			}
		};

	};

	class QueryHandler extends AsyncQueryHandler {

		@SuppressLint("HandlerLeak")
		public QueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// TODO Auto-generated method stub
			super.onQueryComplete(token, cookie, cursor);

			if (cursor == null) {
				Tv_LocalVideos.setText(MainMenuActivity.this.getResources()
						.getText(R.string.activity_mainmenu_total)
						+ " "
						+ 0
						+ " "
						+ MainMenuActivity.this.getResources().getText(
								R.string.activity_mainmenu_item));
			} else {
				Tv_LocalVideos.setText(MainMenuActivity.this.getResources()
						.getText(R.string.activity_mainmenu_total)
						+ " "
						+ cursor.getCount()
						+ " "
						+ MainMenuActivity.this.getResources().getText(
								R.string.activity_mainmenu_item));
				Log.e(TAG, "onQueryComplete-cursor-->>" + cursor.getCount());
			}
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		refreshSdStatus(MtkUtils.isMediaMounted(MainMenuActivity.this));
		QueryBuilder<VideoScanRecords> qb = videoScanRecordsDao.queryBuilder();

		Log.e(TAG, "qb.buildCount().count()--------------->>>"
				+ qb.buildCount().count());
		count = qb.buildCount().count();

		Tv_RecentVideos.setText(MainMenuActivity.this.getResources().getText(
				R.string.activity_mainmenu_total)
				+ " "
				+ count
				+ " "
				+ MainMenuActivity.this.getResources().getText(
						R.string.activity_mainmenu_item));

	}

	public class VideoObserver extends ContentObserver {

		public VideoObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			super.onChange(selfChange);

			mQueryHandler.startQuery(0, null, VIDEO_URI, PROJECTION, null,
					null, null);

		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mStorageListener);
	}

}
