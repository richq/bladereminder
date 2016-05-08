package es.quirk.bladereminder.activities;

import android.support.annotation.NonNull;
import android.util.SparseArray;
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

import com.google.common.collect.Range;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import es.quirk.bladereminder.R;
import es.quirk.bladereminder.ShaveEntry;
import es.quirk.bladereminder.database.DataSource;
import es.quirk.bladereminder.fragments.ShaveListFragment;
import es.quirk.bladereminder.Utils;
import es.quirk.bladereminder.database.Contract.Shaves;
import java.util.List;

import timber.log.Timber;

public class ModifyShaveActivity extends BaseActivity implements OnValueChangeListener {
	private static final int COUNT_RANGE = 10;
	private Uri mShaveId;
	@BindView(R.id.usesDisplay) TextView mUsageText;
	@BindView(R.id.shaveUsageText) NumberPicker mCount;
	@BindView(R.id.shaveCommentField) EditText mComment;
	@BindView(R.id.dateLabel) TextView mDateLabel;
	@BindView(R.id.razorChoice) NumberPicker mRazorChoice;
	@BindView(R.id.razorSepThing) View mRazorSepThing;
	@BindView(R.id.razorLabel) TextView mRazorLabel;

	private final SparseArray<Range<Integer> > mRanges = new SparseArray<>(7);
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
		mComment.setHorizontallyScrolling(false);
		mComment.setMaxLines(8);
		if (edit) {
			mShaveId = Uri.parse(intent.getStringExtra(Shaves.CONTENT_ITEM_TYPE));

			Cursor cursor = ShaveListFragment.getCursorForId(getContentResolver(), mShaveId);
			if (cursor == null)
				return;
			ShaveEntry originalEntry = ShaveListFragment.cursorToEntry(cursor);
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
				DataSource dataSource = new DataSource(getApplicationContext());
				List<String> razors = dataSource.getRazors();
				int size = razors.size();
				if (size > 1) {
					Timber.d("Show razor choice display thingy");
					setRazorChoiceVisibility(View.VISIBLE);
					mRazorChoice.setMinValue(0);
					mRazorChoice.setMaxValue(size - 1);
					String [] ra = new String[size];
					mRazorChoice.setDisplayedValues(razors.toArray(ra));
					try {
						mRazorChoice.setValue(Integer.parseInt(originalEntry.getRazor()) - 1);
					} catch (NumberFormatException ex) {
						// can't deal..
						setRazorChoiceVisibility(View.GONE);
					}
				} else {
					setRazorChoiceVisibility(View.GONE);
				}
			}
		}
	}

	private void setRazorChoiceVisibility(int viz) {
		mRazorChoice.setVisibility(viz);
		mRazorLabel.setVisibility(viz);
		mRazorSepThing.setVisibility(viz);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
		if (mRazorChoice.getVisibility() == View.VISIBLE) {
			DataSource dataSource = new DataSource(getApplicationContext());
			int choice = mRazorChoice.getValue();
			int id = dataSource.getRazorId(choice);
			Timber.d("Razor choice = %d, razor ID = %d", choice, id);
			values.put(Shaves.RAZOR, Integer.toString(id));
		}
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
