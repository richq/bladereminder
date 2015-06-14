package es.quirk.bladereminder.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import es.quirk.bladereminder.R;
import es.quirk.bladereminder.Utils;
import timber.log.Timber;

public class DateLabel extends TextView implements OnSharedPreferenceChangeListener {

	private DateFormat mUserDateFormat;
	private final DateFormat mDateFormat = new SimpleDateFormat(Utils.DATE_FORMAT, Locale.US);
	@SuppressLint("SimpleDateFormat")
	private final DateFormat mLocaleDayFormat = new SimpleDateFormat("EEEE");

	public DateLabel(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public DateLabel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public DateLabel(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);
		setUserDateFormatFromPrefs(prefs);
	}

	private CharSequence niceDate(Resources resources, final CharSequence date) {
		// How can mDateFormat be null? Dunno, but it is sometimes.
		if (date == null || mDateFormat == null)
			return date;

		Date theDate;
		try {
			theDate = mDateFormat.parse(date.toString());
		} catch (ParseException e) {
			return date;
		}
		Date today = Calendar.getInstance().getTime();
		if (mDateFormat.format(today).equals(date)) {
			return String.format("%s (%s)", resources.getString(R.string.today),
					mLocaleDayFormat.format(theDate));
		}
		Date yesterday = new Date(today.getTime() - 24 * 3600 * 1000);
		if (mDateFormat.format(yesterday).equals(date)) {
			return String.format("%s (%s)", resources.getString(R.string.yesterday),
					mLocaleDayFormat.format(theDate));
		}

		Date oneweek = new Date(today.getTime() - 7 * 24 * 3600 * 1000);
		if (oneweek.before(theDate)) {
			return mLocaleDayFormat.format(theDate);
		}
		return mUserDateFormat.format(theDate);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(niceDate(getResources(), text), type);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if ("date_format".equals(key)) {
			setUserDateFormatFromPrefs(prefs);
		}
	}

	@SuppressLint("SimpleDateFormat")
	private void setUserDateFormatFromPrefs(SharedPreferences prefs) {
		try {
			mUserDateFormat = new SimpleDateFormat(prefs.getString("date_format", Utils.DATE_FORMAT));
		} catch (IllegalArgumentException ex) {
			Timber.w(ex, "setUserDateFormatFromPrefs IllegalArgumentException");
			if (mUserDateFormat == null)
				mUserDateFormat = mDateFormat;
		}
	}

}
