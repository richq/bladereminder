package es.quirk.bladereminder;

import android.database.Cursor;
import android.support.annotation.NonNull;
import java.io.Serializable;
import java.util.Locale;


public class ShaveEntry implements Serializable {
	private static final long serialVersionUID = 1L;

	private final long mID;
	private final String mDate;
	private final int mCount;
	private final String mComment;

	public ShaveEntry(String date) {
		this(-1, date, 0, "");
	}

	public ShaveEntry(long id, String date, int count, String comment) {
		mID = id;
		mDate = date;
		mCount = count;
		mComment = comment;
	}

	public long getID() {
		return mID;
	}

	public String getDate() {
		return mDate;
	}

	public int getCount() {
		return mCount;
	}

	public String getComment() {
		return mComment;
	}

	public String toString() {
		return String.format(Locale.US, "ShaveEntry(id=%d, date='%s', count=%d, comment='%s')",
				mID, mDate, mCount, mComment);
	}

	@NonNull
	public static ShaveEntry fromCursor(@NonNull Cursor cursor) {
		return new ShaveEntry(cursor.getInt(0),
				cursor.getString(1),
				cursor.getInt(2),
				cursor.getString(3));
	}

}

