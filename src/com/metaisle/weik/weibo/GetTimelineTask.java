package com.metaisle.weik.weibo;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.format.Time;
import android.widget.Toast;

import com.metaisle.util.FindUrls;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.caching.ImageCachingTask;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.data.UrlTable;
import com.metaisle.weik.data.UserTable;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

/*
 * Timeline
 */
public class GetTimelineTask extends AsyncTask<Void, Void, Void> {
	public enum TimelineType {
		HOME, MENTION, FAVORITE, REPOST, USER, ALL
	}

	private Context mContext;
	private SharedPreferences mPrefs;
	private TaskHandler mTaskHandler = null;
	private JSONArray mStatuses = null;

	private TimelineType mType = null;
	private long mSinceID = -1;
	private long mMaxID = -1;
	private long mUserId = -1;
	private long mRepostedID = -1;

	private long mCount = -1;

	public GetTimelineTask(Context context, TimelineType type) {
		mType = type;
		mContext = context;
		mPrefs = context.getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
	}

	public GetTimelineTask(Context context, TimelineType type, long id) {
		this(context, type);
		mUserId = id;
	}

	public GetTimelineTask setSinceID(long since) {
		mSinceID = since;
		return this;
	}

	public GetTimelineTask setMaxID(long max) {
		mMaxID = max;
		return this;
	}

	public GetTimelineTask setHandler(TaskHandler handler) {
		mTaskHandler = handler;
		return this;
	}

	public GetTimelineTask setRepostedID(long reposted) {
		mRepostedID = reposted;
		return this;
	}

	public GetTimelineTask setCount(int count) {
		mCount = count;
		return this;
	}

	private boolean show_no_new = false;

	public GetTimelineTask setIsShowingNoNew(boolean show) {
		show_no_new = show;
		return this;
	}

	@Override
	protected Void doInBackground(Void... nulls) {

		Weibo weibo = Prefs.getWeibo(mContext);

		Util.log("weibo " + weibo.isSessionValid());
		Util.log("getAppKey " + Weibo.getAppKey());
		Util.log("type " + mType);

		WeiboParameters bundle = new WeiboParameters();
		bundle.add("access_token", weibo.getAccessToken().getToken());

		if (mSinceID > 0) {
			bundle.add("since_id", String.valueOf(mSinceID));
		}

		if (mMaxID > 0) {
			bundle.add("max_id", String.valueOf(mMaxID));
		}

		if (mCount > 0) {
			bundle.add("count", String.valueOf(mCount));
		}

		String url, key, value = null;
		switch (mType) {
		case HOME:
			Util.profile(mContext, "home_timeline.csv", "start");
			url = Weibo.SERVER + "statuses/home_timeline.json";
			key = TimelineTable.IS_HOME;
			value = "1";
			break;
		case MENTION:
			url = Weibo.SERVER + "statuses/mentions.json";
			key = TimelineTable.IS_MENTION;
			value = "1";
			break;
		case USER:
			url = Weibo.SERVER + "statuses/user_timeline.json";
			key = TimelineTable.USER_TIMELINE;
			value = String.valueOf(mUserId);
			bundle.add("uid", String.valueOf(mUserId));
			Util.log("uid " + bundle.getValue("uid"));
			break;
		case REPOST:
			url = Weibo.SERVER + "statuses/repost_timeline.json";
			key = TimelineTable.DIRECT_REPOST;
			value = String.valueOf(mRepostedID);
			bundle.add("id", String.valueOf(mRepostedID));
			break;
		default:
			throw new IllegalStateException("Unknown Timeline Type");
		}
		try {
			Util.log("url " + url);

			String rlt = weibo.request(mContext, url, bundle, "GET",
					weibo.getAccessToken());
			JSONObject resp = new JSONObject(rlt);

			if (mType == TimelineType.HOME && !resp.isNull("statuses")) {
				Util.profile(mContext, "home_timeline.csv", rlt);
			}

			if (!resp.isNull("statuses")) {
				mStatuses = resp.getJSONArray("statuses");
			}

			if (mType != TimelineType.REPOST && !resp.isNull("statuses")) {

				Util.log("statuses " + mStatuses.length());

				if (mSinceID > 0
						&& mStatuses.length() == Prefs.DEFAULT_LOAD_NUMBER) {
					Util.log("delete discontinued statuses");
					mContext.getContentResolver().delete(
							Provider.TIMELINE_CONTENT_URI, null, null);
				}

				insertStatuses(mContext, mStatuses, key, value);

				if (mType == TimelineType.HOME && mStatuses.length() > 0) {
					Util.log("launching caching");
					new ImageCachingTask(mContext).execute();
					// new UrlCachingTask(mContext).execute();
				}
			} else if (mType == TimelineType.REPOST && !resp.isNull("reposts")) {
				JSONArray statuses = resp.getJSONArray("reposts");
				insertStatuses(mContext, statuses, key, value);
			}
		} catch (Exception e) {
			if (mTaskHandler != null)
				mTaskHandler.sendEmptyMessage(TaskHandler.RUN_FAIL);
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);

		if (show_no_new) {
			if (mStatuses == null || mStatuses.length() == 0) {
				Toast.makeText(mContext,
						mContext.getString(R.string.no_new_weibo),
						Toast.LENGTH_SHORT).show();
			}
		}

		Time now = new Time();
		now.setToNow();
		mPrefs.edit().putLong(Prefs.KEY_LAST_UPDATED_AT, now.toMillis(true))
				.commit();
		if (mTaskHandler != null) {
			mTaskHandler.sendEmptyMessage(TaskHandler.RUN_SUCCESS);
		}

	}

	/*
	 * Misc.
	 */
	public static void insertStatuses(Context context, JSONArray statuses,
			String key, String value) throws Exception {

		for (int i = 0; i < statuses.length(); i++) {
			insertStatus(context, statuses.getJSONObject(i), key, value);
		}
	}

	public static void insertStatus(Context context, JSONObject s, String key,
			String value) throws Exception {

		// Util.log("status " + s.toString(2));

		if (!s.isNull("deleted") && s.getString("deleted").equals("1")) {
			return;
		}

		SimpleDateFormat parser = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
		String created = s.getString("created_at");
		long create_time = parser.parse(created).getTime();

		// ----------------------------------------------------------
		ContentValues values = new ContentValues();
		if (key != null)
			values.put(key, value);
		values.put(TimelineTable.CREATED_AT, create_time);
		values.put(TimelineTable.STATUS_ID, s.getLong("id"));

		String status_text = s.getString("text");
		values.put(TimelineTable.STATUS_TEXT, s.getString("text"));

		// try {
		// Weibo weibo = Prefs.getWeibo(context);
		// WeiboParameters bundle = new WeiboParameters();
		// bundle.add("access_token", weibo.getAccessToken().getToken());
		List<String> urls = FindUrls.extractUrls(status_text);

		for (String url : urls) {
			Util.log("url_short " + url);

			ContentValues cvs = new ContentValues();
			cvs.put(UrlTable.URL_SHORT, url);
			cvs.put(UrlTable.STATUS_ID, s.getLong("id"));

			context.getContentResolver().insert(Provider.URL_CONTENT_URI, cvs);
		}

		// bundle.add("url_short", url);
		// String rlt = weibo.request(context, Weibo.SERVER
		// + "short_url/expand.json", bundle, "GET",
		// weibo.getAccessToken());
		// JSONObject resp = new JSONObject(rlt).getJSONArray("urls")
		// .getJSONObject(0);

		// if (resp.getBoolean("result")) {
		// ContentValues cvs = new ContentValues();
		// cvs.put(UrlTable.URL_SHORT, resp.getString("url_short"));
		// cvs.put(UrlTable.URL_LONG, resp.getString("url_long"));
		// cvs.put(UrlTable.URL_TYPE, resp.getString("type"));
		// cvs.put(UrlTable.STATUS_ID, s.getLong("id"));
		//
		// context.getContentResolver().insert(
		// Provider.URL_CONTENT_URI, cvs);
		//
		// Util.profile(context, "url_short.csv", rlt);
		// } else {
		// Util.log(url + " expend failed!");
		// }
		// }
		// } catch (Exception e) {
		// // nothing
		// }

		values.put(TimelineTable.FAVORITED, s.getBoolean("favorited"));
		// values.put(TimelineTable.IN_REPLY_TO_STATUS_ID,
		// s.getLong("in_reply_to_status_id"));
		// values.put(TimelineTable.IN_REPLY_TO_USER_ID,
		// s.getLong("in_reply_to_user_id"));
		// values.put(TimelineTable.IN_REPLY_TO_SCREEN_NAME,
		// s.getString("in_reply_to_user_id"));

		if (!s.isNull("thumbnail_pic")) {
			values.put(TimelineTable.THUMBNAIL_PIC,
					s.getString("thumbnail_pic"));
			values.put(TimelineTable.BMIDDLE_PIC, s.getString("bmiddle_pic"));
			values.put(TimelineTable.ORIGINAL_PIC, s.getString("original_pic"));
		}

		values.put(TimelineTable.GEO, s.getString("geo"));
		values.put(TimelineTable.REPOSTS_COUNT, s.getInt("reposts_count"));
		values.put(TimelineTable.COMMENTS_COUNT, s.getInt("comments_count"));
		if (!s.isNull("annotations")) {
			values.put(TimelineTable.ANNOTATIONS, s.getString("annotations"));
		}
		values.put(TimelineTable.AUTHOR_ID,
				s.getJSONObject("user").getLong("id"));

		Util.log("text " + s.getString("text"));

		if (!s.isNull("retweeted_status")) {
			JSONObject rt = s.getJSONObject("retweeted_status");
			values.put(TimelineTable.RETWEETED_STATUS, rt.getLong("id"));
			Util.log("retweeted original ");
			insertStatus(context, rt, null, null);
		}

		if (!s.isNull("user")) {
			insertUser(context, s.getJSONObject("user"));
		}

		context.getContentResolver().insert(Provider.TIMELINE_CONTENT_URI,
				values);

		// ----------------------------------------------------------
	}

	public static void insertUser(Context context, JSONObject u)
			throws Exception {
		ContentValues values = new ContentValues();

		values.put(UserTable.USER_ID, u.getLong("id"));
		values.put(UserTable.SCREEN_NAME, u.getString("screen_name"));
		values.put(UserTable.USER_NAME, u.getString("name"));
		values.put(UserTable.LOCATION, u.getString("location"));
		values.put(UserTable.DESCRIPTION, u.getString("description"));
		values.put(UserTable.PROFILE_IMAGE_URL,
				u.getString("profile_image_url"));
		values.put(UserTable.FOLLOWERS_COUNT, u.getInt("followers_count"));
		values.put(UserTable.FRIENDS_COUNT, u.getInt("friends_count"));
		values.put(UserTable.STATUSES_COUNT, u.getInt("statuses_count"));

		SimpleDateFormat parser = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
		String created = u.getString("created_at");
		long create_time = parser.parse(created).getTime();
		values.put(UserTable.CREATED_AT, create_time);

		values.put(UserTable.FOLLOWING, u.getBoolean("following"));
		values.put(UserTable.ALLOW_ALL_ACT_MSG,
				u.getBoolean("allow_all_act_msg"));
		values.put(UserTable.AVATAR_LARGE, u.getString("avatar_large"));
		values.put(UserTable.VERIFIED_REASON, u.getString("verified_reason"));
		values.put(UserTable.FOLLOW_ME, u.getBoolean("follow_me"));

		context.getContentResolver().insert(Provider.USER_CONTENT_URI, values);
	}

}