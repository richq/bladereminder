package es.quirk.bladereminder.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import es.quirk.bladereminder.R;
import es.quirk.bladereminder.fragments.SettingsFragment;

public class SetPreferencesActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.empty_settings_workaround);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		getSupportFragmentManager().beginTransaction().replace(R.id.content,
				new SettingsFragment()).commit();
	}

}
