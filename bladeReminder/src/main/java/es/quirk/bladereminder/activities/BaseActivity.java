package es.quirk.bladereminder.activities;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.ButterKnife;
import butterknife.Bind;
import es.quirk.bladereminder.R;
import timber.log.Timber;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

	@Bind(R.id.tool_bar) Toolbar mToolbar;
	private String mThemeSetting = "0";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		reloadTheme();
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	private boolean isLightTheme() {
		return "0".equals(mThemeSetting);
	}

        @Override
	public void setContentView(int resourceId) {
		super.setContentView(resourceId);
		ButterKnife.bind(this);
		try {
		      setSupportActionBar(mToolbar);
		} catch (Throwable t) {
			Timber.w(t, "caught throwable in setSupportActionBar");
		}
		mToolbar.setTitle(R.string.app_name);
	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences prefs, @NonNull String key) {
		Timber.d("onSharedPreferenceChanged for key %s", key);
		if("default_theme".equals(key)) {
			final String currVal = prefs.getString(key, "0");
			Timber.d("onSharedPreferenceChanged for key %s = %s", key, currVal);
			if (!currVal.equals(mThemeSetting)) {
				reloadTheme();
				this.recreate();
			}
		}
	}

	private void reloadTheme() {
		mThemeSetting = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString("default_theme", "0");
		setTheme(isLightTheme() ? R.style.AppBaseTheme : R.style.AppBaseThemeDark);
	}

}
