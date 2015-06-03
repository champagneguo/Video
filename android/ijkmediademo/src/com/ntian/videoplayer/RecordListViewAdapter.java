package com.ntian.videoplayer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Media;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

public class RecordListViewAdapter extends BaseAdapter implements
		OnScrollListener {

	private String TAG = "RecordListViewAdapter";
	private Context mContext;
	private ArrayList<ArrayList<HashMap<String, Object>>> mArrayLists = new ArrayList<ArrayList<HashMap<String, Object>>>();
	private final static Long OneDayMillSeconds = 86400000L;
	private long Time_flag_sqlite = 0L;
	private long Time_flag_now = 0L;
	public static boolean RecordListViewAdapter_Flag = false;

	private static final Uri VIDEO_URI = Media.EXTERNAL_CONTENT_URI;
	private static final String KEY_LOGO_BITMAP = "logo-bitmap";
	private static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
	private static final String EXTRA_ALL_VIDEO_FOLDER = "mediatek.intent.extra.ALL_VIDEO_FOLDER";
	private static final String EXTRA_ENABLE_VIDEO_LIST = "mediatek.intent.extra.ENABLE_VIDEO_LIST";
	private static String TAG1 = "MyGridViewAdapter";
	private RecentRecordActivity mRecentRecordActivity;

	public RecordListViewAdapter(RecentRecordActivity recentRecordActivity,
			ArrayList<ArrayList<HashMap<String, Object>>> mArrayList) {
		// TODO Auto-generated constructor stub
		mContext = recentRecordActivity;
		this.mArrayLists = mArrayList;
		mRecentRecordActivity = (RecentRecordActivity) recentRecordActivity;
	}

	public void setArrayList(
			ArrayList<ArrayList<HashMap<String, Object>>> mArrayLists) {

		this.mArrayLists = mArrayLists;

	}

	@Override
	public void notifyDataSetChanged() {
		// TODO Auto-generated method stub
		super.notifyDataSetChanged();
		if (isEmpty()) {
			mRecentRecordActivity.setNoRecordVisibility();

		} else {
			mRecentRecordActivity.setRecordVisibility();
		}
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub

		return mArrayLists.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mArrayLists.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public View getView(final int parent_position, View convertView,
			ViewGroup parent) {
		Log.e(TAG, "RecordListViewAdapter:getView------------"
				+ parent_position);
		// TODO Auto-generated method stub
		Viewholder1 viewholder = null;
		if (convertView == null) {
			viewholder = new Viewholder1();
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.activity_recentrecords_lv, null, false);
			viewholder.mTextView = (TextView) convertView
					.findViewById(R.id.activity_recentrecords_lv_time);
			viewholder.mGridView = (MyGridView) convertView
					.findViewById(R.id.activity_recentrecords_lv_gv);
			convertView.setTag(viewholder);
		} else {
			viewholder = (Viewholder1) convertView.getTag();
		}
		Date date1 = (Date) mArrayLists.get(parent_position).get(0).get("Time");

		if (parent_position == 0) {
			Time_flag_sqlite = date1.getTime() / OneDayMillSeconds;
			Time_flag_now = (new Date().getTime()) / OneDayMillSeconds;
			if (Time_flag_sqlite == Time_flag_now) {
				viewholder.mTextView.setText("今天");
			} else {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				String date = df.format(date1);
				viewholder.mTextView.setText(date);
			}
		} else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			String date = df.format(date1);
			viewholder.mTextView.setText(date);
		}

		MyGridViewAdapter myGridViewAdapter = new MyGridViewAdapter(mContext,
				mArrayLists.get(parent_position), parent_position);
		viewholder.mGridView.setAdapter(myGridViewAdapter);
		// GridView单击事件
		viewholder.mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.e(TAG1, "onItemClick------------position>>>" + position);

				if (RecentRecordActivity.Flag_Clickable == true) {
					final Object o = view.getTag();
					ViewHolder holder = null;
					if (o instanceof ViewHolder) {
						holder = (ViewHolder) o;
						final Intent intent = new Intent(Intent.ACTION_VIEW);
						String mime = "video/*";
						if (!(holder.mMimetype == null || ""
								.equals(holder.mMimetype.trim()))) {
							mime = holder.mMimetype;
						}
						intent.setDataAndType(ContentUris.withAppendedId(
								VIDEO_URI, holder.mId), mime);
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
						intent.putExtra(KEY_LOGO_BITMAP, BitmapFactory
								.decodeResource(mContext.getResources(),
										R.drawable.ic_video_app));
						intent.setComponent(new ComponentName(
								"com.ntian.videoplayer",
								"com.ntian.videoplayer.VideoPlayerActivity")); // add
																				// by
																				// ChMX
						try {
							mContext.startActivity(intent);
						} catch (final ActivityNotFoundException e) {
							e.printStackTrace();
						}
					}

				} else if (RecentRecordActivity.Flag_Clickable == false) {

					final Object o = view.getTag();
					ViewHolder holder = null;
					if (o instanceof ViewHolder) {

						holder = (ViewHolder) o;

						if (RecentRecordActivity.mArrayClickeds.get(
								parent_position).get(position) == 1) {
							Log.e(TAG1,
									"RecentRecordActivity.Flag_Clickable == false----"
											+ position);
							RecentRecordActivity.mArrayClickeds.get(
									parent_position).set(position, 0);
							holder.mImageView_Blue.setVisibility(View.GONE);
							holder.mImageView_grey.setVisibility(View.VISIBLE);
							RecentRecordActivity.count--;
							mRecentRecordActivity.setCount();
						} else if (RecentRecordActivity.mArrayClickeds.get(
								parent_position).get(position) == 0) {
							RecentRecordActivity.mArrayClickeds.get(
									parent_position).set(position, 1);
							holder.mImageView_Blue.setVisibility(View.VISIBLE);
							holder.mImageView_grey.setVisibility(View.GONE);
							RecentRecordActivity.count++;
							mRecentRecordActivity.setCount();
						}
						if (RecentRecordActivity.count == (RecentRecordActivity.RecordList_Count - 1)) {
							mRecentRecordActivity.setCancleAllDelete();
						}

					}

				}
			}
		});
		viewholder.mGridView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						// TODO Auto-generated method stub
						if (RecentRecordActivity.mGridLongClicked == true) {
							mRecentRecordActivity.setIntoEdit(parent_position,
									position);

							final Object o = view.getTag();
							ViewHolder holder = null;
							if (o instanceof ViewHolder) {

								holder = (ViewHolder) o;

								if (RecentRecordActivity.mArrayClickeds.get(
										parent_position).get(position) == 1) {
									Log.e(TAG1,
											"RecentRecordActivity.Flag_Clickable == false----"
													+ position);

									holder.mImageView_Blue
											.setVisibility(View.VISIBLE);
									holder.mImageView_grey
											.setVisibility(View.GONE);

								}
								if (RecentRecordActivity.count == (RecentRecordActivity.RecordList_Count - 1)) {
									mRecentRecordActivity.setCancleAllDelete();
								}

							}
							notifyDataSetChanged();

						}
						if (RecentRecordActivity.mGridLongClicked == false) {
							Log.e(TAG,
									"RecentRecordActivity.mGridLongClicked------false");

						}

						return true;
					}
				});
		return convertView;

	}

	class Viewholder1 {
		private TextView mTextView;
		private MyGridView mGridView;
	}

	private boolean mFling = false;

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
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

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub

	}

	class MyGridViewAdapter extends BaseAdapter {

		private static final int TASK_GROUP_ID = 1999;// just a number

		private CachedVideoInfo mCachedVideoInfo;
		private Context mContext;
		private ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
		private final ArrayList<ViewHolder> mCachedHolder = new ArrayList<ViewHolder>();
		private int parent_position_1;

		public MyGridViewAdapter(Context mContext,
				ArrayList<HashMap<String, Object>> arrayList, int parent_positon) {

			this.mContext = mContext;
			this.arrayList = arrayList;
			mCachedVideoInfo = new CachedVideoInfo();
			this.parent_position_1 = parent_positon;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return arrayList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return arrayList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			Log.e(TAG1, "MyGridViewAdapter:getView----------" + position);
			ViewHolder viewholder = null;
			if (convertView == null) {
				viewholder = new ViewHolder();
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.activity_recentrecords_lv_gv, null, false);
				viewholder.mIcon = (SimpleDraweeView) convertView
						.findViewById(R.id.activity_recentrecords_lv_gv_image);
				viewholder.mTitleView = (TextView) convertView
						.findViewById(R.id.activity_recentrecords_lv_gv_name);
				viewholder.mDurationView = (TextView) convertView
						.findViewById(R.id.activity_recentrecords_lv_gv_time);
				viewholder.mImageView_Blue = (ImageView) convertView
						.findViewById(R.id.activity_recentrecords_lv_gv_image_sel);
				viewholder.mImageView_grey = (ImageView) convertView
						.findViewById(R.id.activity_recentrecords_lv_gv_image_nor);
				convertView.setTag(viewholder);
				mCachedHolder.add(viewholder);
			} else {
				viewholder = (ViewHolder) convertView.getTag();
			}

			viewholder.mId = (Long) arrayList.get(position).get("mBase_ID");
			viewholder.mData = (String) arrayList.get(position).get(
					"video_path");
			viewholder.mDateModified = (Long) arrayList.get(position).get(
					"mDateModified");
			viewholder.mSupport3D = (Boolean) arrayList.get(position).get(
					"mSupport3D");
			viewholder.mIsDrm = (Boolean) arrayList.get(position).get("mIsDrm");
			viewholder.mDuration = (Long) arrayList.get(position).get(
					"mDuration");
			viewholder.mTitle = (String) arrayList.get(position).get("mTitle");
			viewholder.mFastDrawable = new FastBitmapDrawable(155, 98);
			viewholder.mTitleView.setText(viewholder.mTitle);
			viewholder.mDurationView.setText(mCachedVideoInfo
					.getDuration(viewholder.mDuration));

			String path = VideoApplication.mThumbnailPath.get(viewholder.mId);
			if (path != null) {
				Uri uri = Uri.fromFile(new File(path));
				viewholder.mIcon.setImageURI(uri);
			} else {
				Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(
						mContext.getContentResolver(), viewholder.mId,
						TASK_GROUP_ID, MediaStore.Video.Thumbnails.MICRO_KIND,
						null);
				viewholder.mIcon.setImageBitmap(bitmap);
			}

			if (RecentRecordActivity.Flag_Clickable == false) {
				if (RecentRecordActivity.mArrayClickeds.get(parent_position_1)
						.get(position) == 0) {

					viewholder.mImageView_Blue.setVisibility(View.GONE);
					viewholder.mImageView_grey.setVisibility(View.VISIBLE);

				}
				if (RecentRecordActivity.mArrayClickeds.get(parent_position_1)
						.get(position) == 1) {

					viewholder.mImageView_Blue.setVisibility(View.VISIBLE);
					viewholder.mImageView_grey.setVisibility(View.GONE);

				}

			}
			if (RecentRecordActivity.Flag_Clickable == true) {
				viewholder.mImageView_Blue.setVisibility(View.GONE);
				viewholder.mImageView_grey.setVisibility(View.GONE);
			}

			return convertView;
		}

	}

	class ViewHolder {

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
	}

}
