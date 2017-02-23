package com.metaisle.weik.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import com.metaisle.weik.app.CommentRepostActivity;
import com.metaisle.weik.app.StatusActivity;
import com.metaisle.weik.app.UserActivity;
import com.metaisle.weik.data.CommentTable;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.data.UserTable;
import com.metaisle.weik.weibo.GetCommentTask;
import com.metaisle.weik.weibo.TaskHandler;
import com.nostra13.universalimageloader.core.ImageLoader;

public class CommentFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor>, OnScrollListener, IRefreshable {

	public enum CommentType {
		SHOW, MENTION, TO_ME
	}

	public static final String KEY_COMMENT_TYPE = "KEY_COMMENT_TYPE";
	public static final String KEY_STATUS_ID = "key_status_id";

	public SharedPreferences mPrefs;
	private CommentType mType;

	private long mStatusId;

	private SimpleCursorAdapter mAdapter;
	public ImageLoader imageLoader;

	private PullToRefreshListView mPtrListView;
	private ListView mListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = Prefs.imageLoader(getActivity());

		Bundle b = getArguments();
		if (b != null) {
			mType = (CommentType) b.getSerializable(KEY_COMMENT_TYPE);
			Util.log("Comment type: " + mType);
			mStatusId = b.getLong(KEY_STATUS_ID);
			Util.log("Status ID: " + mStatusId);
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mPtrListView = (PullToRefreshListView) inflater.inflate(
				R.layout.fragment_comment, null);

		mListView = mPtrListView.getRefreshableView();
		// ------------------
		FrameLayout fl = (FrameLayout) inflater.inflate(
				R.layout.footer_loading, null);
		mListView.addFooterView(fl);
		// ------------------

		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.fragment_comment_item, null,

				new String[] { UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_NAME, CommentTable.CREATED_AT,
						CommentTable.COMMENT_TEXT,
						"REC_U_" + UserTable.USER_NAME,
						"REC_" + CommentTable.COMMENT_TEXT,
						"RES_" + TimelineTable.STATUS_ID },

				new int[] { R.id.profile_image, R.id.user_name, R.id.time_span,
						R.id.status_text, R.id.rt_user_name,
						R.id.rt_status_text, R.id.comment_reply_btn }, 0);

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
				else if (view.getId() == R.id.time_span) {
					long time = cursor.getLong(columnIndex);
					((TextView) view).setText(DateUtils
							.getRelativeTimeSpanString(time));
					return true;
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.rt_user_name) {
					if (cursor.isNull(columnIndex)) {
						String res = cursor.getString(cursor
								.getColumnIndex("RES_U_" + UserTable.USER_NAME));
						((TextView) view).setText(res);
						return true;
					}
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.rt_status_text) {
					if (cursor.isNull(columnIndex)) {
						String res = cursor.getString(cursor
								.getColumnIndex("RES_"
										+ TimelineTable.STATUS_TEXT));
						((TextView) view).setText(res);

						return true;
					}
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.comment_reply_btn) {

					if (!cursor.isNull(columnIndex)) {
						long sid = cursor.getLong(cursor.getColumnIndex("RES_"
								+ TimelineTable.STATUS_ID));
						String s_u_name = cursor.getString(cursor
								.getColumnIndex("RES_U_" + UserTable.USER_NAME));

						String s_text = cursor.getString(cursor
								.getColumnIndex("RES_"
										+ TimelineTable.STATUS_TEXT));

						long cid = cursor.getLong(cursor
								.getColumnIndex(CommentTable.COMMENT_ID));
						String c_u_name = cursor.getString(cursor
								.getColumnIndex(UserTable.USER_NAME));

						String c_text = cursor.getString(cursor
								.getColumnIndex(CommentTable.COMMENT_TEXT));

						String pre_text = "@" + c_u_name + ":\n" + c_text
								+ "\n" + "@" + s_u_name + ":\n" + s_text;

						final Bundle b = new Bundle();
						b.putLong(CommentRepostActivity.KEY_COMMENT_STATUS_ID,
								sid);
						b.putLong(CommentRepostActivity.KEY_COMMENT_REPLY_ID,
								cid);
						b.putString(CommentRepostActivity.KEY_ORIGINAL_TEXT,
								pre_text);
						view.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {

								Intent i = new Intent(getActivity(),
										CommentRepostActivity.class);
								i.putExtras(b);
								getActivity().startActivity(i);
							}

						});

						return true;
					}
				}

				return false;
			}
		});

		mListView.setAdapter(mAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> l, View v, int position,
					long id) {
				Cursor cursor = mAdapter.getCursor();
				long sid = cursor.getLong(cursor.getColumnIndex("RES_"
						+ TimelineTable.STATUS_ID));
				// String s_u_name = cursor.getString(cursor
				// .getColumnIndex("RES_U_" + UserTable.USER_NAME));
				//
				// String s_text = cursor.getString(cursor.getColumnIndex("RES_"
				// + TimelineTable.STATUS_TEXT));
				//
				// long cid = cursor.getLong(cursor
				// .getColumnIndex(CommentTable.COMMENT_ID));
				// String c_u_name = cursor.getString(cursor
				// .getColumnIndex(UserTable.USER_NAME));
				//
				// String c_text = cursor.getString(cursor
				// .getColumnIndex(CommentTable.COMMENT_TEXT));
				//
				// String pre_text = "@" + c_u_name + ":\n" + c_text + "\n" +
				// "@"
				// + s_u_name + ":\n" + s_text;
				//
				// Bundle b = new Bundle();
				// b.putLong(CommentRepostActivity.KEY_COMMENT_STATUS_ID, sid);
				// b.putLong(CommentRepostActivity.KEY_COMMENT_REPLY_ID, cid);
				// b.putString(CommentRepostActivity.KEY_ORIGINAL_TEXT,
				// pre_text);
				// Intent i = new Intent(getActivity(),
				// CommentRepostActivity.class);
				// i.putExtras(b);
				//
				// getActivity().startActivity(i);

				Intent i = new Intent(getActivity(), StatusActivity.class);
				i.putExtra(StatusFragment.KEY_STATUS_ID, sid);
				getActivity().startActivity(i);
			}
		});

		mPtrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				doRefresh();
				return;
			}
		});

		getLoaderManager().initLoader(0, null, this);

		return mPtrListView;
	}

	private void doRefresh() {
		Cursor c = mAdapter.getCursor();

		Util.log("Cursor count " + c.getCount());

		if (c == null || c.getCount() < 1) {
			new GetCommentTask(getActivity(), CommentType.TO_ME).setHandler(
					new CommentHandler(getActivity(), mPtrListView)).execute();
			new GetCommentTask(getActivity(), CommentType.MENTION).setHandler(
					new CommentHandler(getActivity(), mPtrListView)).execute();
			return;
		}

		c.moveToFirst();
		long status_id = c.getLong(c.getColumnIndex(TimelineTable.STATUS_ID));
		Util.log("Refresh, since id " + status_id);

		new GetCommentTask(getActivity(), CommentType.TO_ME)
				.setSinceID(status_id)
				.setHandler(new CommentHandler(getActivity(), mPtrListView))
				.execute();
		new GetCommentTask(getActivity(), CommentType.MENTION)
				.setSinceID(status_id)
				.setHandler(new CommentHandler(getActivity(), mPtrListView))
				.execute();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity(), Provider.COMMENT_CONTENT_URI,

				new String[] {
						CommentTable.COMMENT_TABLE + "." + CommentTable._ID,
						CommentTable.COMMENT_TABLE + "."
								+ CommentTable.COMMENT_ID,
						CommentTable.COMMENT_TABLE + "."
								+ CommentTable.AUTHOR_ID,

						UserTable.USER_TABLE + "."
								+ UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_TABLE + "." + UserTable.USER_NAME,
						CommentTable.COMMENT_TABLE + "."
								+ CommentTable.CREATED_AT,
						CommentTable.COMMENT_TABLE + "."
								+ CommentTable.COMMENT_TEXT,

						"REC_U." + UserTable.USER_NAME + " AS REC_U_"
								+ UserTable.USER_NAME,
						"REC_U." + UserTable.USER_ID + " AS REC_U_"
								+ UserTable.USER_ID,

						"REC." + CommentTable.COMMENT_TEXT + " AS REC_"
								+ CommentTable.COMMENT_TEXT,
						"REC." + CommentTable.COMMENT_ID + " AS REC_"
								+ CommentTable.COMMENT_ID,

						"RES_U." + UserTable.USER_NAME + " AS RES_U_"
								+ UserTable.USER_NAME,
						"RES_U." + UserTable.USER_ID + " AS RES_U_"
								+ UserTable.USER_ID,

						"RES." + TimelineTable.STATUS_TEXT + " AS RES_"
								+ TimelineTable.STATUS_TEXT,
						"RES." + TimelineTable.STATUS_ID + " AS RES_"
								+ TimelineTable.STATUS_ID,

				},

				CommentTable.COMMENT_TABLE + "." + CommentTable.IS_TO_ME
						+ "=? OR " + CommentTable.COMMENT_TABLE + "."
						+ CommentTable.IS_MENTIONS + "=?", new String[] { "1",
						"1" }, CommentTable.COMMENT_TABLE + "."
						+ CommentTable.CREATED_AT + " DESC");
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// Util.log("firstVisibleItem " + firstVisibleItem +
		// " visibleItemCount "
		// + visibleItemCount + " totalItemCount " + totalItemCount);

		boolean loadMore = /* maybe add a padding */
		firstVisibleItem + visibleItemCount >= totalItemCount;

		if (loadMore) {
			Cursor c = mAdapter.getCursor();
			if (c == null || c.getCount() == 0) {
				return;
			}
			Util.log("Cursor count " + c.getCount());

			c.moveToLast();
			long comment_id = c.getLong(c
					.getColumnIndex(CommentTable.COMMENT_ID));
			Util.log("Load more, last id " + comment_id);

			new GetCommentTask(getActivity(), CommentType.MENTION).setMaxID(
					comment_id).execute();
			new GetCommentTask(getActivity(), CommentType.TO_ME).setMaxID(
					comment_id).execute();
		}
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

	private class CommentHandler extends TaskHandler {
		PullToRefreshListView mPtr;

		public CommentHandler(Context context, PullToRefreshListView ptr) {
			super(context);
			mPtr = ptr;
		}

		@Override
		protected void RunSuccess() {
			mPtr.onRefreshComplete();
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
