package com.metaisle.weik.weibo;

import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class CommentRepostTask extends AsyncTask<Void, Void, Void> {
	private Context mContext;
	private String mText = null;

	private long mRepostStatusID = 0;
	private long mCommentStatusID = 0;
	private long mCommentReplyID = 0;

	private TaskHandler mTaskHandler = null;

	public CommentRepostTask(Context context, String text, long repostStatus,
			long commentStatus, long commentReply, TaskHandler handler) {
		mContext = context;
		mText = text;

		mRepostStatusID = repostStatus;
		mCommentStatusID = commentStatus;
		mCommentReplyID = commentReply;

		mTaskHandler = handler;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Weibo weibo = Prefs.getWeibo(mContext);

		WeiboParameters b = new WeiboParameters();
		b.add("access_token", weibo.getAccessToken().getToken());

		String url;
		if (mRepostStatusID != 0) {
			url = "statuses/repost.json";
			b.add("id", String.valueOf(mRepostStatusID));
			if (mText != null) {
				b.add("status", mText);
			}
		} else {
			b.add("id", String.valueOf(mCommentStatusID));
			b.add("comment", mText);
			if (mCommentReplyID == 0) {
				url = "comments/create.json";
			} else {
				url = "comments/reply.json";
				b.add("cid", String.valueOf(mCommentReplyID));
			}
		}

		try {
			weibo.request(mContext, Weibo.SERVER + url, b, "POST",
					weibo.getAccessToken());

		} catch (Exception e) {
			if (mTaskHandler != null)
				mTaskHandler.fail();
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		Util.profile(mContext, "social.csv", "comment_repost, "
				+ mRepostStatusID + ", " + mCommentStatusID + ", "
				+ mCommentReplyID);
		mTaskHandler.success();
	}
}
