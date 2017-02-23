package com.metaisle.weik.weibo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class TaskHandler extends Handler {
	public static final int RUN_SUCCESS = 0x983;
	public static final int RUN_FAIL = 0x984;

	private Context mContext;

	public TaskHandler(Context context) {
		mContext = context;
	}

	public void success() {
		this.sendEmptyMessage(RUN_SUCCESS);
	}

	public void fail() {
		this.sendEmptyMessage(RUN_FAIL);
	}

	public void finallyRun() {
	}

	// Run on UI thread.
	protected void RunSuccess() {
	}

	protected void RunFail() {
		Toast.makeText(mContext, "", Toast.LENGTH_LONG).show();
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case RUN_SUCCESS:
			RunSuccess();
			break;
		case RUN_FAIL:
			RunFail();
			break;
		default:
			break;
		}

		finallyRun();
	}
}
