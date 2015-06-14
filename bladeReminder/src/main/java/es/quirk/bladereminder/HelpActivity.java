package es.quirk.bladereminder;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.Button;

import butterknife.InjectView;
import butterknife.OnClick;

public class HelpActivity extends BaseActivity {

	@InjectView(R.id.end_help) Button mEndHelp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@OnClick(R.id.end_help)
	protected void endHelp() {
		finish();
	}

}
