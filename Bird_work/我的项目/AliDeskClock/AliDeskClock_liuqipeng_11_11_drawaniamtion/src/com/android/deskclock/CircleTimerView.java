package com.android.deskclock;
//liuqipeng
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
//liuqipeng
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.android.deskclock.Log;
import android.view.View;
import android.view.View.MeasureSpec;

import com.android.deskclock.stopwatch.Stopwatches;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class CircleTimerView extends View {
    private int mRedColor;
    private int mGrayColor;
    private long mIntervalTime = 0;
    private long mIntervalStartTime = -1;
    private long mMarkerTime = -1;
    private long mCurrentIntervalTime = 0;
    private long mAccumulatedTime = 0;
    private boolean mPaused = false;
    private boolean mAnimate = false;
    private static float mStrokeSize = 4;
    private static float mDotRadius = 6;
    private static float mMarkerStrokeSize = 2;
    private final Paint mPaint = new Paint();
    private final Paint mFill = new Paint();
    private final RectF mArcRect = new RectF();
    private float mRadiusOffset;   // amount to remove from radius to account for markers on circle
    private float mScreenDensity;
    private int handOffset;
//liuqipeng
	private Drawable mStopWatchNum;
	//private Drawable mStopWatchTopMark;
	//private Drawable mStopWatchRightMark;
	private Bitmap mStopWatchTopMark;
	private Bitmap mStopWatchRightMark;
//liuqipeng
    // Stopwatch mode is the default.
    private boolean mTimerMode = false;
    private Context mContext;

    @SuppressWarnings("unused")
    public CircleTimerView(Context context) {
        this(context, null);
    }

    public CircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
//liuqipeng
		Resources mRes=context.getResources();
		mStopWatchNum=mRes.getDrawable(R.drawable.clock_stopwatch_number);
		//mStopWatchTopMark=mRes.getDrawable(R.drawable.stopwatch_top);
		//mStopWatchRightMark=mRes.getDrawable(R.drawable.stopwatch_right);
		 mStopWatchTopMark = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.stopwatch_top);
		 mStopWatchRightMark = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.stopwatch_right);		
//liuqipeng
        init(context);
    }
    
    public void setTime(long time) {
        mSeconds = time;
        postInvalidate();
    }

    public void setIntervalTime(long t) {
        //mSeconds = t;
        //postInvalidate();
    }

    public void setMarkerTime(long t) {
        //mSeconds = t;
        //postInvalidate();
    }

    public void reset() {
        /*mIntervalStartTime = -1;
        mMarkerTime = -1;*/
        mSeconds = 0;
        postInvalidate();
    }
    public void startIntervalAnimation() {
        /*mSeconds = Utils.getTimeNow();
        mAnimate = true;
        invalidate();
        mPaused = false;*/
    }
    public void stopIntervalAnimation() {
        /*mAnimate = false;
        mIntervalStartTime = -1;
        mAccumulatedTime = 0;*/
    }

    public boolean isAnimating() {
        return (mIntervalStartTime != -1);
    }

    public void pauseIntervalAnimation() {
        mAnimate = false;
        mAccumulatedTime += Utils.getTimeNow() - mIntervalStartTime;
        mPaused = true;
    }

    public void abortIntervalAnimation() {
        mAnimate = false;
    }

    public void setPassedTime(long time, boolean drawRed) {
        // The onDraw() method checks if mIntervalStartTime has been set before drawing any red.
        // Without drawRed, mIntervalStartTime should not be set here at all, and would remain at -1
        // when the state is reconfigured after exiting and re-entering the application.
        // If the timer is currently running, this drawRed will not be set, and will have no effect
        // because mIntervalStartTime will be set when the thread next runs.
        // When the timer is not running, mIntervalStartTime will not be set upon loading the state,
        // and no red will be drawn, so drawRed is used to force onDraw() to draw the red portion,
        // despite the timer not running.
        
        mSeconds = mAccumulatedTime = time;
        /*if (drawRed) {
            mIntervalStartTime = Utils.getTimeNow();
        }*/
        postInvalidate();
    }

    private int mDialWidth = 0;
    private int mDialHeight = 0;
    Drawable mSecondHand;
    Drawable background;
    long mSeconds = 0;
    private void init(Context c) {

        /*Resources resources = c.getResources();
        mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        float dotDiameter = resources.getDimension(R.dimen.circletimer_dot_size);
        mMarkerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        mRadiusOffset = Utils.calculateRadiusOffset(
                mStrokeSize, dotDiameter, mMarkerStrokeSize);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mGrayColor = resources.getColor(R.color.grey);
        mRedColor = resources.getColor(R.color.clock_red);
        mScreenDensity = resources.getDisplayMetrics().density;
        mFill.setAntiAlias(true);
        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(mRedColor);
        mDotRadius = dotDiameter / 2f;*/

        background = mContext.getResources().getDrawable(R.drawable.clock_alarm_view2_day_bg);
        mDialWidth = background.getIntrinsicWidth();
        mDialHeight = background.getIntrinsicHeight();
        // mSecondHand = mContext.getResources().getDrawable(R.drawable.clock_stopwatches_second_bg);
        handOffset = (int)getResources().getDimension(R.dimen.minute_and_hour_Hand_offset);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float) heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(
                resolveSize((int) (mDialWidth * scale), widthMeasureSpec),
                resolveSize((int) (mDialHeight * scale), heightMeasureSpec));
    }

    public void setTimerMode(boolean mode) {
        mTimerMode = mode;
    }

    @Override
    public void onDraw(Canvas canvas) {
        /*int xCenter = getWidth() / 2 + 1;
        int yCenter = getHeight() / 2;

        mPaint.setStrokeWidth(mStrokeSize);
        float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        if (mIntervalStartTime == -1) {
            // just draw a complete white circle, no red arc needed
            mPaint.setColor(mGrayColor);
            canvas.drawCircle (xCenter, yCenter, radius, mPaint);
            if (mTimerMode) {
                drawRedDot(canvas, 0f, xCenter, yCenter, radius);
            }
        } else {
            if (mAnimate) {
                mCurrentIntervalTime = Utils.getTimeNow() - mIntervalStartTime + mAccumulatedTime;
            }
            //draw a combination of red and white arcs to create a circle
            mArcRect.top = yCenter - radius;
            mArcRect.bottom = yCenter + radius;
            mArcRect.left =  xCenter - radius;
            mArcRect.right = xCenter + radius;
            float redPercent = (float)mCurrentIntervalTime / (float)mIntervalTime;
            // prevent timer from doing more than one full circle
            redPercent = (redPercent > 1 && mTimerMode) ? 1 : redPercent;

            float whitePercent = 1 - (redPercent > 1 ? 1 : redPercent);
            // draw red arc here
            mPaint.setColor(mRedColor);
            if (mTimerMode){
                canvas.drawArc (mArcRect, 270, - redPercent * 360 , false, mPaint);
            } else {
                canvas.drawArc (mArcRect, 270, + redPercent * 360 , false, mPaint);
            }

            // draw white arc here
            mPaint.setStrokeWidth(mStrokeSize);
            mPaint.setColor(mGrayColor);
            if (mTimerMode) {
                canvas.drawArc(mArcRect, 270, + whitePercent * 360, false, mPaint);
            } else {
                canvas.drawArc(mArcRect, 270 + (1 - whitePercent) * 360,
                        whitePercent * 360, false, mPaint);
            }

            if (mMarkerTime != -1 && radius > 0 && mIntervalTime != 0) {
                mPaint.setStrokeWidth(mMarkerStrokeSize);
                float angle = (float)(mMarkerTime % mIntervalTime) / (float)mIntervalTime * 360;
                // draw 2dips thick marker
                // the formula to draw the marker 1 unit thick is:
                // 180 / (radius * Math.PI)
                // after that we have to scale it by the screen density
                canvas.drawArc (mArcRect, 270 + angle, mScreenDensity *
                        (float) (360 / (radius * Math.PI)) , false, mPaint);
            }
            drawRedDot(canvas, redPercent, xCenter, yCenter, radius);
        }
        if (mAnimate) {
            invalidate();
        }*/

        int availableWidth = getRight() - getLeft();
        int availableHeight = getBottom() - getTop();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        int w = background.getIntrinsicWidth();
        int h = background.getIntrinsicHeight();
        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min((float) availableWidth / (float) w,
                    (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

//liuqipeng begin 
		final Drawable drawBackground = background;
		drawBackground.setBounds(getWidth()/2 - (w / 2), getHeight()/2- (h / 2), getWidth()/2+ (w / 2), getHeight()/2 + (h / 2));
		drawBackground.draw(canvas);
//liuqipeng
//liuqipeng
    	final Drawable stopWatchNum = mStopWatchNum;
        int ww = stopWatchNum.getIntrinsicWidth();
        int hh = stopWatchNum.getIntrinsicHeight();
/*
        if (availableWidth < ww || availableHeight < hh) {
            float scale = Math.min((float) availableWidth / (float) ww,
                    (float) availableHeight / (float) hh);
        }
*/
        stopWatchNum.setBounds(x - (ww / 2), y - (hh / 2), x + (ww / 2), y + (hh / 2));
        stopWatchNum.draw(canvas);
//liuqipeng
//liuqipeng	begin 
		//画top按钮
		int distance=3;
		int mTopWidth=mStopWatchTopMark.getWidth();
		int mTopHeight=mStopWatchTopMark.getHeight()-distance;
		float xxx=(1.0f+getAlpha())/2;
		mTopHeight=(int)(mTopHeight*xxx);
		Rect src=new Rect(0,0,mTopWidth,mTopHeight);	
		Rect dst=new Rect(getWidth()/2-mTopWidth/2,getHeight()/2-mTopHeight-ww/2,getWidth()/2+mTopWidth/2,getHeight()/2-ww/2);
		Bitmap stopWatchTopMark = mStopWatchTopMark;
		canvas.drawBitmap(stopWatchTopMark,src,dst,null);
		//画right按钮
		canvas.save();
		canvas.rotate(45,getWidth()/2,getHeight()/2);
		Bitmap stopWatchRightMark = mStopWatchTopMark;
		canvas.drawBitmap(stopWatchRightMark,src,dst,null);
		canvas.restore();
		/*int mRightWidth=mStopWatchRightMark.getWidth();
		int mRightHeight=mStopWatchRightMark.getHeight();
		float xxx=(1.0f+getAlpha())/2;
		mRightHeight=(int)(mRightHeight*xxx);
		Rect src=new Rect(0,0,mStopWidth,mStopHeight);	
		Rect dst=new Rect(getWidth()/2-mStopWidth/2,getHeight()/2-mStopHeight-ww/2+distance,getWidth()/2+mStopWidth/2,getHeight()/2-ww/2+distance);
		Bitmap stopWatchTopMark = mStopWatchTopMark;
		canvas.drawBitmap(stopWatchTopMark,src,dst,null);*/
//liuqipeng
        /*aihua.shenah remove seconds,becasue of issues of performance.*/
        // canvas.rotate(mSeconds / 60000.0f * 360.0f, x, y);
        // final Drawable secondHand = mSecondHand;
        //if (changed) {
            // w = secondHand.getIntrinsicWidth();
            // h = secondHand.getIntrinsicHeight();
            // secondHand.setBounds(x - (w / 2), y - h + handOffset, x + (w / 2), y + handOffset
                    // );
        //}
        // secondHand.draw(canvas);

        int secondPos = (int)(mSeconds / 60000.0f * 360.0f) / 30;
        Paint pt = new Paint();
        pt.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.second_hand_width));
        pt.setAntiAlias(true);
        pt.setColor(Color.WHITE);

        int secondRadius = getResources().getDimensionPixelSize(R.dimen.second_hand_radius);

        float f1 = (float)(secondRadius * Math.sin(mSeconds / 30000.0f * Math.PI));
        float f2 = (float)(secondRadius * Math.cos(mSeconds / 30000.0f * Math.PI));
        int secondInterval = (int)(mSeconds / 60000.0f * 360.0f) % 30;
//liuqipeng
/*
        if (secondInterval <= 6) {
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin(secondPos * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos(secondPos * Math.PI / 6)), pt);
            pt.setAlpha((int)(secondInterval * 85.0f / 4.0f + 255.0f / 2.0f));
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin((secondPos + 1) * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos((secondPos + 1) * Math.PI / 6)), pt);
            pt.setAlpha((int)(255.0f / 2.0f - secondInterval * 85.0f / 4.0f));
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin((secondPos - 1) * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos((secondPos - 1) * Math.PI / 6)), pt);
        } else if (secondInterval >= 24) {
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin((secondPos + 1) * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos((secondPos + 1) * Math.PI / 6)), pt);
            pt.setAlpha((int)(255.0f - (secondInterval - 24.0f) * 85.0f / 4.0f));
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin(secondPos * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos(secondPos * Math.PI / 6)), pt);
            pt.setAlpha((int)((secondInterval - 24.0f) * 85.0f / 4.0f));
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin((secondPos + 2) * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos((secondPos + 2) * Math.PI / 6)), pt);
        } else {
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin(secondPos * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos(secondPos * Math.PI / 6)), pt);
            canvas.drawLine(x + f1, y - f2,
                    (float)(x + (x - 4) * Math.sin((secondPos + 1) * Math.PI / 6)),
                    (float)(y - (x - 4) * Math.cos((secondPos + 1) * Math.PI / 6)), pt);
        }
*/
//liuqipeng

        canvas.restore();
        canvas.save();

        if (scaled) {
            canvas.restore();
        }
   }

    public static final String PREF_CTV_PAUSED  = "_ctv_paused";
    public static final String PREF_CTV_INTERVAL  = "_ctv_interval";
    public static final String PREF_CTV_INTERVAL_START = "_ctv_interval_start";
    public static final String PREF_CTV_CURRENT_INTERVAL = "_ctv_current_interval";
    public static final String PREF_CTV_ACCUM_TIME = "_ctv_accum_time";
    public static final String PREF_CTV_TIMER_MODE = "_ctv_timer_mode";
    public static final String PREF_CTV_MARKER_TIME = "_ctv_marker_time";

    // Since this view is used in multiple places, use the key to save different instances
    public void writeToSharedPref(SharedPreferences prefs, String key) {
        Log.d("mPaused = "+mPaused+", mIntervalTime = "+mIntervalTime+", mIntervalStartTime = "+mIntervalStartTime
                +", mCurrentIntervalTime = "+mCurrentIntervalTime+", mAccumulatedTime = "+mAccumulatedTime+", mMarkerTime = "
                +mMarkerTime+", mTimerMode = "+mTimerMode);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean (key + PREF_CTV_PAUSED, mPaused);
        editor.putLong (key + PREF_CTV_INTERVAL, mIntervalTime);
        editor.putLong (key + PREF_CTV_INTERVAL_START, mIntervalStartTime);
        editor.putLong (key + PREF_CTV_CURRENT_INTERVAL, mCurrentIntervalTime);
        editor.putLong (key + PREF_CTV_ACCUM_TIME, mAccumulatedTime);
        editor.putLong (key + PREF_CTV_MARKER_TIME, mMarkerTime);
        editor.putBoolean (key + PREF_CTV_TIMER_MODE, mTimerMode);
        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs, String key) {
        mPaused = prefs.getBoolean(key + PREF_CTV_PAUSED, false);
        mIntervalTime = prefs.getLong(key + PREF_CTV_INTERVAL, 0);
        mIntervalStartTime = prefs.getLong(key + PREF_CTV_INTERVAL_START, -1);
        mCurrentIntervalTime = prefs.getLong(key + PREF_CTV_CURRENT_INTERVAL, 0);
        mAccumulatedTime = prefs.getLong(key + PREF_CTV_ACCUM_TIME, 0);
        mMarkerTime = prefs.getLong(key + PREF_CTV_MARKER_TIME, -1);
        mTimerMode = prefs.getBoolean(key + PREF_CTV_TIMER_MODE, false);
        mAnimate = (mIntervalStartTime != -1 && !mPaused);
    }

    public void clearSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove (Stopwatches.PREF_START_TIME);
        editor.remove (Stopwatches.PREF_ACCUM_TIME);
        editor.remove (Stopwatches.PREF_STATE);
        editor.remove (key + PREF_CTV_PAUSED);
        editor.remove (key + PREF_CTV_INTERVAL);
        editor.remove (key + PREF_CTV_INTERVAL_START);
        editor.remove (key + PREF_CTV_CURRENT_INTERVAL);
        editor.remove (key + PREF_CTV_ACCUM_TIME);
        editor.remove (key + PREF_CTV_MARKER_TIME);
        editor.remove (key + PREF_CTV_TIMER_MODE);
        editor.apply();
    }
}
