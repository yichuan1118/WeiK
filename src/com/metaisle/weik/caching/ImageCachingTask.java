package com.metaisle.weik.caching;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

public class ImageCachingTask extends AsyncTask<Void, Void, Boolean> {
	// public static boolean sRunning = false;
	SharedPreferences mPrefs;

	private Context mContext;

	private ImageLoader cachingLoader;
	ImageView dummyImageView;

	private String thumb;
	private String rt_thumb;
	private String bmid;
	private String rt_bmid;
	private String orig;
	private String rt_orig;
	private String mStatusText;
	private long mStatusID = -1;

	public ImageCachingTask(Context context) {
		mContext = context;
		mPrefs = mContext.getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);

		cachingLoader = Prefs.cachingLoader(mContext);
		dummyImageView = new ImageView(mContext);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		dummyImageView.setLayoutParams(lp);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		
		if(!Util.isOnWifi(mContext)){
			return null;
		}

		Cursor c = mContext.getContentResolver().query(
				Provider.TIMELINE_CONTENT_URI,
				new String[] {
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.STATUS_ID,

						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.THUMBNAIL_PIC,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.BMIDDLE_PIC,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.ORIGINAL_PIC,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.STATUS_TEXT,

						"RT." + TimelineTable.THUMBNAIL_PIC + " AS RT_"
								+ TimelineTable.THUMBNAIL_PIC,
						"RT." + TimelineTable.BMIDDLE_PIC + " AS RT_"
								+ TimelineTable.BMIDDLE_PIC,
						"RT." + TimelineTable.ORIGINAL_PIC + " AS RT_"
								+ TimelineTable.ORIGINAL_PIC,

				},
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.CACHED_AT
						+ " IS NULL AND " + TimelineTable.TIMELINE_TABLE + "."
						+ TimelineTable.IS_HOME + "=?",
				new String[] { "1" },
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.CREATED_AT
						+ " DESC LIMIT 1");

		if (!c.moveToFirst()) {
			Util.log("No more to cache");
			return false;
		}

		thumb = c.getString(c.getColumnIndex(TimelineTable.THUMBNAIL_PIC));
		rt_thumb = c.getString(c.getColumnIndex("RT_"
				+ TimelineTable.THUMBNAIL_PIC));
		bmid = c.getString(c.getColumnIndex(TimelineTable.BMIDDLE_PIC));
		rt_bmid = c.getString(c.getColumnIndex("RT_"
				+ TimelineTable.BMIDDLE_PIC));
		orig = c.getString(c.getColumnIndex(TimelineTable.ORIGINAL_PIC));
		rt_orig = c.getString(c.getColumnIndex("RT_"
				+ TimelineTable.ORIGINAL_PIC));
		mStatusText = c.getString(c.getColumnIndex(TimelineTable.STATUS_TEXT));

		mStatusID = c.getLong(c.getColumnIndex(TimelineTable.STATUS_ID));

		// ---------------------------------------------------------------

		Util.log("-----------------------------------------");
		Util.log("Cache tweet " + mStatusText);

		return false;
	}

	private void loadOrig(final String orig) {
		Util.log("orig " + orig);

		initWebView().loadUrl(orig);

		// cachingLoader.displayImage(orig, dummyImageView,
		// new SimpleImageLoadingListener() {
		// @Override
		// public void onLoadingComplete(Bitmap loadedImage) {
		// long size = cachingLoader.getDiscCache().get(orig)
		// .length();
		// Util.log(cachingLoader.getDiscCache().get(orig)
		// .getName()
		// + " orig size " + size);
		// Util.profile(mContext, "image_cache.csv", "orig, "
		// + orig + ", " + size);
		//
		// // -----------------------------
		// ContentValues values = new ContentValues();
		// values.put(TimelineTable.CACHED_AT,
		// System.currentTimeMillis());
		// mContext.getContentResolver().update(
		// Uri.withAppendedPath(
		// Provider.TIMELINE_CONTENT_URI, ""
		// + mStatusID), values, null,
		// null);
		// // -----------------------------
		//
		// new ImageCachingTask(mContext).execute();
		// }
		//
		// @Override
		// public void onLoadingFailed(FailReason failReason) {
		// new ImageCachingTask(mContext).execute();
		// }
		// });
	}

	private void loadBmid(final String bmid, final String orig) {
		Util.log("bmid " + bmid);

		cachingLoader.displayImage(bmid, dummyImageView,
				new SimpleImageLoadingListener() {
					@Override
					public void onLoadingComplete(Bitmap loadedImage) {
						long size = cachingLoader.getDiscCache().get(bmid)
								.length();
						Util.log(cachingLoader.getDiscCache().get(bmid)
								.getName()
								+ " bmid size " + size);
						Util.profile(mContext, "image_cache.csv", "bmid, "
								+ bmid + ", " + size);

						loadOrig(orig);
					}

					@Override
					public void onLoadingFailed(FailReason failReason) {
						loadOrig(orig);
					}
				});
	}

	@Override
	protected void onPostExecute(Boolean already_running) {
		// if (already_running) {
		// return;
		// }
		
		if(!Util.isOnWifi(mContext)){
			return;
		}

		if (mStatusText == null || mStatusID < 0) {
			// sRunning = false;
			return;
		}

		if (thumb != null && bmid != null && orig != null) {
			Util.log("thumb " + thumb);

			cachingLoader.displayImage(thumb, dummyImageView,
					new SimpleImageLoadingListener() {
						@Override
						public void onLoadingComplete(Bitmap loadedImage) {
							long size = cachingLoader.getDiscCache().get(thumb)
									.length();
							Util.log(cachingLoader.getDiscCache().get(thumb)
									.getName()
									+ " thumb size " + size);
							Util.profile(mContext, "image_cache.csv", "thumb, "
									+ thumb + ", " + size);

							loadBmid(bmid, orig);
						}

						@Override
						public void onLoadingFailed(FailReason failReason) {
							loadBmid(bmid, orig);
						}
					});

		} else {
			Util.log("no pic");
		}

		if (rt_thumb != null && rt_bmid != null && rt_orig != null) {
			Util.log("rt_thumb " + rt_thumb);

			cachingLoader.displayImage(rt_thumb, dummyImageView,
					new SimpleImageLoadingListener() {
						@Override
						public void onLoadingComplete(Bitmap loadedImage) {
							long size = cachingLoader.getDiscCache()
									.get(rt_thumb).length();
							Util.log(cachingLoader.getDiscCache().get(rt_thumb)
									.getName()
									+ " rt_thumb size " + size);
							Util.profile(mContext, "image_cache.csv",
									"rt_thumb, " + rt_thumb + ", " + size);

							loadBmid(rt_bmid, rt_orig);
						}

						@Override
						public void onLoadingFailed(FailReason failReason) {
							loadBmid(rt_bmid, rt_orig);
						}
					});
		} else {
			Util.log("no rt pic");
		}

		// -------------------------------------

		if (bmid == null && rt_bmid == null) {
			Util.log("No pic both " + mStatusText + ", launching next.");

			// -----------------------------
			ContentValues values = new ContentValues();
			values.put(TimelineTable.CACHED_AT, System.currentTimeMillis());
			mContext.getContentResolver().update(
					Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI, ""
							+ mStatusID), values, null, null);
			// -----------------------------

			new ImageCachingTask(mContext).execute();
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private WebView initWebView() {
		// ========================================================================
		WebView wv = new WebView(mContext);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setDomStorageEnabled(true);

		wv.getSettings().setAppCacheMaxSize(1024 * 1024 * 100);
		String appCachePath = mContext.getApplicationContext().getCacheDir()
				.getAbsolutePath();
		wv.getSettings().setAppCachePath(appCachePath);
		wv.getSettings().setAllowFileAccess(true);
		wv.getSettings().setAppCacheEnabled(true);

		wv.setWebViewClient(new CachingWebViewClient());
		// ========================================================================

		return wv;
	}

	private class CachingWebViewClient extends WebViewClient {
		private long start_rx = -1L;
		private long start_tx = -1L;

		private boolean mLaunchedNext = false;

		public CachingWebViewClient() {
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Util.log(" shouldOverrideUrlLoading " + url);

			view.loadUrl(url);
			return false;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Util.log("onPageStarted " + url);

			if (start_rx < 0) {
				start_rx = TrafficStats.getUidRxBytes(Process.myUid());
				start_tx = TrafficStats.getUidTxBytes(Process.myUid());
				Util.log("start_rx " + start_rx);
				Util.log("start_tx " + start_tx);
			}

			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Util.log("onPageFinished " + url);

			if (!url.startsWith("http://t.cn") && !mLaunchedNext) {
				mLaunchedNext = true;
				long rx = TrafficStats.getUidRxBytes(Process.myUid())
						- start_rx;
				long tx = TrafficStats.getUidTxBytes(Process.myUid())
						- start_tx;

				Util.log("rx " + rx);
				Util.log("tx " + tx);

				Util.profile(mContext, "image_cache.csv", "orig, " + url + ", "
						+ rx);

				// -----------------------------
				ContentValues values = new ContentValues();
				values.put(TimelineTable.CACHED_AT, System.currentTimeMillis());
				mContext.getContentResolver().update(
						Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI, ""
								+ mStatusID), values, null, null);
				// -----------------------------

				new ImageCachingTask(mContext).execute();

			}
			super.onPageFinished(view, url);
		}
	}

}
