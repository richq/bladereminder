package es.quirk.bladereminder;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import com.google.common.collect.Range;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import timber.log.Timber;

public final class Utils {
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public final static long ONE_DAY_MS = 24 * 3600 * 1000;
	// 54% of 255 = 138
	public final static int DARK_TEXT = Color.argb(138, 0, 0, 0);
	public final static int LIGHT_TEXT = Color.WHITE;
	public final static HashSet<String> COLOUR_PREFS = new HashSet<>();

	static {
		COLOUR_PREFS.add("purple_sharp");
		COLOUR_PREFS.add("white_sharp");
		COLOUR_PREFS.add("blue_sharp");
		COLOUR_PREFS.add("green_sharp");
		COLOUR_PREFS.add("yellow_sharp");
		COLOUR_PREFS.add("orange_sharp");
	}

	// utility class, so no instances.
	private Utils() {}

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
	public static void mkdirp(final File dirname) throws IOException {
		if (!dirname.exists()) {
			if (!dirname.mkdirs())
				throw new IOException("Failed to create directory " + dirname.getPath());
		}
	}

	// open closed that won't throw
	private static Range<Integer> openClosed(int min, int max) {
		try {
			return Range.openClosed(min, max);
		} catch (IllegalArgumentException ex) {
			return Range.atMost(-1);
		}
	}

	public static boolean setRangesFromPrefs(SharedPreferences prefs, HashMap<Integer, Range<Integer> > ranges) {
		int purple = prefs.getInt("purple_sharp", 1);
		int white = prefs.getInt("white_sharp", 2);
		int blue = prefs.getInt("blue_sharp", 3);
		int green = prefs.getInt("green_sharp", 4);
		int yellow = prefs.getInt("yellow_sharp", 5);
		int orange = prefs.getInt("orange_sharp", 6);

		String themeSetting = prefs.getString("default_theme", "0");
		if (themeSetting.equals("0")) {
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

	public static int getCountColour(int count, HashMap<Integer, Range<Integer>> ranges) {
		int resource = 0;
		for (Entry<Integer, Range<Integer>> rangeEntry : ranges.entrySet()) {
			if (rangeEntry.getValue().contains(count)) {
				resource = rangeEntry.getKey();
				break;
			}
		}
		return resource;
	}

	public static int getTextColourForResource(int in) {
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

	public static void close(Closeable c) {
		try {
			if (c != null)
				c.close();
		} catch (IOException e) {
			Timber.w(e, "Exception closing a closeable, not much to do");
		}
	}
}
