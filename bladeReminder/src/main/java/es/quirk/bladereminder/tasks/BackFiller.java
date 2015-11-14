package es.quirk.bladereminder.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.contentprovider.ShaveEntryContentProvider;
import es.quirk.bladereminder.database.Contract.Shaves;
import timber.log.Timber;


public class BackFiller extends AsyncTask<Void, Void, Void> {

	private final ContentResolver mContentResolver;
	private final static String[] PROJECTION = { Shaves._ID, Shaves.DATE };
	private final DateFormat mDateFormat = Utils.createDateFormatYYYYMMDD();

	public BackFiller(ContentResolver contentResolver) {
		super();
		mContentResolver = contentResolver;
	}

	private void fillToDate(long daysPast) {
		if (daysPast <= 0)
			return;
		Date today = Calendar.getInstance().getTime();
		Date thatDay = new Date(today.getTime());
		ContentValues values = new ContentValues();
		for (long i = 0; i < daysPast; i++) {
			thatDay.setTime(today.getTime() - i * Utils.ONE_DAY_MS);
			values.put(Shaves.DATE, mDateFormat.format(thatDay));
			mContentResolver.insert(ShaveEntryContentProvider.CONTENT_URI, values);

		}
	}

	@Nullable
	@Override
	protected Void doInBackground(Void... params) {
		// since the first entry, or since 28 days ago fill in crud
		Cursor cursor = mContentResolver.query(ShaveEntryContentProvider.CONTENT_URI, PROJECTION, null,
				null, Shaves.DATE + " DESC");
		if (cursor == null || !cursor.moveToFirst()) {
			// no entries
			if (cursor != null) {
				cursor.close();
			}
			fillToDate(28);
		} else {
			String lastDate = cursor.getString(1);
			Timber.d("last date = %s", lastDate);
			Date then;
			try {
				then = mDateFormat.parse(lastDate);
				Date today = Calendar.getInstance().getTime();
				long diff = today.getTime() - then.getTime();
				// how many days is that?
				long daysPast = diff / (24 * 60 * 60 * 1000);
				fillToDate(daysPast);
			} catch (ParseException e) {
				Timber.w(e, "could not parse %s", lastDate);
			}
			cursor.close();

		}
		return null;
	}
}

