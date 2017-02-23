package com.metaisle.weik.app;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.util.Util;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.fragment.StatusFragment;

public class StatusPagerAdapter extends FragmentStatePagerAdapter implements
		ViewPager.OnPageChangeListener {
	private SherlockFragmentActivity mFragmentActivity;

	private Cursor mCursor;

	public StatusPagerAdapter(SherlockFragmentActivity activity) {
		super(activity.getSupportFragmentManager());
		mFragmentActivity = activity;
	}

	@Override
	public Fragment getItem(int position) {
		if (mCursor == null) {
			Util.log("dummy frag");
			Bundle b = new Bundle();
			b.putLong(StatusFragment.KEY_STATUS_ID, 0);
			return Fragment.instantiate(mFragmentActivity,
					StatusFragment.class.getName(), b);
		}

		if (position == 0) {

		}

		mCursor.moveToPosition(position);

		long id = mCursor.getLong(mCursor
				.getColumnIndex(TimelineTable.STATUS_ID));

		Bundle b = new Bundle();
		b.putLong(StatusFragment.KEY_STATUS_ID, id);
		return Fragment.instantiate(mFragmentActivity,
				StatusFragment.class.getName(), b);
	}

	@Override
	public int getCount() {
		if (mCursor == null) {
			return 0;
		}

		return mCursor.getCount();
	}

	public void swapCursor(Cursor cursor) {
		mCursor = cursor;
	}

	public long getStatusId(int position) {
		mCursor.moveToPosition(position);

		return mCursor.getLong(mCursor.getColumnIndex(TimelineTable.STATUS_ID));
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		mCursor.moveToPosition(position);

		long id = mCursor.getLong(mCursor
				.getColumnIndex(TimelineTable.STATUS_ID));

		Util.log("selected " + getStatusId(position));
		Util.profile(mFragmentActivity, "status_view.csv", "" + id
				+ ", selected");
	}
}
