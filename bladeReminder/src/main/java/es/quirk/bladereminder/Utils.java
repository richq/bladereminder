package es.quirk.bladereminder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.DrawableRes;
import android.util.SparseArray;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import com.google.common.collect.Range;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

public final class Utils {
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public final static long ONE_DAY_MS = 24 * 3600 * 1000;
	// 54% of 255 = 138
	public final static int DARK_TEXT = Color.argb(138, 0, 0, 0);
	public final static int LIGHT_TEXT = Color.WHITE;
	public final static List<String> COLOUR_PREFS = Arrays.asList(
		"purple_sharp",
		"white_sharp",
		"blue_sharp",
		"green_sharp",
		"yellow_sharp",
		"orange_sharp"
	);

	// utility class, so no instances.
	private Utils() {}

	public static boolean isColourPref(@NonNull String s) {
		return s.endsWith("_sharp");
	}

	@NonNull
	public static DateFormat createDateFormatYYYYMMDD() {
		return new SimpleDateFormat(DATE_FORMAT, Locale.US);
	}

	public static String todaysDate() {
            DateFormat format = createDateFormatYYYYMMDD();
            Date date = Calendar.getInstance().getTime();
            return format.format(date);
	}

	/**
	 * Make a directory, if it does not exist already.
	 * @param dirname the directory to create.
	 */
	public static void mkdirp(@NonNull final File dirname) throws IOException {
		if (!dirname.exists() && !dirname.mkdirs()) {
			throw new IOException("Failed to create directory " + dirname.getPath());
		}
	}

	// open closed that won't throw
	@NonNull
	private static Range<Integer> openClosed(int min, int max) {
		try {
			return Range.openClosed(min, max);
		} catch (IllegalArgumentException ex) {
			return Range.atMost(-1);
		}
	}

	public static boolean setRangesFromPrefs(@NonNull SharedPreferences prefs, @NonNull SparseArray<Range<Integer> > ranges) {
		int purple = prefs.getInt("purple_sharp", 1);
		int white = prefs.getInt("white_sharp", 2);
		int blue = prefs.getInt("blue_sharp", 3);
		int green = prefs.getInt("green_sharp", 4);
		int yellow = prefs.getInt("yellow_sharp", 5);
		int orange = prefs.getInt("orange_sharp", 6);

		String themeSetting = prefs.getString("default_theme", "0");
		if ("0".equals(themeSetting)) {
			ranges.put(R.drawable.round_purple, Range.atMost(purple));
			ranges.put(R.drawable.round_white, openClosed(purple, white));
			ranges.put(R.drawable.round_blue, openClosed(white, blue));
			ranges.put(R.drawable.round_green, openClosed(blue, green));
			ranges.put(R.drawable.round_yellow, openClosed(green, yellow));
			ranges.put(R.drawable.round_orange, openClosed(yellow, orange));
			ranges.put(R.drawable.round_red, Range.greaterThan(orange));
		} else {
			ranges.put(R.drawable.round_purple_dark, Range.atMost(purple));
			ranges.put(R.drawable.round_white_dark, openClosed(purple, white));
			ranges.put(R.drawable.round_blue_dark, openClosed(white, blue));
			ranges.put(R.drawable.round_green_dark, openClosed(blue, green));
			ranges.put(R.drawable.round_yellow_dark, openClosed(green, yellow));
			ranges.put(R.drawable.round_orange_dark, openClosed(yellow, orange));
			ranges.put(R.drawable.round_red_dark, Range.greaterThan(orange));
		}

		return prefs.getBoolean("colours_enabled", true);
	}

	public static int getCountColour(int count, @NonNull SparseArray<Range<Integer>> ranges) {
		int resource = 0;
		int size = ranges.size();
		for (int i = 0; i < size; i++) {
			int key = ranges.keyAt(i);
			if (ranges.get(key).contains(count)) {
				resource = key;
				break;
			}
		}
		return resource;
	}

	public static int getTextColourForResource(@DrawableRes int in) {
		// see scripts/contrast.py for how to generate the text for a given background
		// e.g. ./scripts/contrast.py EF5350
		switch (in) {
			case 0: return LIGHT_TEXT;

			case R.drawable.round_purple: return DARK_TEXT;
			case R.drawable.round_white: return DARK_TEXT;
			case R.drawable.round_blue: return DARK_TEXT;
			case R.drawable.round_green: return DARK_TEXT;
			case R.drawable.round_yellow: return DARK_TEXT;
			case R.drawable.round_orange: return DARK_TEXT;
			case R.drawable.round_red: return DARK_TEXT;

			case R.drawable.round_purple_dark: return LIGHT_TEXT;
			case R.drawable.round_white_dark: return DARK_TEXT;
			case R.drawable.round_blue_dark: return LIGHT_TEXT;
			case R.drawable.round_green_dark: return LIGHT_TEXT;
			case R.drawable.round_yellow_dark: return DARK_TEXT;
			case R.drawable.round_orange_dark: return DARK_TEXT;
			case R.drawable.round_red_dark: return LIGHT_TEXT;
			default: break;
		}
		return DARK_TEXT;
	}

        /**
         * Check if running on the Android Runtime for Chrome.
         * MODEL is "App Runtime for Chrome", and PRODUCT is "arc".
         */
	public static boolean isChrome() {
		return "arc".equalsIgnoreCase(Build.PRODUCT) || Build.MODEL.contains("Chrome");
	}

	public static void close(@Nullable Closeable c) {
		try {
			if (c != null)
				c.close();
		} catch (IOException e) {
			Timber.w(e, "Exception closing a closeable, not much to do");
		}
	}

	public static String niceFormat(float value) {
		String res = String.format(Locale.US, "%.1f", value);
		// remove spurious .0 at the end of floats
		if (res.endsWith(".0"))
			return res.replace(".0", "");

		String prec = String.format(Locale.US, "%.2f", value);
		// replace some funky decimals with vulgar fractions
		if (prec.endsWith(".25"))
			return prec.replace(".25", "¼");
		if (prec.endsWith(".33"))
			return prec.replace(".33", "⅓");
		if (res.endsWith(".5"))
			return res.replace(".5", "½");
		if (prec.endsWith(".67"))
			return prec.replace(".67", "⅔");
		if (prec.endsWith(".75"))
			return prec.replace(".75", "¾");

		return res;
	}
}
