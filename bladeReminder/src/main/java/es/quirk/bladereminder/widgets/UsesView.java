package es.quirk.bladereminder.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatTextView;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.quirk.bladereminder.Utils;
import timber.log.Timber;

public class UsesView extends AppCompatTextView implements OnSharedPreferenceChangeListener {

	private final HashMap<Integer, Range<Integer> > mRanges = Maps.newHashMapWithExpectedSize(7);
	private boolean mColoursEnabled = true;
	private boolean mLightTheme = true;
	private final Pattern mDigits = Pattern.compile("[^0-9]*(\\d+)[^0-9]*");

	public UsesView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public UsesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public UsesView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		Timber.d("init in usesview");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.registerOnSharedPreferenceChangeListener(this);
		if (!isInEditMode()) {
			mColoursEnabled = Utils.setRangesFromPrefs(prefs, mRanges);
			mLightTheme = prefs.getString("default_theme", "0").equals("0");
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		if (text.length() == 0 || "0".equals(text))
			super.setText("-", type);
		else
			super.setText(text, type);
		Timber.d("Setting the text to %s", text);
		setColourFromContent();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if ("colours_enabled".equals(key) || "default_theme".equals(key)
				|| Utils.COLOUR_PREFS.contains(key)) {
			mColoursEnabled = Utils.setRangesFromPrefs(prefs, mRanges);
			setColourFromContent();
		}
	}

	private void setColourFromContent() {
		if (!mColoursEnabled || mRanges == null) {
			Timber.d("no colors or range null en=" + mColoursEnabled);
			setBackgroundResource(0);
			setTextColor(mLightTheme ? Utils.DARK_TEXT : Utils.LIGHT_TEXT);
			return;
		}
		String content = getText().toString();
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
