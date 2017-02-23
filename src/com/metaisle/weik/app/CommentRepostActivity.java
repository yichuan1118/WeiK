package com.metaisle.weik.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.weibo.CommentRepostTask;
import com.metaisle.weik.weibo.TaskHandler;

public class CommentRepostActivity extends SherlockFragmentActivity {


	public static final String KEY_REPOST_STATUS_ID = "KEY_REPOST_STATUS_ID";
	public static final String KEY_COMMENT_STATUS_ID = "KEY_COMMENT_STATUS_ID";
	public static final String KEY_COMMENT_REPLY_ID = "KEY_COMMENT_REPLY_ID";
	public static final String KEY_ORIGINAL_TEXT = "KEY_ORIGINAL_TEXT";
	public static final String KEY_REPOST_PRETEXT = "KEY_REPOST_PRETEXT";

	private long mRepostStatusID;
	private long mCommentStatusID;
	private long mCommentReplyID;
	private String mOriginalText;
	private String mRepostPretext;

	private EditText mTweetEdit;
	private Button mTweetButton;
	private DiscardDialogFragment mDiscardDialogFragment;
	private TextView mOriginalTextView;
	NotificationManager mNotificationManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comment_repost);

		Bundle b = getIntent().getExtras();
		Util.log("KEY_COMMENT_REPLY_ID " + b.getLong(KEY_COMMENT_REPLY_ID));

		mRepostStatusID = b.getLong(KEY_REPOST_STATUS_ID);
		mCommentStatusID = b.getLong(KEY_COMMENT_STATUS_ID);
		mCommentReplyID = b.getLong(KEY_COMMENT_REPLY_ID);
		mOriginalText = b.getString(KEY_ORIGINAL_TEXT);
		mRepostPretext = b.getString(KEY_REPOST_PRETEXT);

		mTweetEdit = (EditText) findViewById(R.id.tweet_edit);
		if (mRepostPretext != null) {
			setTitle(R.string.repost);
			mTweetEdit.setText(mRepostPretext);
		} else {
			setTitle(R.string.comment);
		}
		mOriginalTextView = (TextView) findViewById(R.id.original_text);
		mOriginalTextView.setText(mOriginalText);

		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		mTweetButton = (Button) findViewById(R.id.tweet_button);

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mTweetEdit.getText().length() == 0) {
					finish();
				} else {
					promptExit();
				}
			}
		});

		mTweetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendTweet();
			}
		});

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onBackPressed() {
		if (mTweetEdit.getText().toString().length() == 0) {
			super.onBackPressed();
		} else {
			promptExit();
		}
	}

	@SuppressWarnings("deprecation")
	private void sendTweet() {
		String status_text = mTweetEdit.getText().toString();

		if (status_text.length() == 0) {
			return;
		}
		Util.log("tweeting " + status_text);
		mTweetEdit.setEnabled(false);
		mTweetButton.setEnabled(false);

		new CommentRepostTask(getApplicationContext(), status_text,
				mRepostStatusID, mCommentStatusID, mCommentReplyID,
				new TweetHandler(getApplicationContext())).execute();

		CharSequence tickerText = getString(R.string.sending);
		long when = System.currentTimeMillis();
		Notification notification = new Notification(R.drawable.ic_launcher,
				tickerText, when);
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name),
				getString(R.string.sending), contentIntent);
		mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID, notification);

		finish();
	}

	class TweetHandler extends TaskHandler {

		public TweetHandler(Context context) {
			super(context);
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void RunSuccess() {
			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);

			CharSequence tickerText = getString(R.string.send_success);
			long when = System.currentTimeMillis();
			Notification notification = new Notification(
					R.drawable.ic_launcher, tickerText, when);
			Intent notificationIntent = new Intent();
			PendingIntent contentIntent = PendingIntent.getActivity(
					CommentRepostActivity.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(CommentRepostActivity.this, "", "",
					contentIntent);
			mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID,
					notification);

			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void RunFail() {
			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);

			CharSequence tickerText = getString(R.string.send_failed);
			long when = System.currentTimeMillis();
			Notification notification = new Notification(
					R.drawable.ic_launcher, tickerText, when);
			Intent notificationIntent = new Intent();
			PendingIntent contentIntent = PendingIntent.getActivity(
					CommentRepostActivity.this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(CommentRepostActivity.this, "", "",
					contentIntent);
			mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID,
					notification);

			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);
		}

	}

	private void promptExit() {
		mDiscardDialogFragment = DiscardDialogFragment
				.newInstance(getString(R.string.discard_prompt));
		mDiscardDialogFragment.show(getSupportFragmentManager(), "discard");
	}

	public static class DiscardDialogFragment extends DialogFragment {

		public static DiscardDialogFragment newInstance(String title) {
			DiscardDialogFragment frag = new DiscardDialogFragment();
			Bundle args = new Bundle();
			args.putString("title", title);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			String title = getArguments().getString("title");

			return new AlertDialog.Builder(getActivity())
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(title)
					.setPositiveButton(getString(R.string.discard),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									getActivity().finish();
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dismiss();
								}
							}).create();
		}
	}
}
