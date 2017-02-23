package com.metaisle.weik.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.weik.fragment.RelationshipFragment;

public class RelationshipActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle b = getIntent().getExtras();
			getSupportFragmentManager()
					.beginTransaction()
					.add(android.R.id.content,
							Fragment.instantiate(this,
									RelationshipFragment.class.getName(), b))
					.commit();
		}

	}
}
