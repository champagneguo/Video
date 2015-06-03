package com.ntian.videoplayer;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.ntian.videoplayer.greendao.DaoMaster;
import com.ntian.videoplayer.greendao.DaoMaster.DevOpenHelper;
import com.ntian.videoplayer.greendao.DaoSession;
import com.ntian.videoplayer.greendao.VideoScanRecordsDao;

public class MoviesContentAcitivity extends Activity implements
		OnItemClickListener, android.view.View.OnClickListener {

	private String TAG = "MoviesContentAcitivity";

	private GridView mGridView;
	private Button mBack, mEdit, mCancle, mSeltect, mDelete, mCancelSelected;
	private ImageView mBackArrow;
	private LinearLayout mLinearLayout;
	private FrameLayout mFrameLayout1, mFrameLayout2;
	private static final Uri VIDEO_URI = Media.EXTERNAL_CONTENT_URI;
	private static final String[] PROJECTION = new String[] { BaseColumns._ID,
			MediaColumns.DISPLAY_NAME, VideoColumns.DATE_TAKEN,
			VideoColumns.DURATION, MediaColumns.MIME_TYPE, MediaColumns.DATA,
			MediaColumns.SIZE, Media.IS_DRM, MediaColumns.DATE_MODIFIED /*
																		 * ,
																		 * Media
																		 * .
																		 * STEREO_TYPE
																		 */};
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

	private static final String ORDER_COLUMN = VideoColumns.DATE_TAKEN
			+ " DESC, " + BaseColumns._ID + " DESC ";

	private MovieListAdapter mAdapter;

	// private static final int MENU_DELETE_ALL = 1;
	private static final int MENU_DELETE_ONE = 2;
	private static final int MENU_PROPERTY = 3;
	private static final int MENU_DRM_DETAIL = 4;

	private static final String KEY_LOGO_BITMAP = "logo-bitmap";
	private static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
	private static final String EXTRA_ALL_VIDEO_FOLDER = "mediatek.intent.extra.ALL_VIDEO_FOLDER";
	private static final String EXTRA_ENABLE_VIDEO_LIST = "mediatek.intent.extra.ENABLE_VIDEO_LIST";
	private ProgressDialog mProgressDialog;
	private static String[] sExternalStoragePaths;
	private CachedVideoInfo mCachedVideoInfo;
	private boolean Flag_Clickable = true;
	private int mArrayClicked[];
	private Long mBase_ID[];
	private int count = 0;
	private ProgressDialog mmProgressDialog;
	private int mDeleteCount = 0;
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private VideoScanRecordsDao videoScanRecordsDao;
	private boolean mGridLongClicked = true;
	private VideoObserver mObserver;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_movies_content);

		mGridView = (GridView) findViewById(R.id.activity_movies_content_gv);
		mBack = (Button) findViewById(R.id.activity_movies_content_back);
		mBackArrow = (ImageView) findViewById(R.id.activity_movies_content_backarrow);
		mEdit = (Button) findViewById(R.id.activity_movies_content_edit);
		mCancle = (Button) findViewById(R.id.activity_movies_content_cancle);
		mSeltect = (Button) findViewById(R.id.activity_moviescontent_allselceted);
		mCancelSelected = (Button) findViewById(R.id.activity_moviescontent_cancleaselceted);
		mDelete = (Button) findViewById(R.id.activity_movicescontent_delete);
		mLinearLayout = (LinearLayout) findViewById(R.id.activity_moviescontent_linearbottom);
		mFrameLayout1 = (FrameLayout) findViewById(R.id.FrameLayout1);
		mFrameLayout2 = (FrameLayout) findViewById(R.id.FrameLayout2);
		mBack.setOnClickListener(this);
		mBackArrow.setOnClickListener(this);
		mEdit.setOnClickListener(this);
		mCancle.setOnClickListener(this);
		mSeltect.setOnClickListener(this);
		mCancelSelected.setOnClickListener(this);
		mLinearLayout.setOnClickListener(this);
		mDelete.setOnClickListener(this);

		final StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
		sExternalStoragePaths = storageManager.getVolumePaths();
		Log.e(TAG, "onCreate:sExternalStoragePaths:" + sExternalStoragePaths);
		mObserver = new VideoObserver(new Handler());
		DevOpenHelper myHelper = new DevOpenHelper(this, "records.db", null);
		db = myHelper.getWritableDatabase();
		daoMaster = new DaoMaster(db);
		daoSession = daoMaster.newSession();
		videoScanRecordsDao = daoSession.getVideoScanRecordsDao();
		mAdapter = new MovieListAdapter(this,
				R.layout.activity_moviescontent_gv, null, new String[] {},
				new int[] {});
		Log.e(TAG, "onCreate:mAdapter:" + mAdapter);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnScrollListener(mAdapter);
		mGridView.setOnItemClickListener(this);
		// mGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		mGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				if (mGridLongClicked == true) {

					// 进入编辑状态
					setIntoEdit(position);

					final Object o = view.getTag();
					ViewHolder holder = null;
					if (o instanceof ViewHolder) {

						holder = (ViewHolder) o;

						if (mArrayClicked[position] == 1) {

							holder.mImageView_Blue.setVisibility(View.VISIBLE);
							holder.mImageView_grey.setVisibility(View.GONE);

						}
						if (RecentRecordActivity.count == (RecentRecordActivity.RecordList_Count - 1)) {
							setCancleAllDelete();
						}

					}
					mAdapter.notifyDataSetChanged();

				}
				if (mGridLongClicked == false) {

				}
				return true;

			}
		});
		// registerForContextMenu(mGridView);
		registerStorageListener();

		refreshSdStatus(MtkUtils.isMediaMounted(MoviesContentAcitivity.this));

		// mThumbnailCache = new ThumbnailCache(this);
		// mThumbnailCache.addListener(mAdapter);
		mCachedVideoInfo = new CachedVideoInfo();
		MtkLog.v(TAG, "onCreate(" + savedInstanceState + ")");
		this.getContentResolver().registerContentObserver(VIDEO_URI, true,
				mObserver);
		Log.e(TAG, "onCreate()----------->>>>>>");

	}

	private void refreshMovieList() {
		mAdapter.getQueryHandler().removeCallbacks(null);
		mAdapter.getQueryHandler().startQuery(0, null, VIDEO_URI, PROJECTION,
				null, null, ORDER_COLUMN);
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
						.isMediaMounted(MoviesContentAcitivity.this));
			} else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
				refreshSdStatus(MtkUtils
						.isMediaMounted(MoviesContentAcitivity.this));
			} else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
					|| Intent.ACTION_MEDIA_EJECT.equals(action)) {
				if (intent.hasExtra(StorageVolume.EXTRA_STORAGE_VOLUME)) {
					final StorageVolume storage = (StorageVolume) intent
							.getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
					if (storage != null
							&& storage.getPath().equals(
									sExternalStoragePaths[0])) {
						refreshSdStatus(false);
						mAdapter.changeCursor(null);
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

				refreshMovieList();
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
				MoviesContentAcitivity.this);
		builder.setTitle("提示信息");
		builder.setMessage("SD尚未加载，请退出。");
		builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				finish();

			}
		});
		builder.create().show();
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub

		if (Flag_Clickable == true) {
			final Object o = view.getTag();
			ViewHolder holder = null;
			if (o instanceof ViewHolder) {
				holder = (ViewHolder) o;
				final Intent intent = new Intent(Intent.ACTION_VIEW);
				String mime = "video/*";
				if (!(holder.mMimetype == null || "".equals(holder.mMimetype
						.trim()))) {
					mime = holder.mMimetype;
				}
				intent.setDataAndType(
						ContentUris.withAppendedId(VIDEO_URI, holder.mId), mime);
				intent.putExtra("mBase_ID", holder.mId);
				intent.putExtra("video_path", holder.mData);
				intent.putExtra(EXTRA_ALL_VIDEO_FOLDER, true);
				intent.putExtra(KEY_TREAT_UP_AS_BACK, true);
				intent.putExtra(EXTRA_ENABLE_VIDEO_LIST, true);
				intent.putExtra("mTitle", holder.mTitle);
				intent.putExtra("mDuration", holder.mDuration);
				intent.putExtra("mDateModified", holder.mDateModified);
				intent.putExtra("mIsDrm", holder.mIsDrm);
				intent.putExtra("mSupport3D", holder.mSupport3D);
				intent.putExtra(KEY_LOGO_BITMAP, BitmapFactory.decodeResource(
						getResources(), R.drawable.ic_video_app));
				intent.setComponent(new ComponentName("com.ntian.videoplayer",
						"com.ntian.videoplayer.VideoPlayerActivity")); // add by
																		// ChMX
				try {
					startActivity(intent);
				} catch (final ActivityNotFoundException e) {
					e.printStackTrace();
				}
			}

		} else if (Flag_Clickable == false) {

			final Object o = view.getTag();
			ViewHolder holder = null;
			if (o instanceof ViewHolder) {

				holder = (ViewHolder) o;

				if (mArrayClicked[position] == 1) {
					mArrayClicked[position] = 0;
					holder.mImageView_Blue.setVisibility(View.GONE);
					holder.mImageView_grey.setVisibility(View.VISIBLE);
					count--;
				} else if (mArrayClicked[position] == 0) {
					mArrayClicked[position] = 1;
					holder.mImageView_Blue.setVisibility(View.VISIBLE);
					holder.mImageView_grey.setVisibility(View.GONE);
					count++;
				}
				mDelete.setText(this.getResources().getText(
						R.string.activity_moviescontent_delete)
						+ " ( " + count + " )");
				Log.e(TAG, "OnItemCount-->>>" + count);

				if (count == (mAdapter.getCount() - 1)) {
					mCancelSelected.setVisibility(View.GONE);
					mSeltect.setVisibility(View.VISIBLE);
				}
				if (count == (mAdapter.getCount())) {
					mCancelSelected.setVisibility(View.VISIBLE);
					mSeltect.setVisibility(View.GONE);
				}
			}

		}
		MtkLog.v(TAG, "onItemClick(" + position + ", " + id + ") ");

	}

	// @Override
	// public void onCreateContextMenu(final ContextMenu menu, final View v,
	// final ContextMenuInfo menuInfo) {
	// super.onCreateContextMenu(menu, v, menuInfo);
	// final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
	// final Object obj = info.targetView.getTag();
	// ViewHolder holder = null;
	// if (obj instanceof ViewHolder) {
	// holder = (ViewHolder) obj;
	// menu.setHeaderTitle(holder.mTitle);
	// menu.add(0, MENU_DELETE_ONE, 0, R.string.delete);
	// menu.add(0, MENU_PROPERTY, 0, R.string.media_detail);
	// if (MtkUtils.isSupportDrm() && holder.mIsDrm) {
	// menu.add(0, MENU_DRM_DETAIL, 0,
	// com.mediatek.R.string.drm_protectioninfo_title);
	// }
	// }
	// }
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final Object obj = info.targetView.getTag();
		ViewHolder holder = null;
		if (obj instanceof ViewHolder) {
			holder = (ViewHolder) obj;
		}
		if (holder == null) {
			MtkLog.w(TAG, "wrong context item info " + info);
			return true;
		}
		switch (item.getItemId()) {
		case MENU_DELETE_ONE:
			showDelete(holder.clone());
			return true;
		case MENU_PROPERTY:
			showDetail(holder.clone());
			return true;
		case MENU_DRM_DETAIL:
			if (MtkUtils.isSupportDrm()) {
				MtkUtils.showDrmDetails(this, holder.mData);
			}
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	private void showDetail(final ViewHolder holder) {
		final DetailDialog detailDialog = new DetailDialog(this, holder);
		detailDialog.setTitle(R.string.media_detail);
		detailDialog.show();
	}

	private void showDelete(final ViewHolder holder) {
		MtkLog.v(TAG, "showDelete(" + holder + ")");
		new AlertDialog.Builder(this)
				.setTitle(R.string.delete)
				.setMessage(getString(R.string.delete_tip, holder.mTitle))
				.setCancelable(true)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						MtkLog.v(TAG, "Delete.onClick() " + holder);
						new DeleteTask(holder).execute();
					}

				}).setNegativeButton(android.R.string.cancel, null).create()
				.show();
	}

	public class DeleteTask extends AsyncTask<Void, Void, Void> {
		private final ViewHolder mHolder;

		public DeleteTask(final ViewHolder holder) {
			mHolder = holder;
		}

		@Override
		protected void onPreExecute() {
			showDeleteProgress(getString(R.string.delete_progress,
					mHolder.mTitle));
		}

		@Override
		protected void onPostExecute(final Void result) {
			hideDeleteProgress();
		}

		private void showDeleteProgress(final String message) {
			showProgress(message, null);
		}

		private void hideDeleteProgress() {
			hideProgress();
		}

		@Override
		protected Void doInBackground(final Void... params) {
			final ViewHolder holder = mHolder;
			if (holder == null) {
				MtkLog.w(TAG, "DeleteTask.doInBackground holder=" + holder);
			} else {
				int count = 0;
				try {
					count = getContentResolver().delete(
							ContentUris.withAppendedId(VIDEO_URI, holder.mId),
							null, null);
				} catch (final SQLiteException e) {
					e.printStackTrace();
				}
				MtkLog.v(TAG, "DeleteTask.doInBackground delete count=" + count);
			}
			return null;
		}

	}

	class MovieListAdapter extends SimpleCursorAdapter implements
	/* ThumbnailCache.ThumbnailStateListener, */OnScrollListener {
		private static final String TAG = "MovieListAdapter";
		private final QueryHandler mQueryHandler;
		private final ArrayList<ViewHolder> mCachedHolder = new ArrayList<ViewHolder>();
		private static final String VALUE_IS_DRM = "1";

		QueryHandler getQueryHandler() {
			return mQueryHandler;
		}

		@SuppressWarnings("deprecation")
		public MovieListAdapter(final Context context, final int layout,
				final Cursor c, final String[] from, final int[] to) {
			super(context, layout, c, from, to);
			mQueryHandler = new QueryHandler(getContentResolver());
		}

		@Override
		public void notifyDataSetChanged() {
			// TODO Auto-generated method stub
			super.notifyDataSetChanged();
			if (isEmpty()) {

				mEdit.setVisibility(View.GONE);
				mFrameLayout1.setVisibility(View.GONE);
				mFrameLayout2.setVisibility(View.VISIBLE);

			} else {

				mEdit.setVisibility(View.VISIBLE);
				mFrameLayout1.setVisibility(View.VISIBLE);
				mFrameLayout2.setVisibility(View.GONE);

			}
		}

		@Override
		public View newView(final Context context, final Cursor cursor,
				final ViewGroup parent) {
			final View view = super.newView(context, cursor, parent);
			final ViewHolder holder = new ViewHolder();

			holder.mIcon = (SimpleDraweeView) view
					.findViewById(R.id.activity_moviescontent_gv_image);
			holder.mTitleView = (TextView) view
					.findViewById(R.id.activity_moviescontent_gv_name);
			// holder.mFileSizeView = (TextView)
			// view.findViewById(R.id.item_date);
			holder.mDurationView = (TextView) view
					.findViewById(R.id.activity_moviescontent_gv_time);
			holder.mImageView_grey = (ImageView) view
					.findViewById(R.id.activity_moviescontent_gv_image_nor);
			holder.mImageView_Blue = (ImageView) view
					.findViewById(R.id.activity_moviescontent_gv_image_sel);
			// int width = mThumbnailCache.getDefaultThumbnailWidth();
			// int height = mThumbnailCache.getDefaultThumbnailHeight();
			holder.mFastDrawable = new FastBitmapDrawable(155, 98);
			view.setTag(holder);
			mCachedHolder.add(holder);
			MtkLog.v(TAG,
					"newView() mCachedHolder.size()=" + mCachedHolder.size());
			return view;
		}

		/*
		 * public void onChanged(final long rowId, final int type, final Bitmap
		 * drawable) {
		 * 
		 * Log.e(TAG,
		 * "ThumbnailCache.ThumbnailStateListener:onChanged------------" +
		 * rowId); MtkLog.v(TAG, "onChanged(" + rowId + ", " + type + ", " +
		 * drawable + ")"); for (final ViewHolder holder : mCachedHolder) { if
		 * (holder.mId == rowId) { refreshThumbnail(holder); break; } } }
		 */
		public void clearCachedHolder() {
			mCachedHolder.clear();
		}

		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor) {

			final ViewHolder holder = (ViewHolder) view.getTag();

			holder.mId = cursor.getLong(INDEX_ID);
			holder.mTitle = cursor.getString(INDEX_DISPLAY_NAME);
			holder.mDateTaken = cursor.getLong(INDEX_TAKEN_DATE);
			holder.mMimetype = cursor.getString(INDEX_MIME_TYPE);
			holder.mData = cursor.getString(INDEX_DATA);
			holder.mFileSize = cursor.getLong(INDEX_FILE_SIZE);
			holder.mDuration = cursor.getLong(INDEX_DRUATION);
			holder.mIsDrm = VALUE_IS_DRM.equals(cursor.getString(INDEX_IS_DRM));
			holder.mDateModified = cursor.getLong(INDEX_DATE_MODIFIED);
			//holder.mSupport3D = MtkUtils.isStereo3D(cursor
					//.getInt(INDEX_SUPPORT_3D));

			holder.mTitleView.setText(holder.mTitle);

			if (Flag_Clickable == false) {
				if (mArrayClicked[cursor.getPosition()] == 0) {

					holder.mImageView_Blue.setVisibility(View.GONE);
					holder.mImageView_grey.setVisibility(View.VISIBLE);

				}
				if (mArrayClicked[cursor.getPosition()] == 1) {

					holder.mImageView_Blue.setVisibility(View.VISIBLE);
					holder.mImageView_grey.setVisibility(View.GONE);

				}

			}
			if (Flag_Clickable == true) {
				holder.mImageView_Blue.setVisibility(View.GONE);
				holder.mImageView_grey.setVisibility(View.GONE);
			}

			// holder.mFileSizeView.setText(mCachedVideoInfo.getFileSize(
			// MoviesContentAcitivity.this, holder.mFileSize));
			holder.mDurationView.setText(mCachedVideoInfo
					.getDuration(holder.mDuration));

			String path = VideoApplication.mThumbnailPath.get(holder.mId);
			if (path != null) {
				Uri uri = Uri.fromFile(new File(path));
				holder.mIcon.setImageURI(uri);
			} else {
				Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(
						mContext.getContentResolver(), holder.mId, 1999,
						MediaStore.Video.Thumbnails.MICRO_KIND, null);
				holder.mIcon.setImageBitmap(bitmap);
			}

			// holder.mIcon.setImageURI(uri);
			// refreshThumbnail(holder);
			MtkLog.v(TAG, "bindeView() " + holder);
		}

		@Override
		public void changeCursor(final Cursor c) {
			Log.e(TAG, "changeCursor---->>" + c);
			super.changeCursor(c);
		}

		@Override
		protected void onContentChanged() {
			Log.e(TAG, "onContentChanged:--->>" + getCursor());

			mQueryHandler.onQueryComplete(0, null, getCursor());
			super.onContentChanged();

		}

		class QueryHandler extends AsyncQueryHandler {

			QueryHandler(final ContentResolver cr) {
				super(cr);
			}

			@Override
			protected void onQueryComplete(final int token,
					final Object cookie, final Cursor cursor) {
				MtkLog.v(TAG, "onQueryComplete(" + token + "," + cookie + ","
						+ cursor + ")");
				MtkUtils.disableSpinnerState(MoviesContentAcitivity.this);
				if (cursor == null || cursor.getCount() == 0) {
					// 当数据库没有找到数据时，或cursor找不到
					if (cursor == null) {
						final AlertDialog.Builder builder = new Builder(
								MoviesContentAcitivity.this);
						builder.setTitle("温馨提示：");
						builder.setMessage("视频文件查询失败");
						builder.setPositiveButton("确定", new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								finish();
							}
						});
						builder.create().show();
					}
					if (cursor != null) { // to observe database change

						changeCursor(cursor);

					}
				} else {

					changeCursor(cursor);
				}
				if (cursor != null) {
					MtkLog.v(TAG, "onQueryComplete() end");
				}
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

						notifyDataSetChanged();
						Toast.makeText(MoviesContentAcitivity.this, "删除完成",
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		@Override
		public void onScroll(final AbsListView view,
				final int firstVisibleItem, final int visibleItemCount,
				final int totalItemCount) {

		}

		private boolean mFling = false;

		@Override
		public void onScrollStateChanged(final AbsListView view,
				final int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_IDLE:
				mFling = false;
				// notify data changed to load bitmap from mediastore.
				notifyDataSetChanged();

				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				mFling = false;
				break;
			case OnScrollListener.SCROLL_STATE_FLING:
				mFling = true;
				break;
			default:
				break;
			}
			MtkLog.v(TAG, "onScrollStateChanged(" + scrollState + ") mFling="
					+ mFling);
		}
	}

	public class ViewHolder {
		long mId;
		String mTitle;
		String mMimetype;
		String mData;
		long mDuration;
		long mDateTaken;
		long mFileSize;
		boolean mIsDrm;
		long mDateModified;
		boolean mSupport3D;
		SimpleDraweeView mIcon;
		ImageView mImageView_grey;
		ImageView mImageView_Blue;
		TextView mTitleView;
		TextView mFileSizeView;
		TextView mDurationView;
		FastBitmapDrawable mFastDrawable;

		@Override
		public String toString() {
			return new StringBuilder().append("ViewHolder(mId=").append(mId)
					.append(", mTitle=").append(mTitle).append(", mDuration=")
					.append(mDuration).append(", mIsDrm=").append(mIsDrm)
					.append(", mData=").append(mData).append(", mDateModified")
					.append(mDateModified).append(", mFileSize=")
					.append(mFileSize).append(", mSupport3D=")
					.append(mSupport3D).append(")").toString();
		}

		/**
		 * just clone info
		 */
		@Override
		protected ViewHolder clone() {
			final ViewHolder holder = new ViewHolder();
			holder.mId = mId;
			holder.mTitle = mTitle;
			holder.mMimetype = mMimetype;
			holder.mData = mData;
			holder.mDuration = mDuration;
			holder.mDateTaken = mDateTaken;
			holder.mFileSize = mFileSize;
			holder.mIsDrm = mIsDrm;
			holder.mDateModified = mDateModified;
			holder.mSupport3D = mSupport3D;

			return holder;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mStorageListener);
		if (db != null) {
			db.close();
			db = null;
		}
		mGridLongClicked = true;
		this.getContentResolver().unregisterContentObserver(mObserver);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.activity_movies_content_back:
			finish();
			break;
		case R.id.activity_movies_content_backarrow:
			finish();
			break;
		case R.id.activity_movies_content_edit:

			mEdit.setVisibility(View.GONE);
			mCancle.setVisibility(View.VISIBLE);
			mLinearLayout.setVisibility(View.VISIBLE);
			mCancelSelected.setVisibility(View.GONE);
			mSeltect.setVisibility(View.VISIBLE);
			Flag_Clickable = false;
			mGridLongClicked = false;
			count = 0;
			mArrayClicked = new int[mAdapter.getCount()];
			mBase_ID = new Long[mAdapter.getCount()];
			for (int i = 0; i < mAdapter.getCount(); i++) {
				mArrayClicked[i] = 0;
				mBase_ID[i] = -1L;
			}
			// 将MediaID存入数组；
			Cursor cursor = mAdapter.getCursor();
			cursor.moveToFirst();
			int j = 0;
			do {
				mBase_ID[j++] = cursor.getLong(INDEX_ID);
			} while (cursor.moveToNext());

			mAdapter.notifyDataSetChanged();
			Log.e(TAG, "R.id.activity_movies_content_edit:count--->" + count);
			mDelete.setText(this.getResources().getText(
					R.string.activity_moviescontent_delete)
					+ " ( " + count + " )");

			break;
		case R.id.activity_movies_content_cancle:

			mCancle.setVisibility(View.GONE);
			mEdit.setVisibility(View.VISIBLE);
			mLinearLayout.setVisibility(View.GONE);
			mCancelSelected.setVisibility(View.GONE);
			mSeltect.setVisibility(View.VISIBLE);
			Flag_Clickable = true;
			mGridLongClicked = true;
			count = 0;

			mArrayClicked = new int[mAdapter.getCount()];
			mBase_ID = new Long[mAdapter.getCount()];
			for (int i = 0; i < mAdapter.getCount(); i++) {
				mArrayClicked[i] = 0;
				mBase_ID[i] = -1L;
			}
			mAdapter.notifyDataSetChanged();

			Log.e(TAG, "R.id.activity_movies_content_edit:count--->" + count);
			mDelete.setText(this.getResources().getText(
					R.string.activity_moviescontent_delete)
					+ " ( " + count + " )");

		case R.id.activity_moviescontent_allselceted:

			count = mAdapter.getCount();
			mArrayClicked = new int[mAdapter.getCount()];
			for (int i = 0; i < mAdapter.getCount(); i++) {
				mArrayClicked[i] = 1;
			}
			mAdapter.notifyDataSetChanged();
			mCancelSelected.setVisibility(View.VISIBLE);
			mSeltect.setVisibility(View.GONE);

			Log.e(TAG, "R.id.activity_moviescontent_allselceted:count--->"
					+ count);
			mDelete.setText(this.getResources().getText(
					R.string.activity_moviescontent_delete)
					+ " ( " + count + " )");

			break;
		case R.id.activity_moviescontent_cancleaselceted:

			count = 0;

			mArrayClicked = new int[mAdapter.getCount()];
			for (int i = 0; i < mAdapter.getCount(); i++) {
				mArrayClicked[i] = 0;
			}
			mAdapter.notifyDataSetChanged();
			mSeltect.setVisibility(View.VISIBLE);
			mCancelSelected.setVisibility(View.GONE);

			Log.e(TAG, "R.id.activity_moviescontent_cancleaselceted:count--->"
					+ count);
			mDelete.setText(this.getResources().getText(
					R.string.activity_moviescontent_delete)
					+ " ( " + count + " )");

			break;
		case R.id.activity_movicescontent_delete:

			if (count == 0) {
				Toast.makeText(MoviesContentAcitivity.this, "尚未选中删除条目",
						Toast.LENGTH_SHORT).show();
			} else {

				AlertDialog.Builder builder = new Builder(
						MoviesContentAcitivity.this);
				builder.setTitle("温馨提示");
				builder.setMessage("确认删除" + count + "条记录？");
				builder.setPositiveButton("确认", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

						mmProgressDialog = new ProgressDialog(
								MoviesContentAcitivity.this);
						mmProgressDialog.setMessage("正在删除,请稍等...");
						mmProgressDialog.show();
						for (int i = 0; i < mAdapter.getCount(); i++) {
							if (mArrayClicked[i] == 1) {
								// long _ID = -1L;
								mAdapter.getQueryHandler().startDelete(0, null,
										VIDEO_URI, BaseColumns._ID + " = ?",
										new String[] { "" + mBase_ID[i] });
								mDeleteCount++;
								Log.e(TAG,
										"startDelete:mDeleteCount------->>>>"
												+ mDeleteCount);
								// _ID = mFindRecordByID(mBase_ID[i]);
								// if (_ID > -1) {
								// videoScanRecordsDao.deleteByKey(_ID);
								// }
							}
						}
					}
				});
				builder.setNegativeButton("取消", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
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

	public void setIntoEdit(int position) {
		mEdit.setVisibility(View.GONE);
		mCancle.setVisibility(View.VISIBLE);
		mLinearLayout.setVisibility(View.VISIBLE);
		mCancelSelected.setVisibility(View.GONE);
		mSeltect.setVisibility(View.VISIBLE);
		Flag_Clickable = false;
		mGridLongClicked = false;
		count = 0;
		mArrayClicked = new int[mAdapter.getCount()];
		mBase_ID = new Long[mAdapter.getCount()];
		for (int i = 0; i < mAdapter.getCount(); i++) {
			mArrayClicked[i] = 0;
			if (i == position) {
				mArrayClicked[i] = 1;
				count++;
			}
		}
		// 将MediaID存入数组；
		Cursor cursor = mAdapter.getCursor();
		cursor.moveToFirst();
		int j = 0;
		do {
			mBase_ID[j++] = cursor.getLong(INDEX_ID);
		} while (cursor.moveToNext());
		mDelete.setText(this.getResources().getText(
				R.string.activity_moviescontent_delete)
				+ " ( " + count + " )");
	}

	// 取消全选后操作
	public void setCancleAllDelete() {
		mCancelSelected.setVisibility(View.GONE);
		mSeltect.setVisibility(View.VISIBLE);
	}

	// // 根据mBase_ID值查询ID值；
	// private long mFindRecordByID(Long mBase_ID) {
	// Log.e(TAG, "mFindRecordByID()---------------->>" + mBase_ID);
	// long _id = -1;
	// QueryBuilder<VideoScanRecords> qb = videoScanRecordsDao.queryBuilder();
	// qb.where(VideoScanRecordsDao.Properties.Base_ID.eq(mBase_ID));
	// if (qb.buildCount().count() > 0) {
	// _id = qb.list().get(0).getId();
	// }
	// return _id;
	//
	// }

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.e(TAG, "onResume()::------>>>>>>>");

		Cursor c = getContentResolver().query(
				MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, null, null,
				null, null);
		c.moveToFirst();
		VideoApplication.mThumbnailPath.clear();
		while (!c.isAfterLast()) {
			long id = c.getLong(c
					.getColumnIndex(MediaStore.Video.Thumbnails.VIDEO_ID));
			String path = c.getString(c
					.getColumnIndex(MediaStore.Video.Thumbnails.DATA));
			Log.e("NTVideoPlayer", "id " + id + " path " + path);

			VideoApplication.mThumbnailPath.put(id, path);
			c.moveToNext();
		}
		Log.e(TAG, "c.size()::::::::" + c.getCount());
		c.close();
		c = null;
		Log.e(TAG, "onResume:END!!!");

	}

	private final class VideoObserver extends ContentObserver {

		public VideoObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			super.onChange(selfChange);
			Log.e(TAG, "onChanger-------------------..........>>>>");
			if (Flag_Clickable == false) {
				mCancle.setVisibility(View.GONE);
				mEdit.setVisibility(View.VISIBLE);
				mLinearLayout.setVisibility(View.GONE);
				mCancelSelected.setVisibility(View.GONE);
				mSeltect.setVisibility(View.VISIBLE);
				Flag_Clickable = true;
				mGridLongClicked = true;
				count = 0;
				for (int i = 0; i < mAdapter.getCount(); i++) {
					mArrayClicked[i] = 0;
					mBase_ID[i] = -1L;
				}
				mAdapter.notifyDataSetChanged();

				Log.e(TAG, "R.id.activity_movies_content_edit:count--->"
						+ count);
				mDelete.setText(MoviesContentAcitivity.this.getResources()
						.getText(R.string.activity_moviescontent_delete)
						+ " ( " + count + " )");

			}
			Cursor c = getContentResolver().query(
					MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, null,
					null, null, null);
			c.moveToFirst();
			VideoApplication.mThumbnailPath.clear();
			while (!c.isAfterLast()) {
				long id = c.getLong(c
						.getColumnIndex(MediaStore.Video.Thumbnails.VIDEO_ID));
				String path = c.getString(c
						.getColumnIndex(MediaStore.Video.Thumbnails.DATA));
				Log.d("NTVideoPlayer", "id " + id + " path " + path);

				VideoApplication.mThumbnailPath.put(id, path);
				c.moveToNext();
			}
			c.close();

		}
	}

}
