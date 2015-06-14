package es.quirk.bladereminder;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.webkit.WebView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import timber.log.Timber;
import es.quirk.bladereminder.widgets.RangePreference;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private Activity mActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		Preference aboutButton = findPreference("about_button");
		aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				//code for what you want it to do
				showLicenses();
				return true;
			}
		});

		SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
		updateDateFormatSummary(sp);
		updateRangeSummary(sp);
		sp.registerOnSharedPreferenceChangeListener(this);
	}

	private void updateRangeSummary(SharedPreferences sp) {
		RangePreference rangePref = (RangePreference) findPreference("range");
		String currval = sp.getString("range", "1,9");
		try {
			String template  = getActivity().getResources().getString(R.string.sharpness_range_summary);
			String [] minmax = currval.split(",");
			rangePref.setSummary(String.format(template, minmax[0], minmax[1]));
		} catch (NullPointerException ex) {
			Timber.e(ex, "Error updating range summary!");
		}
	}

	private void updateDateFormatSummary(SharedPreferences sp) {
		EditTextPreference editTextPref = (EditTextPreference) findPreference("date_format");
		String currval = sp.getString("date_format", Utils.DATE_FORMAT);
		String template = getActivity().getResources().getString(R.string.with_template);
		editTextPref.setSummary(String.format(template, currval));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
	}

	private String readTextFile(InputStream inputStream) {
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charsets.UTF_8);
		try {
			return CharStreams.toString(inputStreamReader);
		} catch (IOException ex) {
			Timber.w(ex, "Exception reading in text file (raw license.html)");
		} finally {
			Utils.close(inputStreamReader);
		}
		return "";
	}

	private void showLicenses() {
		final String utf8 = Charsets.UTF_8.name();
		final boolean defaultTheme = getPreferenceScreen().getSharedPreferences().getString("default_theme", "0").equals("0");
		final String licenseText = readTextFile(getResources().openRawResource(R.raw.licenses));
		final String licenseTitle = getResources().getString(R.string.open_source_licences);
		String data = licenseText;
		try {
			// data passed to loadData needs to be URI-escaped.
			// WebView doesn't recode +, see https://stackoverflow.com/a/5034933
			data = URLEncoder.encode(licenseText.replaceAll("@THEME@", defaultTheme ? "light" : "dark"),
					utf8)
				.replaceAll("\\+", "%20");
			Timber.d(data);
		} catch (UnsupportedEncodingException ex) {
			Timber.w(ex, "Unable to encode license page");
		}
		final String dataToLoad = data;
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final WebView webView = new WebView(mActivity);
				webView.loadData(dataToLoad, "text/html", utf8);
				AppCompatDialog dialog = new AlertDialog.Builder(mActivity)
					.setTitle(licenseTitle)
					.setView(webView)
					.setPositiveButton(android.R.string.ok, null)
					.create();
				dialog.show();
			}
		});

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if ("range".equals(key)) {
			updateRangeSummary(prefs);
		}
		if ("date_format".equals(key)) {
			updateDateFormatSummary(prefs);
		}
	}
}
