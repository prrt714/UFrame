package vnd.blueararat.UFrame;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	static final String KEY_FOLDER = "save_location";
	static final int SELECT_FOLDER = 4;
	private SeekbarPref mSeekbarPrefJ;
	private ColorPref mColorPref;
	private ListPreference mSaveFormat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mSeekbarPrefJ = (SeekbarPref) getPreferenceScreen().findPreference(
				"jpeg_quality");
		mSaveFormat = (ListPreference) getPreferenceScreen().findPreference(
				"format");
		mSaveFormat.setSummary(getString(R.string.pictures_will_be_saved) + " "
				+ mSaveFormat.getValue());
		boolean b = mSaveFormat.getValue().equals("JPEG");
		mSeekbarPrefJ.setEnabled(b);
		mColorPref = (ColorPref) getPreferenceScreen().findPreference(
				"background_color");
		mColorPref.setEnabled(b);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		// if (arg1.equals("reset_settings")) {
		//
		// } else if (arg1.equals(KView.KEY_NUMBER_OF_MIRRORS)) {
		//
		// } else if (arg1.equals(MainActivity.KEY_IMAGE_URI)) {
		//
		// } else
		if (arg1.equals("format")) {
			boolean b = mSaveFormat.getValue().equals("JPEG");
			mSeekbarPrefJ.setEnabled(b);
			mColorPref.setEnabled(b);
			mSaveFormat.setSummary(getString(R.string.pictures_will_be_saved)
					+ " " + mSaveFormat.getValue());
		}
	}
}
