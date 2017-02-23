package com.metaisle.weik.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.app.StatusActivity;
import com.metaisle.weik.app.UserActivity;
import com.metaisle.weik.app.WebActivity;
import com.metaisle.weik.data.CommentTable;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.data.UserTable;
import com.metaisle.weik.fragment.CommentFragment.CommentType;
import com.metaisle.weik.weibo.GetCommentTask;
import com.metaisle.weik.weibo.GetTimelineTask;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;
import com.metaisle.weik.weibo.TaskHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

public class StatusFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor> {
	public SharedPreferences mPrefs;
	public static final String KEY_STATUS_ID = "key_status_id";
	private long mStatusID;

	public ImageLoader imageLoader;

	private ImageView profileView;
	private ImageView rtProfileView;
	private ImageView picView;
	private ImageView rtPicView;

	private TextView userNameView;
	private TextView statusView;
	private TextView timeSpanView;
	private TextView rtUserNameView;
	private TextView rtStatusView;
	private TextView rtTimeSpanView;

	private LinearLayout counterContainer;
	private LinearLayout controlContainer;
	private Button commentCounterBtn;
	private Button repostCounterBtn;
	private Button bigPicBtn;

	private RelativeLayout rtContainer;
	private RelativeLayout fragItem;

	private ListView mListView;

	private static final int LOADER_ID_STATUS = 0x30;
	private static final int LOADER_ID_COMMENTS = 0x31;
	private static final int LOADER_ID_REPOSTS = 0x32;

	private int commentCount = -1;
	private int repostCount = -1;

	private StatusCommentRepostAdapter mAdapter;

	private GetCommentTask commentTask;
	private GetTimelineTask timelineTask;

	public Cursor mStatusCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = Prefs.imageLoader(getActivity());

		Bundle b = getArguments();
		if (b != null) {
			mStatusID = b.getLong(KEY_STATUS_ID);
		}

		boolean reminded_status_swipe = mPrefs.getBoolean(
				Prefs.KEY_REMINDED_STATUS_SWIPE, false);

		if (!reminded_status_swipe) {

		}

		mAdapter = new StatusCommentRepostAdapter(getActivity(), null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		getLoaderManager().initLoader(LOADER_ID_STATUS, null, this);
		getLoaderManager().initLoader(LOADER_ID_COMMENTS, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mListView = new ListView(getActivity());

		View v = inflater.inflate(R.layout.fragment_timeline_item, null);

		mListView.addHeaderView(v);

		View footer = inflater.inflate(R.layout.control_placeholder, null);
		mListView.addFooterView(footer);
		mListView.setFooterDividersEnabled(false);

		profileView = (ImageView) v.findViewById(R.id.profile_image);
		rtProfileView = (ImageView) v.findViewById(R.id.rt_profile_image);
		picView = (ImageView) v.findViewById(R.id.status_pic);
		rtPicView = (ImageView) v.findViewById(R.id.rt_status_pic);

		// ----Change ImageView to wrap_content
		LayoutParams lp = picView.getLayoutParams();
		lp.height = lp.width = LayoutParams.WRAP_CONTENT;
		picView.setLayoutParams(lp);

		lp = rtPicView.getLayoutParams();
		lp.height = lp.width = LayoutParams.WRAP_CONTENT;
		rtPicView.setLayoutParams(lp);

		userNameView = (TextView) v.findViewById(R.id.user_name);
		statusView = (TextView) v.findViewById(R.id.status_text);
		timeSpanView = (TextView) v.findViewById(R.id.time_span);
		rtUserNameView = (TextView) v.findViewById(R.id.rt_user_name);
		rtStatusView = (TextView) v.findViewById(R.id.rt_status_text);
		rtTimeSpanView = (TextView) v.findViewById(R.id.rt_time_span);

		counterContainer = (LinearLayout) v
				.findViewById(R.id.counter_container);
		lp = counterContainer.getLayoutParams();
		controlContainer = (LinearLayout) inflater.inflate(
				R.layout.control_comment_repost, null);
		controlContainer.setLayoutParams(lp);
		RelativeLayout status = (RelativeLayout) v;
		status.removeView(counterContainer);
		status.addView(controlContainer);

		commentCounterBtn = (Button) v.findViewById(R.id.comment_switch);
		repostCounterBtn = (Button) v.findViewById(R.id.repost_switch);
		bigPicBtn = (Button) v.findViewById(R.id.big_pic_btn);

		commentCounterBtn.setBackgroundColor(Color.WHITE);
		repostCounterBtn.setBackgroundColor(Color.WHITE);

		rtContainer = (RelativeLayout) v.findViewById(R.id.retweet_container);

		fragItem = (RelativeLayout) v.findViewById(R.id.frag_item);

		userNameView.getPaint().setFakeBoldText(true);
		rtUserNameView.getPaint().setFakeBoldText(true);

		mListView.setAdapter(mAdapter);

		return mListView;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// ===========================================================
		if (id == LOADER_ID_STATUS) {
			return new CursorLoader(getActivity(),
					Provider.TIMELINE_CONTENT_URI, new String[] {
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable._ID,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.AUTHOR_ID,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.STATUS_ID,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.STATUS_TEXT,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.CREATED_AT,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.RETWEETED_STATUS,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.THUMBNAIL_PIC,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.BMIDDLE_PIC,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.ORIGINAL_PIC,

							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.COMMENTS_COUNT,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.REPOSTS_COUNT,
							UserTable.USER_TABLE + "."
									+ UserTable.PROFILE_IMAGE_URL,
							UserTable.USER_TABLE + "." + UserTable.USER_NAME,

							"RT." + TimelineTable.AUTHOR_ID + " AS RT_"
									+ TimelineTable.AUTHOR_ID,
							"RT." + TimelineTable.STATUS_ID + " AS RT_"
									+ TimelineTable.STATUS_ID,
							"RT." + TimelineTable.STATUS_TEXT + " AS RT_"
									+ TimelineTable.STATUS_TEXT,
							"RT." + TimelineTable.CREATED_AT + " AS RT_"
									+ TimelineTable.CREATED_AT,
							"RT." + TimelineTable.THUMBNAIL_PIC + " AS RT_"
									+ TimelineTable.THUMBNAIL_PIC,
							"RT." + TimelineTable.BMIDDLE_PIC + " AS RT_"
									+ TimelineTable.BMIDDLE_PIC,
							"RT." + TimelineTable.ORIGINAL_PIC + " AS RT_"
									+ TimelineTable.ORIGINAL_PIC,

							"RT_USER." + UserTable.PROFILE_IMAGE_URL
									+ " AS RT_USER_"
									+ UserTable.PROFILE_IMAGE_URL,
							"RT_USER." + UserTable.USER_NAME + " AS RT_USER_"
									+ UserTable.USER_NAME

					}, TimelineTable.TIMELINE_TABLE + "."
							+ TimelineTable.STATUS_ID + "=?",
					new String[] { String.valueOf(mStatusID) },
					TimelineTable.TIMELINE_TABLE + "."
							+ TimelineTable.CREATED_AT + " DESC");
		}

		// ===========================================================
		else if (id == LOADER_ID_COMMENTS) {
			return new CursorLoader(
					getActivity(),
					Provider.COMMENT_CONTENT_URI,

					new String[] {
							CommentTable.COMMENT_TABLE + "." + CommentTable._ID,
							CommentTable.COMMENT_TABLE + "."
									+ CommentTable.CREATED_AT,
							CommentTable.COMMENT_TABLE + "."
									+ CommentTable.COMMENT_ID,
							CommentTable.COMMENT_TABLE + "."
									+ CommentTable.COMMENT_TEXT,
							CommentTable.COMMENT_TABLE + "."
									+ CommentTable.AUTHOR_ID,
							CommentTable.COMMENT_TABLE + "."
									+ CommentTable.REPLIED_STATUS,
							UserTable.USER_TABLE + "."
									+ UserTable.PROFILE_IMAGE_URL,
							UserTable.USER_TABLE + "." + UserTable.USER_NAME },

					CommentTable.COMMENT_TABLE + "."
							+ CommentTable.REPLIED_STATUS + "=?",
					new String[] { String.valueOf(mStatusID) },
					CommentTable.COMMENT_TABLE + "." + CommentTable.CREATED_AT
							+ " DESC");
		}
		// ===========================================================
		else if (id == LOADER_ID_REPOSTS) {
			return new CursorLoader(getActivity(),
					Provider.TIMELINE_CONTENT_URI,

					new String[] {
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable._ID,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.CREATED_AT,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.STATUS_ID,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.STATUS_TEXT,
							TimelineTable.TIMELINE_TABLE + "."
									+ TimelineTable.AUTHOR_ID,
							UserTable.USER_TABLE + "."
									+ UserTable.PROFILE_IMAGE_URL,
							UserTable.USER_TABLE + "." + UserTable.USER_NAME },

					TimelineTable.TIMELINE_TABLE + "."
							+ TimelineTable.DIRECT_REPOST + "=?",
					new String[] { String.valueOf(mStatusID) },
					TimelineTable.TIMELINE_TABLE + "."
							+ TimelineTable.CREATED_AT + " DESC");
		}

		// ===========================================================
		else {
			throw new IllegalArgumentException("Unknown loader ID");
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		// ===========================================================
		if (loader.getId() == LOADER_ID_STATUS) {
			mStatusCursor = cursor;
			Util.log("status cursor " + cursor.getCount());
			cursor.moveToFirst();

			// -------------------------------------------------
			final long uid = cursor.getLong(cursor
					.getColumnIndex(TimelineTable.AUTHOR_ID));
			profileView.setTag(R.id.TAG_USER_ID, uid);
			imageLoader.displayImage(cursor.getString(cursor
					.getColumnIndex(UserTable.PROFILE_IMAGE_URL)),
					(ImageView) profileView);

			Util.log(cursor.getString(cursor
					.getColumnIndex(UserTable.PROFILE_IMAGE_URL)));

			profileView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(getActivity(), UserActivity.class);
					i.putExtra(UserActivity.KEY_USER_ID, uid);
					startActivity(i);
				}
			});

			// -------------------------------------------------
			long time = cursor.getLong(cursor
					.getColumnIndex(TimelineTable.CREATED_AT));
			timeSpanView.setText(DateUtils.getRelativeTimeSpanString(time));

			// -------------------------------------------------
			String username = cursor.getString(cursor
					.getColumnIndex(UserTable.USER_NAME));
			userNameView.setText(username);
			userNameView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent i = new Intent(getActivity(), UserActivity.class);
					i.putExtra(UserActivity.KEY_USER_ID, uid);
					startActivity(i);
				}
			});

			// -------------------------------------------------
			String status = cursor.getString(cursor
					.getColumnIndex(TimelineTable.STATUS_TEXT));
			statusView.setText(status);
			Prefs.extractMention2Link(getActivity(), statusView);

			// -------------------------------------------------
			String thumb = cursor.getString(cursor
					.getColumnIndex(TimelineTable.THUMBNAIL_PIC));
			final String bmid = cursor.getString(cursor
					.getColumnIndex(TimelineTable.BMIDDLE_PIC));
			final String orig = cursor.getString(cursor
					.getColumnIndex(TimelineTable.ORIGINAL_PIC));

			if (bmid == null) {
				picView.setVisibility(View.GONE);
			} else if (commentCount < 0) {
				picView.setVisibility(View.VISIBLE);
				imageLoader.displayImage(thumb, picView);

				picView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (orig.endsWith(".gif")) {
							Util.log("orig " + orig);
							LayoutParams lp = picView.getLayoutParams();
							lp.height = LayoutParams.WRAP_CONTENT;
							lp.width = LayoutParams.MATCH_PARENT;
							WebView wv = new WebView(getActivity());
							wv.setLayoutParams(lp);
							wv.loadUrl(orig);
							wv.setVisibility(View.VISIBLE);
							fragItem.addView(wv);
							picView.setVisibility(View.GONE);
							Util.profile(getActivity(), "image_click.csv", orig);
							imageLoader.displayImage(orig, picView,
									new SimpleImageLoadingListener() {
										@Override
										public void onLoadingComplete(
												Bitmap loadedImage) {
											long size = imageLoader
													.getDiscCache().get(orig)
													.length();
											Util.log(imageLoader.getDiscCache()
													.get(orig).getName()
													+ " size " + size);
											Util.profile(getActivity(),
													"image_click.csv", orig
															+ ", " + size);
										}
									});
						} else {
							imageLoader.displayImage(bmid, picView,
									new SimpleImageLoadingListener() {
										@Override
										public void onLoadingComplete(
												Bitmap loadedImage) {
											long size = imageLoader
													.getDiscCache().get(bmid)
													.length();
											Util.log(imageLoader.getDiscCache()
													.get(bmid).getName()
													+ " size " + size);
											Util.profile(getActivity(),
													"image_click.csv", bmid
															+ ", " + size);
										}
									});
							Util.profile(getActivity(), "image_click.csv", bmid);
						}
					}
				});

			}

			// -------------------------------------------------
			if (cursor.isNull(cursor
					.getColumnIndex(TimelineTable.RETWEETED_STATUS))) {
				rtContainer.setVisibility(View.GONE);
			} else {
				rtContainer.setVisibility(View.VISIBLE);

				if (!cursor.isNull(cursor.getColumnIndex("RT_"
						+ TimelineTable.STATUS_TEXT))) {
					final long rt_sid = cursor.getLong(cursor
							.getColumnIndex("RT_" + TimelineTable.STATUS_ID));

					rtContainer.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							Intent i = new Intent(getActivity(),
									StatusActivity.class);
							i.putExtra(StatusFragment.KEY_STATUS_ID, rt_sid);
							getActivity().startActivity(i);
						}
					});
				}

				// -------------------------------------------------
				// -------------------------------------------------
				final long rtuid = cursor.getLong(cursor.getColumnIndex("RT_"
						+ TimelineTable.AUTHOR_ID));
				rtProfileView.setTag(R.id.TAG_USER_ID, rtuid);
				imageLoader.displayImage(
						cursor.getString(cursor.getColumnIndex("RT_USER_"
								+ UserTable.PROFILE_IMAGE_URL)),
						(ImageView) rtProfileView);

				rtProfileView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(getActivity(), UserActivity.class);
						i.putExtra(UserActivity.KEY_USER_ID, rtuid);
						startActivity(i);
					}
				});

				// -------------------------------------------------
				time = cursor.getLong(cursor.getColumnIndex("RT_"
						+ TimelineTable.CREATED_AT));
				rtTimeSpanView.setText(DateUtils
						.getRelativeTimeSpanString(time));

				// -------------------------------------------------
				username = cursor.getString(cursor.getColumnIndex("RT_USER_"
						+ UserTable.USER_NAME));
				rtUserNameView.setText(username);
				rtUserNameView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent i = new Intent(getActivity(), UserActivity.class);
						i.putExtra(UserActivity.KEY_USER_ID, rtuid);
						startActivity(i);
					}
				});

				// -------------------------------------------------
				status = cursor.getString(cursor.getColumnIndex("RT_"
						+ TimelineTable.STATUS_TEXT));
				rtStatusView.setText(status);
				Prefs.extractMention2Link(getActivity(), rtStatusView);

				// -------------------------------------------------
				String rt_thumb = cursor.getString(cursor.getColumnIndex("RT_"
						+ TimelineTable.THUMBNAIL_PIC));
				final String rt_bmid = cursor.getString(cursor
						.getColumnIndex("RT_" + TimelineTable.BMIDDLE_PIC));
				final String rt_orig = cursor.getString(cursor
						.getColumnIndex("RT_" + TimelineTable.ORIGINAL_PIC));

				if (rt_bmid == null) {
					rtPicView.setVisibility(View.GONE);
				} else if (commentCount < 0) {
					rtPicView.setVisibility(View.VISIBLE);
					imageLoader.displayImage(rt_thumb, rtPicView);

					rtPicView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (rt_orig.endsWith(".gif")) {
								Util.log("orig " + rt_orig);
								LayoutParams lp = rtPicView.getLayoutParams();
								lp.height = LayoutParams.MATCH_PARENT;
								lp.width = LayoutParams.MATCH_PARENT;
								WebView wv = new WebView(getActivity());
								wv.setLayoutParams(lp);
								wv.loadUrl(rt_orig);
								wv.setVisibility(View.VISIBLE);
								rtContainer.addView(wv);
								rtPicView.setVisibility(View.GONE);
								Util.profile(getActivity(), "image_click.csv",
										rt_orig);
							} else {
								imageLoader.displayImage(rt_bmid, rtPicView);
								Util.profile(getActivity(), "image_click.csv",
										rt_bmid);
							}
						}
					});
				}
			}
			// -------------------------------------------------
			// -------------------------------------------------
			commentCount = cursor.getInt(cursor
					.getColumnIndex(TimelineTable.COMMENTS_COUNT));
			commentCounterBtn.setText(String.valueOf(commentCount));
			commentCounterBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Util.profile(getActivity(), "social.csv", "get_comment, "
							+ mStatusID);
					commentCounterBtn.setEnabled(false);
					repostCounterBtn.setEnabled(false);

					commentCounterBtn.setBackgroundColor(Color.LTGRAY);
					repostCounterBtn.setBackgroundColor(Color.WHITE);

					if (commentTask != null)
						commentTask.cancel(true);
					if (timelineTask != null)
						timelineTask.cancel(true);
					if (commentTask == null
							|| commentTask.getStatus() != Status.RUNNING) {
						Util.log("getting comments");
						commentTask = new GetCommentTask(getActivity(),
								CommentType.SHOW).setStatusID(mStatusID)
								.setHandler(new CommentHandler(getActivity()));
						commentTask.execute();
					}

					getLoaderManager().initLoader(LOADER_ID_COMMENTS, null,
							StatusFragment.this);
				}
			});

			// -------------------------------------------------
			repostCount = cursor.getInt(cursor
					.getColumnIndex(TimelineTable.REPOSTS_COUNT));
			repostCounterBtn.setText(String.valueOf(repostCount));
			repostCounterBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Util.profile(getActivity(), "social.csv", "get_repost, "
							+ mStatusID);
					commentCounterBtn.setEnabled(false);
					repostCounterBtn.setEnabled(false);

					commentCounterBtn.setBackgroundColor(Color.WHITE);
					repostCounterBtn.setBackgroundColor(Color.LTGRAY);

					if (commentTask != null)
						commentTask.cancel(true);
					if (timelineTask != null)
						timelineTask.cancel(true);
					if (timelineTask == null
							|| timelineTask.getStatus() != Status.RUNNING) {
						Util.log("getting reposts");
						timelineTask = new GetTimelineTask(getActivity(),
								TimelineType.REPOST).setRepostedID(mStatusID)
								.setHandler(new RepostHandler(getActivity()));
						timelineTask.execute();
					}

					getLoaderManager().initLoader(LOADER_ID_REPOSTS, null,
							StatusFragment.this);
				}
			});

			// -------------------------------------------------
			final String rt_orig = cursor.getString(cursor.getColumnIndex("RT_"
					+ TimelineTable.ORIGINAL_PIC));
			if (orig != null) {
				bigPicBtn.setVisibility(View.VISIBLE);
				bigPicBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// String asset = "file://"
						// + Prefs.cachingLoader(getActivity())
						// .getDiscCache().get(orig)
						// .getAbsolutePath();
						//
						// Util.log("asset "+asset);

						Intent intent = new Intent(getActivity(),
								WebActivity.class);
						intent.putExtra(WebActivity.KEY_URL, orig);
						getActivity().startActivity(intent);
					}
				});
			} else if (rt_orig != null) {
				bigPicBtn.setVisibility(View.VISIBLE);
				bigPicBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// String asset = "file://"
						// + Prefs.cachingLoader(getActivity())
						// .getDiscCache().get(rt_orig)
						// .getAbsolutePath();
						//
						// Util.log("asset "+asset);

						Intent intent = new Intent(getActivity(),
								WebActivity.class);
						intent.putExtra(WebActivity.KEY_URL, rt_orig);
						getActivity().startActivity(intent);
					}
				});
			} else {
				bigPicBtn.setVisibility(View.GONE);
			}

		}

		// ===========================================================
		else if (loader.getId() == LOADER_ID_COMMENTS) {
			Util.log("comments cursor " + cursor.getCount());

			mAdapter.swapCursor(cursor);
		}

		// ===========================================================
		else if (loader.getId() == LOADER_ID_REPOSTS) {
			Util.log("reposts cursor " + cursor.getCount());

			mAdapter.swapCursor(cursor);
		}

		else {
			throw new IllegalArgumentException("Unknown loader ID");
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}

	class CommentHandler extends TaskHandler {

		public CommentHandler(Context context) {
			super(context);
		}

		@Override
		public void finallyRun() {
			commentCounterBtn.setEnabled(true);
			repostCounterBtn.setEnabled(true);
		}
	}

	class RepostHandler extends TaskHandler {

		public RepostHandler(Context context) {
			super(context);
		}

		@Override
		public void finallyRun() {
			commentCounterBtn.setEnabled(true);
			repostCounterBtn.setEnabled(true);
		}

	}

	public String getUserName() {
		if (mStatusCursor == null || mStatusCursor.getCount() == 0) {
			return "";
		}

		return mStatusCursor.getString(mStatusCursor
				.getColumnIndex(UserTable.USER_NAME));
	}

	public String getRTUserName() {
		if (mStatusCursor == null || mStatusCursor.getCount() == 0) {
			return "";
		}

		return mStatusCursor.getString(mStatusCursor.getColumnIndex("RT_USER_"
				+ UserTable.USER_NAME));
	}

	public long getStatusIdorRepostId4Repost() {
		if (mStatusCursor == null || mStatusCursor.getCount() == 0) {
			return 0;
		}

		if (mStatusCursor.isNull(mStatusCursor
				.getColumnIndex(TimelineTable.RETWEETED_STATUS))) {
			return mStatusCursor.getLong(mStatusCursor
					.getColumnIndex(TimelineTable.STATUS_ID));
		} else {
			return mStatusCursor.getLong(mStatusCursor
					.getColumnIndex(TimelineTable.RETWEETED_STATUS));
		}
	}

	public long getStatusID() {
		return mStatusID;
	}

	public String getText4Repost() {
		if (mStatusCursor == null || mStatusCursor.getCount() == 0) {
			return "";
		}

		if (mStatusCursor.isNull(mStatusCursor
				.getColumnIndex(TimelineTable.RETWEETED_STATUS))) {
			return "@"
					+ getUserName()
					+ ":\n"
					+ mStatusCursor.getString(mStatusCursor
							.getColumnIndex(TimelineTable.STATUS_TEXT));
		} else {
			String rt_user = getRTUserName();
			return "@"
					+ rt_user
					+ ":\n"
					+ mStatusCursor.getString(mStatusCursor
							.getColumnIndex("RT_" + TimelineTable.STATUS_TEXT));
		}
	}

	public String getText4Comment() {
		if (mStatusCursor == null || mStatusCursor.getCount() == 0) {
			return "";
		}
		if (mStatusCursor.isNull(mStatusCursor
				.getColumnIndex(TimelineTable.RETWEETED_STATUS))) {
			return "@"
					+ getUserName()
					+ ":\n"
					+ mStatusCursor.getString(mStatusCursor
							.getColumnIndex(TimelineTable.STATUS_TEXT));
		} else {

			return "@"
					+ getUserName()
					+ ":\n"
					+ mStatusCursor.getString(mStatusCursor
							.getColumnIndex(TimelineTable.STATUS_TEXT))
					+ "\n@"
					+ getRTUserName()
					+ ":\n"
					+ mStatusCursor.getString(mStatusCursor
							.getColumnIndex("RT_" + TimelineTable.STATUS_TEXT));
		}
	}

	public String getPretext() {
		if (mStatusCursor == null || mStatusCursor.getCount() == 0) {
			return "";
		}

		if (mStatusCursor.isNull(mStatusCursor
				.getColumnIndex(TimelineTable.RETWEETED_STATUS))) {
			return "";
		} else {
			String username = mStatusCursor.getString(mStatusCursor
					.getColumnIndex(UserTable.USER_NAME));
			return "// @"
					+ username
					+ ": "
					+ mStatusCursor.getString(mStatusCursor
							.getColumnIndex(TimelineTable.STATUS_TEXT));
		}

	}

}
