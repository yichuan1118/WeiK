package com.metaisle.weik.caching;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.metaisle.util.Util;
import com.metaisle.weik.data.Prefs;
import com.metaisle.weik.data.Provider;
import com.metaisle.weik.data.TimelineTable;
import com.metaisle.weik.weibo.GetTimelineTask;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;

public class CachingService extends Service {

	SharedPreferences mPrefs;
//	private static final long ONE_HOUR_IN_MILLI = 60 * 60 * 1000L;
	private static final long ONE_MIN_IN_MILLI = 60 * 1000L;
	int caching_freq_in_min;

	AlarmManager am;
	SharedPreferences prefs;

	// PowerManager pm;

	@Override
	public void onCreate() {
		super.onCreate();
		mPrefs = getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);

		am = (AlarmManager) getSystemService(ALARM_SERVICE);
		// pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		prefs=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		caching_freq_in_min=Integer.parseInt(prefs.getString("key_caching_freq", "60"));
		Util.log("caching freq "+caching_freq_in_min);

		long last_caching_time = mPrefs.getLong(Prefs.KEY_LAST_CACHING_TIME, 0);

		Util.log("onStartCommand " + last_caching_time);
		Util.profile(this, "caching_debug.csv", "CachingService called");

		if ((System.currentTimeMillis() - last_caching_time) < caching_freq_in_min*ONE_MIN_IN_MILLI) {

			Intent i = new Intent(this, CachingService.class);
			PendingIntent pi = PendingIntent.getService(this, 0, i,
					PendingIntent.FLAG_CANCEL_CURRENT);

			long next_caching = last_caching_time + caching_freq_in_min*ONE_MIN_IN_MILLI;
			am.set(AlarmManager.RTC_WAKEUP, next_caching, pi);

			Util.log("Cached at "
					+ DateUtils.getRelativeTimeSpanString(last_caching_time)
					+ " next_cahing "
					+ DateUtils.getRelativeTimeSpanString(next_caching));
			Util.profile(
					this,
					"caching_debug.csv",
					"Cached at "
							+ DateUtils
									.getRelativeTimeSpanString(last_caching_time)
							+ " next_cahing "
							+ DateUtils.getRelativeTimeSpanString(next_caching));
			return START_STICKY;
		}
		
		
		
		

		// // if (!Util.isOnWifi(this) || pm.isScreenOn()) {
//		if (!Util.isOnWifi(this)) {
//			Intent i = new Intent(this, CachingService.class);
//			PendingIntent pi = PendingIntent.getService(this, 0, i,
//					PendingIntent.FLAG_CANCEL_CURRENT);
//
//			long next_caching = System.currentTimeMillis() + caching_freq_in_min*ONE_MIN_IN_MILLI;
//			am.set(AlarmManager.RTC_WAKEUP, next_caching, pi);
//
//			Util.log("No Wifi, Cached at "
//					+ DateUtils.getRelativeTimeSpanString(last_caching_time)
//					+ " next_cahing "
//					+ DateUtils.getRelativeTimeSpanString(next_caching));
//
//			Util.profile(
//					this,
//					"caching_debug.csv",
//					"No Wifi, Cached at "
//							+ DateUtils
//									.getRelativeTimeSpanString(last_caching_time)
//							+ " next_cahing "
//							+ DateUtils.getRelativeTimeSpanString(next_caching));
//			return START_STICKY;
//		}

		Util.log("==========================Caching==========================");

		Util.profile(this, "caching_debug.csv", "Start Caching");

		Cursor c = getContentResolver().query(
				Provider.TIMELINE_CONTENT_URI,
				new String[] { TimelineTable.TIMELINE_TABLE + "."
						+ TimelineTable.STATUS_ID },
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.IS_HOME
						+ "=?",
				new String[] { "1" },
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.CREATED_AT
						+ " DESC LIMIT 1");

		if (!c.moveToFirst()) {
			return START_STICKY;
		}
		final long since_id = c.getLong(c
				.getColumnIndex(TimelineTable.STATUS_ID));

		Util.profile(this, "caching_debug.csv", "Start GetTimelineTask");

		new GetTimelineTask(this, TimelineType.HOME).setSinceID(since_id)
				.setCount(40).execute();

		Intent i = new Intent(this, CachingService.class);
		PendingIntent pi = PendingIntent.getService(this, 0, i,
				PendingIntent.FLAG_CANCEL_CURRENT);

		long next_caching = System.currentTimeMillis() + caching_freq_in_min*ONE_MIN_IN_MILLI;
		am.set(AlarmManager.RTC_WAKEUP, next_caching, pi);

		Util.profile(this, "caching_debug.csv", "Next Caching scheduled "
				+ DateUtils.getRelativeTimeSpanString(next_caching));

		mPrefs.edit()
				.putLong(Prefs.KEY_LAST_CACHING_TIME,
						System.currentTimeMillis()).commit();

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
