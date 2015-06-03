package com.ntian.videoplayer;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Media;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.ntian.videoplayer.greendao.DaoMaster;
import com.ntian.videoplayer.greendao.DaoMaster.DevOpenHelper;
import com.ntian.videoplayer.greendao.DaoSession;
import com.ntian.videoplayer.greendao.VideoScanRecords;
import com.ntian.videoplayer.greendao.VideoScanRecordsDao;

import de.greenrobot.dao.query.QueryBuilder;

public class RecentRecordActivity extends Activity implements OnClickListener {

	private String TAG = "RecentRecordActivity";
	private ListView mListView;
	private Button mBack, mEdit, mCancle, mSeltect, mDelete, mCancelSelected;
	private ImageView mBackArrow;
	private LinearLayout mLinearLayout;
	private FrameLayout mFrameLayout1, mFrameLayout2;
	private static final Uri VIDEO_URI = Media.EXTERNAL_CONTENT_URI;
	private RecordListViewAdapter mListAdapter;
	private ProgressDialog mProgressDialog;
	private static String[] sExternalStoragePaths;
	public static boolean Flag_Clickable = true;
	private ArrayList<ArrayList<Long>> mBase_IDs;
	public static int count = 0;
	private ProgressDialog mmProgressDialog;
	private int mDeleteCount = 0;
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private VideoScanRecordsDao videoScanRecordsDao;
	private ArrayList<ArrayList<HashMap<String, Object>>> mArrayLists;
	private long Date_Flag = 0L;
	public static ArrayList<ArrayList<Integer>> mArrayClickeds;
	// RecordList_Count记录总数
	public static long RecordList_Count = 0;
	private MyQueryHandler mQueryhandler;
	public static boolean mGridLongClicked = true;
	private long Total_Count = 0L;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_recent_records);
		mListView = (ListView) findViewById(R.id.activity_recent_records_lv);
		mLinearLayout = (LinearLayout) findViewById(R.id.activity_recent_records_linearbottom);
		mBack = (Button) findViewById(R.id.activity_recent_records_back);
		mBackArrow = (ImageView) findViewById(R.id.activity_recent_records_backarrow);
		mEdit = (Button) findViewById(R.id.activity_recent_records_edit);
		mCancle = (Button) findViewById(R.id.activity_recent_records_cancle);
		mSeltect = (Button) findViewById(R.id.activity_recentrecords_allselceted);
		mDelete = (Button) findViewById(R.id.activity_recentrecords_delete);
		mCancelSelected = (Button) findViewById(R.id.activity_recentrecords_cancleaselceted);
		mFrameLayout1 = (FrameLayout) findViewById(R.id.RecentRecord_FrameLayout1);
		mFrameLayout2 = (FrameLayout) findViewById(R.id.RecentRecord_FrameLayout2);
		mBack.setOnClickListener(this);
		mBackArrow.setOnClickListener(this);
		mEdit.setOnClickListener(this);
		mCancle.setOnClickListener(this);
		mSeltect.setOnClickListener(this);
		mCancelSelected.setOnClickListener(this);
		mLinearLayout.setOnClickListener(this);
		mDelete.setOnClickListener(this);
		mQueryhandler = new MyQueryHandler(getContentResolver());

		final StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
		sExternalStoragePaths = storageManager.getVolumePaths();
		Log.e(TAG, "onCreate:sExternalStoragePaths:" + sExternalStoragePaths);

		DevOpenHelper myHelper = new DevOpenHelper(this, "records.db", null);
		db = myHelper.getWritableDatabase();
		daoMaster = new DaoMaster(db);
		daoSession = daoMaster.newSession();
		videoScanRecordsDao = daoSession.getVideoScanRecordsDao();
		mArrayLists = new ArrayList<ArrayList<HashMap<String, Object>>>();
		mListAdapter = new RecordListViewAdapter(this, mArrayLists);
		mListView.setAdapter(mListAdapter);
		mListView.setOnScrollListener(mListAdapter);

		// registerForContextMenu(mGridView);
		registerStorageListener();
		refreshSdStatus(MtkUtils.isMediaMounted(RecentRecordActivity.this));

	}

	private void QueryDate() {
		Log.e(TAG, " QueryDate()------->>>>>>");
		mArrayClickeds = new ArrayList<ArrayList<Integer>>();
		QueryBuilder<VideoScanRecords> qb = videoScanRecordsDao.queryBuilder()
				.orderDesc(VideoScanRecordsDao.Properties.Last_Played);
		Total_Count = qb.buildCount().count();
		Log.e(TAG, "qb.buildCount--->>>>>" + qb.buildCount().count());
		if (Total_Count == 0) {

			mArrayLists.clear();
			mListAdapter.setArrayList(mArrayLists);
			Log.e(TAG, "000000000000000000");

		} else if (Total_Count == 1) {
			ArrayList<HashMap<String, Object>> mArrayList = new ArrayList<HashMap<String, Object>>();
			ArrayList<Integer> mArrayClicked = new ArrayList<Integer>();
			HashMap<String, Object> mHashMap = new HashMap<String, Object>();
			mHashMap.put("video_path", qb.list().get(0).getVideo_path());
			mHashMap.put("mBase_ID", qb.list().get(0).getBase_ID());
			mHashMap.put("Time", qb.list().get(0).getLast_Played());
			mHashMap.put("Time_flag", qb.list().get(0).getTime_Flag());
			mHashMap.put("mTitle", qb.list().get(0).getTitle());
			mHashMap.put("mDateModified", qb.list().get(0).getDateModified());
			mHashMap.put("mDuration", qb.list().get(0).getDuration());
			mHashMap.put("mIsDrm", qb.list().get(0).getIsDrm());
			mHashMap.put("mSupport3D", qb.list().get(0).getSupport3D());

			mArrayList.add(mHashMap);
			mArrayClicked.add(0);
			mArrayLists.add(mArrayList);
			mArrayClickeds.add(mArrayClicked);
			Log.e(TAG, "mArrayLists-------->>>>>>>" + mArrayList);
		} else {
			int i = 0;
			int j = 0;
			int k = 0;
			long n = qb.buildCount().count();
			RecordList_Count = n;
			Log.e(TAG, "qb.buildCount().count()-------->>>>>>>" + n);
			HashMap<String, Object> mHashMap1 = new HashMap<String, Object>();
			for (j = 0; j < n; j++) {
				ArrayList<HashMap<String, Object>> mArrayList = new ArrayList<HashMap<String, Object>>();
				ArrayList<Integer> mArrayClicked = new ArrayList<Integer>();
				for (i = j; i < n - 1; i++) {
					Log.e(TAG, "i-----" + i);
					mArrayClicked.add(0);
					Date_Flag = qb.list().get(i).getTime_Flag();
					HashMap<String, Object> mHashMap = new HashMap<String, Object>();
					mHashMap.put("video_path", qb.list().get(i).getVideo_path());
					mHashMap.put("mBase_ID", qb.list().get(i).getBase_ID());
					mHashMap.put("Time", qb.list().get(i).getLast_Played());
					mHashMap.put("Time_flag", qb.list().get(i).getTime_Flag());
					mHashMap.put("mTitle", qb.list().get(i).getTitle());
					mHashMap.put("mDateModified", qb.list().get(i)
							.getDateModified());
					mHashMap.put("mDuration", qb.list().get(i).getDuration());
					mHashMap.put("mIsDrm", qb.list().get(i).getIsDrm());
					mHashMap.put("mSupport3D", qb.list().get(i).getSupport3D());

					if (Date_Flag > qb.list().get(i + 1).getTime_Flag()) {
						mArrayList.add(mHashMap);
						// mArrayLists.add(mArrayList);
						k++;
						j = i;
						if (i == n - 2) {
							mHashMap1.put("video_path", qb.list().get(i + 1)
									.getVideo_path());
							mHashMap1.put("mBase_ID", qb.list().get(i + 1)
									.getBase_ID());
							mHashMap1.put("Time", qb.list().get(i + 1)
									.getLast_Played());
							mHashMap1.put("Time_flag", qb.list().get(i + 1)
									.getTime_Flag());
							mHashMap1.put("mTitle", qb.list().get(i + 1)
									.getTitle());
							mHashMap1.put("mDateModified", qb.list().get(i + 1)
									.getDateModified());
							mHashMap1.put("mDuration", qb.list().get(i + 1)
									.getDuration());
							mHashMap1.put("mIsDrm", qb.list().get(i + 1)
									.getIsDrm());
							mHashMap1.put("mSupport3D", qb.list().get(i + 1)
									.getSupport3D());
						}
						break;
					}
					if (Date_Flag == qb.list().get(i + 1).getTime_Flag()) {
						mArrayList.add(mHashMap);
						j = i;
						if (i == n - 2) {
							mHashMap1.put("video_path", qb.list().get(i + 1)
									.getVideo_path());
							mHashMap1.put("mBase_ID", qb.list().get(i + 1)
									.getBase_ID());
							mHashMap1.put("Time", qb.list().get(i + 1)
									.getLast_Played());
							mHashMap1.put("Time_flag", qb.list().get(i + 1)
									.getTime_Flag());
							mHashMap1.put("mTitle", qb.list().get(i + 1)
									.getTitle());
							mHashMap1.put("mDateModified", qb.list().get(i + 1)
									.getDateModified());
							mHashMap1.put("mDuration", qb.list().get(i + 1)
									.getDuration());
							mHashMap1.put("mIsDrm", qb.list().get(i + 1)
									.getIsDrm());
							mHashMap1.put("mSupport3D", qb.list().get(i + 1)
									.getSupport3D());

						}
					}
				}
				if (i == n - 1) {
					mArrayClicked.add(0);
					mArrayList.add(mHashMap1);
					Log.e(TAG, "i-----" + i);
					mArrayLists.add(mArrayList);
					mArrayClickeds.add(mArrayClicked);
					// Log.e(TAG, "mArrayLists.get(" + (k - 1) + ")-------"
					// + mArrayLists.get(k - 1).size());
					break;
				}
				Log.e(TAG, "j---------" + j);
				mArrayLists.add(mArrayList);
				mArrayClickeds.add(mArrayClicked);
				Log.e(TAG, "mArrayLists.get(" + (k - 1) + ")-------"
						+ mArrayLists.get(k - 1).size());
			}
			// Log.e(TAG, "mArrayLists.get(1)-------" +
			// mArrayLists.get(1).size());
			// Log.e(TAG, "mArrayLists-------" + mArrayLists.size());
		}

		Cursor c = getContentResolver().query(
				MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, null, null,
				null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			long id = c.getLong(c
					.getColumnIndex(MediaStore.Video.Thumbnails.VIDEO_ID));
			String path = c.getString(c
					.getColumnIndex(MediaStore.Video.Thumbnails.DATA));
			Log.d("NTVideoPlayer", "id " + id + " path " + path);
			VideoApplication.mThumbnailPath.put(id, path);
			c.moveToNext();
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

	private final BroadcastReceiver mStorageListener = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			MtkLog.v(TAG, "mStorageListener.onReceive(" + intent + ")");
			final String action = intent.getAction();
			if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
				refreshSdStatus(MtkUtils
						.isMediaMounted(RecentRecordActivity.this));
			} else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
				refreshSdStatus(MtkUtils
						.isMediaMounted(RecentRecordActivity.this));
			} else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
					|| Intent.ACTION_MEDIA_EJECT.equals(action)) {
				if (intent.hasExtra(StorageVolume.EXTRA_STORAGE_VOLUME)) {
					final StorageVolume storage = (StorageVolume) intent
							.getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
					if (storage != null
							&& storage.getPath().equals(
									sExternalStoragePaths[0])) {
						refreshSdStatus(false);
						mListAdapter.notifyDataSetChanged();
						;
					} // else contentObserver will listen it.
					MtkLog.v(TAG, "mStorageListener.onReceive() eject storage="
							+ (storage == null ? "null" : storage.getPath()));
				}
			}
		};

	};

	private void refreshSdStatus(final boolean mounted) {
		MtkLog.v(TAG, "refreshSdStatus(" + mounted + ")");
		if (mounted) {
			if (MtkUtils.isMediaScanning(this)) {
				MtkLog.v(TAG, "refreshSdStatus() isMediaScanning true");
				showScanningProgress();

				MtkUtils.disableSpinnerState(this);
			} else {
				MtkLog.v(TAG, "refreshSdStatus() isMediaScanning false");
				hideScanningProgress();

				mListAdapter.notifyDataSetChanged();
				MtkUtils.enableSpinnerState(this);
			}
		} else {
			hideScanningProgress();
			closeActivity();
			MtkUtils.disableSpinnerState(this);
		}
	}

	private void closeActivity() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				RecentRecordActivity.this);
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

	private void hideScanningProgress() {
		hideProgress();
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

	private void hideProgress() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	class MyQueryHandler extends AsyncQueryHandler {

		public MyQueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			// TODO Auto-generated method stub
			super.onDeleteComplete(token, cookie, result);
			if (result > 0) {
				mDeleteCount--;
				Log.e(TAG, "onDeleteComplete----------->" + mDeleteCount);
				if (mDeleteCount == 0) {
					mmProgressDialog.dismiss();
					mCancle.setVisibility(View.GONE);
					mEdit.setVisibility(View.VISIBLE);
					mLinearLayout.setVisibility(View.GONE);
					mCancelSelected.setVisibility(View.GONE);
					mSeltect.setVisibility(View.VISIBLE);
					Flag_Clickable = true;
					mListAdapter.notifyDataSetChanged();
					Toast.makeText(RecentRecordActivity.this, "删除完成",
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.activity_recent_records_back:
			finish();
			break;
		case R.id.activity_recent_records_backarrow:
			finish();
			break;
		case R.id.activity_recent_records_edit:

			mEdit.setVisibility(View.GONE);
			mCancle.setVisibility(View.VISIBLE);
			mLinearLayout.setVisibility(View.VISIBLE);
			mCancelSelected.setVisibility(View.GONE);
			mSeltect.setVisibility(View.VISIBLE);
			mBase_IDs = new ArrayList<ArrayList<Long>>();
			Flag_Clickable = false;
			count = 0;
			for (int i = 0; i < mArrayLists.size(); i++) {
				ArrayList<Long> mBase_ID = new ArrayList<Long>();
				for (int j = 0; j < mArrayLists.get(i).size(); j++) {
					mBase_ID.add((Long) mArrayLists.get(i).get(j)
							.get("mBase_ID"));
					mArrayClickeds.get(i).set(j, 0);
				}
				mBase_IDs.add(mBase_ID);
			}
			mListAdapter.notifyDataSetChanged();
			mDelete.setText(this.getResources().getText(
					R.string.activity_recentrecords_delete)
					+ " ( " + count + " )");

			break;
		case R.id.activity_recent_records_cancle:
			mCancle.setVisibility(View.GONE);
			mEdit.setVisibility(View.VISIBLE);
			mLinearLayout.setVisibility(View.GONE);
			mCancelSelected.setVisibility(View.GONE);
			mSeltect.setVisibility(View.VISIBLE);
			Flag_Clickable = true;
			mGridLongClicked = true;
			count = 0;
			for (int i = 0; i < mArrayLists.size(); i++) {
				for (int j = 0; j < mArrayLists.get(i).size(); j++) {
					mArrayClickeds.get(i).set(j, 0);
				}

			}
			mListAdapter.notifyDataSetChanged();
			mDelete.setText(this.getResources().getText(
					R.string.activity_recentrecords_delete)
					+ " ( " + count + " )");

		case R.id.activity_recentrecords_allselceted:

			count = 0;
			for (int i = 0; i < mArrayLists.size(); i++) {
				for (int j = 0; j < mArrayLists.get(i).size(); j++) {
					mArrayClickeds.get(i).set(j, 1);
					count++;
				}
			}
			mListAdapter.notifyDataSetChanged();
			mCancelSelected.setVisibility(View.VISIBLE);
			mSeltect.setVisibility(View.GONE);
			mDelete.setText(this.getResources().getText(
					R.string.activity_recentrecords_delete)
					+ " ( " + count + " )");

			break;
		case R.id.activity_recentrecords_cancleaselceted:

			count = 0;

			for (int i = 0; i < mArrayLists.size(); i++) {
				for (int j = 0; j < mArrayLists.get(i).size(); j++) {
					mArrayClickeds.get(i).set(j, 0);
				}
			}
			mListAdapter.notifyDataSetChanged();
			mSeltect.setVisibility(View.VISIBLE);
			mCancelSelected.setVisibility(View.GONE);
			mDelete.setText(this.getResources().getText(
					R.string.activity_recentrecords_delete)
					+ " ( " + count + " )");

			break;
		case R.id.activity_recentrecords_delete:

			if (count == 0) {
				Toast.makeText(RecentRecordActivity.this, "尚未选中删除条目",
						Toast.LENGTH_SHORT).show();
			} else {

				AlertDialog.Builder builder = new Builder(
						RecentRecordActivity.this);
				builder.setTitle("温馨提示");
				builder.setMessage("确认删除" + count + "条记录？");
				builder.setPositiveButton("确认",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								// 添加删除动作
								mmProgressDialog = new ProgressDialog(
										RecentRecordActivity.this);
								mmProgressDialog.setMessage("正在删除请稍等...");
								mmProgressDialog.show();
								for (int i = 0; i < mArrayClickeds.size(); i++) {
									for (int j = 0; j < mArrayClickeds.get(i)
											.size(); j++) {
										if (mArrayClickeds.get(i).get(j) == 1) {
											long _ID = -1L;
											// mQueryhandler.startDelete(0,
											// null,
											// VIDEO_URI, BaseColumns._ID
											// + " = ?",
											// new String[] { ""
											// + mBase_IDs.get(i)
											// .get(j) });
											
											count--;
											_ID = mFindRecordByID(mBase_IDs
													.get(i).get(j));
											if (_ID > -1) {
												videoScanRecordsDao
														.deleteByKey(_ID);

												Log.e(TAG,
														"onDeleteComplete----------->"
																+ mDeleteCount);

											}
										}
									}
								}

								if (count == 0) {
									mmProgressDialog.dismiss();
									mCancle.setVisibility(View.GONE);
									mEdit.setVisibility(View.VISIBLE);
									mLinearLayout.setVisibility(View.GONE);
									mCancelSelected.setVisibility(View.GONE);
									mSeltect.setVisibility(View.VISIBLE);
									Flag_Clickable = true;

									QueryDate();
									Log.e(TAG, "mArrayLists---->>>"
											+ mArrayLists.size());
									mListAdapter.notifyDataSetChanged();
									Toast.makeText(RecentRecordActivity.this,
											"删除完成", Toast.LENGTH_SHORT).show();
								}
							}
						});
				builder.setNegativeButton("取消",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
							}
						});
				builder.create().show();
			}
			break;
		default:
			break;
		}
	}

	public void setIntoEdit(int parent_position, int position) {
		Log.e(TAG, "setIntoEdit--------parent_position" + parent_position);
		Log.e(TAG, "setIntoEdit--------parent_position" + position);
		mEdit.setVisibility(View.GONE);
		mCancle.setVisibility(View.VISIBLE);
		mLinearLayout.setVisibility(View.VISIBLE);
		mCancelSelected.setVisibility(View.GONE);
		mSeltect.setVisibility(View.VISIBLE);
		mBase_IDs = new ArrayList<ArrayList<Long>>();
		Flag_Clickable = false;
		mGridLongClicked = false;
		count = 0;
		for (int i = 0; i < mArrayLists.size(); i++) {
			ArrayList<Long> mBase_ID = new ArrayList<Long>();
			for (int j = 0; j < mArrayLists.get(i).size(); j++) {
				mBase_ID.add((Long) mArrayLists.get(i).get(j).get("mBase_ID"));
				mArrayClickeds.get(i).set(j, 0);
				if (i == parent_position && j == position) {
					mArrayClickeds.get(i).set(j, 1);
					count++;
					Log.e(TAG, "count------------->>>>>>>" + count);
				}
			}
			mBase_IDs.add(mBase_ID);
		}
		// mArrayClickeds.get(parent_position).set(position, 1);

		mDelete.setText(this.getResources().getText(
				R.string.activity_recentrecords_delete)
				+ " ( " + count + " )");

	}

	// 及时显示选中被删除的个数
	public void setCount() {
		mDelete.setText(this.getResources().getText(
				R.string.activity_recentrecords_delete)
				+ " ( " + count + " )");
		if (count == Total_Count) {

			mCancelSelected.setVisibility(View.VISIBLE);
			mSeltect.setVisibility(View.GONE);

		}
	}

	// 取消全选后操作
	public void setCancleAllDelete() {
		mCancelSelected.setVisibility(View.GONE);
		mSeltect.setVisibility(View.VISIBLE);
	}

	// 根据mBase_ID值查询ID值；
	private long mFindRecordByID(Long mBase_ID) {
		Log.e(TAG, "mFindRecordByID()---------------->>" + mBase_ID);
		long _id = -1;
		QueryBuilder<VideoScanRecords> qb = videoScanRecordsDao.queryBuilder();
		qb.where(VideoScanRecordsDao.Properties.Base_ID.eq(mBase_ID));
		if (qb.buildCount().count() > 0) {
			_id = qb.list().get(0).getId();
		}
		return _id;

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mArrayLists.clear();
		QueryDate();
		mListAdapter.notifyDataSetChanged();

	}

	public void setNoRecordVisibility() {

		mEdit.setVisibility(View.GONE);
		mFrameLayout1.setVisibility(View.GONE);
		mFrameLayout2.setVisibility(View.VISIBLE);

	}

	public void setRecordVisibility() {

		mEdit.setVisibility(View.VISIBLE);
		mFrameLayout1.setVisibility(View.VISIBLE);
		mFrameLayout2.setVisibility(View.GONE);

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (db != null) {
			db.close();
			db = null;
		}
		if (mArrayClickeds != null) {
			mArrayClickeds.clear();
			mArrayClickeds = null;
		}
		Flag_Clickable = true;
		count = 0;
		RecordList_Count = 0;
		mGridLongClicked = true;
		unregisterReceiver(mStorageListener);
	}
}
