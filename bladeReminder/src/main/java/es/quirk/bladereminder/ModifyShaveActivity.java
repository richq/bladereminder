package es.quirk.bladereminder;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import butterknife.InjectView;
import es.quirk.bladereminder.database.Contract.Shaves;
import timber.log.Timber;

public class ModifyShaveActivity extends BaseActivity implements OnValueChangeListener {
	private static final int COUNT_RANGE = 10;
	private Uri mShaveId;
	@InjectView(R.id.usesDisplay) TextView mUsageText;
	@InjectView(R.id.shaveUsageText) NumberPicker mCount;
	@InjectView(R.id.shaveCommentField) EditText mComment;
	@InjectView(R.id.dateLabel) TextView mDateLabel;

	private final HashMap<Integer, Range<Integer> > mRanges = Maps.newHashMapWithExpectedSize(7);
	private boolean mEnableColours = true;
	private final DateFormat mDateFormat = Utils.createDateFormatYYYYMMDD();
	private final DateFormat mLocalFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.modify_shave_view);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		mToolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Timber.d("onclick pressed in setNavigationOnClickListener");
				finish();
			}
		});

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mEnableColours = Utils.setRangesFromPrefs(prefs, mRanges);
		mCount.setMaxValue(COUNT_RANGE);
		mCount.setMinValue(1);
		mCount.setOnValueChangedListener(this);
		updateUsageText(1);
		Intent intent = getIntent();
		boolean edit = intent.getAction().equals(Intent.ACTION_EDIT);
		if (edit) {
			mShaveId = Uri.parse(intent.getStringExtra(Shaves.CONTENT_ITEM_TYPE));
			Cursor cursor = getContentResolver().query(mShaveId, ShaveFragment.PROJECTION,
					null, null, null);
			if (cursor == null)
				return;
			ShaveEntry originalEntry = ShaveFragment.cursorToEntry(cursor);
			if (originalEntry != null) {
				try {
					Date date = mDateFormat.parse(originalEntry.getDate());
					mDateLabel.setText(mLocalFormat.format(date));
				} catch (ParseException e) {
					mDateLabel.setText(originalEntry.getDate());
				}
				int count = originalEntry.getCount();
				mCount.setMaxValue(count + COUNT_RANGE);
				mCount.setValue(count);
				updateUsageText(count);
				mUsageText.setText(Integer.toString(count));
				mComment.setText(originalEntry.getComment());
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.modify_done) {
			done();
			return true;
		}
		if (itemId == android.R.id.home) {
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.modify_shave_activity, menu);
		return true;
	}

	private void done() {
		String comment = mComment.getText().toString();
		ContentValues values = new ContentValues();
		values.put(Shaves.COUNT, mCount.getValue());
		values.put(Shaves.COMMENT, comment.replace('\n', ' '));
		getContentResolver().update(mShaveId, values, null, null);
		finish();
	}

	@Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		updateUsageText(newVal);
	}

	private void updateUsageText(int newVal) {
		mUsageText.setText(Integer.toString(newVal));
		if (mEnableColours)
			mUsageText.setBackgroundResource(Utils.getCountColour(newVal, mRanges));
		else
			mUsageText.setBackgroundResource(0);
	}

}
