package com.metaisle.weik.weibo;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.weik.data.CommentTable;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.fragment.CommentFragment.CommentType;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class GetCommentTask extends AsyncTask<Void, Void, Void> {

	private CommentType mType;

	private Context mContext;
	private TaskHandler mTaskHandler = null;
	private long mSinceID = 0;
	private long mMaxID = 0;
	private long mStatusID = 0;

	public GetCommentTask(Context context, CommentType type) {
		mContext = context;
		mType = type;
	}

	public GetCommentTask setSinceID(long since) {
		mSinceID = since;
		return this;
	}

	public GetCommentTask setMaxID(long max) {
		mMaxID = max;
		return this;
	}

	public GetCommentTask setStatusID(long id) {
		mStatusID = id;
		return this;
	}

	public GetCommentTask setHandler(TaskHandler handler) {
		mTaskHandler = handler;
		return this;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Weibo weibo = Prefs.getWeibo(mContext);

		WeiboParameters bundle = new WeiboParameters();
		bundle.add("access_token", weibo.getAccessToken().getToken());

		if (mSinceID != 0) {
			bundle.add("since_id", String.valueOf(mSinceID));
		}

		if (mMaxID != 0) {
			bundle.add("max_id", String.valueOf(mMaxID));
		}

		String url, value = null;
		String key = null;

		switch (mType) {
		case SHOW:
			url = Weibo.SERVER + "comments/show.json";
			if (mStatusID == 0)
				throw new IllegalArgumentException("Status ID is 0");
			bundle.add("id", String.valueOf(mStatusID));
			key = CommentTable.REPLIED_STATUS;
			value = String.valueOf(mStatusID);
			break;
		case TO_ME:
			url = Weibo.SERVER + "comments/to_me.json";
			key = CommentTable.IS_TO_ME;
			value = "1";
			break;
		case MENTION:
			url = Weibo.SERVER + "comments/mentions.json";
			key = CommentTable.IS_MENTIONS;
			value = "1";
			break;
		default:
			throw new IllegalStateException("Unknown Comment Type");
		}

		try {
			String rlt = weibo.request(mContext, url, bundle, "GET",
					weibo.getAccessToken());
			insertComments(mContext, rlt, key, value);
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
		if (mTaskHandler != null) {
			mTaskHandler.sendEmptyMessage(TaskHandler.RUN_SUCCESS);
		}
	}

	/*
	 * Misc.
	 */
	public static void insertComments(Context context, String response,
			String key, String value) throws Exception {
		JSONObject resp = new JSONObject(response);
		JSONArray comments = resp.getJSONArray("comments");

		for (int i = 0; i < comments.length(); i++) {
			insertComment(context, comments.getJSONObject(i), key, value);
		}
	}

	public static void insertComment(Context context, JSONObject comment,
			String key, String value) throws Exception {

		// Util.log("---------------------------------------------------------");
		// Util.log("comment " + comment.toString(2));

		SimpleDateFormat parser = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
		String created = comment.getString("created_at");
		long create_time = parser.parse(created).getTime();

		// ----------------------------------------------------------
		ContentValues values = new ContentValues();

		if (key != null) {
			values.put(key, value);
		}

		values.put(CommentTable.CREATED_AT, create_time);
		values.put(CommentTable.COMMENT_ID, comment.getString("id"));
		values.put(CommentTable.COMMENT_TEXT, comment.getString("text"));
		values.put(CommentTable.AUTHOR_ID, comment.getJSONObject("user")
				.getString("id"));

		if (!comment.isNull("status")) {
			values.put(CommentTable.REPLIED_STATUS,
					comment.getJSONObject("status").getLong("id"));
		}

		if (key != CommentTable.REPLIED_STATUS && !comment.isNull("status")) {
			GetTimelineTask.insertStatus(context,
					comment.getJSONObject("status"), null, null);
		}

		// Util.log("text " + comment.getString("text"));

		if (!comment.isNull("user")) {
			GetTimelineTask.insertUser(context, comment.getJSONObject("user"));
		}

		if (!comment.isNull("reply_comment")) {
			JSONObject reply_comment = comment.getJSONObject("reply_comment");
			values.put(CommentTable.REPLIED_COMMENT,
					reply_comment.getLong("id"));
			insertComment(context, reply_comment, CommentTable.REPLIED_STATUS,
					"" + comment.getJSONObject("status").getLong("id"));
		}

		context.getContentResolver().insert(Provider.COMMENT_CONTENT_URI,
				values);

		// Util.log("=============text " + comment.getString("text"));

		// ----------------------------------------------------------
	}
}
