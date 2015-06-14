package es.quirk.bladereminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import es.quirk.bladereminder.ShaveEntry;
import es.quirk.bladereminder.Utils;
import timber.log.Timber;

public class DataSource {
	private SQLiteDatabase mDatabase;
	private final UsageHelper mDbHelper;
	private final DateFormat mDateFormat = Utils.createDateFormatYYYYMMDD();

	public DataSource(Context context) {
		mDbHelper = new UsageHelper(context);
	}

	private void open() {
		if (mDatabase == null || !mDatabase.isOpen())
			mDatabase = mDbHelper.getWritableDatabase();
	}

	private void close() {
		mDbHelper.close();
	}

	public static ShaveEntry cursorToEntry(Cursor cursor) {
		return new ShaveEntry(cursor.getInt(0),
				cursor.getString(1),
				cursor.getInt(2),
				cursor.getString(3));
	}

	private List<ShaveEntry> getEntry(final String date) {
		List<ShaveEntry> entries = Lists.newArrayList();
		String query = Contract.Shaves.SELECT +
			String.format(" where %s = ?", Contract.Shaves.DATE) +
			Contract.Shaves.ORDER_BY_DATE;
		String args[] = new String[] { date };
		// SELECT * from shaves where date >= 4-weeks-ago:
		Cursor cursor = mDatabase.rawQuery(query, args);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			entries.add(cursorToEntry(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		if (entries.isEmpty()) {
			entries.add(new ShaveEntry(date));
		}
		return entries;
	}

	public void bubbleForwards(final String date) {
		open();
		try {
			final String nextNew = "select s.date from shaves s where s.date > ? and s.count = 1 order by s.date ASC LIMIT 1";
			String args[] = new String[] { date };
			Cursor cursor = mDatabase.rawQuery(nextNew, args);
			cursor.moveToFirst();
			String endDate = "";
			if (cursor.isAfterLast()) {
				cursor.close();
				// oh. bubble until the end of the db
				final String maxDate = "select max(date) from shaves";
				cursor = mDatabase.rawQuery(maxDate, null);
				try {
					cursor.moveToFirst();
					if (cursor.isAfterLast()) {
						// nothing to do, empty
						return;
					}
					String maxStr = cursor.getString(0);
					try {
						Date d = mDateFormat.parse(maxStr);
						endDate = mDateFormat.format(new Date(d.getTime() +  Utils.ONE_DAY_MS));
					} catch (ParseException e) {
						Timber.e(e, "Parse error in bubble forwards");
						return;
					}
				} finally {
					cursor.close();
				}
			} else {
				endDate = cursor.getString(0);
				cursor.close();
			}

			final String updateQ = "update shaves set count = count + 1 where " +
				"date < ? and date > ? and count >= 1";
			args = new String[] { endDate, date };
			cursor = mDatabase.rawQuery(updateQ, args);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				cursor.moveToNext();
			}
			cursor.close();
			Timber.d("update shaves to count + 1 for %s to %s", endDate, date);
		} finally {
			close();
		}
	}

	private void saveEntry(final ShaveEntry entry) {

		ContentValues values = new ContentValues();
		boolean isUpdate = entry.getID() != -1;
		values.put(Contract.Shaves.DATE, entry.getDate());
		values.put(Contract.Shaves.COUNT, entry.getCount());
		values.put(Contract.Shaves.COMMENT, entry.getComment());

		if (isUpdate) {
			String args[] = new String[] { Long.toString(entry.getID()) };
			Timber.d("Updating the entry %s: day:%s cnt:%d comment:%s",
					args[0], entry.getDate(), entry.getCount(), entry.getComment());
			mDatabase.update(Contract.Shaves.TABLE_NAME,
					values,
					BaseColumns._ID + " = ?",
					args);
		} else {
			Timber.d("saving a new entry");
			mDatabase.insert(Contract.Shaves.TABLE_NAME, null, values);
		}

	}

	private String getOldestDate() {
		// get the oldest date in the database
		String query = Contract.Shaves.SELECT + Contract.Shaves.ORDER_BY_DATE_ASC + " LIMIT 1";
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		String result = null;
		if (!cursor.isAfterLast()) {
			result = cursor.getString(1);
		}
		cursor.close();
		return result;
	}

	private String getNewestDate() {
		// get the newest date - today
		Date today = Calendar.getInstance().getTime();
		return mDateFormat.format(today);
	}

	public void toCSV(FileOutputStream outstream) throws IOException {
		open();
		String query = Contract.Shaves.SELECT + Contract.Shaves.ORDER_BY_DATE_ASC;
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		String oldestDate = getOldestDate();
		if (oldestDate == null) {
			throw new IllegalArgumentException("No entries");
		}
		String newestDate = getNewestDate();
		Date current;
		try {
			current = mDateFormat.parse(oldestDate);
		} catch (ParseException ex) {
			Timber.w(ex, "Unable to parse %s ", oldestDate);
			current = Calendar.getInstance().getTime();
		}
		Writer fw = new OutputStreamWriter(outstream, Charsets.UTF_8);
		CSVWriter writer = new CSVWriter(fw, ',');
		String outputData[] = new String[3];
		while (true) {
			String date = "";
			if (!cursor.isAfterLast()) {
				date = cursor.getString(1);
			}
			String thisDay = mDateFormat.format(current);
			String countStr = "-";
			String comment = "";

			if (date.equals(thisDay)) {
				// is the next on the cursor
				int count = cursor.getInt(2);
				comment = cursor.getString(3);
				countStr = Integer.toString(count);
				cursor.moveToNext();
			}
			outputData[0] = thisDay;
			outputData[1] = countStr;
			outputData[2] = comment;
			writer.writeNext(outputData);
			if (thisDay.equals(newestDate)) {
				break;
			}
			// move to next day
			current.setTime(current.getTime() + Utils.ONE_DAY_MS);
		}
		writer.close();

		cursor.close();
		close();
	}

	public void importData(InputStream is) throws IOException {
		open();
		Closer closer = Closer.create();
		try {
			CSVReader reader = new CSVReader(new InputStreamReader(is, Charsets.UTF_8), ',');
			closer.register(reader);
			while (true) {
				String[] str = reader.readNext();
				if (str == null)
					break;
				if (str.length < 3)
					continue;
				String date = str[0];
				String count = str[1];
				String comment = str[2];
				boolean emptyCount = "-".equals(count);
				if (emptyCount && comment.isEmpty())
					continue;
				try {
					mDateFormat.parse(date);
					int countVal = 1;
					try {
						if (!emptyCount)
							countVal = Integer.parseInt(count);
					} catch (NumberFormatException ex) {
						// pass - use 1
						Timber.w(ex, "import failed to parse number %s", count);
					}
					List<ShaveEntry> entries = getEntry(date);
					ShaveEntry entry = entries.get(0);
					ShaveEntry newEntry = new ShaveEntry(entry.getID(), date, countVal, comment);
					if (entry.getID() >= 0 && !shouldReplace(entry, newEntry)) {
						// replacement entry
						continue;
					}
					saveEntry(newEntry);
				} catch (ParseException ex) {
					Timber.w(ex, "importing data, failed to parse %s", Arrays.toString(str));
				}
			}
		} catch (Throwable ex) {
			Timber.e(ex, "importing data unknown exception");
			closer.rethrow(ex);
		} finally {
			closer.close();
		}
		close();
	}

	private boolean shouldReplace(final ShaveEntry entry, final ShaveEntry newEntry) {
		// replace if entry had no comment, new entry has comment
		final String origComment = entry.getComment();
		if (Strings.isNullOrEmpty(origComment))
			return true;
		final String newComment = newEntry.getComment();
		if (Strings.isNullOrEmpty(newComment))
			return false;
		if (newComment.length() > origComment.length())
			return true;
		// same comment, changed count... (this could mess up)
		return origComment.equals(newComment) &&
			entry.getCount() != newEntry.getCount();
	}
}
