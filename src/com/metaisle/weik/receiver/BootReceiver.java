package com.metaisle.weik.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.metaisle.profiler.CollectorService;
import com.metaisle.weik.caching.CachingService;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		context.startService(new Intent(context, CollectorService.class));
		context.startService(new Intent(context, CachingService.class));
	}

}
