package es.quirk.bladereminder.appwidget;

import android.support.annotation.NonNull;
import android.util.SparseArray;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.google.common.collect.Range;

import es.quirk.bladereminder.activities.BladeReminderActivity;
import es.quirk.bladereminder.R;
import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.database.Contract.Shaves;

public class ReminderAppWidgetProvider extends AppWidgetProvider {

    private final SparseArray<Range<Integer> > mRanges = new SparseArray<>(7);
    private boolean mColoursEnabled = true;

    private static class Info {
        String date;
        int count;
    }

    private class WidgetUpdateTask extends AsyncTask<Void, Void, Info> {

        private final Context mContext;
        private final AppWidgetManager mAppWidgetManager;
        private final int[] mAppWidgetIds;

        public WidgetUpdateTask(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, @NonNull int[] appWidgetIds) {
            mContext = context;
            mAppWidgetManager = appWidgetManager;
            mAppWidgetIds = appWidgetIds.clone();
        }

        @Override
        protected Info doInBackground(Void... params) {
            return getLatestInfo(mContext);
        }

        @Override
        protected void onPostExecute(Info result) {
            for (int appWidgetId : mAppWidgetIds) {
                updateAppWidget(mContext, mAppWidgetManager, appWidgetId, result);
            }
        }
    }

    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, @NonNull int[] appWidgetIds) {
        ShaveWatch.register(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mColoursEnabled = Utils.setRangesFromPrefs(prefs, mRanges);
        new WidgetUpdateTask(context, appWidgetManager, appWidgetIds).execute();
    }

    @Override
    public void onEnabled(@NonNull Context context) {
        ShaveWatch.register(context);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        ShaveWatch.register(context);
        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = ShaveWatch.nameFromContext(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onDisabled(@NonNull Context context) {
        ShaveWatch.unregister(context);
    }

    @NonNull
    private Info getLatestInfo(@NonNull Context context) {
        Uri uri = ShaveEntryContentProvider.CONTENT_URI;
        String[] projection = { Shaves.DATE, Shaves.COUNT };
        String selection = Shaves.COUNT + " > 0";
        String sortOrder = Shaves.DATE + " DESC";

        int count = 0;
        Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, sortOrder);
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

    private void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId, @NonNull Info latest) {
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
