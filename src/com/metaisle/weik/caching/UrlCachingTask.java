package com.metaisle.weik.caching;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Process;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.UrlTable;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;

public class UrlCachingTask extends AsyncTask<Void, Void, Void> {
	// public static boolean sRunning = false;
	SharedPreferences mPrefs;

	private Context mContext;

	private Cursor url_cursor;
	private String url_short;
	private String url_long;

	// private Map<String, String> cacheHeaders;

	public UrlCachingTask(Context context) {
		mContext = context;
		mPrefs = mContext.getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);

		// cacheHeaders = new HashMap<String, String>(1);
		// int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
		// cacheHeaders.put("Cache-Control", "public");
	}

	@Override
	protected Void doInBackground(Void... params) {

		Util.log("+++++++++++++++++++++++++++++++++++++++++");

		url_cursor = mContext.getContentResolver().query(
				Provider.URL_CONTENT_URI,
				new String[] { UrlTable.URL_SHORT, UrlTable.URL_LONG },
				UrlTable.CACHED_AT + " IS NULL", null,
				UrlTable._ID + " DESC LIMIT 1");

		if (url_cursor != null && url_cursor.getCount() > 0) {
			url_cursor.moveToFirst();
			url_short = url_cursor.getString(url_cursor
					.getColumnIndex(UrlTable.URL_SHORT));

			Weibo weibo = Prefs.getWeibo(mContext);
			WeiboParameters bundle = new WeiboParameters();
			bundle.add("access_token", weibo.getAccessToken().getToken());

			if (url_cursor.isNull(url_cursor.getColumnIndex(UrlTable.URL_LONG))) {
				Util.log("url_short " + url_short);
				bundle.add("url_short", url_short);

				try {
					String rlt = weibo.request(mContext, Weibo.SERVER
							+ "short_url/expand.json", bundle, "GET",
							weibo.getAccessToken());
					JSONObject resp = new JSONObject(rlt).getJSONArray("urls")
							.getJSONObject(0);

					if (resp.getBoolean("result")) {
						ContentValues cvs = new ContentValues();
						url_long = resp.getString("url_long");
						cvs.put(UrlTable.URL_SHORT, resp.getString("url_short"));
						cvs.put(UrlTable.URL_LONG, url_long);
						cvs.put(UrlTable.URL_TYPE, resp.getString("type"));

						mContext.getContentResolver().update(
								Provider.URL_CONTENT_URI, cvs,
								UrlTable.URL_SHORT + "=?",
								new String[] { url_short });

						Util.profile(mContext, "url_short.csv", rlt);
					} else {
						url_long = url_short;
						Util.log(url_short + " expend failed!");
					}

				} catch (Exception e) {
					url_long = url_short;
				}

			} else {
				url_long = url_cursor.getString(url_cursor
						.getColumnIndex(UrlTable.URL_LONG));
			}
		} else {
			Util.log("No URL to cache");
		}

		Util.log("url_long " + url_long);

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {

		if (url_cursor != null && url_cursor.getCount() > 0) {
			Util.log("Url caching, " + url_long);

			initWebView().loadUrl(url_long);

		} else {
			Util.log("no Url to cache");
		}

		super.onPostExecute(result);

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

				Util.profile(mContext, "url_cache.csv", url_short + ", " + url
						+ ", " + rx + ", " + tx);

				// -----------------------------
				ContentValues values = new ContentValues();
				values.put(UrlTable.CACHED_AT, System.currentTimeMillis());
				mContext.getContentResolver().update(Provider.URL_CONTENT_URI,
						values, UrlTable.URL_SHORT + "=?",
						new String[] { url_short });
				// -----------------------------

				new UrlCachingTask(mContext).execute();

			}
			super.onPageFinished(view, url);
		}
	}

}
