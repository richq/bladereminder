package es.quirk.bladereminder.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Button;

import butterknife.BindView;
import butterknife.OnClick;
import es.quirk.bladereminder.R;

public class HelpActivity extends BaseActivity {

	@BindView(R.id.end_help) Button mEndHelp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@OnClick(R.id.end_help)
	protected void endHelp() {
		finish();
	}

}
