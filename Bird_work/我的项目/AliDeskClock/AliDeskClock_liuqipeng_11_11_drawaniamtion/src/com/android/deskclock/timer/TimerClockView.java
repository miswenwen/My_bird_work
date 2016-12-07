package com.android.deskclock.timer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.Log;

import com.android.deskclock.R;
import com.android.deskclock.SetGroupView;
import com.android.deskclock.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.aliyun.ams.ta.TA;
import com.android.deskclock.DeskClock;

public class TimerClockView extends View {

    public boolean mInit = true;
    public TimerGroupView mSetGroup;
    public TextView mTimeTextView;
    /** The context */
    private Context mContext;
    public int clockTag = 0;
    public long time = 0;
    public long nowTime = 0;
    public long secondTime = 0;
    public long longTime = 0;

    /** The listener to listen for changes */
    public OnSeekChangeListener mListener;

    /** The color of the progress ring */
    private Paint circleColor;

    /** the color of the inside circle. Acts as background color */
    private Paint innerColor;

    /** The progress circle ring background */
    private Paint circleRing;

    /** The angle of progress */
    private float angle = 0;
    private float lastAngle = 0;
    /** The start angle (12 O'clock */
    private float startAngle = 0;

    /** The width of the progress ring */
    private int barWidth = 1;

    /** The width of the view */
    private int width;

    /** The height of the view */
    private int height;

    /** The maximum progress amount */
    private int maxProgress = 100;

    /** The current progress */
    private int progress;

    /** The progress percent */
    private int progressPercent;

    /** The radius of the inner circle */
    private float innerRadius;

    /** The radius of the outer circle */
    private float outerRadius;

    /** The circle's center X coordinate */
    private float cx;

    /** The circle's center Y coordinate */
    private float cy;

    /** The left bound for the circle RectF */
    private float left;

    /** The right bound for the circle RectF */
    private float right;

    /** The top bound for the circle RectF */
    private float top;

    /** The bottom bound for the circle RectF */
    private float bottom;

    /** The X coordinate for the top left corner of the marking drawable */
    private float dx;
//liuqipeng begin
	private float dxNew;
	private float dyNew;
	private int distanceNew=135;
	TimerFragment parentFrag;
//liuqipeng
    /** The Y coordinate for the top left corner of the marking drawable */
    private float dy;

    /** The X coordinate for the top left corner of the marking drawable */
    private float hdx;

    /** The Y coordinate for the top left corner of the marking drawable */
    private float hdy;

    /** The X coordinate for 12 O'Clock */
    private float startPointX;

    /** The Y coordinate for 12 O'Clock */
    private float startPointY;

    /**
     * The X coordinate for the current position of the marker, pre adjustment
     * to center
     */
    private float markPointX;

    /**
     * The Y coordinate for the current position of the marker, pre adjustment
     * to center
     */
    private float markPointY;

    private final static float STRKE_SIZE = (float)38/(float)3;
    
    /**
     * The adjustment factor. This adds an adjustment of the specified size to
     * both sides of the progress bar, allowing touch events to be processed
     * more user friendlily (yes, I know that's not a word)
     */
    private float adjustmentFactor = 3;

    /** The progress mark when the view is being progress modified. */
    private Bitmap progressMarkPressed;
//liuqipeng begin
	private Bitmap progressMarkPressedNew;
//liuqipeng 
    private Drawable mBackGround;
    //private Drawable minnerBackGround;

    /** The progress mark when the view isn't being progress modified */
    private Bitmap hourProgressMark;

    /** The progress mark when the view is being progress modified. */
    private Bitmap hourProgressMarkPressed;

    /** The flag to see if view is pressed */
    private boolean IS_PRESSED = false;

    /**
     * The flag to see if the setProgress() method was called from our own
     * View's setAngle() method, or externally by a user.
     */
    private boolean CALLED_FROM_ANGLE = false;

    /** The rectangle containing our circles and arcs. */
    private RectF rect = new RectF();
    private final RectF mArcRect = new RectF();
    private SharedPreferences mPrefs;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    boolean timeUp = false;
    {
        mListener = new OnSeekChangeListener() {

            @Override
            public void onProgressChange(TimerClockView view, int newProgress) {

            }
        };

        circleColor = new Paint();
        innerColor = new Paint();
        circleRing = new Paint();

        circleColor.setColor(Color.GRAY); // Set default
                                                                // progress
                                                                // color to holo
                                                                // blue.
        innerColor.setColor(Color.WHITE); // Set default background color to
                                            // black
        circleRing.setColor(Color.GRAY);// Set default background color to Gray

        circleColor.setAntiAlias(true);
        innerColor.setAntiAlias(true);
        circleRing.setAntiAlias(true);

        circleColor.setStrokeWidth(5);
        innerColor.setStrokeWidth(5);
        circleRing.setStrokeWidth(5);

        circleColor.setStyle(Paint.Style.FILL);
        setBackGroundColor(android.graphics.Color.parseColor("#FFFFFF"));
    }

    /**
     * Instantiates a new circular seek bar.
     * 
     * @param context
     *            the context
     * @param attrs
     *            the attrs
     * @param defStyle
     *            the def style
     */
//liuqipeng begin
	public void setFragment(TimerFragment frag){
	parentFrag= frag;
	}
//liuqipeng
    public TimerClockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initDrawable();
    }

    /**
     * Instantiates a new circular seek bar.
     * 
     * @param context
     *            the context
     * @param attrs
     *            the attrs
     */
    public TimerClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initDrawable();
    }

    /**
     * Instantiates a new circular seek bar.
     * 
     * @param context
     *            the context
     */
    public TimerClockView(Context context) {
        super(context);
        mContext = context;
        initDrawable();
    }
    
    public void SetTimeTextView(TextView timeTextView) {
        mTimeTextView = timeTextView;
    }

    private static float mMarkerStrokeSize = 2;
    private float mStrokeSize;
    private int mRedColor;
    private int mSecondColor;
    long mStartTime;
    /**
     * Inits the drawable.
     */
    public void initDrawable() {
        Resources resources = mContext.getResources();
        mBackGround = mContext.getResources().getDrawable(R.drawable.clock_alarm_view2_day_bg);
        //minnerBackGround = mContext.getResources().getDrawable(R.drawable.clock_timer_minute);
//liuqipeng begin
        //progressMarkPressed = BitmapFactory.decodeResource(mContext.getResources(),
        //        R.drawable.clock_timer_minute_point);
        progressMarkPressed = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.timer_follow_button);
        progressMarkPressedNew = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.timer_follow_button);		
//liuqipeng
        float dotDiameter = resources.getDimension(R.dimen.circletimer_dot_size);
        mMarkerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        mStrokeSize = resources.getDimension(R.dimen.circletimer_stroke_size);
        mRadiusOffset = Utils.calculateRadiusOffset(
                mStrokeSize, dotDiameter, mMarkerStrokeSize);
        mRedColor = resources.getColor(R.color.clock_red);
        mSecondColor = resources.getColor(R.color.clock_second);
        mStartTime = Utils.getTimeNow();
        mRadiusOffset = resources.getDimension(R.dimen.circletimer_radius_Offset);
        DisplayMetrics dm = new DisplayMetrics();  
        dm = resources.getDisplayMetrics();  
        mDensity  = dm.density;
        mHeight = dm.heightPixels;
        Log.d("TimerClockView - mHeight = "+mHeight+" mDensity="+mDensity);
        if (mHeight == 854) {
            mRadiusOffset = 10.0f;
        }
        //x = minnerBackGround.getIntrinsicWidth()/2;
        //y = minnerBackGround.getIntrinsicHeight()/2;
    }
    int x,y;
    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //width = getWidth(); // Get View Width
        //height = getHeight()*7/10;// Get View Height
        width = mBackGround.getIntrinsicWidth()* 2 /3;
        height = mBackGround.getIntrinsicHeight()* 2 /3;
        int size = (width > height) ? height : width; // Choose the smaller
                                                        // between width and
                                                        // height to make a
                                                        // square

        cx = mBackGround.getIntrinsicWidth() / 2; // Center X for circle
        cy = mBackGround.getIntrinsicHeight() / 2; // Center Y for circle
        outerRadius = size / 2; // Radius of the outer circle

        innerRadius = outerRadius - barWidth; // Radius of the inner circle

        /*left = cx - outerRadius; // Calculate left bound of our rect
        right = cx + outerRadius;// Calculate right bound of our rect
        top = cy - outerRadius;// Calculate top bound of our rect
        bottom = cy + outerRadius;// Calculate bottom bound of our rect

        startPointX = cx; // 12 O'clock X coordinate
        startPointY = cy - outerRadius;// 12 O'clock Y coordinate
        markPointX = startPointX;// Initial locatino of the marker X coordinate
        markPointY = startPointY;// Initial locatino of the marker Y coordinate
        //rect.set(left, top, right, bottom); // assign size to rect

        cx = mBackGround.getIntrinsicWidth() / 2;*/
        x = (int) cx;
        y = (int) cy;
        //Log.d("cx = "+cx+", cy = "+cy+", x = "+x+", y = "+y);
        /*android.view.ViewGroup.LayoutParams ps = mSetGroup.circle.getLayoutParams();
        ps.height = height;
        ps.width = width;
        mSetGroup.circle.setLayoutParams(ps);
        mSetGroup.circle.setTop((int)top);
        mSetGroup.circle.setLeft((int)left);
        mSetGroup.circle.setRight((int)right);
        mSetGroup.circle.setBottom((int)bottom);*/
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void resetView() {
        markPointX = startPointX;// Initial locatino of the marker X coordinate
        markPointY = startPointY;// Initial locatino of the marker Y coordinate
        angle = 0;
        lastAngle = 0;
        progress = 0;
        mListener.onProgressChange(this, this.getProgress());
        invalidate();
    }

    public void setView(int hour, int min) {
//        angle = min*6;
//        progress = angle*100/360;
//        Log.i("alarm","angle="+angle+"progress="+progress);
//        dx = (float) (cx+(outerRadius+innerRadius)/2*Math.cos(Math.PI/180*(angle-90)));
//        dy = (float) (cy+(outerRadius+innerRadius)/2*Math.sin(Math.PI/180*(angle-90)));
//        markPointX=dx+progressMarkPressed.getWidth();
//        markPointY=dy+progressMarkPressed.getHeight();
//        Log.i("alarm","angle="+angle+"progress="+progress);
//        mListener.onProgressChange(this, this.getProgress());
//        invalidate();
    }

    private float mRadiusOffset;
    private float mDensity;
    private int mHeight;
    private Paint mPaint = new Paint();
    private Paint mSecondPanit = new Paint();
    private LinearGradient linearGradient = null;
    private long mStart1 = 0;
    private long mStart2 = 0;
    private static final long GUIDE_TIME = 900000;
    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mAnimate) {
            if (!mRevert) {
                if (i <= temp) {
                    longTime = i;
                    secondTime = i/1000%60;
                    longTime = longTime - secondTime*1000;
                    time = longTime/1000%3600;
                    nowTime = longTime/1000-time;

                    long seconds, minutes, hours;
                    seconds = i / 1000;
                    minutes = seconds / 60;
                    seconds = seconds - minutes * 60;
                    hours = minutes / 60;
                    minutes = minutes - hours * 60;
                    if (hours > 999) {
                        hours = 0;
                    }

                    //angle = (float) ((longTime - nowTime*1000)*360/3600)/1000;
                    angle = (float) ((i)*360/3600)/1000;
                    lastAngle = angle;
                    mListener.onProgressChange(this, this.getProgress());
                    i += 60000;
                    mSetGroup.mTimerFragment.setEnabled(false);
                    mSetGroup.setDrawableEnable(false);
                } else {
                    i = 0;
                    mAnimate = false;
                    mSetGroup.mTimerFragment.setEnabled(true);
                    mSetGroup.setDrawableEnable(true);
                }
            } else {
                if (i >= temp) {
                    longTime = i;
                    secondTime = i/1000%60;
                    longTime = longTime - secondTime*1000;
                    time = longTime/1000%3600;
                    nowTime = longTime/1000-time;

                    long seconds, minutes, hours;
                    seconds = i / 1000;
                    minutes = seconds / 60;
                    seconds = seconds - minutes * 60;
                    hours = minutes / 60;
                    minutes = minutes - hours * 60;
                    if (hours > 999) {
                        hours = 0;
                    }
                    //angle = (float) ((longTime - nowTime*1000)*360/3600)/1000;
                    angle = (float) ((longTime - nowTime*1000 + secondTime*1000)*360/3600)/1000;
                    lastAngle = angle;
                    mListener.onProgressChange(this, this.getProgress());
                    i -= 60000;
                    mSetGroup.mTimerFragment.setEnabled(false);
                    mSetGroup.setDrawableEnable(false);
                } else {
                    i = 0;
                    mAnimate = false;
                    mSetGroup.mTimerFragment.setEnabled(true);
                    mSetGroup.setDrawableEnable(true);
                }
            }
        }

        if (mGuide) {
            if (mStart1 <= GUIDE_TIME) {
                longTime = mStart1;
                secondTime = mStart1/1000%60;
                longTime = longTime - secondTime*1000;
                time = longTime/1000%3600;
                nowTime = longTime/1000-time;

                long seconds, minutes, hours;
                seconds = mStart1 / 1000;
                minutes = seconds / 60;
                seconds = seconds - minutes * 60;
                hours = minutes / 60;
                minutes = minutes - hours * 60;
                if (hours > 999) {
                    hours = 0;
                }
                //angle = (float) ((longTime - nowTime*1000)*360/3600)/1000;
                angle = (float) ((mStart1)*360/3600)/1000;
                lastAngle = angle;
                mListener.onProgressChange(this, this.getProgress());
                mStart1 += 20000;
                //mStart2 = mStart1;
                //mSetGroup.mTimerFragment.setEnabled(false);
                //mSetGroup.setDrawableEnable(false);
            } else {
                mGuide = false;
                mStart1 = 0;
                mSetGroup.mTimerFragment.setEnabled(true);
                mSetGroup.setDrawableEnable(true);
            }
            /*else {
                if (mStart2 >= 0) {
                    longTime = mStart2;
                    secondTime = mStart2/1000%60;
                    longTime = longTime - secondTime*1000;
                    time = longTime/1000%3600;
                    nowTime = longTime/1000-time;

                    long seconds, minutes, hours;
                    seconds = mStart2 / 1000;
                    minutes = seconds / 60;
                    seconds = seconds - minutes * 60;
                    hours = minutes / 60;
                    minutes = minutes - hours * 60;
                    if (hours > 999) {
                        hours = 0;
                    }
                    //angle = (float) ((longTime - nowTime*1000)*360/3600)/1000;
                    angle = (float) ((mStart2)*360/3600)/1000;
                    lastAngle = angle;
                    mListener.onProgressChange(this, this.getProgress());
                    mStart2 -= 20000;
                } else {
                    mGuide = false;
                    mStart1 = 0;
                    mStart2 = 0;
                    mSetGroup.mTimerFragment.setEnabled(true);
                    mSetGroup.setDrawableEnable(true);
                }
            }*/
        }

        drawMarkerAtProgress(canvas);
        super.onDraw(canvas);
        if (mInit) {
            mInit = false;
        }
        if (mAnimate || mGuide) {
            invalidate();
        }
    }

    /**
     * Draw marker at the current progress point onto the given canvas.
     * 
     * @param canvas
     *            the canvas
     */
    public void drawMarkerAtProgress(Canvas canvas) {
        long nt = nowTime;
        long t = time;
        
        if (this.lastAngle-this.angle >= 180) {
            nt = nt + 60*60;
        } else if (this.lastAngle-this.angle <= -180) {
            nt = nt - 60*60;
        } else {
            //this.hourAngle = (this.hourAngle + (int)(30*(float)(this.angle-this.lastAngle)/180))%360;
            t = (long) (angle * 60*60 / 360);
            t = t - t%60;
        }
        if (mSetGroup.mTimerFragment.mTimerObjTag == null || mSetGroup.mTimerFragment.mTimerObjTag.mState != TimerObj.STATE_TIMESUP) {
            if (nt < 0) {
                time = 0;
                nowTime = 0;
                secondTime = 0;
                dx = (float) (cx-progressMarkPressed.getWidth()/2+(outerRadius+innerRadius)/2*Math.cos(Math.PI/180*(0-90)));
                dy = (float) (cy-progressMarkPressed.getHeight()/2+(outerRadius+innerRadius)/2*Math.sin(Math.PI/180*(0-90)));
                markPointX=dx+progressMarkPressed.getWidth();
                markPointY=dy+progressMarkPressed.getHeight();
//liuqipeng begin 
                //canvas.drawBitmap(progressMarkPressed, dx, dy, null);
				//canvas.drawBitmap(progressMarkPressed, dx+113, dy+113, null);
				dxNew=(float) (getWidth()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.cos(Math.PI/180*(0-90)));
				dyNew=(float) (getHeight()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.sin(Math.PI/180*(0-90)));
				canvas.drawBitmap(progressMarkPressedNew, dxNew, dyNew, null);
//liuqipeng
                long seconds, minutes, hours;
                seconds = (nowTime+time + secondTime);
                minutes = seconds / 60;
                seconds = seconds - minutes * 60;
                hours = minutes / 60;
                minutes = minutes - hours * 60;
                if (hours > 999) {
                    hours = 0;
                }

                String toText = ((hours<10)?"0":"")+hours+":"+((minutes<10)?"0":"")+minutes+":"+((seconds<10)?"0":"")+seconds;
                if (mTimeTextView.getText().toString().equals(toText)) {
                    mTimeTextView.setText(toText);
                }
                mSetGroup.mTimerFragment.mStartBtn.setVisibility(View.VISIBLE);
                //mSetGroup.mTimerFragment.mAddSecondBtn.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStopBtn.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mCancelBtn.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStartTitle.setVisibility(View.VISIBLE);
                //mSetGroup.mTimerFragment.mAddSecondTitle.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStopTitle.setVisibility(View.INVISIBLE);
                mSetGroup.mTimerFragment.mCancelTitle.setVisibility(View.GONE);

                mSetGroup.mTimerFragment.mStartBtn.setEnabled(false);
                mSetGroup.mTimerFragment.mStartTitle.setEnabled(false);
                mTimeTextView.setText(R.string.time_clock_start);
                drawcircle(canvas, 0.0f);
                if (mSetGroup.isMinutesSelect()) {
                    mSetGroup.resetDrawable();
                }
                return;
            }

            if (nt+t+secondTime > 356400+3540+59) {
                nowTime = 356400;
                time=3540;
                dx = (float) (cx-progressMarkPressed.getWidth()/2+(outerRadius+innerRadius)/2*Math.cos(Math.PI/180*(0-90)));
                dy = (float) (cy-progressMarkPressed.getHeight()/2+(outerRadius+innerRadius)/2*Math.sin(Math.PI/180*(0-90)));
                markPointX=dx+progressMarkPressed.getWidth();
                markPointY=dy+progressMarkPressed.getHeight();

//liuqipeng begin 
                //canvas.drawBitmap(progressMarkPressed, dx, dy, null);
				//canvas.drawBitmap(progressMarkPressed, dx+113, dy+113, null);
				dxNew=(float) (getWidth()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.cos(Math.PI/180*(0-90)));
				dyNew=(float) (getHeight()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.sin(Math.PI/180*(0-90)));
				canvas.drawBitmap(progressMarkPressedNew, dxNew, dyNew, null);
//liuqipeng
                long seconds, minutes, hours;
                seconds = (nowTime+time + secondTime);
                minutes = seconds / 60;
                seconds = seconds - minutes * 60;
                hours = minutes / 60;
                minutes = minutes - hours * 60;
                if (hours > 999) {
                    hours = 0;
                }
                String toText = ((hours<10)?"0":"")+hours+":"+((minutes<10)?"0":"")+minutes+":"+((seconds<10)?"0":"")+seconds;
                if (!mTimeTextView.getText().toString().equals(toText)) {
                    mTimeTextView.setText(toText);
                }
                drawcircle(canvas, 360.0f);
                return;
            }
        }
        dx = (float) (cx-progressMarkPressed.getWidth()/2+(outerRadius+innerRadius)/2*Math.cos(Math.PI/180*(angle-90)));
        dy = (float) (cy-progressMarkPressed.getHeight()/2+(outerRadius+innerRadius)/2*Math.sin(Math.PI/180*(angle-90)));
        markPointX=dx+progressMarkPressed.getWidth();
        markPointY=dy+progressMarkPressed.getHeight();

        time = t;
        nowTime = nt;
        drawcircle(canvas, this.angle);

        this.lastAngle = this.angle;

        if (!timeUp) {
//liuqipeng begin 
                //canvas.drawBitmap(progressMarkPressed, dx, dy, null);
				//canvas.drawBitmap(progressMarkPressed, dx+113, dy+113, null);
				dxNew=(float) (getWidth()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.cos(Math.PI/180*(angle-90)));
				dyNew=(float) (getHeight()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.sin(Math.PI/180*(angle-90)));
				//canvas.drawBitmap(progressMarkPressedNew, dxNew, dyNew, null);
				canvas.save();
				canvas.rotate(angle,getWidth()/2,getHeight()/2);
				canvas.drawBitmap(progressMarkPressedNew,(float) (getWidth()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.cos(Math.PI/180*(0-90))),(float) (getHeight()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.sin(Math.PI/180*(0-90))),null);
				canvas.restore();
//liuqipeng
        } else {
            dx = (float) (cx-progressMarkPressed.getWidth()/2+(outerRadius+innerRadius)/2*Math.cos(Math.PI/180*(0-90)));
            dy = (float) (cy-progressMarkPressed.getHeight()/2+(outerRadius+innerRadius)/2*Math.sin(Math.PI/180*(0-90)));
            markPointX=dx+progressMarkPressed.getWidth();
            markPointY=dy+progressMarkPressed.getHeight();
//liuqipeng begin 
                //canvas.drawBitmap(progressMarkPressed, dx, dy, null);
				//canvas.drawBitmap(progressMarkPressed, dx+113, dy+113, null);
				dxNew=(float) (getWidth()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.cos(Math.PI/180*(0-90)));
				dyNew=(float) (getHeight()/2-progressMarkPressedNew.getWidth()/2+(outerRadius+distanceNew)*Math.sin(Math.PI/180*(0-90)));
				canvas.drawBitmap(progressMarkPressedNew, dxNew, dyNew, null);
//liuqipeng
        }
        long seconds, minutes, hours;
        if (mAnimate) {
            seconds = i / 1000;
        } else {
            seconds = (nowTime+time + secondTime);
        }
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }
        String toText;
        if (!mGuide) {
            toText = ((hours<10)?"0":"")+hours+":"+((minutes<10)?"0":"")+minutes+":"+((seconds<10)?"0":"")+seconds;
        } else {
            toText = ((hours<10)?"0":"")+hours+":"+((minutes<10)?"0":"")+minutes+":"+("00");
        }
        
        if (!mTimeTextView.getText().toString().equals(toText)) {
            mTimeTextView.setText(toText);
        }

        /*if (timeUp) {
            mTimeTextView.setText("-"+toText);
        }*/
        if (mSetGroup.mTimerFragment.isFullScreen != true) {
            if (time+nowTime+secondTime == 0 && angle == 0) {
                mSetGroup.mTimerFragment.mStartBtn.setVisibility(View.VISIBLE);
                //mSetGroup.mTimerFragment.mAddSecondBtn.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStopBtn.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mCancelBtn.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStartTitle.setVisibility(View.VISIBLE);
                //mSetGroup.mTimerFragment.mAddSecondTitle.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStopTitle.setVisibility(View.INVISIBLE);
                mSetGroup.mTimerFragment.mCancelTitle.setVisibility(View.GONE);
                mSetGroup.mTimerFragment.mStartBtn.setEnabled(false);
                mSetGroup.mTimerFragment.mStartTitle.setEnabled(false);
            } else {
                if (!mAnimate && !mGuide) {
                    mSetGroup.mTimerFragment.mStartBtn.setEnabled(true);
                    mSetGroup.mTimerFragment.mStartTitle.setEnabled(true);
                }
            }
        }
    }

    private void drawcircle(Canvas canvas, float angle){
//liuqipeng
        //float radius = Math.min(x, y) - mRadiusOffset;
		float radius = Math.min(x, y) - mRadiusOffset+5;
//liuqipeng
        //draw a combination of red and white arcs to create a circle
//liuqipeng
		int xx=getWidth()/2;
		int yy=getHeight()/2;
		mArcRect.top = yy - radius;
        mArcRect.bottom = yy + radius;
        mArcRect.left =  xx - radius;
        mArcRect.right = xx + radius;

       // mArcRect.top = y - radius;
       // mArcRect.bottom = y + radius;
       // mArcRect.left =  x - radius;
       // mArcRect.right = x + radius;
//liuqipeng
        float angleTemp = angle;
        /*if (nowTime < 3600 && angleTemp < 1.0f) {
            angleTemp = 1.0f;
        }*/
        float redPercent = (float)angleTemp/360.0f;
        //Log.d("radius = "+radius+",    redPercent = "+redPercent + ", x = "+x+", y = "+y+",mRadiusOffset = "+mRadiusOffset
                //+", nowTime = "+nowTime);
        linearGradient = new LinearGradient(x, y - radius, (float)(x + radius * Math.sin(2.0f * redPercent * Math.PI)),
                (float)(y - radius * Math.cos(2.0f * redPercent * Math.PI)), Color.TRANSPARENT,
                mContext.getResources().getColor(R.color.clock_red), Shader.TileMode.CLAMP);
        //if (nowTime < 7200) {
            // prevent timer from doing more than one full circle
        //redPercent =  nowTime >= 3600 ? 1 : redPercent;
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
//liuqipeng 注释
        //mPaint.setShader(linearGradient);
//liuqipeng
        if (mHeight == 854) {
            mPaint.setStrokeWidth(20);
        } else {
//liuqipeng
            //mPaint.setStrokeWidth(STRKE_SIZE * mDensity);
			mPaint.setStrokeWidth(22);
//liuqipeng
        }
        // draw red arc here
//liuqipeng begin
        //mPaint.setColor(mRedColor);
		mPaint.setColor(Color.WHITE);
//liuqipeng
        //if (mTimerMode){
            //canvas.drawArc (mArcRect, 270, - redPercent * 360 , false, mPaint);
        //} else {
        canvas.drawArc (mArcRect, 270, + redPercent * 360 , false, mPaint);
        Paint circlePaint = new Paint();
        circlePaint.setColor(mRedColor);
        circlePaint.setAntiAlias(true);
        if (redPercent <= 0.01f) {
            circlePaint.setAlpha(0);
        }
//liuqipeng 注释
        //canvas.drawCircle((float)(x + radius * Math.sin(2.0f * redPercent * Math.PI)),
        //        (float)(y - radius * Math.cos(2.0f * redPercent * Math.PI)),
       //         mPaint.getStrokeWidth() / 2.0f, circlePaint);
//liuqipeng

        /*if (nowTime >= 3600) {
            float secondPercent = (float)angleTemp/360.0f;
            secondPercent =  nowTime >= 7200 ? 1 : secondPercent;
            mSecondPanit.setAntiAlias(true);
            mSecondPanit.setStyle(Paint.Style.STROKE);
            if (mHeight == 854) {
                mSecondPanit.setStrokeWidth(17);
            } else {
                mSecondPanit.setStrokeWidth(STRKE_SIZE * mDensity);
            }
            mSecondPanit.setColor(mSecondColor);
            canvas.drawArc (mArcRect, 270, + secondPercent * 360 , false, mSecondPanit);
        }*/
    }

    /**
     * Gets the X coordinate of the arc's end arm's point of intersection with
     * the circle
     * 
     * @return the X coordinate
     */
    public float getXFromAngle() {
        int adjust = progressMarkPressed.getWidth();
        float x = markPointX - (adjust / 2);
        return x;
    }

    /**
     * Gets the Y coordinate of the arc's end arm's point of intersection with
     * the circle
     * 
     * @return the Y coordinate
     */
    public float getYFromAngle() {
        int adjust = progressMarkPressed.getHeight();
        float y = markPointY - (adjust / 2);
        return y;
    }

    /**
     * Get the angle.
     * 
     * @return the angle
     */
    public float getAngle() {
        return angle;
    }

    /**
     * Set the angle.
     * 
     * @param angle
     *            the new angle
     */
    public void setAngle(float angle) {
    	this.angle = angle;
        float donePercent = (((float) this.angle) / 360) * 100;
        float progress = (donePercent / 100) * getMaxProgress();
        setProgressPercent(Math.round(donePercent));
        CALLED_FROM_ANGLE = true;
        setProgress(Math.round(progress));
    }

    /**
     * Sets the seek bar change listener.
     * 
     * @param listener
     *            the new seek bar change listener
     */
    public void setSeekBarChangeListener(OnSeekChangeListener listener) {
        mListener = listener;
    }

    /**
     * Gets the seek bar change listener.
     * 
     * @return the seek bar change listener
     */
    public OnSeekChangeListener getSeekBarChangeListener() {
        return mListener;
    }

    /**
     * Gets the bar width.
     * 
     * @return the bar width
     */
    public int getBarWidth() {
        return barWidth;
    }

    /**
     * Sets the bar width.
     * 
     * @param barWidth
     *            the new bar width
     */
    public void setBarWidth(int barWidth) {
        this.barWidth = barWidth;
    }

    /**
     * The listener interface for receiving onSeekChange events. The class that
     * is interested in processing a onSeekChange event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's
     * <code>setSeekBarChangeListener(OnSeekChangeListener)<code> method. When
     * the onSeekChange event occurs, that object's appropriate
     * method is invoked.
     * 
     * @see OnSeekChangeEvent
     */
    public interface OnSeekChangeListener {

        /**
         * On progress change.
         * 
         * @param view
         *            the view
         * @param newProgress
         *            the new progress
         */
        public void onProgressChange(TimerClockView view, int newProgress);
    }

    /**
     * Gets the max progress.
     * 
     * @return the max progress
     */
    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * Sets the max progress.
     * 
     * @param maxProgress
     *            the new max progress
     */
    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    /**
     * Gets the progress.
     * 
     * @return the progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Sets the progress.
     * 
     * @param progress
     *            the new progress
     */
    public void setProgress(int progress) {
        if (this.progress != progress) {
            this.progress = progress;
            if (!CALLED_FROM_ANGLE) {
                int newPercent = (this.progress / this.maxProgress) * 100;
                int newAngle = (newPercent / 100) * 360;
                this.setAngle(newAngle);
                this.setProgressPercent(newPercent);
            }
            mListener.onProgressChange(this, this.getProgress());
            CALLED_FROM_ANGLE = false;
        }
    }

    /**
     * Gets the progress percent.
     * 
     * @return the progress percent
     */
    public int getProgressPercent() {
        return progressPercent;
    }

    /**
     * Sets the progress percent.
     * 
     * @param progressPercent
     *            the new progress percent
     */
    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Sets the ring background color.
     * 
     * @param color
     *            the new ring background color
     */
    public void setRingBackgroundColor(int color) {
        circleRing.setColor(color);
    }

    /**
     * Sets the back ground color.
     * 
     * @param color
     *            the new back ground color
     */
    public void setBackGroundColor(int color) {
        innerColor.setColor(color);
    }

    /**
     * Sets the progress color.
     * 
     * @param color
     *            the new progress color
     */
    public void setProgressColor(int color) {
        circleColor.setColor(color);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
//  @Override  
//  public boolean dispatchTouchEvent(MotionEvent event) { 
        float x = event.getX();
        float y = event.getY();
        boolean up = false;

        /*if (mSetGroup.mTimerFragment.mTimerObjTag != null &&
                (mSetGroup.mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_RUNNING 
                || mSetGroup.mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_TIMESUP
                || mSetGroup.mTimerFragment.mTimerObjTag.mState == TimerObj.STATE_DONE
                || mAnimate
                || mGuide)) {
        	return true;
        }*/
        if ((mPrefs != null && mPrefs.getBoolean("isPauseStatus",false))|| (mPrefs != null && mPrefs.getBoolean("isRunning",false)) || mAnimate || mGuide) {
            return true;
        }

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
//liuqipeng begin
            //if (Math.abs(x - dx-progressMarkPressed.getWidth()) < progressMarkPressed.getWidth()*2 && Math.abs(y - dy-progressMarkPressed.getHeight()) < progressMarkPressed.getHeight()*2) {
            if (Math.abs(x - dxNew-progressMarkPressed.getWidth()) < progressMarkPressed.getWidth()*2 && Math.abs(y - dyNew-progressMarkPressed.getHeight()) < progressMarkPressed.getHeight()*2) {
//liuqipeng beging
                clockTag = 1;
                TA.getInstance().getDefaultTracker().commitEvent("Page_TimerFragment",
                        2101, "control_setTimewithball", null, null, null);
                if (mSetGroup.mTimerFragment.isFullScreen != true) {
                     DeskClock deskClock = (DeskClock) mSetGroup.mTimerFragment.getActivity();
                     if (deskClock != null && deskClock.mViewPager != null) {
                         deskClock.mViewPager.setScrollable(false);
                     }
                 }
            } else {
                clockTag = 0;
                return true;
            }
            moved(x, y, up);
            break;
        case MotionEvent.ACTION_MOVE:
            if (clockTag == 0) {
                return true;
            }
            if (mSetGroup.isMinutesSelect()) {
                mSetGroup.resetDrawable();
            }
//liuqipeng begin
			if(mSetGroup.mTimerFragment.isListViewItemSelected()){
				mSetGroup.mTimerFragment.invalidateListView();
			}				
//liuqipeng
            moved(x, y, up);
            break;
        case MotionEvent.ACTION_UP:
            if (mSetGroup.mTimerFragment.isFullScreen != true) {
                DeskClock deskClock = (DeskClock) mSetGroup.mTimerFragment.getActivity();
                if (deskClock != null && deskClock.mViewPager != null) {
                    deskClock.mViewPager.setScrollable(true);
                }
            }
            if (clockTag == 0) {
                return true;
            }
            up = true;
            moved(x, y, up);
            clockTag = 0;
            break;
        }
        return true;
    }

    /**
     * Moved.
     * 
     * @param x
     *            the x
     * @param y
     *            the y
     * @param up
     *            the up
     */
    private void moved(float x, float y, boolean up) {
        float distance = (float) Math.sqrt(Math.pow((x - cx), 2) + Math.pow((y - cy), 2));
        if (!up) {
            IS_PRESSED = true;
            if (clockTag == 1) {
                markPointX = cx+(outerRadius+innerRadius)/2/distance*(x-cx);
                markPointY = cy+(outerRadius+innerRadius)/2/distance*(y-cy);
            }
//liuqipeng begin
            //float degrees = (float) ((float) ((Math.toDegrees(Math.atan2(x - cx, cy - y)) + 360.0)) % 360.0);
			float degrees = (float) ((float) ((Math.toDegrees(Math.atan2(x - getWidth()/2, getHeight()/2 - y)) + 360.0)) % 360.0);
//liuqipeng
            // and to make it count 0-360
            if (degrees < 0) {
                degrees += 2 * Math.PI;
            }

            setAngle(degrees);
            invalidate();

        } else {
            IS_PRESSED = false;
            float angleTemp = (float) ((time*1000 + secondTime*1000)*360/3600)/1000;
            setAngle(angleTemp);
            invalidate();
        }
    }

    /**
     * Gets the adjustment factor.
     * 
     * @return the adjustment factor
     */
    public float getAdjustmentFactor() {
        return adjustmentFactor;
    }

    /**
     * Sets the adjustment factor.
     * 
     * @param adjustmentFactor
     *            the new adjustment factor
     */
    public void setAdjustmentFactor(float adjustmentFactor) {
        this.adjustmentFactor = adjustmentFactor;
    }

    public long getTime() {
        return nowTime+time+secondTime;
    }

    public void setTime(long t){
        if (t < 0) {
            timeUp = true;
            t = -t;
        } else {
            timeUp = false;
        }

        longTime = t;
        secondTime = t/1000%60;
        longTime = longTime - secondTime*1000;
        time = longTime/1000%3600;
        nowTime = longTime/1000-time;

        long seconds, minutes, hours;
        seconds = t / 1000;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }

        angle = (float) ((longTime - nowTime*1000 + secondTime*1000)*360/3600)/1000;
        lastAngle = angle;
        mListener.onProgressChange(this, this.getProgress());
        invalidate();
    }

    private boolean mAnimate = false;
    private long temp;
    private long i;
    private boolean mRevert = false;
    private boolean mGuide = false;

    public void startIntervalAnimation(long t) {
        mAnimate = true;
        i = getTime() * 1000;
        if (i == t) {
            mAnimate = false;
            return;
        }
        i -= i % 60000;
        if (i > 3600000) {
            i = 3600000 + i % 3600000;
        }
        temp = t;
        Log.d("temp = "+temp+", i = "+i);
        if (temp < i){
            mRevert = true;
        } else {
            mRevert = false;
        }
        if (t < 0) {
            timeUp = true;
            t = -t;
        } else {
            timeUp = false;
        }
        invalidate();
    }

    public void startGuide() {
        mGuide = true;
        mSetGroup.mTimerFragment.setEnabled(false);
        mSetGroup.setDrawableEnable(false);
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, 500);
        //invalidate();
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

    public void setGroupView(TimerGroupView groupView) {
        mSetGroup = groupView;
        if (mSetGroup.mTimerFragment.isFullScreen) {
            circleColor.setColor(Color.TRANSPARENT);
            innerColor.setColor(Color.TRANSPARENT);
            circleRing.setColor(Color.TRANSPARENT);
        } else {
            circleColor.setColor(Color.GRAY);
            innerColor.setColor(mContext.getResources().getColor(R.color.timer_clock_background));
            circleRing.setColor(Color.GRAY);
            circleColor.setStyle(Paint.Style.FILL);
            setBackGroundColor(mContext.getResources().getColor(R.color.timer_clock_background));
        }
    }
}
