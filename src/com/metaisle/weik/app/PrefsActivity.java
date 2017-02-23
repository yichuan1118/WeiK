package com.metaisle.weik.app;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.metaisle.weik.R;


public class PrefsActivity extends PreferenceActivity {

	private Preference mEnableTimeline;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
//		mEnableTimeline = findPreference(Prefs.KEY_ENABLE_TIMELINE);
//		mEnableTimeline
//				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//
//					@Override
//					public boolean onPreferenceChange(Preference preference,
//							Object newValue) {
//						if (preference == mEnableTimeline) {
//							Intent i = new Intent(PrefsActivity.this,
//									PhotoPagerActivity.class);
//							i.putExtra(Prefs.KEY_INVALIDATE, true);
//							startActivity(i);
//						}
//						return true;
//					}
//				});
	}

}
