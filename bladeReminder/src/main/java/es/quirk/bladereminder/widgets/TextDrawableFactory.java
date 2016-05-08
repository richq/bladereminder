package es.quirk.bladereminder.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;

public final class TextDrawableFactory {

	private static Typeface sRazorFont;
	private static Typeface sGoogleMaterial;
	public final static String RAZOR = "a";
	public final static String BLADE = "b";

	private TextDrawableFactory() { }

	private static String getIconUtfChars(@NonNull String icon) {
		int res = 0;
		switch (icon) {
			case "gmd-add":
				res = 0xe145;
				break;
			case "gmd-delete":
				res = 0xe872;
				break;
			case "gmd-help":
				res = 0xe887;
				break;
			case "gmd-file-upload":
				res = 0xe2c6;
				break;
			case "gmd-edit":
				res = 0xe3c9;
				break;
			case "gmd-settings":
				res = 0xe8b8;
				break;
			default:
				break;
		}
		return String.valueOf(Character.toChars(res));
	}

	@NonNull
	public static TextDrawable createIcon(@NonNull Context context, @NonNull String txt) {
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
			drawable.setText(getIconUtfChars(txt));
		}
		drawable.setTextSize(30);
		return drawable;
	}

}
