package es.quirk.bladereminder.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import timber.log.Timber;

public class UsageHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "shaves.db";
	private static final int DATABASE_VERSION = 1;

	public UsageHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(Contract.Shaves.DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Timber.w("Upgrading database from version %d to %d, which would destroy all data!",
				oldVersion, newVersion);
		/*db.execSQL("DROP TABLE IF EXISTS " + Contract.Shaves.TABLE_NAME);
		onCreate(db);*/
	}

}
