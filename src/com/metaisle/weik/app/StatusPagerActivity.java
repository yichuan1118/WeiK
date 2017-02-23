package com.metaisle.weik.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.fragment.StatusFragment;
import com.metaisle.weik.fragment.TimelineFragment;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;

public class StatusPagerActivity extends SherlockFragmentActivity implements
		LoaderCallbacks<Cursor> {
	public static final String KEY_TIMELINE_TYPE = "key_timeline_type";
	public static final String KEY_USER_ID = "key_user_id";
	public static final String KEY_STATUS_ID = "key_status_id";

	private ViewPager mPager;
	private StatusPagerAdapter mAdapter;

	private TimelineType mType;
	private long mUserID;
	private long mStatusID;

	private boolean mSetPositionOnce = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_status_pager);

		Intent i = getIntent();
		mType = (TimelineType) i.getSerializableExtra(KEY_TIMELINE_TYPE);
		mUserID = i.getLongExtra(KEY_USER_ID, 0);
		mStatusID = i.getLongExtra(KEY_STATUS_ID, 0);

		mPager = (ViewPager) findViewById(R.id.status_pager);
		mAdapter = new StatusPagerAdapter(this);
		mPager.setAdapter(mAdapter);
		mPager.setOnPageChangeListener(mAdapter);

		Util.log("mType " + mType);
		Util.log("mUserID " + mUserID);
		Util.log("mStatusID " + mStatusID);

		getSupportLoaderManager().initLoader(0, null, this);
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
			selectionArg = new String[] { String.valueOf(mUserID) };
			break;

		default:
			throw new IllegalArgumentException("Unknown Fragment type. ");
		}
		return new CursorLoader(this, Provider.TIMELINE_CONTENT_URI,
				new String[] { TimelineTable.TIMELINE_TABLE + "."
						+ TimelineTable.STATUS_ID, }, selection, selectionArg,
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.CREATED_AT
						+ " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Util.log("data " + data.getCount());
		mAdapter.swapCursor(data);

		if (!mSetPositionOnce) {
			mSetPositionOnce = true;
			int idx = data.getColumnIndex(TimelineTable.STATUS_ID);
			while (data.moveToNext()) {
				if (data.getLong(idx) == mStatusID) {
					mPager.setCurrentItem(data.getPosition());
					break;
				}
			}
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void finish() {
		Intent intent = this.getIntent();
		intent.putExtra(TimelineFragment.KEY_POSITION, mPager.getCurrentItem());
		setResult(RESULT_OK, intent);

		Util.log("result " + mPager.getCurrentItem());

		super.finish();
	}

	@Override
	protected void onPause() {
		int pos = mPager.getCurrentItem();
		StatusFragment fragment = (StatusFragment) mPager.getAdapter()
				.instantiateItem(mPager, pos);
		String text = fragment.getText4Comment();
		Util.log("Leaving status " + text);
		long id = fragment.getStatusID();
		Util.profile(this, "status_view.csv", "" + id + ", leave");
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Util.profile(this, "status_view.csv", "" + "resume");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_status, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int position = mPager.getCurrentItem();

		Bundle b = new Bundle();
		if (item.getItemId() == R.id.menu_retweet) {
			StatusFragment fragment = (StatusFragment) mAdapter
					.instantiateItem(mPager, position);
			if (fragment.mStatusCursor == null) {
				return true;
			}
			long status_id = fragment.getStatusIdorRepostId4Repost();
			String pretext = fragment.getPretext();
			String text = fragment.getText4Repost();
			b.putLong(CommentRepostActivity.KEY_REPOST_STATUS_ID, status_id);

			b.putString(CommentRepostActivity.KEY_REPOST_PRETEXT, pretext);

			b.putString(CommentRepostActivity.KEY_ORIGINAL_TEXT, text);
		} else if (item.getItemId() == R.id.menu_comment) {
			Util.log("comment");
			StatusFragment fragment = (StatusFragment) mAdapter
					.instantiateItem(mPager, position);

			if (fragment.mStatusCursor == null) {
				return true;
			}

			long status_id = mAdapter.getStatusId(position);
			String text = fragment.getText4Comment();
			b.putLong(CommentRepostActivity.KEY_COMMENT_STATUS_ID, status_id);

			b.putString(CommentRepostActivity.KEY_ORIGINAL_TEXT, text);
		} else if (item.getItemId() == R.id.menu_left) {
//			StatusFragment fragment = (StatusFragment) mAdapter
//					.instantiateItem(mPager, 0);
//			long id = fragment.getStatusID();
//			new GetTimelineTask(StatusPagerActivity.this, TimelineType.HOME)
//					.setSinceID(id).execute();
			mPager.setCurrentItem(0);
			return true;
		}

		Intent i = new Intent(StatusPagerActivity.this,
				CommentRepostActivity.class);
		i.putExtras(b);
		startActivity(i);

		return true;
	}
}
