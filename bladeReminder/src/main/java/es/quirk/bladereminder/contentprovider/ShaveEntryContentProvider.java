package es.quirk.bladereminder.contentprovider;

import android.appwidget.AppWidgetManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

import es.quirk.bladereminder.SC;
import es.quirk.bladereminder.database.Contract.Shaves;
import es.quirk.bladereminder.database.UsageHelper;
import timber.log.Timber;

public class ShaveEntryContentProvider extends ContentProvider {

	private UsageHelper mDatabase;

	// used for the UriMacher
	private static final int SHAVES = 10;
	private static final int SHAVE_ID = 20;
	private static final String SCHEME = "content://";
        private static final String AUTHORITY = "es.quirk.bladereminder.contentprovider";
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + "/shaves");
	private static final String[] AVAILABLE = {
		Shaves._ID,
		Shaves.DATE,
		Shaves.COUNT,
		Shaves.COMMENT,
	};

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(AUTHORITY, Shaves.BASE_PATH + "/#", SHAVE_ID);
		URI_MATCHER.addURI(AUTHORITY, Shaves.BASE_PATH, SHAVES);
	}

	@Override
	public boolean onCreate() {
		mDatabase = new UsageHelper(getContext());
		Timber.d("onCreate called");
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		Timber.d("query called %s %s %s %s",
			uri,
			selection,
			Arrays.toString(selectionArgs),
			sortOrder);
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(Shaves.TABLE_NAME);

		int uriType = URI_MATCHER.match(uri);
		if (uriType == SHAVE_ID) {
			// adding the ID to the original query
			String where = Shaves._ID + "=" + uri.getLastPathSegment();
			Timber.d("where clause = '%s'", where);
			queryBuilder.appendWhere(where);
		} else if (uriType != SHAVES) {
			throw new IllegalArgumentException(SC.UNKNOWN_URI + uri);
		}

		SQLiteDatabase db = mDatabase.getReadableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);
		// make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	private void checkColumns(String[] projection) {
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<>(Arrays.asList(AVAILABLE));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = URI_MATCHER.match(uri);
		String theSelection;
		String [] theArgs = null;
		if (uriType == SHAVES) {
			theSelection = selection;
			theArgs = selectionArgs;
		} else if (uriType == SHAVE_ID) {
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				theSelection = Shaves._ID + "=" + id;
			} else {
				theSelection = Shaves._ID + "=" + id + " and " + selection;
				theArgs = selectionArgs;
			}
		} else {
			throw new IllegalArgumentException(SC.UNKNOWN_URI + uri);
		}
		SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		int rowsDeleted = sqlDB.delete(Shaves.TABLE_NAME, theSelection,
				theArgs);
		notifyWidgets(uri);
		return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		int uriType = URI_MATCHER.match(uri);
		SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		long id;
		if (uriType == SHAVES) {
			id = sqlDB.insert(Shaves.TABLE_NAME, null, values);
		} else {
			throw new IllegalArgumentException(SC.UNKNOWN_URI + uri);
		}
		notifyWidgets(uri);
		return Uri.parse(Shaves.BASE_PATH + "/" + id);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = URI_MATCHER.match(uri);
		String theSelection;
		String [] theArgs = null;
		if (uriType == SHAVES) {
			theSelection = selection;
			theArgs = selectionArgs;
		} else if (uriType == SHAVE_ID) {
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				theSelection = Shaves._ID + "=" + id;
			} else {
				theSelection = Shaves._ID + "=" + id
						+ " and "
						+ selection;
				theArgs = selectionArgs;
			}
		} else {
			throw new IllegalArgumentException(SC.UNKNOWN_URI + uri);
		}
		SQLiteDatabase sqlDB = mDatabase.getWritableDatabase();
		int rowsUpdated = sqlDB.update(Shaves.TABLE_NAME,
				values,
				theSelection,
				theArgs);
		notifyWidgets(uri);
		return rowsUpdated;
	}

        private void notifyWidgets(Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null);
                Intent initialUpdateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                initialUpdateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                getContext().sendBroadcast(initialUpdateIntent);
        }
}
