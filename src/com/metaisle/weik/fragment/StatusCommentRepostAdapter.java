package com.metaisle.weik.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.metaisle.weik.R;
import com.metaisle.weik.app.CommentRepostActivity;
import com.metaisle.weik.app.UserActivity;
import com.metaisle.weik.data.CommentTable;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.data.UserTable;
import com.nostra13.universalimageloader.core.ImageLoader;

public class StatusCommentRepostAdapter extends CursorAdapter {
	private Context mContext;
	private LayoutInflater mInflater;
	private ImageLoader mImageLoader;

	public StatusCommentRepostAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mContext = context;
		mInflater = LayoutInflater.from(mContext);
		mImageLoader = Prefs.imageLoader(mContext);
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		View v = mInflater.inflate(R.layout.fragment_status_comment, null);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		TextView timeSpan = (TextView) view.findViewById(R.id.time_span);
		TextView statusText = (TextView) view.findViewById(R.id.status_text);
		TextView userName = (TextView) view.findViewById(R.id.user_name);
		ImageView commentBtn = (ImageView) view.findViewById(R.id.comment_iv);
		ImageView profileImage = (ImageView) view
				.findViewById(R.id.profile_image);

		long created;
		final String text;
		String name = cursor.getString(cursor
				.getColumnIndex(UserTable.USER_NAME));
		String profile = cursor.getString(cursor
				.getColumnIndex(UserTable.PROFILE_IMAGE_URL));

		created = cursor
				.getLong(cursor.getColumnIndex(CommentTable.CREATED_AT));
		text = cursor.getString(cursor
				.getColumnIndex(CommentTable.COMMENT_TEXT));
		final long cid = cursor.getLong(cursor
				.getColumnIndex(CommentTable.COMMENT_ID));
		final long uid = cursor.getLong(cursor
				.getColumnIndex(CommentTable.AUTHOR_ID));

		long sid = -1;
		if (!cursor.isNull(cursor.getColumnIndex(CommentTable.REPLIED_STATUS))) {
			sid = cursor.getLong(cursor
					.getColumnIndex(CommentTable.REPLIED_STATUS));
		}

		final long rep_sid = sid;

		sid = cursor.getLong(cursor.getColumnIndex(TimelineTable.STATUS_ID));
		final long orig_sid = sid;

		timeSpan.setText(DateUtils.getRelativeTimeSpanString(created));
		statusText.setText(text);
		Prefs.extractMention2Link(mContext, statusText);
		
		userName.setText(name);
		mImageLoader.displayImage(profile, profileImage);
		profileImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(mContext, UserActivity.class);
				i.putExtra(UserActivity.KEY_USER_ID, uid);
				mContext.startActivity(i);
			}
		});

		commentBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();
				if (rep_sid > 0) {
					b.putLong(CommentRepostActivity.KEY_COMMENT_STATUS_ID,
							rep_sid);
				} else {
					b.putLong(CommentRepostActivity.KEY_REPOST_STATUS_ID,
							orig_sid);
				}
				b.putLong(CommentRepostActivity.KEY_COMMENT_REPLY_ID, cid);
				b.putString(CommentRepostActivity.KEY_ORIGINAL_TEXT, text);
				Intent i = new Intent(mContext, CommentRepostActivity.class);
				i.putExtras(b);

				mContext.startActivity(i);
			}
		});
	}

}
