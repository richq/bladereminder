package es.quirk.bladereminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import timber.log.Timber;

public class UsageHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "shaves.db";
	private static final int DATABASE_VERSION = 4;
	private static final String DATABASE_ALTER_ADD_RAZOR = "ALTER TABLE "
		+ Contract.Shaves.TABLE_NAME + " ADD COLUMN " + Contract.Shaves.RAZOR + " string;";

	public UsageHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private void insertDefaultRazor(SQLiteDatabase database) {
		// insert default value
		ContentValues values = new ContentValues();
		values.put(Contract.Razors.NAME, "---");
		database.insert(Contract.Razors.TABLE_NAME, null, values);
	}

	private void updateShavesTable(SQLiteDatabase database) {
		// update shaves set razor = 0 where razor is null;
		ContentValues values = new ContentValues();
		Cursor cursor = database.rawQuery("SELECT _ID FROM RAZORS LIMIT 1", null);
		cursor.moveToFirst();
		int id = 0;
		while (!cursor.isAfterLast()) {
			id = cursor.getInt(0);
			cursor.moveToNext();
		}
		cursor.close();

		values.put(Contract.Shaves.RAZOR, id);
		String theSelection = Contract.Shaves.RAZOR + " IS NULL AND " +
			Contract.Shaves.COUNT + " > 0";
		String [] theArgs = null;
		database.update(Contract.Shaves.TABLE_NAME, values, theSelection, null);
	}

	@Override
	public void onCreate(@NonNull SQLiteDatabase database) {
		database.execSQL(Contract.Shaves.DATABASE_CREATE);
		database.execSQL(Contract.Razors.DATABASE_CREATE);
		insertDefaultRazor(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			Timber.w("Upgrading database from version %d to %d, adding razor column",
					oldVersion, newVersion);
			try {
				db.execSQL(DATABASE_ALTER_ADD_RAZOR);
			} catch (Exception e) {
				Timber.e(e, "Error adding new column");
			}
		}
		if (oldVersion < 3) {
			Timber.w("Upgrading database from version %d to %d, adding razor table",
					oldVersion, newVersion);
			try {
				db.execSQL(Contract.Razors.DATABASE_CREATE);
			} catch (Exception e) {
				Timber.e(e, "Error adding new table");
			}
		}

		if (oldVersion < 4) {
			Timber.w("Upgrading database from version %d to %d, adding razors",
					oldVersion, newVersion);
			try {
				insertDefaultRazor(db);
				updateShavesTable(db);
			} catch (Exception e) {
				Timber.e(e, "Error adding new table");
			}
		}
	}

}
