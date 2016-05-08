package es.quirk.bladereminder.widgets;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatTextView;

import com.google.common.collect.Range;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.R;
import timber.log.Timber;

public class UsesView extends AppCompatTextView implements OnSharedPreferenceChangeListener {

	private final SparseArray<Range<Integer> > mRanges = new SparseArray<>(7);
	private final static String EMPTY_DASH = "-";
	private boolean mColoursEnabled = true;
	private boolean mLightTheme = true;
	private final Pattern mDigits = Pattern.compile("[^0-9]*(\\d+)[^0-9]*");

	public UsesView(@NonNull Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public UsesView(@NonNull Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public UsesView(@NonNull Context context) {
		super(context);
		init(context);
	}

	private void init(@NonNull Context context) {
		Timber.d("init in usesview");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);
		if (!isInEditMode()) {
			mColoursEnabled = Utils.setRangesFromPrefs(prefs, mRanges);
			mLightTheme = prefs.getString("default_theme", "0").equals("0");
		}
	}

	@Override
	public void setText(@NonNull CharSequence text, BufferType type) {
		if (text.length() == 0 || "0".equals(text))
			super.setText(EMPTY_DASH, type);
		else
			super.setText(text, type);
		setColourFromContent();
	}

	@Override
	public void setEnabled(boolean isen) {
		super.setEnabled(isen);
		Timber.d("________ set uses enabled ");
		setColourFromContent();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if ("colours_enabled".equals(key) || "default_theme".equals(key)
				|| Utils.isColourPref(key)) {
			mColoursEnabled = Utils.setRangesFromPrefs(prefs, mRanges);
			setColourFromContent();
		}
	}

	private void setColourFromContent() {
		String content = getText().toString();
		if (!mColoursEnabled || mRanges == null || EMPTY_DASH.equals(content)) {
			Timber.d("no colors or range null enabled = %s", Boolean.toString(mColoursEnabled));
			setBackgroundResource(0);

			if (isEnabled()) {
				setTextColor(mLightTheme ? Utils.DARK_TEXT : Utils.LIGHT_TEXT);
			} else {
				setTextColor(mLightTheme ? ContextCompat.getColor(getContext(), R.color.grey_400):
						ContextCompat.getColor(getContext(), R.color.grey_700));
			}
			return;
		}
		if (!isEnabled()) {
			if (mLightTheme) {
				setBackgroundResource(R.drawable.round_gray);
				setTextColor(Utils.LIGHT_TEXT);
			} else {
				setBackgroundResource(R.drawable.round_gray_dark);
				setTextColor(Utils.DARK_TEXT);
			}
			return;
		}
		try {
			Matcher m = mDigits.matcher(content);
			if (m.find()) {
				content = m.group(1);
			}
			Timber.d("checking the text '%s'", content);
			int count = Integer.parseInt(content);
			int colourResource = Utils.getCountColour(count, mRanges);
			setBackgroundResource(colourResource);
			setTextColor(Utils.getTextColourForResource(colourResource));
		} catch (NumberFormatException ex) {
			Timber.d("NFE!!");
			setBackgroundResource(0);
			setTextColor(mLightTheme ? Utils.DARK_TEXT : Utils.LIGHT_TEXT);
		}
	}

}
