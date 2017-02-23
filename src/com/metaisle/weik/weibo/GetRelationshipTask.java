package com.metaisle.weik.weibo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.RelationshipTable;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class GetRelationshipTask extends AsyncTask<Void, Void, Void> {
	private Context mContext;
	private TaskHandler mTaskHandler = null;

	private long mFollowee = -1;
	private long mFollower = -1;
	private int mCursor = -1;
	private long mCount = -1;

	private boolean mSaveProfile = false;

	public static final int MAX_TWEETS_IN_DB = 1000;

	public GetRelationshipTask setFollowee(long id) {
		mFollowee = id;
		return this;
	}

	public GetRelationshipTask setSaveProfile(boolean save) {
		mSaveProfile = save;
		return this;
	}

	public GetRelationshipTask setFollower(long id) {
		mFollower = id;
		return this;
	}

	public GetRelationshipTask setCursor(int id) {
		mCursor = id;
		return this;
	}

	public GetRelationshipTask setHandler(TaskHandler handler) {
		mTaskHandler = handler;
		return this;
	}

	public GetRelationshipTask setCount(int count) {
		mCount = count;
		return this;
	}

	public GetRelationshipTask(Context context) {
		mContext = context;
	}

	@Override
	protected Void doInBackground(Void... params) {

		if (mFollowee < 0 && mFollower < 0) {
			throw new IllegalArgumentException(
					"Provide either Followee or Follower");
		}

		Util.log("mFollowee " + mFollowee + " mFollower " + mFollower
				+ " cursor " + mCursor);

		Weibo weibo = Prefs.getWeibo(mContext);

		WeiboParameters b = new WeiboParameters();

		b.add("access_token", weibo.getAccessToken().getToken());

		if (mCursor > 0) {
			b.add("cursor", String.valueOf(mCursor));
		}

		if (mCount > 0) {
			b.add("count", String.valueOf(mCount));
		}

		String url = null;
		if (mFollowee > 0) {
			if (mCursor < 0) {
				Util.log("delete");
				mContext.getContentResolver().delete(
						Provider.FOLLOWER_CONTENT_URI,
						RelationshipTable.FOLLOWEE + "=?",
						new String[] { String.valueOf(mFollowee) });
			}
			url = "friendships/followers.json";
			b.add("uid", String.valueOf(mFollowee));
		} else if (mFollower > 0) {
			if (mCursor < 0) {
				Util.log("delete");
				mContext.getContentResolver().delete(
						Provider.FRIEND_CONTENT_URI,
						RelationshipTable.FOLLOWER + "=?",
						new String[] { String.valueOf(mFollower) });
			}
			url = "friendships/friends.json";
			b.add("uid", String.valueOf(mFollower));
		}

		try {
			Util.log(url);
			String rlt = weibo.request(mContext, Weibo.SERVER + url, b, "GET",
					weibo.getAccessToken());

			JSONObject resp = new JSONObject(rlt);
			if (!resp.isNull("users")) {

				String filename = null;
				if (mFollowee > 0) {
					filename = "followers.csv";
				} else {
					filename = "friends.csv";
				}

				if (mSaveProfile)
					Util.profile(mContext, filename, rlt);
			}

			insertRelationships(mContext, resp.getJSONArray("users"));

		} catch (Exception e) {
			if (mTaskHandler != null)
				mTaskHandler.fail();
			e.printStackTrace();
		}
		return null;
	}

	public void insertRelationships(Context context, JSONArray users)
			throws JSONException, Exception {
		for (int i = 0; i < users.length(); i++) {
			ContentValues values = new ContentValues();
			if (mFollowee > 0) {
				values.put(RelationshipTable.FOLLOWEE, mFollowee);
				values.put(RelationshipTable.FOLLOWER, users.getJSONObject(i)
						.getLong("id"));
			} else if (mFollower > 0) {
				values.put(RelationshipTable.FOLLOWER, mFollower);
				values.put(RelationshipTable.FOLLOWEE, users.getJSONObject(i)
						.getLong("id"));
			}
			context.getContentResolver().insert(Provider.FOLLOWER_CONTENT_URI,
					values);

			Util.log("inserting " + users.getJSONObject(i).getString("name"));

			GetTimelineTask.insertUser(context, users.getJSONObject(i));
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		if (mTaskHandler != null)
			mTaskHandler.success();
	}

}
