package vnd.blueararat.UFrame;

import vnd.blueararat.UFrame.SettingsDialog.OnSettingsChangedListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {

	private OnSettingsChangedListener mListener;
	private static int sInitialColor;
	private Paint mPaint;
	private Paint mPaint2;
	private Paint mPaint3;
	private Paint mPaint4;
	private Path mPath = new Path();
	private Paint mCenterPaint;
	private int[] mColors;
	private int mAlpha = 255;
	private float mDarkness = 0;
	private static float sD;
	private static int mCenter;
	private static int mCenterRadius;
	private int mWidth, mHeight, mDY;
	private static final float PI = 3.1415926f;
	private float mCornerRadius;
	private String mAlphaString, mDarknessString, mSaturationString;
	private static float sStrokeWidth;

	public ColorPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mColors = new int[] { 0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF,
				0xFF00FF00, 0xFFFFFF00, 0xFFFF0000 };
		final Shader s = new SweepGradient(0, 0, mColors, null);

		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {
			{
				setShader(s);
				setStyle(Paint.Style.STROKE);
				setStrokeWidth(mCenterRadius);
			}
		};
		mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {
			{
				setColor(sInitialColor);
				// setStrokeWidth(5);
			}
		};

		mPaint2 = new Paint() {
			{
				setAntiAlias(true);
				setDither(true);
				setColor(Color.WHITE);
				setShadowLayer(1, 1, 1, Color.BLACK);
			}
		};

		mPaint3 = new Paint() {
			{
				setAntiAlias(true);
				setDither(true);
				setColor(Color.WHITE);
				setTextAlign(Align.CENTER);
			}
		};

		mPaint4 = new Paint() {
			{
				setAntiAlias(true);
				setDither(true);
				setColor(MainActivity.HATCH_COLOR_LINES);
				setStyle(Paint.Style.STROKE);
				setStrokeWidth(2);
			}
		};
		mAlpha = Color.alpha(sInitialColor);
		mDarkness = getDarkness(sInitialColor);
		mDarknessString = context.getString(R.string.darkness)
				+ String.format("%.1f", mDarkness) + "%";
		mAlphaString = context.getString(R.string.transparency)
				+ String.format("%.1f", 100 * (1.f - (float) mAlpha / 255))
				+ "%";
		mSaturationString = context.getString(R.string.double_tap);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float r = mCenter - mPaint.getStrokeWidth() * 0.5f;

		canvas.translate(mCenter, mCenter);
		canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
		canvas.drawText("α", 0, 5, mPaint3);

		canvas.drawCircle(0, 0, mCenterRadius, mPaint4);
		canvas.drawCircle(0, 0, mCenterRadius, mCenterPaint);

		float mt = (3 * PI * r - mPaint2.measureText(mSaturationString)) / 2;
		mPath.addCircle(0, 0, r, Path.Direction.CW);
		canvas.drawTextOnPath(mSaturationString, mPath, mt, 0, mPaint2);

		mPaint3.setShader(new LinearGradient(-mCenter, 0, mCenter, 0,
				Color.TRANSPARENT, Color.WHITE, Shader.TileMode.MIRROR));
		int u1 = mCenter + mDY;
		float u2 = mCenter - mCornerRadius;
		int u3 = mHeight - mCenter;
		canvas.drawRoundRect(new RectF(-mCenter, mCenter + 10, mCenter, u1),
				mCornerRadius, mCornerRadius, mPaint3);
		mPaint3.setShader(null);
		canvas.drawText(mAlphaString, -u2, u2 + mDY, mPaint2);

		mPaint3.setShader(new LinearGradient(-mCenter, 0, mCenter, 0,
				Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));
		canvas.drawRoundRect(new RectF(-mCenter, u1 + 10, mCenter, u3),
				mCornerRadius, mCornerRadius, mPaint3);
		mPaint3.setShader(null);
		canvas.drawText(mDarknessString, -u2, u3 - mCornerRadius, mPaint2);
		mPath.reset();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int m = Math.min(w, h);
		mWidth = m;
		mHeight = 4 * m / 3 + 20;
		if (mHeight > h) {
			mHeight = h;
			mWidth = 3 * (mHeight - 20) / 4;
		}
		mDY = mWidth / 6 + 10;
		mCornerRadius = mWidth / 24;
		setMeasuredDimension(mWidth, mHeight);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mCenter = w / 2;
		mCenterRadius = w / 6;
		float t1 = w / 12;
		float t2 = w / 16;
		mPaint3.setTextSize(t1);
		mPaint2.setTextSize(t2);
		mPaint.setStrokeWidth(mCenterRadius);
	}

	private int floatToByte(float x) {
		int n = Math.round(x);
		return n;
	}

	private int pinToByte(int n) {
		if (n < 0) {
			n = 0;
		} else if (n > 255) {
			n = 255;
		}
		return n;
	}

	private int ave(int s, int d, float p) {
		return s + Math.round(p * (d - s));
	}

	private int interpColor(int colors[], float unit) {
		if (unit <= 0) {
			return colors[0];
		}
		if (unit >= 1) {
			return colors[colors.length - 1];
		}

		float p = unit * (colors.length - 1);
		int i = (int) p;
		p -= i;

		int c0 = colors[i];
		int c1 = colors[i + 1];
		int a = ave(Color.alpha(c0), Color.alpha(c1), p);
		int r = ave(Color.red(c0), Color.red(c1), p);
		int g = ave(Color.green(c0), Color.green(c1), p);
		int b = ave(Color.blue(c0), Color.blue(c1), p);

		return Color.argb(a, r, g, b);
	}

	private int rotateColor(int color, float rad) {
		float deg = rad * 180 / 3.1415927f;
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);

		ColorMatrix cm = new ColorMatrix();
		ColorMatrix tmp = new ColorMatrix();

		cm.setRGB2YUV();
		tmp.setRotate(0, deg);
		cm.postConcat(tmp);
		tmp.setYUV2RGB();
		cm.postConcat(tmp);

		final float[] a = cm.getArray();

		int ir = floatToByte(a[0] * r + a[1] * g + a[2] * b);
		int ig = floatToByte(a[5] * r + a[6] * g + a[7] * b);
		int ib = floatToByte(a[10] * r + a[11] * g + a[12] * b);

		return Color.argb(Color.alpha(color), pinToByte(ir), pinToByte(ig),
				pinToByte(ib));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		float x = event.getX() - mCenter;
		float y = event.getY() - mCenter;
		boolean inCenter = FloatMath.sqrt(x * x + y * y) <= mCenter
				- mCenterRadius;
		boolean alpha = y > mCenter && y < mCenter + mDY;
		boolean darkness = y > mCenter + mDY;
		int P = event.getPointerCount();
		if (P == 1) {
			switch (action) {
			case MotionEvent.ACTION_UP:
				return true;
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				if (inCenter) {
					return true;
				} else if (alpha) {
					mAlpha = (int) (event.getX() * 255 / mWidth);

					if (mAlpha < 0) {
						mAlpha = 0;
					} else if (mAlpha > 255) {
						mAlpha = 255;
					}
					mAlphaString = getContext()
							.getString(R.string.transparency)
							+ String.format("%.1f",
									100 * (1.f - (float) mAlpha / 255)) + "%";

					mCenterPaint.setAlpha(mAlpha);
					invalidate();
				} else if (darkness) {
					float fx = event.getX() / mWidth;
					mDarkness = 100 * (1 - fx);
					if (mDarkness < 0) {
						mDarkness = 0;
					} else if (mDarkness > 100) {
						mDarkness = 100;
					}
					mDarknessString = getContext().getString(R.string.darkness)
							+ String.format("%.1f", mDarkness) + "%";
					int color = adjustDarkness(mDarkness,
							mCenterPaint.getColor());
					mCenterPaint.setColor(color);
					mCenterPaint.setAlpha(mAlpha);
					invalidate();
				} else {
					float angle = (float) Math.atan2(y, x);
					float unit = angle / (2 * PI);
					if (unit < 0) {
						unit += 1;
					}
					mCenterPaint.setColor(adjustDarkness(mDarkness,
							interpColor(mColors, unit)));
					mCenterPaint.setAlpha(mAlpha);
					invalidate();
				}
				break;
			}
			if (mListener != null)
				mListener.settingsChanged(mCenterPaint.getColor(), -1, -1,
						sStrokeWidth);

		} else if (P == 2) {
			if (action == MotionEvent.ACTION_POINTER_DOWN) {
				float sX2 = event.getX(0) - event.getX(1);
				float sY2 = event.getY(0) - event.getY(1);
				sD = FloatMath.sqrt(sX2 * sX2 + sY2 * sY2);
				return true;
			} else if (action == MotionEvent.ACTION_MOVE) {
				float sX2 = event.getX(0) - event.getX(1);
				float sY2 = event.getY(0) - event.getY(1);
				float sD2 = FloatMath.sqrt(sX2 * sX2 + sY2 * sY2);
				float fx = ((sD2 - sD) > 0) ? -0.01f : 0.01f;
				float[] f = new float[3];
				for (int i = 0; i < 7; i++) {
					Color.colorToHSV(mColors[i], f);
					f[1] += fx;
					mColors[i] = Color.HSVToColor(f);
				}
				Shader s = new SweepGradient(0, 0, mColors, null);
				int i = (int) (f[1] * 100);
				if (i > 100) {
					i = 100;
				} else if (i < 0) {
					i = 0;
				}
				mSaturationString = getContext().getString(R.string.saturation)
						+ i + "%";

				mPaint.setShader(s);
				invalidate();
			}
		}
		return true;
	}

	private static float getDarkness(int color) {
		float[] f = new float[3];
		Color.colorToHSV(color, f);
		return 100 * (1 - f[2]);
	}

	private int adjustDarkness(float darkness, int color) {
		float[] f = new float[3];
		Color.colorToHSV(color, f);
		f[2] = 1 - darkness / 100;
		color = Color.HSVToColor(f);
		return color;
	}

	public void setSettingsChangedListener(OnSettingsChangedListener l) {
		mListener = l;
	}

	public static void setInitialColor(int color) {
		sInitialColor = color;
	}

	public int getColor() {
		return mCenterPaint.getColor();
	}

}
