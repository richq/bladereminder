package es.quirk.bladereminder.activities;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.Bind;
import es.quirk.bladereminder.R;
import es.quirk.bladereminder.fragments.ShaveFragment;
import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.database.DataSource;
import es.quirk.bladereminder.fragments.StatisticsFragment;
import es.quirk.bladereminder.tasks.RemoveShareTask;
import es.quirk.bladereminder.tasks.StartLoggerTask;
import es.quirk.bladereminder.widgets.TextDrawable;
import es.quirk.bladereminder.widgets.TextDrawableFactory;
import timber.log.Timber;

public class BladeReminderActivity extends BaseActivity  {

    private final static String SHARE_TYPE = "text/csv";
    @Bind(R.id.progressBar1) ProgressBar mProgressBar;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logStart();
        setContentView(R.layout.activity_blade_reminder);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mProgressBar.setVisibility(View.GONE);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ShaveFragment(), "shave_fragment")
                    .commit();
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private int getResetFlag() {
        final int sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion >= Build.VERSION_CODES.LOLLIPOP)
            return Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        else
            return Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
    }

    private void removeStatsEntry(@NonNull Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.action_stats);
        menuItem.setVisible(false);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.blade_reminder, menu);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(getResetFlag());
        shareIntent.setType(SHARE_TYPE);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        Context context = getApplicationContext();
        TextDrawable drawableUpload = TextDrawableFactory.createIcon(context, "gmd-file-upload");
        TextDrawable drawableHelp = TextDrawableFactory.createIcon(context, "gmd-help");
        if (menuItem != null) {
            menuItem.setIcon(drawableUpload);
            new RemoveShareTask(context, shareIntent, menuItem).execute();
        }
        menuItem = menu.findItem(R.id.action_help);
        menuItem.setIcon(drawableHelp);

        removeStatsEntry(menu);

        return true;
    }

    @NonNull
    private static FileOutputStream lessBrokenOpenFileOutput(@NonNull Context context, @NonNull String path) throws IOException {
        File file = new File(context.getFilesDir(), path);
        Timber.d("mkdir " + file.getParentFile());
        Utils.mkdirp(file.getParentFile());
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException(context.getString(R.string.unable_to_create_file));
        }
        return new FileOutputStream(file);
    }

    private class ShareThread extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        public ShareThread(Context context) {
            super();
            mContext = context;
        }

        @Nullable
        @Override
        protected Void doInBackground(Void... params) {
            FileOutputStream tempStream = null;
            final String shavesCsv = getString(R.string.shaves_csv);
            try {
                tempStream = lessBrokenOpenFileOutput(mContext, shavesCsv);
                DataSource datasource = new DataSource(mContext);
                datasource.toCSV(tempStream);
                tempStream.flush();
            } catch (IOException io) {
                Timber.e(io, "Error saving file");
            } catch (IllegalArgumentException arg) {
                Timber.w(arg, "Nothing to share to");
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(mContext, R.string.nothing_to_share, duration).show();
            } finally {
                Utils.close(tempStream);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(getResetFlag());
            // see http://developer.android.com/training/secure-file-sharing/index.html
            // For a file in shared storage.  For data in private storage, use a ContentProvider.
            Uri fileUri = getFileUri(mContext);
            // set mime type manually - android uses text/comma-separated-file which is not correct!
            shareIntent.setType(SHARE_TYPE);
            // putExtra or setData?? this works with gdrive at least, but not owncloud
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            String subject = getString(R.string.shave_file_backup) + " " + Utils.todaysDate();
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.backup));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(shareIntent);
            } catch (ActivityNotFoundException ex) {
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(mContext, R.string.nothing_to_share_to, duration).show();
                Timber.w(ex, "No activity found to share to");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(this, SetPreferencesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_share) {
            // create the data file
            ShareThread st = new ShareThread(getApplicationContext());
            st.execute();
            return true;
        } else if (id == R.id.action_help) {
            Intent intent = new Intent();
            intent.setClass(BladeReminderActivity.this, HelpActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_stats) {
            StatisticsFragment statisticsFragment = StatisticsFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out);
            transaction.replace(R.id.container, statisticsFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
        return super.onOptionsItemSelected(item);
    }

    private Uri getFileUri(@NonNull Context context) {
        final String shavesCsv = getString(R.string.shaves_csv);
        File requestFile = new File(context.getFilesDir(), shavesCsv);
        Timber.d("request file: %s", requestFile.toString());
        Uri fileUri = FileProvider.getUriForFile(
                BladeReminderActivity.this,
                "es.quirk.bladereminder.fileprovider",
                requestFile);
        Timber.d("fileUri = %s", fileUri.toString());
        return fileUri;
    }

    private void logStart() {
        new StartLoggerTask().execute(getApplicationContext());
    }

    public void start() {
        //setProgressBarIndeterminateVisibility(true);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void stop() {
        //setProgressBarIndeterminateVisibility(false);
        mProgressBar.setVisibility(View.GONE);
    }

}
