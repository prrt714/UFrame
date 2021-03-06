package vnd.blueararat.UFrame;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import vnd.blueararat.UFrame.SettingsDialog.OnSettingsChangedListener;

public class MainActivity extends AppCompatActivity implements OnSettingsChangedListener {

    private Paint mPaint, mPaint5, mBitmapPaint, mTextPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private UFrameView mv;
    private MainActivity ma;
    private static int mColor = 0xFFAAAAFA;
    private int mBckgrWidth, mBckgrHeight;
    private int mBitmapWidth, mBitmapHeight;
    private static float sStrokeWidth = 5;
    private boolean isRainbow;
    private boolean adjustRainbow;
    private boolean isBlur;
    private boolean isNone;
    private float mCenterRainbowX;
    private float mCenterRainbowY;
    public static final int HATCH_COLOR_BG = 0xFF222222;
    public static final int HATCH_COLOR_LINES = 0xFF888888;
    private static String sInputPath = "";
    private static String sOutputPath = "";
    static final int SELECT_FOLDER = 1;
    private int sMode1;
    private int sMode2;
    private final static int PADX = 20;
    private final static int PADX2 = 2 * PADX;
    private static boolean isJPG;
    private static int mBackgroundColor;
    private static int mFontColor;
    private SharedPreferences preferences;
    int[] mColors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF,
            0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
    private static TextView sInfo;
    private FilenameFilter mFilenameFilter;
    private static volatile boolean mustStop = false;
    private static boolean mustFit = true;
    private static volatile boolean isRunning = false;
    private float mDx, mDy;
    private float mStartX, mStartY, mSmooth;
    private static boolean mustRetainExif = false;
    private static boolean isCircle = false;
    private int mNumWaves;
    private static long lastTime = System.currentTimeMillis();
    private static boolean shouldDrawText = false;
    private float[] dt = new float[]{0, 0};
    private boolean adjustTextPosition = false;
    private float pathLength = 0;
    private float currentLength = 0;
    private static String sText = "";
    private static boolean fillWithText = false;
    private static Typeface font;

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    static void setTypeface(String path) {
        if (path == null || path.length() == 0) {
            font = null;
        } else {
            try {
                font = Typeface.createFromFile(new File(path));
            } catch (Exception e) {
                font = null;
            }
        }
    }

    private static float adjust(boolean none, boolean blur, float strokewidth) {
        float adjustStrokeWidth = strokewidth / 2;
        if (none && !blur) {
            adjustStrokeWidth = -adjustStrokeWidth;
        } else if (none && blur) {
            adjustStrokeWidth = 0;
        } else if (blur) {
            adjustStrokeWidth *= 2;
        }
        return adjustStrokeWidth;
    }

    private static float[] fitPath(float width, float height, float mDx,
                                   float mDy, float mSmooth, float startX, float startY,
                                   float strokewidth, boolean none, boolean blur) {
        float sX, sY;
        float dx2, dy2;
        float adjustStrokeWidth = adjust(none, blur, strokewidth);
        int numberOfWavesX, numberOfWavesY;
        Path p = new Path();
        RectF bounds = new RectF();
        // int k = 0;
        do {
            sX = startX;
            sY = startY;
            numberOfWavesX = (int) ((width - sX * 2) / mDy);
            numberOfWavesY = (int) ((height - sY * 2) / mDy);
            if (numberOfWavesX % 2 == 0) {
                numberOfWavesX--;
            }
            if (numberOfWavesY % 2 == 0) {
                numberOfWavesY--;
            }
            dy2 = ((float) height - sY * 2) / numberOfWavesY;
            dx2 = ((float) width - sX * 2) / numberOfWavesX;

            p.moveTo(0, 0);
            float j = mDx;
            p.cubicTo(j, dy2 / 2 - mSmooth, j, dy2 / 2 + mSmooth, 0, dy2);
            p.computeBounds(bounds, false);
            p.reset();
            startX = 3 * (bounds.right - bounds.left) / 4 + adjustStrokeWidth;

            p.moveTo(0, 0);
            p.cubicTo(dx2 / 2 - mSmooth, -j, dx2 / 2 + mSmooth, -j, dx2, 0);
            bounds = new RectF();
            p.computeBounds(bounds, true);
            p.reset();
            startY = 3 * (bounds.bottom - bounds.top) / 4 + adjustStrokeWidth;
            // k++;

        } while (Math.abs(sX - startX) > 0.01 || Math.abs(sY - startY) > 0.01);

        return new float[]{sX, sY, dx2, dy2, numberOfWavesX, numberOfWavesY};
    }

    private class Export extends AsyncTask<Integer, String, String> {
        private final String done = "<font color='#a1e3ef'>"
                + getString(R.string.saved) + "</font><br />";
        private final String skip = "<font color='#a8a1ef'>"
                + getString(R.string.exists) + "</font><br />";
        private final String error = "<font color='#a6efa1'>"
                + getString(R.string.error) + "</font><br />";
        private final String success = "<font color='#efa1eb'>"
                + getString(R.string.success) + "</font><br />";
        private final String nopic = "<font color='#efa1a3'>"
                + getString(R.string.no_pictures) + "</font><br />";
        private final String stop = "<font color='#e0efa1'>"
                + getString(R.string.stopped) + "</font><br />";
        private int total = 0;
        // private Bitmap.Config mG;
        private Bitmap.CompressFormat mCf;
        private String mExt;
        private int mQ;
        private float strwidth;
        private Paint paint = new Paint(mPaint);
        private volatile float scaleX, scaleY;
        private final float lStrokeWidth = sStrokeWidth;
        private final float lDx = mDx;
        private final float lDy = mDy;
        private final float lCenterRainbowX = mCenterRainbowX;
        private final float lCenterRainbowY = mCenterRainbowY;
        private final File directory = new File(sOutputPath);
        // private final String lOutputPath = sOutputPath;
        private final float lStartX = mStartX;
        private final float lStartY = mStartY;
        private final float lBitmapWidth = mBitmapWidth;
        private final float lBitmapHeight = mBitmapHeight;
        private final float lSmooth = mSmooth;
        private final boolean lBlur = isBlur;
        private final boolean lEmboss = (sMode1 == SettingsDialog.EMBOSS);
        private final boolean lNone = isNone;
        private final boolean lRainbow = isRainbow;
        private final boolean lFit = mustFit;
        private final boolean isPNG = !isJPG;
        private final int lBackgroundColor = mBackgroundColor;
        private final int lFontColor = mFontColor;
        private final boolean lExif = mustRetainExif;
        private ExifInterface exif;
        private final boolean lCircle = isCircle;
        private final int lNumWaves = mNumWaves;
        double dangle = 2 * Math.PI / lNumWaves;

        private final Paint lTextPaint = new Paint(mTextPaint);
        private final String lText = sText;
        private final boolean lShouldDrawText = shouldDrawText;
        private final float lPathLength = pathLength;
        private float pLength, cLength;
        private final float lCurrentLength = currentLength;
        private final boolean lFillWithText = fillWithText;
        private float ladj1;
        private int ltw;

        private void lUpdateTextBounds() {
            Rect bounds = new Rect();
            lTextPaint.getTextBounds(sText + "...", 0, sText.length() + 3,
                    bounds);
            ltw = bounds.right;
            ladj1 = -(bounds.bottom + bounds.top) / 2;
        }

        @Override
        protected String doInBackground(Integer... params) {
            return export();
        }

        private Path drawFrameExport(Bitmap bitmap) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            scaleX = (float) width / lBitmapWidth;
            scaleY = (float) height / lBitmapHeight;

            float startX, startY, dx, dy, smooth;

            // if (lCircle) {
            float scale = Math.min(width, height)
                    / Math.min(lBitmapWidth, lBitmapHeight);
            startX = lStartX * scaleX;
            startY = lStartY * scaleY;
            dx = lDx * scale;
            dy = lDy * scale;
            smooth = lSmooth * scale;
            strwidth = lStrokeWidth * scale;
            // } else {
            // startX = lStartX * scaleY;
            // startY = lStartY * scaleY;
            // dx = lDx * scaleY;
            // dy = lDy * scaleY;
            // smooth = lSmooth * scaleY;
            // strwidth = lStrokeWidth * scaleY;
            // }

            int numberOfWavesX, numberOfWavesY;
            float dx2, dy2;

            Path path = new Path();

            float j = dx;
            if (lCircle) {
                float adj = j + adjust(lNone, lBlur, strwidth);
                float cX = width / 2;
                float cY = height / 2;
                float radius = Math.min(cX, cY) - adj;
                float x = Math.max(j, j + cX - cY) + adj;

                if (!lFit) {
                    cX += startX;
                    cY += startY;
                    x += startX;
                }

                float y = cY;

                double ang = 0;
                path.moveTo(x, y);
                float cos = (float) Math.cos(ang);
                float sin = (float) Math.sin(ang);
                float x1, x2, y1, y2;
                for (int n = 0; n < lNumWaves; n++) {
                    x1 = x + smooth * sin;
                    y1 = y + smooth * cos;
                    ang += dangle;
                    cos = (float) Math.cos(ang);
                    sin = (float) Math.sin(ang);
                    x = cX - (radius + j) * cos;
                    y = cY + (radius + j) * sin;
                    x2 = x - smooth * sin;
                    y2 = y - smooth * cos;
                    path.cubicTo(x1, y1, x2, y2, x, y);
                    j = -j;
                }
                path.close();
            } else {
                if (lFit) {
                    float[] f = fitPath(width, height, dx, dy, smooth, startX,
                            startY, strwidth, lNone, lBlur);
                    startX = f[0];
                    startY = f[1];
                    dx2 = f[2];
                    dy2 = f[3];
                    numberOfWavesX = (int) f[4];
                    numberOfWavesY = (int) f[5];
                } else {
                    numberOfWavesX = (int) ((width - startX * 2) / dy);
                    numberOfWavesY = (int) ((height - startY * 2) / dy);
                    if (numberOfWavesX % 2 == 0) {
                        numberOfWavesX--;
                    }
                    if (numberOfWavesY % 2 == 0) {
                        numberOfWavesY--;
                    }
                    dx2 = ((float) width - startX * 2) / numberOfWavesX;
                    dy2 = ((float) height - startY * 2) / numberOfWavesY;
                }

                float x = startX;
                float y = startY;
                path.moveTo(startX, startY);
                for (int n = 1; n <= numberOfWavesY; n++) {
                    y += dy2;
                    path.cubicTo(x - j, y - dy2 / 2 - smooth, x - j, y - dy2
                            / 2 + smooth, x, y);
                    j = -j;
                }
                for (int n = 1; n <= numberOfWavesX; n++) {
                    x += dx2;
                    path.cubicTo(x - dx2 / 2 - smooth, y - j, x - dx2 / 2
                            + smooth, y - j, x, y);
                    j = -j;
                }
                j = -j;
                for (int n = 1; n <= numberOfWavesY; n++) {
                    y -= dy2;
                    path.cubicTo(x - j, y + dy2 / 2 + smooth, x - j, y + dy2
                            / 2 - smooth, x, y);
                    j = -j;
                }
                for (int n = 1; n <= numberOfWavesX; n++) {
                    x -= dx2;
                    path.cubicTo(x + dx2 / 2 + smooth, y - j, x + dx2 / 2
                            - smooth, y - j, x, y);
                    j = -j;
                }
                path.close();
            }
            return path;
        }

        private Bitmap drawIntoBitmap(String filePath) {
            Bitmap.Config g;
            if (isPNG) { // && bitmap.hasAlpha()
                g = Bitmap.Config.ARGB_8888;
            } else {
                g = Bitmap.Config.RGB_565;
            }
            boolean b = true;
            Options opts = new Options();
            Bitmap bitmap = null;
            Bitmap bitmap2 = null;
            while (b) {
                try {
                    bitmap = BitmapFactory.decodeFile(filePath, opts);
                    bitmap2 = Bitmap.createBitmap(bitmap.getWidth(),
                            bitmap.getHeight(), g);
                    b = false;
                } catch (OutOfMemoryError e) {
                    opts.inSampleSize++;
                    bitmap = null;
                    bitmap2 = null;
                    System.gc();
                }
            }
            Canvas canvas = new Canvas(bitmap2);
            if (!isPNG)
                canvas.drawColor(lBackgroundColor);

            Path path = drawFrameExport(bitmap);
            canvas.clipPath(path);
            canvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);
            bitmap.recycle();
            System.gc();
            System.gc();

            paint.setStrokeWidth(strwidth);
            if (lRainbow) {
                float x = 2 * (lCenterRainbowX - lBitmapWidth / 4.f) * scaleX;
                float y = 2 * (lCenterRainbowY - lBitmapHeight / 4.f) * scaleY;
                Shader s = new SweepGradient(x, y, mColors, null);
                paint.setShader(s);
            }
            if (lBlur) {
                MaskFilter blur = new BlurMaskFilter(strwidth,
                        BlurMaskFilter.Blur.NORMAL);
                paint.setMaskFilter(blur);
            } else if (lEmboss) {
                MaskFilter emboss = new EmbossMaskFilter(
                        new float[]{1, 1, 1}, 0.4f, 6, 3.5f * scaleY);
                paint.setMaskFilter(emboss);
            }

            if (lStartX == 0 && lStartY == 0 && !lCircle) {
                float p = strwidth / 2;
                canvas.drawRect(p, p, bitmap.getWidth() - p, bitmap.getHeight()
                        - p, paint);
            }

            canvas = new Canvas(bitmap2);
            canvas.drawPath(path, paint);

            if (lShouldDrawText) {
                PathMeasure pm = new PathMeasure(path, true);
                pLength = pm.getLength();
                cLength = pLength * lCurrentLength / lPathLength;
                lTextPaint.setTextSize(strwidth);
                lUpdateTextBounds();

                if (lFillWithText) {
                    int count = (int) pLength / ltw;
                    for (int i = 0; i < count; i++) {
                        canvas.drawTextOnPath(sText, path, cLength + i * ltw,
                                ladj1, lTextPaint);
                    }
                } else {
                    canvas.drawTextOnPath(sText, path, cLength, ladj1,
                            lTextPaint);
                }
            }

            if (isCircle && lFit) {
                int l = 0, u = 0, dr;
                int w = bitmap2.getWidth();
                int h = bitmap2.getHeight();
                if (w > h) {
                    l = (w - h) / 2;
                    dr = h;
                } else if (w < h) {
                    u = (h - w) / 2;
                    dr = w;
                } else {
                    return bitmap2;
                }
                bitmap2 = Bitmap.createBitmap(bitmap2, l, u, dr, dr);
            }

            return bitmap2;
        }

        private String export() {
            File f = new File(sInputPath);
            File[] files = f.listFiles(mFilenameFilter);
            if (files == null || files.length == 0) {
                publishProgress(nopic);
                return getString(R.string.no_pictures);
            }
            int i = 0;
            int j = files.length;
            for (File file : files) {
                if (mustStop) {
                    publishProgress("<font color='#efc9a1'>" + total
                            + getString(R.string.pictures_were_processed)
                            + "</font><br />");
                    publishProgress(stop);
                    return getString(R.string.stopped);
                }
                if (file.isFile()) {
                    String filename = file.getName();
                    String fn = filename.toLowerCase();
                    if (!isPNG && lExif && (!fn.endsWith("png"))) {
                        try {
                            exif = new ExifInterface(file.getAbsolutePath());
                        } catch (IOException e) {
                            // exif = null;
                        }
                    }

                    System.gc();
                    String str1 = exportImage(file, filename);
                    i++;
                    publishProgress(i + getString(R.string.of) + j + ": "
                            + filename, str1);
                }
            }
            // sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
            // Uri.parse("file://"
            // + Environment.getExternalStorageDirectory())));
            publishProgress("<font color='#efc9a1'>" + total
                    + getString(R.string.pictures_were_processed)
                    + "</font><br />");
            publishProgress(success);
            return getString(R.string.success);
        }

        String exportImage(File file1, String filename) {
            // String path = preferences.getString(Prefs.KEY_FOLDER, "");
            // if (path.equals("")) {
            // path = Environment.getExternalStoragePublicDirectory(
            // Environment.DIRECTORY_PICTURES).toString();
            // }

            String fn = filename.toLowerCase();
            if (!fn.endsWith(mExt)) {
                int last_dot = filename.lastIndexOf(".");
                filename = filename.substring(0, last_dot) + mExt;
            }

            File file = new File(directory, filename);
            if (file.exists()) {
                // try {
                // Thread.sleep(15);
                // } catch (InterruptedException e) {
                // // e.printStackTrace();
                // }
                return skip;
            }

            Bitmap bitmap = drawIntoBitmap(file1.getAbsolutePath());// , opts

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(mCf, mQ, stream);
            bitmap.recycle();
            System.gc();

            byte[] byteArray = stream.toByteArray();
            stream = null;

            BufferedOutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(file));
                out.write(byteArray);
            } catch (Exception e) {
            } finally {
                try {
                    out.close();
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }

            byteArray = null;
            System.gc();
            String str;
            if (file.exists()) {
                str = done;
                total++;
                if (exif != null) {
                    try {
                        ExifInterface newexif = new ExifInterface(
                                file.getAbsolutePath());
                        str = copyExif(newexif) + str;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }
                    exif = null;
                }
            } else {
                str = error;
            }
            new SingleMediaScanner(ma, file);
            return str;
        }

        private String copyExif(ExifInterface newexif) {
            int i = 0;
            String s = exif.getAttribute(ExifInterface.TAG_APERTURE);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_APERTURE, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_DATETIME, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_FLASH);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_FLASH, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_ISO);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_ISO, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_MAKE);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_MAKE, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_MODEL);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_MODEL, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_ORIENTATION, s);
                i++;
            }
            s = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            if (s != null) {
                newexif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, s);
                i++;
            }
            if (i > 0) {
                try {
                    newexif.saveAttributes();
                    // return "("+i+" EXIF tags written) ";
                } catch (Exception e) {
                    return getString(R.string.error_copying_exif);
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            isRunning = false;
            Toast.makeText(ma, result, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {
            isRunning = true;
            mustStop = false;

            directory.mkdirs();
            // mExt = preferences.getString("format",
            // getString(R.string.default_save_format));
            // isPNG = mExt.equals("PNG");
            mExt = isPNG ? ".png" : ".jpg";
            if (isPNG) {
                // mG = Bitmap.Config.ARGB_8888;
                mCf = Bitmap.CompressFormat.PNG;
                mQ = 100;
            } else {
                // mG = Bitmap.Config.RGB_565;
                mCf = Bitmap.CompressFormat.JPEG;
                mQ = 50 + preferences.getInt("jpeg_quality", 40);
            }

        }

        @Override
        protected void onProgressUpdate(String... str) {
            if (sInfo.getHeight() >= mBckgrHeight) {
                sInfo.setText("");
            }
            if (str.length == 2) {
                sInfo.append(Html.fromHtml(str[0] + ": " + str[1]));
            } else {
                sInfo.append(Html.fromHtml(str[0]));
            }

        }
    }

    private class Remove extends AsyncTask<Boolean, String, String> {

        @Override
        protected String doInBackground(Boolean... params) {
            SingleMediaScanner.sUri = null;
            File folder = new File(sOutputPath);
            if (folder.exists()) {
                File[] files = folder.listFiles(mFilenameFilter);
                if (files != null && files.length != 0) {
                    int i = 0;
                    if (params[0]) {
                        for (File file : files) {
                            if (file.lastModified() > lastTime) {
                                file.delete();// getContentResolver().delete(Uri.fromFile(file),
                                // null, null);
                                i++;
                            }
                        }
                    } else {
                        for (File file : files) {
                            file.delete();
                            i++;
                        }
                    }
                    return i + getString(R.string.files_were_deleted);
                } else {
                    return getString(R.string.nothing_to_delete);
                }
            } else {
                return getString(R.string.dir_doesnt_exist);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(ma, result, Toast.LENGTH_SHORT).show();
        }
    }

    public class UFrameView extends View {

        private Bitmap mBitmap;
        private float mDy2, mDx2;
        private int mNumberOfWavesX, mNumberOfWavesY, tw;
        private float mX, mY, adj1;
        float sD, sMx, sMy, mSmoothInitial;
        private int mRainbowD = PADX;
        private float mAdjusted;
        private Paint mDotPaint = new Paint();

        public UFrameView(Context c) {
            super(c);
            mDotPaint.setColor(Color.RED);
        }

        private void drawBackgroundBitmap() {
            mBckgrWidth = getWidth();
            mBckgrHeight = getHeight();
            mBitmapWidth = mBckgrWidth - PADX2;
            mBitmapHeight = mBckgrHeight - PADX2;

            mBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight,
                    Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(mBitmap);
            canvas.drawColor(HATCH_COLOR_BG);
            for (int i = -mBckgrWidth; i < mBckgrHeight; i += 20) {
                canvas.drawLine(0, i, mBckgrWidth, mBckgrWidth + i, mPaint5);
            }
        }

        private Path drawFrame(Bitmap bitmap) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Path path = new Path();
            float j = mDx;

            if (isCircle) {
                mNumWaves = 2 * (int) ((width + height) / mDy);
                mAdjusted = mDx + adjust(isNone, isBlur, sStrokeWidth);
            } else {
                if (mustFit) {
                    float[] f = fitPath(width, height, mDx, mDy, mSmooth,
                            mStartX, mStartY, sStrokeWidth, isNone, isBlur);
                    mStartX = f[0];
                    mStartY = f[1];
                    mDx2 = f[2];
                    mDy2 = f[3];
                    mNumberOfWavesX = (int) f[4];
                    mNumberOfWavesY = (int) f[5];
                } else {
                    mNumberOfWavesX = (int) ((width - mStartX * 2) / mDy);
                    mNumberOfWavesY = (int) ((height - mStartY * 2) / mDy);

                    if (mNumberOfWavesX % 2 == 0) {
                        mNumberOfWavesX--;
                    }
                    if (mNumberOfWavesY % 2 == 0) {
                        mNumberOfWavesY--;
                    }
                    mDy2 = ((float) height - mStartY * 2) / mNumberOfWavesY;
                    mDx2 = ((float) width - mStartX * 2) / mNumberOfWavesX;
                }
            }
            if (isCircle) {
                float cX = width / 2;
                float cY = height / 2;
                float radius = Math.min(cX, cY) - mAdjusted;
                float x = Math.max(j, j + cX - cY) + mAdjusted;

                if (!mustFit) {
                    cX += mStartX;
                    cY += mStartY;
                    x += mStartX;
                }

                float y = cY;
                double dangle = 2 * Math.PI / mNumWaves;
                double ang = 0;
                path.moveTo(x, y);
                float cos = (float) Math.cos(ang);
                float sin = (float) Math.sin(ang);
                float x1, x2, y1, y2;
                for (int n = 0; n < mNumWaves; n++) {
                    x1 = x + mSmooth * sin;
                    y1 = y + mSmooth * cos;
                    ang += dangle;
                    cos = (float) Math.cos(ang);
                    sin = (float) Math.sin(ang);
                    x = cX - (radius + j) * cos;
                    y = cY + (radius + j) * sin;
                    x2 = x - mSmooth * sin;
                    y2 = y - mSmooth * cos;
                    path.cubicTo(x1, y1, x2, y2, x, y);
                    j = -j;
                }
                path.close();
            } else {
                float x = mStartX;
                float y = mStartY;

                path.moveTo(mStartX, mStartY);
                for (int n = 1; n <= mNumberOfWavesY; n++) {
                    y += mDy2;
                    path.cubicTo(x - j, y - mDy2 / 2 - mSmooth, x - j, y - mDy2
                            / 2 + mSmooth, x, y);
                    j = -j;
                }
                for (int n = 1; n <= mNumberOfWavesX; n++) {
                    x += mDx2;
                    path.cubicTo(x - mDx2 / 2 - mSmooth, y - j, x - mDx2 / 2
                            + mSmooth, y - j, x, y);
                    j = -j;
                }
                j = -j;
                for (int n = 1; n <= mNumberOfWavesY; n++) {
                    y -= mDy2;
                    path.cubicTo(x - j, y + mDy2 / 2 + mSmooth, x - j, y + mDy2
                            / 2 - mSmooth, x, y);
                    j = -j;
                }
                for (int n = 1; n <= mNumberOfWavesX; n++) {
                    x -= mDx2;
                    path.cubicTo(x + mDx2 / 2 + mSmooth, y - j, x + mDx2 / 2
                            - mSmooth, y - j, x, y);
                    j = -j;
                }

                path.close();
            }
            return path;
        }

        @Override
        protected void onDraw(Canvas canvas) {

            Bitmap bitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight,
                    isJPG ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(bitmap);
            if (isJPG)
                canvas2.drawColor(mBackgroundColor);

            Path path = drawFrame(mBitmap);

            canvas2.clipPath(path);
            canvas2.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

            if (mStartX == 0 && mStartY == 0 && !isCircle) {
                float p = sStrokeWidth / 2;
                canvas2.drawRect(p, p, mBitmapWidth - p, mBitmapHeight - p,
                        mPaint);
                Path pt = new Path();
                pt.addRect(PADX, PADX, mBitmapWidth + PADX, mBitmapHeight
                        + PADX, Path.Direction.CW);
                canvas.clipPath(pt);
            }

            canvas.drawBitmap(bitmap, PADX, PADX, mBitmapPaint);
            // if (!isNone || isBlur) {
            canvas.translate(PADX, PADX);
            canvas.drawPath(path, mPaint);
            if (shouldDrawText) {
                PathMeasure pm = new PathMeasure(path, true);
                pathLength = pm.getLength();

                if (fillWithText && tw > 0) {
                    int count = (int) pathLength / tw;
                    for (int i = 0; i < count; i++) {
                        canvas.drawTextOnPath(sText, path, currentLength + i
                                * tw, adj1, mTextPaint);
                    }
                } else {
                    canvas.drawTextOnPath(sText, path, currentLength, adj1,
                            mTextPaint);
                }

                if (!adjustTextPosition) {
                    pm.getPosTan(currentLength, dt, null);
                    canvas.drawCircle(dt[0], dt[1], 4, mDotPaint);
                }
            }
            canvas.translate(-PADX, -PADX);

            if (isRainbow) {
                canvas.translate(PADX, PADX);
                canvas.drawLine(mCenterRainbowX - PADX, mCenterRainbowY,
                        mCenterRainbowX + PADX, mCenterRainbowY, mPaint);
                canvas.drawLine(mCenterRainbowX, mCenterRainbowY - PADX,
                        mCenterRainbowX, mCenterRainbowY + PADX, mPaint);
                canvas.drawCircle(2 * (mCenterRainbowX - mBitmapWidth / 4.f),
                        2 * (mCenterRainbowY - mBitmapHeight / 4.f), 5, mPaint);
                canvas.translate(-PADX, -PADX);
            }
            canvas.drawRect(PADX, PADX, mBckgrWidth - PADX,
                    mBckgrHeight - PADX, mPaint5);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mDy = mDx = (w + h) / 40;
            mSmooth = mDy / 2;
            drawBackgroundBitmap();
            settingsChanged(mColor, SettingsDialog.sMode1,
                    SettingsDialog.sMode2, SettingsDialog.sStrokeWidth);
        }

        private void updateTextBounds() {
            Rect bounds = new Rect();
            mTextPaint.getTextBounds(sText + "...", 0, sText.length() + 3,
                    bounds);
            tw = bounds.right;
            adj1 = -(bounds.bottom + bounds.top) / 2;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int P = event.getPointerCount();
            // int N = event.getHistorySize();
            if (action != MotionEvent.ACTION_MOVE) {
                if (action == MotionEvent.ACTION_DOWN) {
                    touch_start(event.getX(0), event.getY(0));
                    return true;
                } else if (adjustTextPosition
                        && action == MotionEvent.ACTION_UP) {
                    adjustTextPosition = false;
                    invalidate();
                    return true;
                }
                if (action == MotionEvent.ACTION_POINTER_DOWN) {
                    if (P == 2) {
                        float sX2 = event.getX(0) - event.getX(1);
                        float sY2 = event.getY(0) - event.getY(1);
                        sD = (float) Math.sqrt(sX2 * sX2 + sY2 * sY2);
                        mSmoothInitial = mSmooth;
                        return true;
                    } else if (P == 3 && !mustFit) {
                        sMx = mStartX - event.getX(2);
                        sMy = mStartY - event.getY(2);
                        return true;
                    }
                    return false;
                }
            } else {
                if (P == 1) {
                    touch_move(event.getX(), event.getY());
                } else if (P == 2) {
                    float sX2 = event.getX(0) - event.getX(1);
                    float sY2 = event.getY(0) - event.getY(1);
                    float sD2 = (float) Math.sqrt(sX2 * sX2 + sY2 * sY2);
                    mSmooth = mSmoothInitial + sD2 - sD;
                } else if (P == 3 && !mustFit) {
                    mStartX = (int) (sMx + event.getX(2));
                    mStartY = (int) (sMy + event.getY(2));
                } else {
                    return false;
                }
                invalidate();
            }
            return true;
        }

        private void touch_move(float x, float y) {
            if (adjustRainbow) {
                mCenterRainbowX = x - PADX;
                mCenterRainbowY = y - PADX;
                Shader s = new SweepGradient(
                        2 * (mCenterRainbowX - mBitmapWidth / 4.f),
                        2 * (mCenterRainbowY - mBitmapHeight / 4.f), mColors,
                        null);
                mPaint.setShader(s);
            } else if (adjustTextPosition) {
                float dx = (x - mX);
                float dy = (y - mY);
                if (x > mBckgrWidth / 2 + PADX)
                    dy = -dy;
                if (y < mBckgrHeight / 2 + PADX)
                    dx = -dx;
                currentLength += dx + dy;
                if (currentLength > pathLength)
                    currentLength = 0;
                else if (currentLength < 0)
                    currentLength = pathLength;
                mX = x;
                mY = y;
            } else {
                float dx = x - mX;
                float dy = y - mY;
                mDx = Math.abs(mDx + dx);
                mDy = Math.abs(mDy + dy);

                if (mDy < 2) {
                    mDy = 2;
                } else {
                    mSmooth = mSmooth + dy / 2;
                }
                if (mDx < 2)
                    mDx = 2;
                mX = x;
                mY = y;
            }
        }

        private void touch_start(float x, float y) {
            mX = x;
            mY = y;
            if (isRainbow) {
                if (Math.abs(x - mCenterRainbowX - PADX) < mRainbowD
                        && Math.abs(y - mCenterRainbowY - PADX) < mRainbowD) {
                    adjustRainbow = true;
                } else {
                    adjustRainbow = false;
                }
            }
            if (shouldDrawText) {
                if (Math.abs(x - dt[0] - PADX) < 20
                        && Math.abs(y - dt[1] - PADX) < 20) {
                    adjustTextPosition = true;
                } else {
                    adjustTextPosition = false;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FOLDER) {
            if (resultCode == RESULT_OK) {
                String sFolder = data.getStringExtra(FileDialog.RESULT_PATH);
                sInputPath = sFolder;
                String folder_name = new File(sInputPath).getName();
                sOutputPath = new File(sInputPath, folder_name + "-Framed")
                        .getPath();
                SingleMediaScanner.sUri = null;
                Editor et = preferences.edit();
                et.putString(Prefs.KEY_FOLDER, sInputPath);
                et.commit();
                updateInfo();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    3);
        }

        mPaint = new Paint() {
            {
                setAntiAlias(true);
                setDither(true);
                setColor(mColor);
                setStyle(Paint.Style.STROKE);
                setStrokeJoin(Paint.Join.ROUND);
                setStrokeCap(Paint.Cap.ROUND);
                setStrokeWidth(sStrokeWidth);
            }
        };

        mPaint5 = new Paint() {
            {
                setStrokeWidth(1);
                setColor(HATCH_COLOR_LINES);
                setStyle(Paint.Style.STROKE);
                setAntiAlias(true);
            }
        };

        mTextPaint = new Paint() {
            {
                setAntiAlias(true);
                setTextSize(sStrokeWidth);
            }
        };

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mFilenameFilter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                String fn = filename.toLowerCase();
                return (fn.endsWith(".jpg") || fn.endsWith(".png") || fn
                        .endsWith(".jpeg"));
            }
        };

        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 6, 3.5f);

        mBlur = new BlurMaskFilter(sStrokeWidth != 0 ? sStrokeWidth : 1,
                BlurMaskFilter.Blur.NORMAL);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        sInputPath = preferences.getString(
                Prefs.KEY_FOLDER,
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString());
        String folder_name = new File(sInputPath).getName();
        sOutputPath = new File(sInputPath, folder_name + "-Framed").getPath();

        String sfont = preferences.getString(Prefs.KEY_FONT, "");
        setTypeface(sfont);

        setContentView(R.layout.main);
        mv = new UFrameView(this);
        FrameLayout mFrame = (FrameLayout) findViewById(R.id.frame);
        sInfo = new TextView(this);
        sInfo.setTextAppearance(this, android.R.attr.textAppearanceSmall);
        sInfo.setShadowLayer(1, 1, 1, Color.BLACK);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        updateInfo();
        if (sText.length() == 0)
            sText = getString(R.string.edit_text);
        mFrame.addView(mv);
        mFrame.addView(sInfo, lp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.color:
                new SettingsDialog(this, this, mPaint.getColor()).show();
                break;
            case R.id.settings:
                Intent intent = new Intent(this, Prefs.class);
                startActivity(intent);
                break;
            case R.id.input:
                Intent intent2 = new Intent(getBaseContext(), FileDialog.class);
                intent2.putExtra(FileDialog.START_PATH, sInputPath);
                intent2.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
                intent2.putExtra(FileDialog.CAN_SELECT_DIR, true);
                startActivityForResult(intent2, SELECT_FOLDER);
                break;
            case R.id.run:
                if (isRunning)
                    break;
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        // .setTitle("")
                        .setMessage(
                                getString(R.string.are_you_sure_run) + sInputPath
                                        + " ?")
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        new Export().execute();
                                    }

                                }).setNegativeButton(android.R.string.no, null)
                        .show();
                break;
            case R.id.open:
                File dir = new File(sOutputPath);
                if (dir.exists()) {
                    Uri uri = SingleMediaScanner.sUri;
                    if (uri == null) {
                        new SingleMediaScanner(this, dir);
                    } else {
                        Intent openimage = new Intent(Intent.ACTION_VIEW);
                        openimage.setData(uri);
                        try {
                            startActivity(openimage);
                        } catch (ActivityNotFoundException e) {
                            // SingleMediaScanner.sUri = null;
                            new SingleMediaScanner(this, dir);
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.dir_doesnt_exist,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.remove:
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        // .setTitle("")
                        .setMessage(
                                getString(R.string.are_you_sure_remove)
                                        + sOutputPath + " ?")
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                        File dir = new File(sOutputPath);
                                        if (dir.exists()) {
                                            new Remove().execute(false);
                                        } else {
                                            Toast.makeText(
                                                    ma,
                                                    getString(R.string.dir_doesnt_exist),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                }).setNegativeButton(android.R.string.no, null)
                        .show();
                break;
            case R.id.removeLast:
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        // .setTitle("")
                        .setMessage(
                                getString(R.string.are_you_sure_remove_last)
                                        + sOutputPath + " ?")
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                        File dir = new File(sOutputPath);
                                        if (dir.exists()) {
                                            new Remove().execute(true);
                                        } else {
                                            Toast.makeText(
                                                    ma,
                                                    getString(R.string.dir_doesnt_exist),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                }).setNegativeButton(android.R.string.no, null)
                        .show();
                break;
//            case R.id.exif:
//                item.setChecked(mustRetainExif = !mustRetainExif);
//                break;
            case R.id.stop:
                mustStop = true;
                break;
            case R.id.fit:
                item.setChecked(mustFit = !mustFit);
                // Toast.makeText(this, lastTime + "", 0).show();
                mv.invalidate();
                break;
            case R.id.circle:
                item.setChecked(isCircle = !isCircle);
                mv.invalidate();
                break;
            case R.id.postmark:
                if (!mustFit && !isCircle) {
                    mStartX = 0;
                    mStartY = 0;
                    mv.invalidate();
                }
                break;
            case R.id.straighten:
                mStartX = mStartY = mSmooth = mDx = 0;
                mv.invalidate();
                break;
            case R.id.should_draw_text:
                item.setChecked(shouldDrawText = !shouldDrawText);
                mv.invalidate();
                break;
            case R.id.fill_with_text:
                item.setChecked(fillWithText = !fillWithText);
                mv.invalidate();
                break;
            case R.id.edit_text:
                final EditText input = new EditText(this);
                input.setText(sText);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.edit_text)
                        // .setMessage(R.string.edit_text)
                        .setView(input)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        sText = input.getText().toString();
                                        mv.updateTextBounds();
                                        if (sText.length() == 0)
                                            shouldDrawText = false;
                                        mv.invalidate();
                                    }
                                }).setNegativeButton(android.R.string.no, null)
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
//        menu.findItem(R.id.exif).setEnabled(isJPG);
//        menu.findItem(R.id.exif).setChecked(mustRetainExif);
        menu.findItem(R.id.stop).setEnabled(isRunning);
        menu.findItem(R.id.fit).setChecked(mustFit);
        menu.findItem(R.id.circle).setChecked(isCircle);
        menu.findItem(R.id.postmark).setEnabled(
                !isCircle && !mustFit && (mStartX != 0 || mStartY != 0));
        menu.findItem(R.id.should_draw_text).setChecked(shouldDrawText);
        menu.findItem(R.id.fill_with_text).setChecked(fillWithText);
        menu.findItem(R.id.fill_with_text).setEnabled(shouldDrawText);
        menu.findItem(R.id.edit_text).setEnabled(shouldDrawText);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        ma = this;
        String ext = preferences.getString("format",
                getString(R.string.default_save_format));
        isJPG = !ext.equals("PNG");
        mBackgroundColor = preferences.getInt("background_color", 0xFFFFFFFF);
        mFontColor = preferences.getInt("font_color", mBackgroundColor);
        mTextPaint.setColor(mFontColor);
        if (font != null)
            mTextPaint.setTypeface(font);
        mv.updateTextBounds();
    }

    @Override
    public void settingsChanged(int color, int mode1, int mode2,
                                float strokewidth) {
        if (adjustRainbow)
            adjustRainbow = false;
        if (color != -1) {
            mColor = color;
            mPaint.setColor(color);

            float[] f1 = new float[3];
            float[] f = new float[3];
            Color.colorToHSV(mColor, f1);
            for (int i = 0; i < 7; i++) {
                Color.colorToHSV(mColors[i], f);
                f[1] = f1[1];
                f[2] = f1[2];
                mColors[i] = Color.HSVToColor(f);
            }
            if (isRainbow) {
                Shader s = new SweepGradient(
                        2 * (mCenterRainbowX - mBitmapWidth / 4.f),
                        2 * (mCenterRainbowY - mBitmapHeight / 4.f), mColors,
                        null);
                mPaint.setShader(s);
            }
        }
        if (strokewidth != -1) {
            mPaint.setStrokeWidth(strokewidth);
            mTextPaint.setTextSize(strokewidth);
            mv.updateTextBounds();
            if (strokewidth != 0)
                mBlur = new BlurMaskFilter(strokewidth,
                        BlurMaskFilter.Blur.NORMAL);
            sStrokeWidth = strokewidth;
            if (isBlur) {
                mPaint.setMaskFilter(mBlur);
            }
        }
        if (mode1 != -1)
            sMode1 = mode1;
        if (mode2 != -1)
            sMode2 = mode2;
        switch (mode1) {
            case SettingsDialog.NORMAL:
                // mPaint.setXfermode(null);
                mPaint.setMaskFilter(null);
                isBlur = false;
                break;
            case SettingsDialog.BLUR:
                // mPaint.setXfermode(null);
                mPaint.setMaskFilter(mBlur);
                isBlur = true;
                break;
            case SettingsDialog.EMBOSS:
                // mPaint.setXfermode(null);
                mPaint.setMaskFilter(mEmboss);
                isBlur = false;
                break;
        }
        switch (mode2) {
            case SettingsDialog.COLOR:
                mPaint.setXfermode(null);
                mPaint.setShader(null);
                isRainbow = false;
                isNone = false;
                break;
            case SettingsDialog.RAINBOW:
                mPaint.setXfermode(null);
                if (!isRainbow) {
                    if (mColors[0] == mColors[2]) {
                        mColors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF,
                                0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
                    }
                    mCenterRainbowX = mBckgrWidth / 2 - PADX;
                    mCenterRainbowY = mBckgrHeight / 2 - PADX;
                    Shader s = new SweepGradient(mCenterRainbowX, mCenterRainbowY,
                            mColors, null);
                    mPaint.setShader(s);
                    isRainbow = true;
                }
                isNone = false;
                break;
            case SettingsDialog.NONE:
                // mPaint.setMaskFilter(null);
                isRainbow = false;
                mPaint.setShader(null);
                isNone = true;
                if (isJPG) {
                    if (isBlur) {
                        mPaint.setColor(mColor);
                        mPaint.setXfermode(new PorterDuffXfermode(
                                PorterDuff.Mode.CLEAR));
                    } else {
                        mPaint.setColor(mBackgroundColor);
                        mPaint.setXfermode(null);
                    }
                } else {
                    mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                }
                break;
        }
        mv.invalidate();
    }

    void updateInfo() {
        if (sInfo.getHeight() >= mBckgrHeight) {
            sInfo.setText("");
        }
        if (!isRunning) {
            sInfo.append(getString(R.string.help));
            sInfo.append(Html
                    .fromHtml(getString(R.string.input_folder)
                            + ": <font color='#efefa1'>" + sInputPath
                            + "</font><br />"));
            sInfo.append(Html.fromHtml(getString(R.string.output_folder)
                    + ": <font color='#a1efc1'>" + sOutputPath
                    + "</font><br />"));
        }
    }
}