package es.quirk.bladereminder.widgets;

import android.content.Context;
import android.graphics.Typeface;

public final class TextDrawableFactory {

	private static Typeface sRazorFont;
	private static Typeface sGoogleMaterial;
	public final static String RAZOR = "a";
	public final static String BLADE = "b";

	private TextDrawableFactory() { }

	public static TextDrawable createIcon(Context context, String txt) {
		if (sRazorFont == null) {
			sRazorFont = Typeface.createFromAsset(context.getResources().getAssets(),
				"blade-font.ttf");
		}
		if (sGoogleMaterial == null) {
			sGoogleMaterial = Typeface.createFromAsset(context.getResources().getAssets(),
				"google_material_design.ttf");
		}
		TextDrawable drawable = new TextDrawable(context);
		if (txt.length() == 1) {
			drawable.setTypeface(sRazorFont);
			drawable.setText(txt);
		} else {
			drawable.setTypeface(sGoogleMaterial);
			drawable.setText(Droidicon.getIconUtfChars(txt));
		}
		/*boolean isLightTheme = ((BaseActivity)mShaveFragment.getActivity()).mThemeSetting.equals("0");
		drawable.setTextColor(isLightTheme ? Utils.DARK_TEXT : Utils.LIGHT_TEXT);*/
		drawable.setTextSize(30);
		return drawable;
	}

}
