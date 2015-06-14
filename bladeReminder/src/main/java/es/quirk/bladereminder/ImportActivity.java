package es.quirk.bladereminder;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;

import butterknife.InjectView;
import butterknife.OnClick;
import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.database.DataSource;
import timber.log.Timber;

public class ImportActivity extends BaseActivity {

    @InjectView(R.id.progressBar1) ProgressBar mProgressBar;

    private class DoImport extends AsyncTask<Uri, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Uri... data) {
            try {
                importData(data[0]);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean importOK) {
            mProgressBar.setVisibility(View.GONE);
            if (importOK)
                showMainActivity();
            finish();
        }

    }

    @OnClick(R.id.cancel_import)
    void cancelImport(Button button) {
        showMainActivity();
    }

    @OnClick(R.id.run_import)
    void runImport(Button button) {
        Intent intent = getIntent();
        if (intent != null) {
            Uri data = intent.getData();
            if (data != null) {
                // import
                mProgressBar.setVisibility(View.VISIBLE);
                intent.setData(null);
                new DoImport().execute(data);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_data);
    }

    private void showMainActivity() {
        Intent mainIntent = new Intent(getApplicationContext(), BladeReminderActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mainIntent.putExtra(Intent.EXTRA_TEXT, "Import Finished");
        startActivity(mainIntent);
    }

    private void importData(Uri data) {
        final String scheme = data.getScheme();
        if (!ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            return;
        }
        try {
            ContentResolver cr = getContentResolver();
            InputStream is = cr.openInputStream(data);
            if (is == null)
                return;
            DataSource dataSource = new DataSource(getApplicationContext());
            dataSource.importData(is);
            is.close();
            getContentResolver().notifyChange(ShaveEntryContentProvider.CONTENT_URI, null);
        } catch (IOException ex) {
            Timber.e(ex, "importData %s", data);
        }
    }

}
