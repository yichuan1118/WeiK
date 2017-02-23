package com.metaisle.weik.weibo;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Button;

import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.UserTable;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class FollowTask extends AsyncTask<Void, Void, Boolean> {
	private Context mContext;

	private long mTargetId;

	private Button mFollowButton;
	private TaskHandler mTaskHandler = null;

	public FollowTask(Context context, long target_id, Button btn) {
		mContext = context;
		mTargetId = target_id;
		mFollowButton = btn;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		Weibo weibo = Prefs.getWeibo(mContext);

		WeiboParameters b = new WeiboParameters();
		b.add("access_token", weibo.getAccessToken().getToken());
		b.add("uid", String.valueOf(mTargetId));

		try {
			ContentValues cvs = new ContentValues();

			if (mFollowButton.getText().toString()
					.equals(mContext.getString(R.string.follow))) {
				weibo.request(mContext, Weibo.SERVER
						+ "friendships/create.json", b, "POST",
						weibo.getAccessToken());

				cvs.put(UserTable.FOLLOWING, true);
				mContext.getContentResolver().update(Provider.USER_CONTENT_URI,
						cvs, UserTable.USER_ID + "=?",
						new String[] { String.valueOf(mTargetId) });
				return true;
			} else {
				weibo.request(mContext, Weibo.SERVER
						+ "friendships/destroy.json", b, "POST",
						weibo.getAccessToken());
				
				cvs.put(UserTable.FOLLOWING, false);
				mContext.getContentResolver().update(Provider.USER_CONTENT_URI,
						cvs, UserTable.USER_ID + "=?",
						new String[] { String.valueOf(mTargetId) });
				return false;
			}

		} catch (Exception e) {
			if (mTaskHandler != null)
				mTaskHandler.sendEmptyMessage(TaskHandler.RUN_FAIL);
			e.printStackTrace();
		}

		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mFollowButton.setEnabled(true);
		if (result) {
			mFollowButton.setText(mContext.getString(R.string.unfollow));
			Util.profile(mContext, "social.csv", "follow, " + mTargetId);
		} else {
			mFollowButton.setText(mContext.getString(R.string.follow));
			Util.profile(mContext, "social.csv", "unfollow, " + mTargetId);
		}

	}
}
