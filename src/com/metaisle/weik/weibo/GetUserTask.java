package com.metaisle.weik.weibo;

import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class GetUserTask extends AsyncTask<Void, Void, Void> {

	private Context mContext;
	private TaskHandler mTaskHandler = null;

	private String mUserName = null;

	public GetUserTask(Context context, String name) {
		mContext = context;
		mUserName = name;
		if (mUserName == null) {
			throw new IllegalArgumentException("no user name");
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		Weibo weibo = Prefs.getWeibo(mContext);

		Util.log("weibo " + weibo.isSessionValid());

		WeiboParameters bundle = new WeiboParameters();
		bundle.add("access_token", weibo.getAccessToken().getToken());
		bundle.add("screen_name", mUserName);

		try {
			String rlt = weibo.request(mContext, Weibo.SERVER
					+ "users/show.json", bundle, "GET", weibo.getAccessToken());
			JSONObject resp = new JSONObject(rlt);

			GetTimelineTask.insertUser(mContext, resp);

		} catch (Exception e) {
			if (mTaskHandler != null)
				mTaskHandler.sendEmptyMessage(TaskHandler.RUN_FAIL);
			e.printStackTrace();
		}

		return null;
	}

}
