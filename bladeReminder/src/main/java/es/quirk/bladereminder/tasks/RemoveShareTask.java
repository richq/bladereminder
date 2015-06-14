package es.quirk.bladereminder.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.MenuItem;

import es.quirk.bladereminder.Utils;

public class RemoveShareTask extends AsyncTask<Void, Void, Boolean> {
    private final MenuItem mMenuItem;
    private final Intent mIntent;
    private final Context mContext;

    public RemoveShareTask(Context context, Intent intent, MenuItem menuItem) {
        super();
        mMenuItem = menuItem;
        mIntent = intent;
        mContext = context;
        menuItem.setVisible(false);
    }

    @Override
    protected Boolean doInBackground(Void... action) {
        return !Utils.isChrome() && mContext.getPackageManager().resolveActivity(mIntent, 0) != null;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mMenuItem.setVisible(result);
    }

}