package es.quirk.bladereminder.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.Utils;

final class ShaveWatch extends ContentObserver implements OnSharedPreferenceChangeListener {

	private final AppWidgetManager mAppWidgetManager;
	@NonNull
	private final ComponentName mComponentName;
	@NonNull
	private final Context mContext;
	private final Handler mHandler;

	@Nullable
	private static ShaveWatch sContentObserver;

	@NonNull
	public static ComponentName nameFromContext(@NonNull Context c) {
		return new ComponentName(c, ReminderAppWidgetProvider.class);
	}

	private ShaveWatch(@NonNull Context context, Handler handler) {
		super(handler);
		mHandler = handler;
		mContext = context;
		mAppWidgetManager = AppWidgetManager.getInstance(context);
		mComponentName = nameFromContext(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}


	@Override
	public void onChange(boolean selfChange) {
		Intent intent = new Intent(mContext, ReminderAppWidgetProvider.class);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
		// since it seems the onUpdate() is only fired on that:
		int[] ids = mAppWidgetManager.getAppWidgetIds(mComponentName);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		mContext.sendBroadcast(intent);
	}

	@NonNull
	private static Handler getBackgroundThread() {
		HandlerThread thread = new HandlerThread("ReminderAppWidgetProvider-worker");
		thread.start();
		return new Handler(thread.getLooper());
	}

	private void unregisterAndStop(@NonNull Context context) {
		context.getContentResolver().unregisterContentObserver(this);
		mHandler.getLooper().quit();
	}

	public synchronized static void register(@NonNull Context context) {
		if (sContentObserver == null) {
			sContentObserver = new ShaveWatch(context, getBackgroundThread());
			context.getContentResolver().registerContentObserver(
					ShaveEntryContentProvider.CONTENT_URI,
					true,
					sContentObserver);
		}
	}

	public synchronized static void unregister(@NonNull Context context) {
		if (sContentObserver == null)
			return;
		// any widgets active?
		final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
		if (mgr.getAppWidgetIds(nameFromContext(context)).length > 0)
			return;

		// no? then unregister
		sContentObserver.unregisterAndStop(context);
		sContentObserver = null;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if ("colours_enabled".equals(key) || "default_theme".equals(key)
				|| Utils.isColourPref(key)) {
			// update too
			onChange(false);
		}
	}
}
