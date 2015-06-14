package es.quirk.bladereminder.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;

import java.util.HashMap;

import es.quirk.bladereminder.BladeReminderActivity;
import es.quirk.bladereminder.R;
import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.database.Contract.Shaves;

public class ReminderAppWidgetProvider extends AppWidgetProvider {

    private final HashMap<Integer, Range<Integer> > mRanges = Maps.newHashMapWithExpectedSize(7);
    private boolean mColoursEnabled = true;

    private static class Info {
        String date;
        int count;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(ReminderAppWidgetProvider.class.getName(), "onUpdate called");
        ShaveWatch.register(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mColoursEnabled = Utils.setRangesFromPrefs(prefs, mRanges);
        Info latest = getLatestInfo(context);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, latest);
        }
    }

    @Override
    public void onEnabled(Context context) {
        ShaveWatch.register(context);
        Log.d(ReminderAppWidgetProvider.class.getName(), "onEnabled called for app widget");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(ReminderAppWidgetProvider.class.getName(), "onReceive called");
        ShaveWatch.register(context);
        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = ShaveWatch.nameFromContext(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
            Log.d(ReminderAppWidgetProvider.class.getName(), "working??");
        }
    }

    @Override
    public void onDisabled(Context context) {
        ShaveWatch.unregister(context);
    }

    private Info getLatestInfo(Context context) {
        Log.d(ReminderAppWidgetProvider.class.getName(), "getLatestInfo called");
        Uri uri = ShaveEntryContentProvider.CONTENT_URI;
        String[] projection = { Shaves.DATE, Shaves.COUNT };
        String selection = Shaves.COUNT + " > 0";
        String[] selectionArgs = null;
        String sortOrder = Shaves.DATE + " DESC";

        int count = 0;
        Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        String date = "";
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                date = cursor.getString(0);
                count = cursor.getInt(1);
            }
            cursor.close();
        }
        Info info = new Info();
        info.date = date;
        info.count = count;
        return info;
    }

    private int getColourForContent(int count) {
        if (!mColoursEnabled || mRanges == null) {
            return 0;
        }
        try {
            return Utils.getCountColour(count, mRanges);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Info latest) {
        // Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, BladeReminderActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.example_appwidget);
        views.setOnClickPendingIntent(R.id.button1, pendingIntent);

        views.setTextViewText(R.id.button1, Integer.toString(latest.count));
        int colourResource = getColourForContent(latest.count);
        views.setInt(R.id.button1, "setBackgroundResource", colourResource);
        views.setTextColor(R.id.button1, Utils.getTextColourForResource(colourResource));
        views.setTextViewText(R.id.textView1, latest.date);

        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

}
