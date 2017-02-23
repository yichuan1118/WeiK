package com.metaisle.weik.weibo;

import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class TweetTask extends AsyncTask<Void, Void, Boolean> {
	private Context mContext;

	private String mText;
	private String mPic;

	private TaskHandler mTaskHandler = null;

	public TweetTask(Context context, String text, String pic,
			TaskHandler handler) {
		mContext = context;
		mText = text;
		mPic = pic;
		mTaskHandler = handler;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Weibo weibo = Prefs.getWeibo(mContext);

		WeiboParameters b = new WeiboParameters();
		b.add("access_token", weibo.getAccessToken().getToken());
		b.add("status", mText);
		String url;
		if (mPic == null) {
			url = "statuses/update.json";
		} else {
			url = "statuses/upload.json";
			b.add("pic", mPic);
		}

		try {
			weibo.request(mContext, Weibo.SERVER + url, b, "POST",
					weibo.getAccessToken());

			return true;
		} catch (Exception e) {
			if (mTaskHandler != null)
				mTaskHandler.fail();
			e.printStackTrace();
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		Util.profile(mContext, "social.csv", "tweet, " + mText + ", " + mPic);
		if (mTaskHandler != null)
			mTaskHandler.success();
	}
}
