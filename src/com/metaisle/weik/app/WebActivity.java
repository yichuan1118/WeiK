package com.metaisle.weik.app;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.metaisle.weik.data.Prefs;

@SuppressLint("SetJavaScriptEnabled")
public class WebActivity extends SherlockActivity {
	public static final String KEY_URL = "KEY_URL";

	private static final Uri WEB_URI = Uri.parse(Prefs.WEB_SCHEMA);

	private WebView mWebView;
	private String mUrl = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		mWebView = new WebView(this);
		setContentView(mWebView);

		mWebView.getSettings().setSupportZoom(true);
		String appCachePath = getApplicationContext().getCacheDir()
				.getAbsolutePath();
		mWebView.getSettings().setAppCachePath(appCachePath);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings()
				.setPluginState(WebSettings.PluginState.ON_DEMAND);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDomStorageEnabled(true);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.getSettings().setAppCacheEnabled(true);
		mWebView.getSettings()
				.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		mWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 100);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Map<String, String> cacheHeaders = new HashMap<String,
				// String>(
				// 1);
				// int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
				// cacheHeaders.put("Cache-Control", "max-stale=" + maxStale);
				// view.loadUrl(url, cacheHeaders);
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
				return true;
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Util.log("onPageStarted " + url);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				Util.log("onPageFinished " + url);
			}

		});
		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				WebActivity.this.setSupportProgress(progress * 100);
			}

		});

		setSupportProgressBarVisibility(true);
		setSupportProgressBarIndeterminateVisibility(true);

		Uri uri = getIntent().getData();
		if (uri != null && WEB_URI.getScheme().equals(uri.getScheme())) {
			Util.log(uri.toString());
			mUrl = uri.getQueryParameter(Prefs.PARAM_url);
			Util.log("from url: " + mUrl);
		}

		if (mUrl == null) {
			mUrl = getIntent().getStringExtra(KEY_URL);
		}

		Util.profile(WebActivity.this, "url_click.csv", mUrl);

		Map<String, String> cacheHeaders = new HashMap<String, String>(1);
		int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
		cacheHeaders.put("Cache-Control", "max-stale=" + maxStale);

		mWebView.loadUrl(mUrl, cacheHeaders);

	}

	@Override
	public void finish() {
		Util.profile(this, "url_click.csv", mUrl + ", leave");
		super.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_web, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_open_in_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(mUrl));
			startActivity(browserIntent);

			break;

		default:
			break;
		}

		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Check if the key event was the Back button and if there's history
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		}
		// If it wasn't the Back key or there's no web page history, bubble up
		// to the default
		// system behavior (probably exit the activity)
		return super.onKeyDown(keyCode, event);
	}
}
