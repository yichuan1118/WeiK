package com.metaisle.weik.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.weik.fragment.StatusFragment;

public class StatusActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle b = new Bundle();
			b.putLong(StatusFragment.KEY_STATUS_ID,
					getIntent().getLongExtra(StatusFragment.KEY_STATUS_ID, -1));
			getSupportFragmentManager()
					.beginTransaction()
					.add(android.R.id.content,
							Fragment.instantiate(this,
									StatusFragment.class.getName(), b))
					.commit();
		}

	}

}
