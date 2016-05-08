package es.quirk.bladereminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;

import android.util.SparseIntArray;
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

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import es.quirk.bladereminder.ShaveEntry;
import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.RazorUndoAction;
import timber.log.Timber;

public class DataSource {
	private SQLiteDatabase mDatabase;
	private static final String EQ_ARG = " = ?";
	@NonNull
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

	@NonNull
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
			entries.add(ShaveEntry.fromCursor(cursor));
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

	private void saveEntry(@NonNull final ShaveEntry entry) {

		ContentValues values = new ContentValues();
		boolean isUpdate = entry.getID() != -1;
		values.put(Contract.Shaves.DATE, entry.getDate());
		values.put(Contract.Shaves.COUNT, entry.getCount());
		values.put(Contract.Shaves.COMMENT, entry.getComment());
		values.put(Contract.Shaves.RAZOR, entry.getRazor());

		if (isUpdate) {
			String args[] = new String[] { Long.toString(entry.getID()) };
			Timber.d("Updating the entry %s: day:%s cnt:%d comment:%s",
					args[0], entry.getDate(), entry.getCount(), entry.getComment());
			mDatabase.update(Contract.Shaves.TABLE_NAME,
					values,
					BaseColumns._ID + EQ_ARG,
					args);
		} else {
			Timber.d("saving a new entry: %s", entry.toString());
			mDatabase.insert(Contract.Shaves.TABLE_NAME, null, values);
		}

	}

	@NonNull
	public String getOldestDate() {
		// get the oldest date in the database
		String query = Contract.Shaves.SELECT + Contract.Shaves.ORDER_BY_DATE_ASC + " LIMIT 1";
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		String result = "";
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

	public void toCSV(@NonNull FileOutputStream outstream) throws IOException {
		open();
		String query = Contract.Shaves.SELECT + Contract.Shaves.ORDER_BY_DATE_ASC;
		List<String> razors = getRazors();
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		String oldestDate = getOldestDate();
		if (oldestDate == null) {
			throw new IllegalArgumentException("No entries");
		}
		String newestDate = getNewestDate();
		Calendar currentDay = Calendar.getInstance();
		try {
			currentDay.setTime(mDateFormat.parse(oldestDate));
		} catch (ParseException ex) {
			Timber.w(ex, "Unable to parse %s ", oldestDate);
		}
		Writer fw = new OutputStreamWriter(outstream, Charsets.UTF_8);
		CSVWriter writer = new CSVWriter(fw, ',');
		String outputData[] = new String[4];
		while (true) {
			String date = "";
			if (!cursor.isAfterLast()) {
				date = cursor.getString(1);
			}
			String thisDay = mDateFormat.format(currentDay.getTime());
			String countStr = "-";
			String comment = "";
			String razorName = "";

			if (date.equals(thisDay)) {
				// is the next on the cursor
				int count = cursor.getInt(2);
				comment = cursor.getString(3);
				String razorId = cursor.getString(4);
				try {
					int rid = Integer.parseInt(razorId);
					rid--;
					if (rid < razors.size()) {
						razorName = razors.get(rid);
					}
				} catch (NumberFormatException ex) {
				}

				countStr = Integer.toString(count);
				cursor.moveToNext();
			}
			outputData[0] = thisDay;
			outputData[1] = countStr;
			outputData[2] = comment;
			outputData[3] = razorName;
			writer.writeNext(outputData);
			if (thisDay.equals(newestDate)) {
				break;
			}
			// move to next day
			currentDay.add(Calendar.DATE, 1);
		}
		writer.close();

		cursor.close();
		close();
	}

	public void importData(@NonNull InputStream is) throws IOException {
		open();
		Closer closer = Closer.create();
		try {
			CSVReader reader = new CSVReader(new InputStreamReader(is, Charsets.UTF_8), ',');
			closer.register(reader);
			List<String> razors = getRazors();
			SparseIntArray lazyRazorTable = new SparseIntArray();
			while (true) {
				String[] str = reader.readNext();
				if (str == null)
					break;
				if (str.length < 3)
					continue;
				String date = str[0];
				String count = str[1];
				String comment = str[2];
				String razorName = "";
				if (str.length > 3)
					razorName = str[3];

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
					int razorId = 1;
					if (!razorName.isEmpty()) {
						int idx = razors.indexOf(razorName);
						if (idx == -1) {
							// don't add a new razor if all we have are empties so far
							// instead edit the 0th entry
							if (razors.size() == 1 && "---".equals(razors.get(0))) {
								editRazor("0", razorName);
							} else {
								addRazor(razorName);
							}
							razors = getRazors();
						}
						idx = razors.indexOf(razorName);
						// did it work? if so, use the new id
						if (idx != -1) {
							razorId = lazyRazorTable.get(idx, -1);
							// cache the razor ids to avoid db look up on every row
							if (razorId == -1) {
								razorId = getRazorId(idx);
								lazyRazorTable.put(idx, razorId);
							}
						}
					}
					List<ShaveEntry> entries = getEntry(date);
					ShaveEntry entry = entries.get(0);
					ShaveEntry newEntry = new ShaveEntry(entry.getID(),
							date, countVal, comment, Integer.toString(razorId));
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

	private boolean shouldReplace(@NonNull final ShaveEntry entry, @NonNull final ShaveEntry newEntry) {
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

	private int getUsageEntries(String what) {
		// select count(_id) from shaves where count >= 1;
		int numberEntries = 0;
		String query = String.format("select count(%s) from %s where %s %s 1",
				Contract.Shaves._ID, Contract.Shaves.TABLE_NAME, Contract.Shaves.COUNT,
				what);
		Timber.d("getUsageEntries: %s", query);
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();

		if (!cursor.isAfterLast()) {
			numberEntries = cursor.getInt(0);
		}
		cursor.close();
		return numberEntries;
	}

	public int getRazorCount() {
		open();
		int count = 0;
		try {
			String query = Contract.Razors.SELECT_COUNT;
			Cursor cursor = mDatabase.rawQuery(query, null);
			cursor.moveToFirst();
			if (!cursor.isAfterLast()) {
				count = cursor.getInt(0);
			}
			cursor.close();
		} finally {
			close();
		}
		return count;
	}

	public List<String> getRazors() {
		boolean didOpen = openIfNeeded();
		try {
			List<String> entries = Lists.newArrayList();
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(Contract.Razors.TABLE_NAME);
			String[] projection = { Contract.Razors.NAME };
			Cursor cursor = queryBuilder.query(mDatabase, projection, null,
					null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				entries.add(cursor.getString(0));
				cursor.moveToNext();
			}
			cursor.close();
			return entries;
		} finally {
			if (didOpen)
				close();
		}
	}

	public String getRazor(int position) {
		String result = "";
		boolean didOpen = openIfNeeded();
		try {

			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(Contract.Razors.TABLE_NAME);
			String[] projection = { Contract.Razors.NAME };
			Cursor cursor = queryBuilder.query(mDatabase, projection, null,
					null, null, null, null);
			cursor.moveToFirst();
			int index = 0;
			while (!cursor.isAfterLast()) {
				result = cursor.getString(0);
				cursor.moveToNext();
				if (index == position)
					break;
				index++;
			}
			cursor.close();
		} finally {
			if (didOpen)
				close();
		}
		return result;
	}

	public void addRazor(String name) {
		List<String> names = getRazors();
		if (names.contains(name)) {
			// do nothing!
			return;
		}
		boolean didOpen = openIfNeeded();
		try {
			ContentValues values = new ContentValues();
			values.put(Contract.Razors.NAME, name);
			Timber.d("adding razor %s", name);
			mDatabase.insert(Contract.Razors.TABLE_NAME, null, values);
		} finally {
			if (didOpen)
				close();
		}
	}

	private boolean openIfNeeded() {
		boolean didOpen = false;
		if (mDatabase == null || !mDatabase.isOpen()) {
			open();
			didOpen = true;
		}
		return didOpen;
	}

	public void editRazor(String position, String newName) {
		boolean didOpen = openIfNeeded();
		int id = getRazorId(Integer.parseInt(position));
		try {
			ContentValues values = new ContentValues();
			String args[] = new String[] { Integer.toString(id) };
			values.put(Contract.Razors.NAME, newName);
			mDatabase.update(Contract.Razors.TABLE_NAME,
					values,
					Contract.Razors._ID + EQ_ARG,
					args);
		} finally {
			if (didOpen)
				close();
		}
	}

	public int getRazorId(int position) {
		// since _ID is random, just the the 1st, 2nd, 3rd... from the list
		int result = 1;
		boolean didOpen = openIfNeeded();
		try {
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(Contract.Razors.TABLE_NAME);
			String[] projection = { Contract.Razors._ID };
			Cursor cursor = queryBuilder.query(mDatabase, projection, null,
					null, null, null, null);
			cursor.moveToFirst();
			int index = 0;
			while (!cursor.isAfterLast()) {
				result = cursor.getInt(0);
				cursor.moveToNext();
				if (position == index)
					break;
				index++;
			}
			cursor.close();
		} finally {
			if (didOpen)
				close();
		}
		return result;
	}

	public RazorUndoAction deleteRazor(int position) {
		open();
		String name = getRazor(position);
		int id = getRazorId(position);
		RazorUndoAction undoAction = new RazorUndoAction(id, name);
		try {
			// get all entries that belong to this razor from the table
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(Contract.Shaves.TABLE_NAME);
			String[] projection = { Contract.Shaves._ID };
			String[] selectionArgs = { Integer.toString(id) };
			Cursor cursor = queryBuilder.query(mDatabase, projection, Contract.Shaves.RAZOR + EQ_ARG,
					selectionArgs, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				undoAction.addShave(cursor.getInt(0));
				cursor.moveToNext();
			}
			cursor.close();

			// now set all entries to 1 for id in the shave table
			ContentValues values = new ContentValues();
			// try to delete first razor... fix that
			if (position == 0) {
				int nextId = getRazorId(1);
				values.put(Contract.Shaves.RAZOR, Integer.toString(nextId));
			} else {
				values.put(Contract.Shaves.RAZOR, "1");
			}

			String args[] = new String[] { Integer.toString(id) };
			Timber.d("update razor to 1 from %s", args[0]);
			// update shaves set razor = 1 where razor = id;
			mDatabase.update(Contract.Shaves.TABLE_NAME,
					values,
					Contract.Shaves.RAZOR + EQ_ARG,
					args);

			// finally, remove the actual shave entry thing...
			mDatabase.delete(Contract.Razors.TABLE_NAME,
					Contract.Razors._ID + EQ_ARG, args);

		} finally {
			close();
		}
		return undoAction;
	}

	public void undoDelete(RazorUndoAction undo) {
		open();
		try {
			// restore the razor to the razor table
			// insert into razors values(_id, name)
			ContentValues values = new ContentValues();
			values.put(Contract.Razors._ID, undo.getID());
			values.put(Contract.Razors.NAME, undo.getName());
			mDatabase.insert(Contract.Razors.TABLE_NAME, null, values);

			// for each entry in the undo action, update that row and restore the right value
			values = new ContentValues();
			values.put(Contract.Shaves.RAZOR, Integer.toString(undo.getID()));
			String args[] = new String[] { "0" };
			for (Integer shaveId : undo.getModifiedShaves()) {
				args[0] = shaveId.toString();
				mDatabase.update(Contract.Shaves.TABLE_NAME,
						values, Contract.Shaves._ID + EQ_ARG,
						args);
			}
		} finally {
			close();
		}
	}

	/**
	 * @returns the average value.
	 * @throws ArithmeticException when blades are 0
	 * @throws NoSuchFieldException when number entries not enough.
	 */
	public float getAverage() throws ArithmeticException, NoSuchFieldException {
		// the calculation is:
		// number of entries = number of shaves
		// number of entries with count of 1 = number of blades used
		// average shaves per blade = number shaves / number blades

		int numberEntries = getUsageEntries(">=");
		if (numberEntries < 3)
			throw new NoSuchFieldException();
		int bladeCount = getUsageEntries("=");
		if (bladeCount < 1)
			throw new ArithmeticException();
		return ((float) numberEntries) / bladeCount;
	}

	@NonNull
	public String getOldestUse() {
		open();
		// get the oldest date in the database
		String query = Contract.Shaves.SELECT + Contract.Shaves.WHERE_COUNT_GT_1 +
			Contract.Shaves.ORDER_BY_DATE_ASC + " LIMIT 1";
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		String result = "";
		if (!cursor.isAfterLast()) {
			result = cursor.getString(1);
		}
		cursor.close();
		return result;
	}

	@Nullable
	private ShaveEntry getHighLowUses(String query) {
		Cursor cursor = mDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		ShaveEntry result = null;
		if (!cursor.isAfterLast()) {
			// _ID, DATE, COUNT, COMMENT
			result = ShaveEntry.fromCursor(cursor);
		}
		cursor.close();
		return result;
	}

	@Nullable
	public ShaveEntry getHighestUse() {
		// get the oldest date in the database
		String query = Contract.Shaves.SELECT +
			Contract.Shaves.ORDER_BY_COUNT + " LIMIT 1";
		return getHighLowUses(query);
	}

}
