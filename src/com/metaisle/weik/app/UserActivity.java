package com.metaisle.weik.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.UserTable;
import com.metaisle.weik.fragment.RelationshipFragment;
import com.metaisle.weik.fragment.TimelineFragment;
import com.metaisle.weik.weibo.FollowTask;
import com.metaisle.weik.weibo.GetRelationshipTask;
import com.metaisle.weik.weibo.GetTimelineTask;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;
import com.metaisle.weik.weibo.GetUserTask;
import com.nostra13.universalimageloader.core.ImageLoader;

public class UserActivity extends SherlockFragmentActivity implements
		LoaderCallbacks<Cursor> {

	private static final Uri MENTIONS_URI = Uri.parse(Prefs.MENTIONS_SCHEMA);

	private ImageView profile_image;
	private TextView user_name;
	private TextView follow_me;
	private TextView verified_reason;
	private TextView description;
	private Button status_count;
	private Button friends_count;
	private Button followers_count;
	private Button follow_btn;

	public static final String KEY_USER_ID = "key_user_id";

	private long mUserID = -1;
	private String mUserName = null;
	private ImageLoader imageLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		imageLoader = Prefs.imageLoader(this);

		mUserID = getIntent().getLongExtra(KEY_USER_ID, 0);

		Uri uri = getIntent().getData();
		if (uri != null && MENTIONS_URI.getScheme().equals(uri.getScheme())) {
			Util.log(uri.toString());
			mUserName = uri.getQueryParameter(Prefs.PARAM_USERNAME);
			Util.log("from url: " + mUserName);
			new GetUserTask(this, mUserName).execute();
		}

		if (mUserID < 0 && mUserName == null) {
			finish();
		}

		setContentView(R.layout.activity_user);

		profile_image = (ImageView) findViewById(R.id.profile_image);
		user_name = (TextView) findViewById(R.id.user_name);
		follow_me = (TextView) findViewById(R.id.follow_me);
		verified_reason = (TextView) findViewById(R.id.verified_reason);
		description = (TextView) findViewById(R.id.description);
		status_count = (Button) findViewById(R.id.status_count);
		friends_count = (Button) findViewById(R.id.friends_count);
		followers_count = (Button) findViewById(R.id.followers_count);
		follow_btn = (Button) findViewById(R.id.follow_btn);

		getSupportLoaderManager().initLoader(0, null, this);

		// -----
		status_count.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new GetTimelineTask(UserActivity.this, TimelineType.USER,
						mUserID).execute();
				Intent i = new Intent(UserActivity.this, TimelineActivity.class);
				i.putExtra(TimelineFragment.KEY_USER_ID, mUserID);
				startActivity(i);
			}
		});

		// -----
		friends_count.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new GetRelationshipTask(UserActivity.this).setCursor(0)
						.setFollower(mUserID).execute();
				Intent i = new Intent(UserActivity.this,
						RelationshipActivity.class);
				i.putExtra(RelationshipFragment.KEY_USER_ID, mUserID);
				startActivity(i);
			}
		});

		// -----
		followers_count.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new GetRelationshipTask(UserActivity.this).setCursor(0)
						.setFollowee(mUserID).execute();
				Intent i = new Intent(UserActivity.this,
						RelationshipActivity.class);
				i.putExtra(RelationshipFragment.KEY_USER_ID, mUserID);
				startActivity(i);
			}
		});

		// -----
		follow_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				follow_btn.setEnabled(false);
				new FollowTask(UserActivity.this, mUserID, follow_btn)
						.execute();
			}
		});
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		if (mUserID > 0) {
			Uri uri = Uri.parse(Provider.USER_CONTENT_URI + "/" + mUserID);

			return new CursorLoader(this, uri, new String[] { UserTable._ID,
					UserTable.USER_ID, UserTable.AVATAR_LARGE,
					UserTable.USER_NAME, UserTable.SCREEN_NAME,
					UserTable.FRIENDS_COUNT, UserTable.FOLLOWERS_COUNT,
					UserTable.DESCRIPTION, UserTable.STATUSES_COUNT,
					UserTable.FOLLOW_ME, UserTable.FOLLOWING,
					UserTable.VERIFIED_REASON }, null, null, null);
		} else if (mUserName != null) {
			return new CursorLoader(this, Provider.USER_CONTENT_URI,
					new String[] { UserTable._ID, UserTable.USER_ID,
							UserTable.AVATAR_LARGE, UserTable.USER_NAME,
							UserTable.SCREEN_NAME, UserTable.FRIENDS_COUNT,
							UserTable.FOLLOWERS_COUNT, UserTable.DESCRIPTION,
							UserTable.STATUSES_COUNT, UserTable.FOLLOW_ME,
							UserTable.FOLLOWING, UserTable.VERIFIED_REASON },
					UserTable.USER_NAME + "=?", new String[] { mUserName },
					null);
		} else {
			throw new IllegalArgumentException("provides either username or id");
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor.getCount() == 0)
			return;
		cursor.moveToFirst();

		mUserID = cursor.getLong(cursor.getColumnIndex(UserTable.USER_ID));

		imageLoader
				.displayImage(cursor.getString(cursor
						.getColumnIndex(UserTable.AVATAR_LARGE)), profile_image);

		user_name.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.USER_NAME)));

		boolean is_following_me = cursor.getInt(cursor
				.getColumnIndex(UserTable.FOLLOW_ME)) > 0;
		if (is_following_me) {
			follow_me.setText(getText(R.string.is_following_me));
		} else {
			follow_me.setText(getText(R.string.is_not_following_me));
		}

		description.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.DESCRIPTION)));

		status_count.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.STATUSES_COUNT))
				+ "\n"
				+ getString(R.string.weibo));

		friends_count.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.FRIENDS_COUNT))
				+ "\n"
				+ getString(R.string.following));

		followers_count.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.FOLLOWERS_COUNT))
				+ "\n"
				+ getString(R.string.followers));

		int following = cursor.getInt(cursor
				.getColumnIndex(UserTable.FOLLOWING));
		Util.log("following " + following);
		boolean is_following = following > 0;
		if (is_following) {
			follow_btn.setText(getText(R.string.unfollow));
		} else {
			follow_btn.setText(getText(R.string.follow));
		}

		if (cursor.isNull(cursor.getColumnIndex(UserTable.VERIFIED_REASON))) {
			verified_reason.setVisibility(View.GONE);
		} else {
			verified_reason.setVisibility(View.VISIBLE);
			verified_reason.setText(cursor.getString(cursor
					.getColumnIndex(UserTable.VERIFIED_REASON)));
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		profile_image.setImageDrawable(null);
		user_name.setText(null);
	}

}
