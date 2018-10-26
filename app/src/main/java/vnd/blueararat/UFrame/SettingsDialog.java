package vnd.blueararat.UFrame;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class SettingsDialog extends Dialog {

    public interface OnSettingsChangedListener {
        void settingsChanged(int color, int mode1, int mode2, float strokewidth);
    }

    private OnSettingsChangedListener mListener;
    private int mInitialColor;
    private Context mContext;
    private int mColor;
    // private static boolean isBlur, isEmpty, isEmboss;
    static final int NORMAL = 0;
    static final int BLUR = 1;
    static final int EMBOSS = 2;
    static final int COLOR = 0;
    static final int RAINBOW = 1;
    static final int NONE = 2;
    static int sMode1 = NORMAL;
    static int sMode2 = COLOR;
    static float sStrokeWidth = 4;

    public SettingsDialog(Context context, OnSettingsChangedListener listener,
                          int initialColor) {
        super(context);

        mListener = listener;
        mInitialColor = initialColor;
        mContext = context;
    }

    @Override
    protected void onStart() {
        final RadioGroup rd1 = (RadioGroup) findViewById(R.id.radiogroup1);
        rd1.check(rd1.getChildAt(sMode1).getId());
        final RadioGroup rd2 = (RadioGroup) findViewById(R.id.radiogroup2);
        rd2.check(rd2.getChildAt(sMode2).getId());
        final EditText et = (EditText) findViewById(R.id.strokewidth);
        et.setText(String.format("%.1f", sStrokeWidth));

        OnCheckedChangeListener cl = new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (group == rd1) {
                    sMode1 = group.indexOfChild(findViewById(checkedId));
                } else if (group == rd2) {
                    sMode2 = group.indexOfChild(findViewById(checkedId));
                }
                mListener.settingsChanged(-1, sMode1, sMode2, -1);
            }

        };
        rd1.setOnCheckedChangeListener(cl);
        rd2.setOnCheckedChangeListener(cl);

        final SeekBar sb = (SeekBar) findViewById(R.id.strokewidth_seekBar);
        sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    sStrokeWidth = (float) progress;
                    et.setText(String.format("%.1f", sStrokeWidth));
                    mListener.settingsChanged(-1, -1, -1, sStrokeWidth);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });
        if (sStrokeWidth > 20) {
            sb.setMax((int) sStrokeWidth);
        }
        sb.setProgress((int) sStrokeWidth);

        et.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {

                try {
                    sStrokeWidth = Float.parseFloat(et.getText().toString()
                            .replace(",", "."));
                } catch (NumberFormatException e) {
                    et.setText(String.format("%.1f", sStrokeWidth));
                }
                if (sStrokeWidth > 20) {
                    sb.setMax((int) sStrokeWidth);
                }
                sb.setProgress((int) sStrokeWidth);
                mListener.settingsChanged(-1, -1, -1, sStrokeWidth);
                return false;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnSettingsChangedListener l = new OnSettingsChangedListener() {
            @Override
            public void settingsChanged(int color, int mode1, int mode2,
                                        float strokewidth) {
                mColor = color;
                if (mode1 == -1) {
                    RadioGroup rd = (RadioGroup) findViewById(R.id.radiogroup1);
                    sMode1 = rd.indexOfChild(findViewById(rd
                            .getCheckedRadioButtonId()));
                } else {
                    sMode1 = mode1;
                }
                if (mode2 == -1) {
                    RadioGroup rd = (RadioGroup) findViewById(R.id.radiogroup2);
                    sMode2 = rd.indexOfChild(findViewById(rd
                            .getCheckedRadioButtonId()));
                } else {
                    sMode2 = mode2;
                }
                EditText et = (EditText) findViewById(R.id.strokewidth);
                try {
                    sStrokeWidth = Float.parseFloat(et.getText().toString()
                            .replace(",", "."));
                } catch (NumberFormatException e) {
                    // TODO
                }
                mListener.settingsChanged(mColor, sMode1, sMode2, sStrokeWidth);
            }
        };
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        ColorPickerView.setInitialColor(mInitialColor);

        try {
            setContentView(R.layout.settings_dialog);
        } catch (InflateException e) {
            Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();
        }
        ColorPickerView cpv = (ColorPickerView) findViewById(R.id.ColorPickerView);
        cpv.setSettingsChangedListener(l);
    }
}
