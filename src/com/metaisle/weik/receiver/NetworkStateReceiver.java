package com.metaisle.weik.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.metaisle.util.Util;
import com.metaisle.weik.caching.CachingService;

public class NetworkStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Util.log("Connectivity Change.");
		ConnectivityManager conn = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conn.getActiveNetworkInfo();

		if (networkInfo != null
				&& networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			Util.log("Wifi connected");
			Util.profile(context, "caching_debug.csv", "Wifi connected");

			context.startService(new Intent(context, CachingService.class));
		} else if (networkInfo != null) {
		} else {
		}

	}

}
