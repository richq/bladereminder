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
	private final String mRazorId;

	public ShaveEntry(String date) {
		this(-1, date, 0, "", "1");
	}

	public ShaveEntry(long id, String date, int count, String comment, String razorId) {
		mID = id;
		mDate = date;
		mCount = count;
		mComment = comment;
		mRazorId = razorId;
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

	public String getRazor() {
		return mRazorId;
	}

	public String toString() {
		return String.format(Locale.US, "ShaveEntry(id=%d, date='%s', count=%d, comment='%s', razorid=%s)",
				mID, mDate, mCount, mComment, mRazorId);
	}

	@NonNull
	public static ShaveEntry fromCursor(@NonNull Cursor cursor) {
		return new ShaveEntry(cursor.getInt(0),
				cursor.getString(1),
				cursor.getInt(2),
				cursor.getString(3),
				cursor.getString(4));
	}

}

