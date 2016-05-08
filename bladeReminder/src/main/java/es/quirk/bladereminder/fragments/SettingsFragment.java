package es.quirk.bladereminder.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
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

import es.quirk.bladereminder.R;
import es.quirk.bladereminder.Utils;
import timber.log.Timber;
import es.quirk.bladereminder.widgets.RangePreference;

public class SettingsFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {

	private Context mContext;
	private static final String ARG_PREFERENCE_DIALOG = "android.support.v7.preference.PreferenceFragmentCompat.DIALOG";

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String key) {
		addPreferencesFromResource(R.xml.preferences);
		findPreference("about_button").setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference unused) {
						showWebView(R.raw.licenses, R.string.open_source_licences);
						return true;
					}
				});
		findPreference("changelog_button").setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference unused) {
						showWebView(R.raw.changes, R.string.changelog);
						return true;
					}
				});

		SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
		updateDateFormatSummary(sp);
		updateRangeSummary(sp);
		sp.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference instanceof RangePreference) {
			DialogFragment fragment;
			fragment = RangePreference.RangePreferenceDialogFragmentCompat.newInstance((RangePreference)preference);
			fragment.setTargetFragment(this, 0);
			fragment.show(getFragmentManager(), ARG_PREFERENCE_DIALOG);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	private void updateRangeSummary(@NonNull SharedPreferences sp) {
		RangePreference rangePref = (RangePreference) findPreference("range");
		String currval = sp.getString("range", "1,9");
		try {
			String template  = getString(R.string.sharpness_range_summary);
			String [] minmax = currval.split(",");
			rangePref.setSummary(String.format(template, minmax[0], minmax[1]));
		} catch (IllegalStateException ex) {
			Timber.e(ex, "Illegal state updating range!");
		} catch (NullPointerException ex) {
			Timber.e(ex, "Error updating range summary!");
		}
	}

	private void updateDateFormatSummary(@NonNull SharedPreferences sp) {
		EditTextPreference editTextPref = (EditTextPreference) findPreference("date_format");
		String currval = sp.getString("date_format", Utils.DATE_FORMAT);
		String template = getString(R.string.with_template);
		editTextPref.setSummary(String.format(template, currval));
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		mContext = activity;
	}

	private String readTextFile(@NonNull InputStream inputStream) {
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charsets.UTF_8);
		try {
			return CharStreams.toString(inputStreamReader);
		} catch (IOException ex) {
			Timber.w(ex, "Exception reading in text file");
		} finally {
			Utils.close(inputStreamReader);
		}
		return "";
	}

	private void showWebView(@RawRes int textFile, @StringRes int titleString) {
		final String utf8 = Charsets.UTF_8.name();
		final boolean defaultTheme = getPreferenceScreen().getSharedPreferences().getString("default_theme", "0").equals("0");
		final String viewText = readTextFile(getResources().openRawResource(textFile));
		final String viewTitle = getString(titleString);
		String data = viewText;
		try {
			// data passed to loadData needs to be URI-escaped.
			// WebView doesn't recode +, see https://stackoverflow.com/a/5034933
			data = URLEncoder.encode(viewText.replaceAll("@THEME@", defaultTheme ? "light" : "dark"),
					utf8)
				.replaceAll("\\+", "%20");
			Timber.d(data);
		} catch (UnsupportedEncodingException ex) {
			Timber.w(ex, "Unable to encode text page");
			return;
		}
		final String dataToLoad = data;
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final WebView webView = new WebView(mContext);
				webView.loadData(dataToLoad, "text/html", utf8);
				AppCompatDialog dialog = new AlertDialog.Builder(mContext)
					.setTitle(viewTitle)
					.setView(webView)
					.setPositiveButton(android.R.string.ok, null)
					.create();
				dialog.show();
			}
		});

	}

	@Override
	public void onSharedPreferenceChanged(@NonNull SharedPreferences prefs, String key) {
		if ("range".equals(key)) {
			updateRangeSummary(prefs);
		}
		if ("date_format".equals(key)) {
			updateDateFormatSummary(prefs);
		}
	}
}
