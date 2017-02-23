package com.metaisle.weik.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.app.StatusPagerActivity;
import com.metaisle.weik.app.UserActivity;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.data.UserTable;
import com.metaisle.weik.weibo.GetTimelineTask;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;
import com.metaisle.weik.weibo.TaskHandler;
import com.nostra13.universalimageloader.core.ImageLoader;

public class TimelineFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor>, OnScrollListener, IRefreshable {
	public SharedPreferences mPrefs;
	private TimelineType mType;

	private long mUserId;

	public static final String KEY_TIMELINE_TYPE = "key_timeline_type";
	public static final String KEY_USER_ID = "key_user_id";

	private SimpleCursorAdapter mAdapter;
	public ImageLoader imageLoader;

	private PullToRefreshListView mPtrListView;
	private ListView mListView;

	private static int LOADER_ALL = 0;
	private static int LOADER_CACHED_ONLY = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = Prefs.imageLoader(getActivity());

		mType = TimelineType.HOME;
		Bundle b = getArguments();
		if (b != null) {
			mType = (TimelineType) b.getSerializable(KEY_TIMELINE_TYPE);
			Util.log("Timeline type: " + mType);
			mUserId = b.getLong(KEY_USER_ID);
			Util.log("User ID: " + mUserId);
		}

		getLoaderManager().initLoader(LOADER_ALL, null, this);
	}

	public boolean cachedOnly = false;

	public boolean toggleCachedOnly() {
		if (cachedOnly) {
			cachedOnly = false;
			getLoaderManager().initLoader(LOADER_ALL, null, this);
		} else {
			cachedOnly = true;
			getLoaderManager().initLoader(LOADER_CACHED_ONLY, null, this);
		}
		return cachedOnly;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = null;
		String[] selectionArg = null;

		switch (mType) {
		case HOME:
			selection = TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.IS_HOME + "=?";
			selectionArg = new String[] { "1" };
			break;
		case MENTION:
			selection = TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.IS_MENTION + "=?";
			selectionArg = new String[] { "1" };
			break;
		case FAVORITE:
			selection = TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.FAVORITED + "=?";
			selectionArg = new String[] { "1" };
			break;
		case USER:
			selection = TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.USER_TIMELINE + "=?";
			selectionArg = new String[] { String.valueOf(mUserId) };
			break;

		default:
			throw new IllegalArgumentException("Unknown Fragment type. ");
		}

		if (id == LOADER_CACHED_ONLY) {
			selection = selection + " AND " + TimelineTable.TIMELINE_TABLE
					+ "." + TimelineTable.CACHED_AT + " NOT NULL";
		}

		// TimelineTable.STATUS_ID has to be index 1!!!
		return new CursorLoader(getActivity(), Provider.TIMELINE_CONTENT_URI,
				new String[] {
						TimelineTable.TIMELINE_TABLE + "." + TimelineTable._ID,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.AUTHOR_ID,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.STATUS_ID,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.STATUS_TEXT,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.CREATED_AT,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.RETWEETED_STATUS,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.THUMBNAIL_PIC,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.COMMENTS_COUNT,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.REPOSTS_COUNT,
						UserTable.USER_TABLE + "."
								+ UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_TABLE + "." + UserTable.USER_NAME,

						"RT." + TimelineTable.AUTHOR_ID + " AS RT_"
								+ TimelineTable.AUTHOR_ID,
						"RT." + TimelineTable.STATUS_ID + " AS RT_"
								+ TimelineTable.STATUS_ID,
						"RT." + TimelineTable.STATUS_TEXT + " AS RT_"
								+ TimelineTable.STATUS_TEXT,
						"RT." + TimelineTable.CREATED_AT + " AS RT_"
								+ TimelineTable.CREATED_AT,
						"RT." + TimelineTable.THUMBNAIL_PIC + " AS RT_"
								+ TimelineTable.THUMBNAIL_PIC,
						"RT_USER." + UserTable.PROFILE_IMAGE_URL
								+ " AS RT_USER_" + UserTable.PROFILE_IMAGE_URL,
						"RT_USER." + UserTable.USER_NAME + " AS RT_USER_"
								+ UserTable.USER_NAME

				}, selection, selectionArg, TimelineTable.TIMELINE_TABLE + "."
						+ TimelineTable.CREATED_AT + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Util.log("cursor " + data.getCount());
		mAdapter.swapCursor(data);
		mListView.setOnScrollListener(this);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mPtrListView = (PullToRefreshListView) inflater.inflate(
				R.layout.fragment_timeline, null);

		mListView = mPtrListView.getRefreshableView();
		// ------------------
		FrameLayout fl = (FrameLayout) inflater.inflate(
				R.layout.footer_loading, null);
		mListView.addFooterView(fl);
		// ------------------
		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.fragment_timeline_item, null,

				new String[] { UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_NAME, TimelineTable.STATUS_TEXT,
						TimelineTable.CREATED_AT,
						TimelineTable.RETWEETED_STATUS,
						TimelineTable.THUMBNAIL_PIC,
						"RT_USER_" + UserTable.PROFILE_IMAGE_URL,
						"RT_USER_" + UserTable.USER_NAME,
						"RT_" + TimelineTable.STATUS_TEXT,
						"RT_" + TimelineTable.THUMBNAIL_PIC,
						"RT_" + TimelineTable.CREATED_AT,
						TimelineTable.COMMENTS_COUNT,
						TimelineTable.REPOSTS_COUNT },

				new int[] { R.id.profile_image, R.id.user_name,
						R.id.status_text, R.id.time_span,
						R.id.retweet_container, R.id.status_pic,
						R.id.rt_profile_image, R.id.rt_user_name,
						R.id.rt_status_text, R.id.rt_status_pic,
						R.id.rt_time_span, R.id.comment_counter,
						R.id.repost_counter }, 0);

		mAdapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {

				// -------------------------------------------------
				if (view.getId() == R.id.profile_image) {
					final long uid = cursor.getLong(cursor
							.getColumnIndex(TimelineTable.AUTHOR_ID));
					view.setTag(R.id.TAG_USER_ID, uid);
					imageLoader.displayImage(cursor.getString(columnIndex),
							(ImageView) view);

					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent i = new Intent(getActivity(),
									UserActivity.class);
							i.putExtra(UserActivity.KEY_USER_ID, uid);
							startActivity(i);
						}
					});

					return true;
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.time_span
						|| view.getId() == R.id.rt_time_span) {
					long time = cursor.getLong(columnIndex);
					((TextView) view).setText(DateUtils
							.getRelativeTimeSpanString(time));
					return true;
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.status_pic
						|| view.getId() == R.id.rt_status_pic) {

					((View) view.getParent()).setTag(R.id.TAG_STATUS_ID, cursor
							.getLong(cursor
									.getColumnIndex(TimelineTable.STATUS_ID)));

					// ViewGroup.LayoutParams lp = view.getLayoutParams();
					// lp.height = thumbSize;
					// lp.width = thumbSize;
					// view.setLayoutParams(lp);
					String url = cursor.getString(columnIndex);
					if (url == null) {
						view.setVisibility(View.GONE);
					} else {
						view.setVisibility(View.VISIBLE);
						imageLoader.displayImage(url, (ImageView) view);
					}
					return true;
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.retweet_container) {
					if (cursor.isNull(columnIndex)) {
						view.setVisibility(View.GONE);
					} else {
						view.setVisibility(View.VISIBLE);
					}
					return true;
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.rt_profile_image) {
					final long uid = cursor.getLong(cursor.getColumnIndex("RT_"
							+ TimelineTable.AUTHOR_ID));
					view.setTag(R.id.TAG_USER_ID, uid);
					imageLoader.displayImage(cursor.getString(columnIndex),
							(ImageView) view);

					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent i = new Intent(getActivity(),
									UserActivity.class);
							i.putExtra(UserActivity.KEY_USER_ID, uid);
							startActivity(i);
						}
					});

					return true;
				}

				else if (view.getId() == R.id.user_name
						|| view.getId() == R.id.rt_user_name) {
					((TextView) view).getPaint().setFakeBoldText(true);
				}

				return false;
			}
		});

		mListView.setAdapter(mAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position,
					long id) {
				// Util.log("click " + v.getId() + " id " + id);
				if (v.getTag(R.id.TAG_STATUS_ID) == null)
					return;
				long status_id = (Long) v.getTag(R.id.TAG_STATUS_ID);
				Util.log("click " + status_id);
				Util.profile(getActivity(), "status_view.csv", "" + status_id
						+ ", click");
				Intent i = new Intent(getActivity(), StatusPagerActivity.class);
				i.putExtra(StatusPagerActivity.KEY_TIMELINE_TYPE, mType);
				i.putExtra(StatusPagerActivity.KEY_USER_ID, mUserId);
				i.putExtra(StatusPagerActivity.KEY_STATUS_ID, status_id);
				startActivityForResult(i, REQUEST_CODE_STATUS);
			}
		});

		mPtrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {

				Cursor c = mAdapter.getCursor();

				Util.log("Cursor count " + c.getCount());

				if (c == null || c.getCount() < 1) {
					mTask = new GetTimelineTask(getActivity(), mType)
							.setHandler(new TimelineHandler(getActivity(),
									mPtrListView));
					mTask.execute();
					return;
				}

				// Util.log("refresh");

				c.moveToFirst();
				long status_id = c.getLong(c
						.getColumnIndex(TimelineTable.STATUS_ID));
				Util.log("Refresh, since id " + status_id);

				mTask = new GetTimelineTask(getActivity(), mType).setSinceID(
						status_id).setHandler(
						new TimelineHandler(getActivity(), mPtrListView));
				mTask.execute();

			}
		});

		return mPtrListView;
	}

	private class TimelineHandler extends TaskHandler {
		PullToRefreshListView mPtr;

		public TimelineHandler(Context context, PullToRefreshListView ptr) {
			super(context);
			mPtr = ptr;
		}

		@Override
		protected void RunSuccess() {
			mPtr.onRefreshComplete();
		}

	}

	private GetTimelineTask mTask = null;

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// Util.log("firstVisibleItem " + firstVisibleItem +
		// " visibleItemCount "
		// + visibleItemCount + " totalItemCount " + totalItemCount);

		boolean loadMore = /* maybe add a padding */
		firstVisibleItem + visibleItemCount >= totalItemCount;

		if (loadMore
				&& (mTask == null || mTask.getStatus() != AsyncTask.Status.RUNNING)) {
			// FooterView being the last one.
			Cursor c = mAdapter.getCursor();

			if (c == null || c.getCount() == 0) {
				return;
			}

			Util.log("Cursor count " + c.getCount());

			c.moveToLast();
			long status_id = c.getLong(c
					.getColumnIndex(TimelineTable.STATUS_ID));
			Util.log("Load more, last id " + status_id);

			if (mType == TimelineType.USER) {
				mTask = new GetTimelineTask(getActivity(), mType, mUserId)
						.setMaxID(status_id);
				mTask.execute();
			} else {
				mTask = new GetTimelineTask(getActivity(), mType)
						.setMaxID(status_id);
				mTask.execute();
			}
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	public static final int REQUEST_CODE_STATUS = 0x678;
	public static final String KEY_POSITION = "KEY_POSITION";

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Util.log("onResult " + requestCode + " " + resultCode);

		if (requestCode == REQUEST_CODE_STATUS
				&& resultCode == Activity.RESULT_OK) {
			int pos = data.getIntExtra(KEY_POSITION, -1);
			if (pos >= 0) {
				// Util.log("pos " + pos);
				mListView.setSelection(pos + 1);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void refresh() {
		if (mPtrListView != null) {

			Cursor c = mAdapter.getCursor();

			Util.log("Cursor count " + c.getCount());

			if (c == null || c.getCount() < 1) {
				new GetTimelineTask(getActivity(), mType)
						.setHandler(
								new TimelineHandler(getActivity(), mPtrListView))
						.setHandler(
								new TimelineHandler(getActivity(), mPtrListView))
						.execute();
				return;
			}

			// Util.log("refresh");

			c.moveToFirst();
			long status_id = c.getLong(c
					.getColumnIndex(TimelineTable.STATUS_ID));
			Util.log("Refresh, since id " + status_id);

			new GetTimelineTask(getActivity(), mType).setSinceID(status_id)
					.setIsShowingNoNew(true).execute();

			mListView.setSelection(0);
		}
	}
}
