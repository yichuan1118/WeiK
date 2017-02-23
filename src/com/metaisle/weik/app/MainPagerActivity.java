package com.metaisle.weik.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.metaisle.profiler.CollectorService;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.caching.CachingService;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.fragment.IRefreshable;
import com.metaisle.weik.weibo.GetRelationshipTask;
import com.metaisle.weik.weibo.GetTimelineTask;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;
import com.metaisle.weik.weibo.TaskHandler;
import com.metaisle.weik.weibo.TweetTask;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.weibo.net.DialogError;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;

public class MainPagerActivity extends SherlockFragmentActivity {
	private ActionBar mBar;
	private ViewPager mPager;
	private Button mLoginButton;

	private SharedPreferences mPrefs;

	// public static final long ONE_MONTH_IN_MILI = 30L * 24 * 60 * 60 * 1000L;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);
		mPrefs.edit().putBoolean(Prefs.KEY_DEBUG, false).commit();

		SharedPreferences dprefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		dprefs.edit()
				.putString("KEY_FTP_SERVER", "weik_profile.metaisle.com")
				.commit();

//		Settings.System.putInt(getContentResolver(),
//				Settings.System.WIFI_SLEEP_POLICY,
//				Settings.System.WIFI_SLEEP_POLICY_NEVER);

		Util.log("onCreate");

//		startService(new Intent(this, CollectorService.class));

		setContentView(R.layout.activity_main_pager);

		mPager = (ViewPager) findViewById(R.id.main_pager);
		mPager.setAdapter(new MainPagerAdapter(this, mPager));

		mBar = getSupportActionBar();
		mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mLoginButton = (Button) findViewById(R.id.login_button);
		mLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Prefs.getWeibo(MainPagerActivity.this).authorize(
						MainPagerActivity.this, new AuthDialogListener());
			}
		});

		promote();
	}

	@Override
	protected void onResume() {
		super.onResume();

		Util.log("onResume");
		Weibo weibo = Prefs.getWeibo(getApplicationContext());
		if (!weibo.isSessionValid()) {
			mLoginButton.setVisibility(View.VISIBLE);
			mPager.setVisibility(View.GONE);
			mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

			weibo.authorize(MainPagerActivity.this, new AuthDialogListener());
		} else {
			mLoginButton.setVisibility(View.GONE);
			mPager.setVisibility(View.VISIBLE);
			mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			startService(new Intent(this, CachingService.class));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_tweet:
			startActivity(new Intent(MainPagerActivity.this,
					TweetActivity.class));

			break;

		// case R.id.menu_cached_only:
		// int pos = mPager.getCurrentItem();
		// if (pos == 0) {
		// TimelineFragment fragment = (TimelineFragment) mPager
		// .getAdapter().instantiateItem(mPager, pos);
		//
		// boolean cachedOnly = fragment.toggleCachedOnly();
		// if (cachedOnly) {
		// item.setTitle(getString(R.string.show_everything));
		// } else {
		// item.setTitle(getString(R.string.show_cached_only));
		// }
		// }
		// break;
			
		case R.id.menu_prefs:
			startActivity(new Intent(this,PrefsActivity.class));
			break;

		case R.id.menu_clear_cache:
			ImageLoader loader = Prefs.imageLoader(MainPagerActivity.this);
			loader.clearDiscCache();
			loader.clearMemoryCache();

			getContentResolver().delete(Provider.TIMELINE_CONTENT_URI, null,
					null);
			getContentResolver().delete(Provider.USER_CONTENT_URI, null, null);
			getContentResolver().delete(Provider.COMMENT_CONTENT_URI, null,
					null);
			getContentResolver().delete(Provider.FOLLOWER_CONTENT_URI, null,
					null);
			getContentResolver()
					.delete(Provider.FRIEND_CONTENT_URI, null, null);
			getContentResolver().delete(Provider.URL_CONTENT_URI, null, null);

			SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_NAME,
					MODE_PRIVATE);

			prefs.edit().remove(Prefs.KEY_LAST_CACHING_TIME)
					.remove(Prefs.KEY_LAST_UPDATED_AT).commit();

			// prefs.edit().remove(Prefs.KEY_LAST_CACHING_TIME)
			// .remove(Prefs.KEY_OAUTH_TOKEN).remove(Prefs.KEY_EXPIRES_IN)
			// .commit();
			//
			// Prefs.sWeibo = null;
			//
			// mLoginButton.setVisibility(View.VISIBLE);
			// mPager.setVisibility(View.GONE);
			// mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

			break;

		case R.id.menu_refresh:
			Toast.makeText(MainPagerActivity.this,
					getString(R.string.getting_new_weibo), Toast.LENGTH_SHORT)
					.show();
			IRefreshable refreshable = (IRefreshable) mPager.getAdapter()
					.instantiateItem(mPager, mPager.getCurrentItem());

			refreshable.refresh();
			break;

		default:
			break;
		}

		return true;
	}

	class AuthDialogListener implements WeiboDialogListener {

		@Override
		public void onComplete(Bundle values) {
			Util.log("onComplete");
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			final String uid = values.getString("uid");

			Util.log("uid " + uid);
			Util.log("token " + token);
			Util.log("expires_in " + expires_in);

			SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_NAME,
					MODE_PRIVATE);
			prefs.edit().putLong(Prefs.KEY_UID, Long.parseLong(uid)).commit();

			Weibo weibo = Prefs.setWeibo(MainPagerActivity.this, token,
					expires_in);

			if (!weibo.isSessionValid()) {
				mLoginButton.setVisibility(View.VISIBLE);
				mPager.setVisibility(View.GONE);
				mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			} else {
				new GetTimelineTask(MainPagerActivity.this, TimelineType.HOME)
						.setHandler(new TaskHandler(MainPagerActivity.this) {
							@Override
							protected void RunSuccess() {
								new GetRelationshipTask(MainPagerActivity.this)
										.setFollower(Long.parseLong(uid))
										.setCount(200).execute();
								new GetRelationshipTask(MainPagerActivity.this)
										.setFollowee(Long.parseLong(uid))
										.setCount(200).execute();
							}
						}).execute();

				// new GetTimelineTask(MainPagerActivity.this,
				// TimelineType.MENTION).execute();
				// new GetCommentTask(MainPagerActivity.this,
				// CommentType.MENTION)
				// .execute();
				// new GetCommentTask(MainPagerActivity.this, CommentType.TO_ME)
				// .execute();

				mLoginButton.setVisibility(View.GONE);
				mPager.setVisibility(View.VISIBLE);
				mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			}

			// startActivity(new Intent(this, cls))
		}

		@Override
		public void onError(DialogError e) {
			e.printStackTrace();
			Util.log(e.getMessage());
			Util.log(e.getFailingUrl());
			if (e.getFailingUrl().startsWith(
					"http://weik.metaisle.com/authorized")) {
				return;
			}

			Toast.makeText(getApplicationContext(),
					"Auth error : " + e.getMessage(), Toast.LENGTH_SHORT)
					.show();
		}

		@Override
		public void onCancel() {
			Toast.makeText(getApplicationContext(), "Auth cancel",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onWeiboException(WeiboException e) {
			e.printStackTrace();
			Util.log(e.getMessage());
			Toast.makeText(getApplicationContext(),
					"Auth exception : " + e.getMessage(), Toast.LENGTH_SHORT)
					.show();
		}

	}

	private void promote() {
		SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_NAME,
				MODE_PRIVATE);
		int times = prefs.getInt(Prefs.KEY_LAUNCHED_TIMES, 0);
		Util.log("times " + times);
		if (Util.isOnline(this)) {
			if (times == 3) {
				FragmentTransaction ft = getSupportFragmentManager()
						.beginTransaction();
				Fragment prev = getSupportFragmentManager().findFragmentByTag(
						"promote");
				if (prev != null) {
					ft.remove(prev);
				}
				ft.addToBackStack(null);

				// Create and show the dialog.
				PromoteDialog newFragment = new PromoteDialog();
				newFragment.show(ft, "promote");
			}
			prefs.edit().putInt(Prefs.KEY_LAUNCHED_TIMES, times + 1).commit();
		}
	}

	public static class PromoteDialog extends DialogFragment {
		public static final String promote_url = "http://weik.metaisle.com";
		Button yes;
		Button no;
		EditText tweet;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater
					.inflate(R.layout.dialog_promote, container, false);

			tweet = (EditText) v.findViewById(R.id.promote_tweet);
			yes = (Button) v.findViewById(R.id.promote_yes);
			no = (Button) v.findViewById(R.id.promote_no);

			yes.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String t = tweet.getText().toString();
					new TweetTask(getActivity(), t + " " + promote_url, null,
							null).execute();
					Util.profile(getActivity(), "promote.csv", "yes");
					dismiss();
				}
			});

			no.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Util.profile(getActivity(), "promote.csv", "no");
					dismiss();
				}
			});
			return v;
		}
	}

}
