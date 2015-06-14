package es.quirk.bladereminder;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

	@InjectView(R.id.tool_bar) Toolbar mToolbar;
	private String mThemeSetting;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		reloadTheme();
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

        @Override
	public void setContentView(int resourceId) {
		super.setContentView(resourceId);
		ButterKnife.inject(this);
		mToolbar.setPopupTheme(mThemeSetting.equals("0") ? R.style.ThemeOverlay_AppCompat_Light : R.style.ThemeOverlay_AppCompat_Dark);
		try {
		      setSupportActionBar(mToolbar);
		} catch (Throwable t) {
			Timber.w(t, "caught throwable in setSupportActionBar");
		}
		if (mToolbar != null) {
			mToolbar.setTitle(R.string.app_name);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		Timber.d("onSharedPreferenceChanged for key %s", key);
		if(key.equals("default_theme")) {
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
		setTheme(mThemeSetting.equals("0") ? R.style.AppBaseTheme : R.style.AppBaseThemeDark);
		if (mToolbar != null)
			mToolbar.setPopupTheme(mThemeSetting.equals("0") ?
					R.style.ThemeOverlay_AppCompat_Light :
					R.style.ThemeOverlay_AppCompat_Dark);
	}

}
