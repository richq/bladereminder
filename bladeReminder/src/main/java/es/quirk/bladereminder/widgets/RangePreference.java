package es.quirk.bladereminder.widgets;

import butterknife.InjectView;
import butterknife.ButterKnife;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar;
import com.google.common.base.Strings;

import java.util.List;

import butterknife.InjectViews;
import es.quirk.bladereminder.R;

public class RangePreference extends DialogPreference implements RangeSeekBar.OnRangeSeekBarChangeListener<Integer>, OnClickListener
{
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	RangeSeekBar<Integer> mRangeSeekBar;
	@InjectView(R.id.splashText) TextView mSplashText;
	@InjectView(R.id.valueText) TextView mValueText;
	@InjectViews(
	{R.id.use1, R.id.use2, R.id.use3, R.id.use4, R.id.use5, R.id.use6, R.id.use7}
	) List<UsesView> mUsesViews;

	private final String mDialogMessage;
	private final String mSuffix;
	private int mDefaultMin;
	private int mDefaultMax;
	private int mSelectedMin;
	private int mSelectedMax;

	public RangePreference(Context context, AttributeSet attrs) {
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

	private void setDefaultValuesFromString(String val) {
		if (Strings.isNullOrEmpty(val)) {
			mDefaultMin  = 1;
			mDefaultMax = 9;
		} else {
			String[] minmax = val.split(",");
			mDefaultMin = Integer.parseInt(minmax[0]);
			mDefaultMax = Integer.parseInt(minmax[1]);
		}
	}

	private void setSelectedValuesFromString(String val) {
		if (Strings.isNullOrEmpty(val)) {
			mSelectedMin = mDefaultMin;
			mSelectedMax = mDefaultMax;
		} else {
			String[] minmax = val.split(",");
			mSelectedMin = Integer.parseInt(minmax[0]);
			mSelectedMax = Integer.parseInt(minmax[1]);
		}
	}

	private static int calcMaxFromRange(int max) {
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

	// DialogPreference methods
	@Override
	protected View onCreateDialogView() {
		View result = super.onCreateDialogView();
		ButterKnife.inject(this, result);
		mRangeSeekBar = ButterKnife.findById(result, R.id.rangeSeekBar);
		if (mDialogMessage != null)
			mSplashText.setText(mDialogMessage);

		if (shouldPersist()) {
			setSelectedValuesFromString(getPersistedString(""));
		} else {
			mSelectedMin = mDefaultMin;
			mSelectedMax = mDefaultMax;
		}
		int rangeMax = calcMaxFromRange(mSelectedMax);

		mRangeSeekBar.setOnRangeSeekBarChangeListener(this);
		mRangeSeekBar.setNotifyWhileDragging(true);
		mRangeSeekBar.setRangeValues(1, rangeMax);
		mRangeSeekBar.setSelectedMinValue(mSelectedMin);
		mRangeSeekBar.setSelectedMaxValue(mSelectedMax);
		updateUsesView(mSelectedMin, mSelectedMax);

		return result;
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		mRangeSeekBar.setSelectedMinValue(mSelectedMin);
		mRangeSeekBar.setSelectedMaxValue(mSelectedMax);
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


	@Override
	public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
		updateUsesView(minValue, maxValue);
	}

	private void updateValueText(Integer minValue, Integer maxValue) {
		StringBuilder sb = new StringBuilder();
		sb.append(minValue);
		sb.append(" - ");
		sb.append(maxValue);
		if (!Strings.isNullOrEmpty(mSuffix)) {
			sb.append(' ');
			sb.append(mSuffix);
		}
		String t = sb.toString();
		mValueText.setText(t);
	}

	private void updateUsesView(Integer minValue, Integer maxValue) {
		updateValueText(minValue, maxValue);
		// redo the range...
		SharedPreferences prefs = getSharedPreferences();
		float step = (maxValue - minValue) / 5.0f;

		SharedPreferences.Editor editor = prefs.edit();

		editor.putInt("purple_sharp", minValue);
		mUsesViews.get(0).setText("" + minValue, TextView.BufferType.NORMAL);
		float next = minValue + step;
		editor.putInt("white_sharp", Math.round(next));
		mUsesViews.get(1).setText("" + Math.round(next), TextView.BufferType.NORMAL);
		next += step;
		editor.putInt("blue_sharp", Math.round(next));
		mUsesViews.get(2).setText("" + Math.round(next), TextView.BufferType.NORMAL);
		next += step;
		editor.putInt("green_sharp", Math.round(next));
		mUsesViews.get(3).setText("" + Math.round(next), TextView.BufferType.NORMAL);
		next += step;
		editor.putInt("yellow_sharp", Math.round(next));
		mUsesViews.get(4).setText("" + Math.round(next), TextView.BufferType.NORMAL);
		next += step;
		editor.putInt("orange_sharp", Math.round(next));
		mUsesViews.get(5).setText("" + Math.round(next), TextView.BufferType.NORMAL);
		next += 1;
		mUsesViews.get(6).setText(""+ Math.round(next) + "+");

		editor.apply();

	}

	// Set the positive button listener and onClick action
	@Override
	public void showDialog(Bundle state) {

		super.showDialog(state);

		Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
		positiveButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		if (shouldPersist()) {
			mSelectedMin = mRangeSeekBar.getSelectedMinValue();
			mSelectedMax = mRangeSeekBar.getSelectedMaxValue();
			persistValues(mSelectedMin, mSelectedMax);
		}

		getDialog().dismiss();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (!positiveResult) {
			// restore old values..
			setSelectedValuesFromString(getPersistedString(""));
			updateUsesView(mSelectedMin, mSelectedMax);
		}
		super.onDialogClosed(positiveResult);
	}
}