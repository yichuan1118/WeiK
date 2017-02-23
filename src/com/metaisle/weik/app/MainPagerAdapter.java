package com.metaisle.weik.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.fragment.CommentFragment;
import com.metaisle.weik.fragment.CommentFragment.CommentType;
import com.metaisle.weik.fragment.RelationshipFragment;
import com.metaisle.weik.fragment.TimelineFragment;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;

public class MainPagerAdapter extends FragmentPagerAdapter implements
		TabListener, ViewPager.OnPageChangeListener {

	private final static String[] mFragmentTitles = { "Home", "Mentions",
			"Comments", "People" };

	private SherlockFragmentActivity mFragmentActivity;
	private ViewPager mPager;
	private ActionBar mActionBar;

	private long myID;

	public MainPagerAdapter(SherlockFragmentActivity activity, ViewPager pager) {
		super(activity.getSupportFragmentManager());
		mFragmentActivity = activity;
		mPager = pager;
		mPager.setOnPageChangeListener(this);
		mActionBar = activity.getSupportActionBar();

		mActionBar.addTab(mActionBar.newTab().setIcon(R.drawable.ic_tab_home)
				.setText(R.string.tab_home).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab().setText(R.string.tab_mention)
				.setIcon(R.drawable.ic_tab_mention).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab().setText(R.string.tab_comment)
				.setIcon(R.drawable.ic_tab_comment).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab().setIcon(R.drawable.ic_tab_people)
				.setText(R.string.tab_people).setTabListener(this));
		// mActionBar.addTab(mActionBar.newTab().setIcon(R.drawable.ic_tab_more)
		// .setTabListener(this));

		SharedPreferences prefs = mFragmentActivity.getSharedPreferences(
				Prefs.PREFS_NAME, Context.MODE_PRIVATE);

		try {
			myID = prefs.getLong(Prefs.KEY_UID, -1);
		} catch (Exception e) {
			String idstr = prefs.getString(Prefs.KEY_UID, null);
			if (idstr != null) {
				myID = Long.parseLong(idstr);
				prefs.edit().putLong(Prefs.KEY_UID, myID).commit();
			}
		}
		Util.log("uid " + myID);
	}

	@Override
	public int getCount() {
		return mFragmentTitles.length;
	}

	@Override
	public Fragment getItem(int position) {
		Bundle b = new Bundle();
		switch (position) {
		case 0: {
			b.putSerializable(TimelineFragment.KEY_TIMELINE_TYPE,
					TimelineType.HOME);
			break;
		}
		case 1: {
			b.putSerializable(TimelineFragment.KEY_TIMELINE_TYPE,
					TimelineType.MENTION);
			break;
		}
		case 2: {
			b.putSerializable(CommentFragment.KEY_COMMENT_TYPE,
					CommentType.TO_ME);
			return Fragment.instantiate(mFragmentActivity,
					CommentFragment.class.getName());
		}
		case 3: {
			Util.log("uid long " + myID);
			b.putLong(RelationshipFragment.KEY_USER_ID, myID);
			return Fragment.instantiate(mFragmentActivity,
					RelationshipFragment.class.getName(), b);
		}

		}

		return Fragment.instantiate(mFragmentActivity,
				TimelineFragment.class.getName(), b);
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return mFragmentTitles[position];
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		mActionBar.setSelectedNavigationItem(position);
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		int position = tab.getPosition();
		mPager.setCurrentItem(position);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

}
