package com.metaisle.weik.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.widget.TextView;

import com.metaisle.util.Util;
import com.metaisle.weik.R;
import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.weibo.net.AccessToken;
import com.weibo.net.Weibo;

public class Prefs {
	private static final String CONSUMER_KEY = "2282257429";
	private static final String CONSUMER_SECRET = "83d149fccf5760d3e7271e3355209e76";

	public static final boolean DEBUG = true;

	public static final String PREFS_NAME = "weik.prefs";

	public static final String KEY_OAUTH_TOKEN = "KEY_OAUTH_TOKEN";
	public static final String KEY_EXPIRES_IN = "key_expires_in";
	public static final String KEY_LAST_UPDATED_AT = "key_last_updated_at";
	public static final String KEY_UID = "KEY_UID";
	public static final String KEY_REMINDED_STATUS_SWIPE = "KEY_REMINDED_STATUS_SWIPE";
	public static final String KEY_LAUNCHED_TIMES = "KEY_LAUNCHED_TIMES";
	public static final String KEY_DEBUG = "KEY_DEBUG";

	public static final int NOTIFICATION_TWEET_ID = 0x1;
	public static final int NOTIFICATION_RETWEET_ID = 0x2;

	public static Weibo sWeibo;

	public static final int DEFAULT_LOAD_NUMBER = 20;

	public static final String KEY_LAST_CACHING_TIME = "KEY_LAST_CACHING_TIME";

	public static Weibo getWeibo(Context context) {

		SharedPreferences mPrefs = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);

		if (sWeibo == null) {
			String accessToken = mPrefs.getString(Prefs.KEY_OAUTH_TOKEN, null);
			long expires = mPrefs.getLong(Prefs.KEY_EXPIRES_IN, 0);

			// Util.log("accessToken " + accessToken);
			sWeibo = Weibo.getInstance();
			sWeibo.setupConsumerConfig(CONSUMER_KEY, CONSUMER_SECRET);
			sWeibo.setRedirectUrl("http://weik.metaisle.com/authorized");

			AccessToken token = new AccessToken(accessToken, CONSUMER_SECRET);
			token.setExpiresIn(expires);
			sWeibo.setAccessToken(token);
		}
		return sWeibo;
	}

	public static Weibo setWeibo(Context context, String token,
			String expires_in) {
		if (sWeibo == null) {
			sWeibo = getWeibo(context);
		}
		AccessToken accessToken = new AccessToken(token, CONSUMER_SECRET);
		long expires = System.currentTimeMillis() + Long.parseLong(expires_in)
				* 1000;
		accessToken.setExpiresIn(expires);
		sWeibo.setAccessToken(accessToken);

		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);

		prefs.edit().putString(Prefs.KEY_OAUTH_TOKEN, token)
				.putLong(Prefs.KEY_EXPIRES_IN, expires).commit();

		return sWeibo;
	}

	private static ImageLoader loader;
	private static ImageLoader cachingLoader;

	public static ImageLoader imageLoader(Context context) {
		if (loader == null) {
			DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
					.cacheInMemory().cacheOnDisc().resetViewBeforeLoading()
					.showStubImage(R.drawable.preview_pic_loading).build();
			ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
					context.getApplicationContext())
					.memoryCache(
							new LRULimitedMemoryCache((int) (Runtime
									.getRuntime().maxMemory() / 4)))
					.discCacheSize(100 * 1024 * 1024)
					.denyCacheImageMultipleSizesInMemory().threadPoolSize(2)
					.threadPriority(Thread.NORM_PRIORITY)
					.defaultDisplayImageOptions(defaultOptions).build();
			loader = ImageLoader.getInstance();

			Util.log("mem " + Runtime.getRuntime().maxMemory() / 4);
			// Runtime.getRuntime().maxMemory()/4
			loader.init(config);

		}
		return loader;
	}

	public static ImageLoader cachingLoader(Context context) {
		// ------------------------------------------------------------------
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheOnDisc().build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				context.getApplicationContext())
				.discCacheSize(100 * 1024 * 1024).threadPoolSize(2)
				.defaultDisplayImageOptions(defaultOptions).build();
		cachingLoader = ImageLoader.getInstance();

		Util.log("mem " + Runtime.getRuntime().maxMemory() / 4);
		// Runtime.getRuntime().maxMemory()/4
		cachingLoader.init(config);
		// ------------------------------------------------------------------

		return cachingLoader;
	}

	public static final String MENTIONS_SCHEMA = "weik://mentions";
	public static final String HASHTAGS_SCHEMA = "weik://hashtag";
	public static final String PARAM_USERNAME = "user_name";
	public static final String PARAM_HASHTAG = "hashtag";

	public static final String WEB_SCHEMA = "weik://web";
	public static final String PARAM_url = "url";

	public static void extractMention2Link(final Context context, TextView v) {
		v.setAutoLinkMask(0);

		// Pattern mentionsPattern = Pattern.compile("@(\\w+?)(?=\\W|$)(.)");
		Pattern mentionsPattern = Pattern.compile("@(\\w+?)(?=\\W|$)");
		String mentionsScheme = String.format("%s/?%s=", MENTIONS_SCHEMA,
				PARAM_USERNAME);
		Linkify.addLinks(v, mentionsPattern, mentionsScheme, new MatchFilter() {

			@Override
			public boolean acceptMatch(CharSequence s, int start, int end) {
				return s.charAt(end - 1) != '.';
			}

		}, new TransformFilter() {
			@Override
			public String transformUrl(Matcher match, String url) {
				Util.log(match.group(1));
				return match.group(1);
			}
		});

		String webScheme = String.format("%s/?%s=", WEB_SCHEMA, PARAM_url);
		String regex = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		Pattern p = Pattern.compile(regex);
		Linkify.addLinks(v, p, webScheme, null, new TransformFilter() {
			@Override
			public String transformUrl(Matcher match, String url) {
				String u = match.group(0);

				Util.log("url " + u);
				Util.log("url arg " + url);

				Cursor c = context.getContentResolver().query(
						Provider.URL_CONTENT_URI,
						new String[] { UrlTable.URL_LONG },
						UrlTable.URL_SHORT + "=?", new String[] { u }, null);

				int idx = c.getColumnIndex(UrlTable.URL_LONG);
				if (c.moveToFirst() && !c.isNull(idx)) {
					String u_long = c.getString(idx);
					Util.log(u_long);
					return u_long;
				}

				return u;
			}
		});

		// Pattern trendsPattern = Pattern.compile("#(\\w+?)#");
		// String trendsScheme = String.format("%s/?%s=", HASHTAGS_SCHEMA,
		// PARAM_HASHTAG);
		// Linkify.addLinks(v, trendsPattern, trendsScheme, null,
		// new TransformFilter() {
		// @Override
		// public String transformUrl(Matcher match, String url) {
		// Util.log(match.group(1));
		// return match.group(1);
		// }
		// });

	}

}