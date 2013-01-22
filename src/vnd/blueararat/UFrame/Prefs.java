package vnd.blueararat.UFrame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

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
		mSeekbarPrefJ = (SeekbarPref) findPreference("jpeg_quality");
		mSaveFormat = (ListPreference) findPreference("format");
		mSaveFormat.setSummary(getString(R.string.pictures_will_be_saved) + " "
				+ mSaveFormat.getValue());
		boolean b = mSaveFormat.getValue().equals("JPEG");
		mSeekbarPrefJ.setEnabled(b);
		mColorPref = (ColorPref) findPreference("background_color");
		mColorPref.setEnabled(b);
		Preference media_scanner = findPreference("media_scanner");
		media_scanner
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						searchMedia();
						return true;
					}
				});
	}

	private void searchMedia() {
		sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + Environment.getExternalStorageDirectory())));
		Toast.makeText(this, R.string.media_scanning, Toast.LENGTH_LONG).show();
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
