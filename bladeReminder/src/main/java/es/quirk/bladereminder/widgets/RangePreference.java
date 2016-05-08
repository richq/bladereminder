package es.quirk.bladereminder.widgets;

import butterknife.ButterKnife;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import org.florescu.android.rangeseekbar.RangeSeekBar;
import com.google.common.base.Strings;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import es.quirk.bladereminder.R;
import es.quirk.bladereminder.Utils;

public class RangePreference extends DialogPreference {
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	@NonNull
	private final String mDialogMessage;
	@NonNull
	private final String mSuffix;
	private int mDefaultMin;
	private int mDefaultMax;
	private int mSelectedMin;
	private int mSelectedMax;

	public RangePreference(@NonNull Context context, @NonNull AttributeSet attrs) {
		super(context,attrs);
		setDialogLayoutResource(R.layout.range_preference);

		int msgid = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
		mDialogMessage = (msgid == 0) ? attrs.getAttributeValue(androidns, "dialogMessage")
			: context.getString(msgid);

		int suffixid = attrs.getAttributeResourceValue(androidns, "text", 0);
		mSuffix = (suffixid == 0) ? attrs.getAttributeValue(androidns, "text")
			: context.getString(suffixid);

		String defaultValue = attrs.getAttributeValue(androidns, "defaultValue");
		setDefaultValuesFromString(defaultValue);
	}

	private void setDefaultValuesFromString(@NonNull String val) {
		if (Strings.isNullOrEmpty(val)) {
			mDefaultMin  = 1;
			mDefaultMax = 9;
		} else {
			String[] minmax = val.split(",");
			mDefaultMin = Integer.parseInt(minmax[0]);
			mDefaultMax = Integer.parseInt(minmax[1]);
		}
	}

	private void setSelectedValuesFromString(@NonNull String val) {
		if (Strings.isNullOrEmpty(val)) {
			mSelectedMin = mDefaultMin;
			mSelectedMax = mDefaultMax;
		} else {
			String[] minmax = val.split(",");
			mSelectedMin = Integer.parseInt(minmax[0]);
			mSelectedMax = Integer.parseInt(minmax[1]);
		}
	}

	public static int calcMaxFromRange(int max) {
		int result = 99; // default
		if (max < 10) {
			// 0 - 10 --- 15
			result = 15;
		} else if (max < 15) {
			// 0-15 ----- 30
			result = 30;
		} else if (max < 30) {
			// 0-30 ----- 50
			result = 50;
		} else if (max < 50) {
			// 0-50 ----- 75
			result = 75;
		}
		// if it is 99 then who cares about remembering?
		return result;
	}

	public static class RangePreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat
			implements RangeSeekBar.OnRangeSeekBarChangeListener<Integer>  {

		private RangeSeekBar<Integer> mRangeSeekBar;
		@BindView(R.id.splashText) TextView mSplashText;
		@BindView(R.id.valueText) TextView mValueText;
		@BindViews(
		{R.id.use1, R.id.use2, R.id.use3, R.id.use4, R.id.use5, R.id.use6, R.id.use7}
		) List<UsesView> mUsesViews;

		private RangePreference mRangePreference;

		// setup boilerplate
		public static RangePreferenceDialogFragmentCompat newInstance(RangePreference preference) {
			RangePreferenceDialogFragmentCompat fragment = new RangePreferenceDialogFragmentCompat();
			fragment.setRangePreference(preference);
			Bundle bundle = new Bundle(1);
			bundle.putString(ARG_KEY, preference.getKey());
			fragment.setArguments(bundle);
			return fragment;
		}

		// inject external class instance
		private void setRangePreference(RangePreference preference) {
			mRangePreference = preference;
		}

		// DialogPreference methods
		@Override
		protected View onCreateDialogView(Context context) {
			View result = super.onCreateDialogView(context);
			ButterKnife.bind(this, result);
			mRangeSeekBar = ButterKnife.findById(result, R.id.rangeSeekBar);
			mSplashText.setText(mRangePreference.mDialogMessage);

			mRangePreference.initSelectedMinMax();
			int max = mRangePreference.mSelectedMax;
			int min = mRangePreference.mSelectedMin;
			int rangeMax = calcMaxFromRange(max);

			mRangeSeekBar.setOnRangeSeekBarChangeListener(this);
			mRangeSeekBar.setNotifyWhileDragging(true);
			mRangeSeekBar.setRangeValues(1, rangeMax);
			mRangeSeekBar.setSelectedMinValue(min);
			mRangeSeekBar.setSelectedMaxValue(max);
			updateUsesView(min, max);

			return result;
		}

		@Override
		protected void onBindDialogView(@NonNull View v) {
			super.onBindDialogView(v);
			mRangeSeekBar.setSelectedMinValue(mRangePreference.mSelectedMin);
			mRangeSeekBar.setSelectedMaxValue(mRangePreference.mSelectedMax);
		}

		@Override
		public void onDialogClosed(boolean positiveResult) {
			if (positiveResult) {
				mRangePreference.maybeStore(mRangeSeekBar.getSelectedMinValue(),
						mRangeSeekBar.getSelectedMaxValue());
			} else {
				// restore old values..
				mRangePreference.setSelectedValuesFromString(mRangePreference.getPersistedString(""));
				updateUsesView(mRangePreference.mSelectedMin, mRangePreference.mSelectedMax);
			}
		}

		@Override
		public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
			updateUsesView(minValue, maxValue);
		}

		private void updateValueText(int minValue, int maxValue) {
			String valueText = minValue + " - " + maxValue;
			if (!Strings.isNullOrEmpty(mRangePreference.mSuffix)) {
				valueText = valueText + " " + mRangePreference.mSuffix;
			}
			mValueText.setText(valueText);
		}

		private void updateEditorViewWhatever(@NonNull SharedPreferences.Editor editor, String entry, int value, int viewId) {
			editor.putInt(entry, value);
			mUsesViews.get(viewId).setText(Integer.toString(value), TextView.BufferType.NORMAL);
		}

		private void updateUsesView(int minValue, int maxValue) {
			updateValueText(minValue, maxValue);
			// redo the range...
			SharedPreferences prefs = mRangePreference.getSharedPreferences();
			float step = (maxValue - minValue) / 5.0f;

			SharedPreferences.Editor editor = prefs.edit();
			float next = minValue;
			for (int i = 0; i < 6; i++) {
				if (i > 0)
					next += step;
				updateEditorViewWhatever(editor, Utils.COLOUR_PREFS.get(i), Math.round(next), i);
			}
			next += 1;
			mUsesViews.get(6).setText(Integer.toString(Math.round(next)) + "+");
			editor.apply();
		}


	}


	protected void maybeStore(int min, int max) {
		if (shouldPersist()) {
			mSelectedMin = min;
			mSelectedMax = max;
			persistValues(mSelectedMin, mSelectedMax);
		}
	}

	protected void initSelectedMinMax() {
		if (shouldPersist()) {
			setSelectedValuesFromString(getPersistedString(""));
		} else {
			mSelectedMin = mDefaultMin;
			mSelectedMax = mDefaultMax;
		}
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue)
	{
		super.onSetInitialValue(restore, defaultValue);
		if (restore) {
			setSelectedValuesFromString(shouldPersist() ? getPersistedString("") : "");
		} else {
			setSelectedValuesFromString((String)defaultValue);
		}
	}

	private void persistValues(int min, int max) {
		String res = String.valueOf(min) + ',' + max;
		persistString(res);
		callChangeListener(res);
	}

}
