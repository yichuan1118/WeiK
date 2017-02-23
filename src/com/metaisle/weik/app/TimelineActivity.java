package com.metaisle.weik.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.weik.fragment.TimelineFragment;
import com.metaisle.weik.weibo.GetTimelineTask.TimelineType;

public class TimelineActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle b = new Bundle();
			b.putSerializable(TimelineFragment.KEY_TIMELINE_TYPE,
					TimelineType.USER);
			b.putLong(TimelineFragment.KEY_USER_ID, getIntent()
					.getLongExtra(TimelineFragment.KEY_USER_ID, 0));
			getSupportFragmentManager()
					.beginTransaction()
					.add(android.R.id.content,
							Fragment.instantiate(this,
									TimelineFragment.class.getName(), b))
					.commit();
		}

	}
}
