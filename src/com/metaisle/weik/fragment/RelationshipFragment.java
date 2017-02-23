package com.metaisle.weik.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.app.UserActivity;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.RelationshipTable;
import com.metaisle.weik.data.UserTable;
import com.metaisle.weik.weibo.GetRelationshipTask;
import com.metaisle.weik.weibo.TaskHandler;
import com.nostra13.universalimageloader.core.ImageLoader;

public class RelationshipFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor>, OnScrollListener, IRefreshable {

	public SharedPreferences mPrefs;

	private long mUserID;

	public static final String KEY_USER_ID = "KEY_USER_ID";

	private int CURRENT_LOADER = LOADER_FRIENDS;
	public static final int LOADER_FRIENDS = 0x101;
	public static final int LOADER_FOLLOWERS = 0x102;

	private SimpleCursorAdapter mAdapter;
	public ImageLoader imageLoader;

	private PullToRefreshListView mPtrListView;
	private ListView mListView;

	private Button mFriendsButton;
	private Button mFollowerButton;

	private GetRelationshipTask mTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = Prefs.imageLoader(getActivity());

		Bundle b = getArguments();
		if (b != null) {
			Util.log(b.toString());
			mUserID = b.getLong(KEY_USER_ID, -1);
			Util.log("mUserID: " + mUserID);
		}
		if (mUserID < 0) {
			mUserID = mPrefs.getLong(Prefs.KEY_UID, -1);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		String selection = null;
		String[] selectionArgs = null;
		Uri uri = null;
		if (id == LOADER_FOLLOWERS) {
			selection = RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FOLLOWEE + "=?";
			selectionArgs = new String[] { String.valueOf(mUserID) };
			uri = Provider.FOLLOWER_CONTENT_URI;
		} else if (id == LOADER_FRIENDS) {
			selection = RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FOLLOWER + "=?";
			selectionArgs = new String[] { String.valueOf(mUserID) };
			uri = Provider.FRIEND_CONTENT_URI;
		}

		return new CursorLoader(getActivity(), uri, new String[] {
				RelationshipTable.RELATIONSHIP_TABLE + "."
						+ RelationshipTable._ID,
				UserTable.USER_TABLE + "." + UserTable.PROFILE_IMAGE_URL,
				UserTable.USER_TABLE + "." + UserTable.USER_NAME,
				UserTable.USER_TABLE + "." + UserTable.USER_ID }, selection,
				selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Util.log("cursor " + data.getCount());
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_relationship, null);

		View view = v.findViewById(R.id.control_switch);
		view.setVisibility(View.VISIBLE);
		mFriendsButton = (Button) v.findViewById(R.id.friends_btn);
		mFollowerButton = (Button) v.findViewById(R.id.followers_btn);

		mFriendsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CURRENT_LOADER = LOADER_FRIENDS;
				getLoaderManager().restartLoader(CURRENT_LOADER, null,
						RelationshipFragment.this);
			}
		});

		mFollowerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CURRENT_LOADER = LOADER_FOLLOWERS;
				getLoaderManager().restartLoader(CURRENT_LOADER, null,
						RelationshipFragment.this);
			}
		});

		mPtrListView = (PullToRefreshListView) v
				.findViewById(R.id.relationship_list);

		mListView = mPtrListView.getRefreshableView();
		// ------------------
		FrameLayout fl = (FrameLayout) inflater.inflate(
				R.layout.footer_loading, null);
		mListView.addFooterView(fl);
		// ------------------

		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.fragment_relationship_item, null, new String[] {
						UserTable.PROFILE_IMAGE_URL, UserTable.USER_NAME },
				new int[] { R.id.profile_image, R.id.user_name }, 0);

		mAdapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (view.getId() == R.id.profile_image) {
					final long uid = cursor.getLong(cursor
							.getColumnIndex(UserTable.USER_ID));
					view.setTag(R.id.TAG_USER_ID, uid);

					imageLoader.displayImage(cursor.getString(columnIndex),
							(ImageView) view);
					return true;
				}

				return false;
			}
		});

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Cursor cursor = mAdapter.getCursor();
				final long uid = cursor.getLong(cursor
						.getColumnIndex(UserTable.USER_ID));
				Intent i = new Intent(getActivity(), UserActivity.class);
				i.putExtra(UserActivity.KEY_USER_ID, uid);
				startActivity(i);
			}
		});

		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(this);

		mPtrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				doRefresh();
			}
		});

		getLoaderManager().initLoader(CURRENT_LOADER, null, this);

		return v;
	}

	private void doRefresh() {
		if (CURRENT_LOADER == LOADER_FRIENDS) {
			if (mTask == null || mTask.getStatus() != Status.RUNNING) {
				mTask = new GetRelationshipTask(getActivity()).setFollowee(
						mUserID).setHandler(
						new RelationshipHandler(getActivity(), mPtrListView));
				mTask.execute();
			}
		} else if (CURRENT_LOADER == LOADER_FOLLOWERS) {
			if (mTask == null || mTask.getStatus() != Status.RUNNING) {

				mTask = new GetRelationshipTask(getActivity()).setFollower(
						mUserID).setHandler(
						new RelationshipHandler(getActivity(), mPtrListView));
				mTask.execute();
			}
		} else {
			Util.log("no id");
			mPtrListView.onRefreshComplete();
		}
	}

	private class RelationshipHandler extends TaskHandler {
		PullToRefreshListView mPtr;

		public RelationshipHandler(Context context, PullToRefreshListView ptr) {
			super(context);
			mPtr = ptr;
		}

		@Override
		protected void RunSuccess() {
			mPtr.onRefreshComplete();
		}

		@Override
		public void finallyRun() {
			mTask = null;
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		boolean loadMore = /* maybe add a padding */
		firstVisibleItem + visibleItemCount >= totalItemCount;

		// Util.log("mTask is null " + (mTask == null) + " loadmore " +
		// loadMore);

		if (loadMore) {

			if (mTask != null && mTask.getStatus() == Status.RUNNING) {
				return;
			}

			// FooterView being the last one.
			Cursor c = mAdapter.getCursor();
			if (c == null || c.getCount() == 0) {
				return;
			}

			// Util.log("Load more, Cursor count " + c.getCount());

			if (CURRENT_LOADER == LOADER_FRIENDS) {
				mTask = new GetRelationshipTask(getActivity())
						.setFollowee(mUserID)
						.setCursor(c.getCount())
						.setHandler(
								new RelationshipHandler(getActivity(),
										mPtrListView));
				mTask.execute();
			} else if (CURRENT_LOADER == LOADER_FOLLOWERS) {
				mTask = new GetRelationshipTask(getActivity())
						.setFollower(mUserID)
						.setCursor(c.getCount())
						.setHandler(
								new RelationshipHandler(getActivity(),
										mPtrListView));
				mTask.execute();
			}
		}

	}

	@Override
	public void refresh() {
		if (mPtrListView != null) {
			doRefresh();
			mListView.setSelection(0);
		}
	}
}
