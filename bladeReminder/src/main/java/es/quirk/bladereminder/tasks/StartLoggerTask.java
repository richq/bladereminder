package es.quirk.bladereminder.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import es.quirk.bladereminder.Utils;

public class StartLoggerTask extends AsyncTask<Context, Void, Void> {

    // max value for start count that will be logged
    // after this, I don't care.
    private static final int MAX_COUNT_LOG = 100;

    @Nullable
    @Override
    protected Void doInBackground(Context... action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(action[0]);
        int count = prefs.getInt("start_count", 0);
        if (count < MAX_COUNT_LOG) {
            String lastStart = prefs.getString("last_start", "");
            String today = Utils.todaysDate();
            if (!today.equals(lastStart)) {
                prefs.edit()
                    .putInt("start_count", count + 1)
                    .putString("last_start", today)
                    .apply();
            }
        }
        return null;
    }

}
