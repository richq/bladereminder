package es.quirk.bladereminder;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		if (isDebuggable()) {
			StrictMode.enableDefaults();
			Timber.plant(new DebugTree());
		} else {
			Timber.plant(new Timber.HollowTree());
		}
	}

	private boolean isDebuggable() {
		return 0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
	}
}
