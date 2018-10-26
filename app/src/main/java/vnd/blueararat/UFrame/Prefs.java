package vnd.blueararat.UFrame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import java.io.File;

public class Prefs extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    static final String KEY_FOLDER = "save_location";
    static final String KEY_FONT = "font";
    static final int SELECT_FILE = 1;
    private SeekbarPref mSeekbarPrefJ;
    private ColorPref mBackgroundColorPref;
    private ListPreference mSaveFormat;
    private FontPref mPrefFont;

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
        mBackgroundColorPref = (ColorPref) findPreference("background_color");
        mBackgroundColorPref.setEnabled(b);
//        Preference media_scanner = findPreference("media_scanner");
//        media_scanner
//                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
//                    public boolean onPreferenceClick(Preference preference) {
//                        searchMedia();
//                        return true;
//                    }
//                });
        mPrefFont = (FontPref) findPreference(KEY_FONT);
        mPrefFont.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getBaseContext(), FileDialog.class);
                File f = new File(mPrefFont.getString());
                if (f != null && f.getParentFile() != null
                        && f.getParentFile().exists()) {
                    intent.putExtra(FileDialog.START_PATH, f.getParent());
                } else {
                    String sf = "/sdcard/Fonts";
                    f = new File(sf);
                    if (f.exists()) {
                        intent.putExtra(FileDialog.START_PATH, sf);
                    } else {
                        intent.putExtra(FileDialog.START_PATH, "/sdcard");
                    }
                }
                intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
                intent.putExtra(FileDialog.SELECTION_MODE,
                        SelectionMode.MODE_OPEN);
                intent.putExtra(FileDialog.FORMAT_FILTER, new String[]{"ttf",
                        "otf"});
                startActivityForResult(intent, SELECT_FILE);
                return true;
            }
        });
        if (mPrefFont.getString().length() == 0)
            mPrefFont.setSummary(R.string.font_select);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FILE) {
            if (resultCode == RESULT_OK) {
                String sFile = data.getStringExtra(FileDialog.RESULT_PATH);
                mPrefFont.setString(sFile);
                MainActivity.setTypeface(sFile);
            }
        }
    }

//    private void searchMedia() {
//        sendBroadcast(new Intent(
//                Intent.ACTION_MEDIA_MOUNTED,
//                Uri.parse("file://" + Environment.getExternalStorageDirectory())));
//        Toast.makeText(this, R.string.media_scanning, Toast.LENGTH_LONG).show();
//    }

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
            mBackgroundColorPref.setEnabled(b);
            mSaveFormat.setSummary(getString(R.string.pictures_will_be_saved)
                    + " " + mSaveFormat.getValue());
        }
    }
}
