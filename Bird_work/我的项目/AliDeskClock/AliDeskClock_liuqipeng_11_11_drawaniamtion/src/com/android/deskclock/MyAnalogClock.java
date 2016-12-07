package com.android.deskclock;

import java.util.Calendar;
import java.util.TimeZone;
//liuqipeng begin
import android.content.res.Resources;
import android.util.Log;
//liuqipeng

import com.android.deskclock.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AnalogClock;
import android.os.Handler;
import android.os.Message;

public class MyAnalogClock extends AnalogClock implements Handler.Callback {

    private final static String TAG = "MyAnalogClock";
    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;
    private Drawable mDot;
//liuqipeng begin
private Drawable mDialNum;
private Drawable mDialLeft;
private Drawable mDialRight;
private Drawable mHourIndex;
private Drawable mMinuteIndex;
private Drawable mDotIndex;
private float mViewAlpha=0.0f;
private float alreadyDrawAngle=0;
private boolean nowDrawClockAnimation=true;
//liuqipeng

    private boolean mAttached;
    private int mDialWidth;
    private int mDialHeight;
    private final Handler mHandler = new Handler();
    private float mHour;
    private float mMinutes;
    private float mSeconds;
    private int handOffset;
    private boolean mIsTimeChanged;
    private Time mCalendar;
    private ClockTickListener mListener;
    private Handler mUpdater;
    private Context mContext;

    private boolean mIsFlagUpdate = true;

    public void updateTimeIndicator(boolean isUpdate) {
        if (isUpdate) {
            mIsFlagUpdate = true;
            mUpdater.sendEmptyMessage(0);
        } else {
            mIsFlagUpdate = false;
            //setVisibility(View.INVISIBLE);
        }
    }
    public Time getmCalendar() {
        return mCalendar;
    }

    public void setmCalendar(Time mCalendar) {
        this.mCalendar = mCalendar;
    }
//liuqipeng begin
	public void enableClockAnimation(){
	nowDrawClockAnimation=true;
	}
//liuqipeng

    public float getmHour() {
        return mHour;
    }

    public void setmHour(float mHour) {
        this.mHour = mHour;
    }

    public float getmMinutes() {
        return mMinutes;
    }

    public void setmMinutes(float mMinutes) {
        this.mMinutes = mMinutes;
    }

    public interface ClockTickListener {
        public void clockTick();
    }

    public void setClockTickListener(ClockTickListener listener) {
        this.mListener = listener;
    }

    public MyAnalogClock(Context context) {
        this(context, null);
    }

    public MyAnalogClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDialDrawable(Context context, int resid) {
        if (resid != 0) {
            Log.i(TAG, "setDialDrawable");
            mDial = null;
            mDial = context.getResources().getDrawable(resid);
        }
    }

    public void setHourHandDrawable(Context context, int resid) {
        if (resid != 0) {
            Log.i(TAG, "setHourHandDrawable");
            mHourHand = null;
            mHourHand = context.getResources().getDrawable(resid);
        }
    }

    public void setMinuteHandDrawable(Context context, int resid) {
        if (resid != 0) {
            Log.i(TAG, "setMinuteHandDrawable");
            mMinuteHand = null;
            mMinuteHand = context.getResources().getDrawable(resid);
        }
    }

    /*aihua.shenah remove seconds,becasue of issues of performance.*/
    public void setSecondHandDrawable(Context context, int resid) {
        if (resid != 0) {
            Log.i(TAG, "setSecondHandDrawable");
            mSecondHand = null;
            mSecondHand = context.getResources().getDrawable(resid);
        }
    }

    public void setDotDrawable(Context context, int resid) {
        if (resid != 0) {
            Log.i(TAG, "setDotDrawable");
            mDot = null;
            mDot = context.getResources().getDrawable(resid);
        }
    }

    public MyAnalogClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.my_analog_clock);
//liuqipeng begin
		Resources mRes=context.getResources();
		mDialNum=mRes.getDrawable(R.drawable.clock_alarm_number);
		mDialLeft=mRes.getDrawable(R.drawable.clockl_left);
		mDialRight=mRes.getDrawable(R.drawable.clockl_right);
		mHourIndex=mRes.getDrawable(R.drawable.clock_hour_index);
		mMinuteIndex=mRes.getDrawable(R.drawable.clock_minute_index);
		mDotIndex=mRes.getDrawable(R.drawable.clock_dot_index);
//liuqipeng
        mDial = a.getDrawable(R.styleable.my_analog_clock_dial);
        mHourHand = a.getDrawable(R.styleable.my_analog_clock_hand_hour);
        mMinuteHand = a.getDrawable(R.styleable.my_analog_clock_hand_minute);
        /*aihua.shenah remove seconds,becasue of issues of performance.*/
        mSecondHand = a.getDrawable(R.styleable.my_analog_clock_hand_second);
        mDot = a.getDrawable(R.styleable.my_analog_clock_dot);
        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();
        //handOffset = (int)getResources().getDimension(R.dimen.minute_and_hour_Hand_offset);
        handOffset = 0;
        mCalendar = new Time();
        mUpdater = new Handler(this);
    }

    // TY ZhangYan 20110118 update
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null,
                    mHandler);
        }
        onTimeChanged();
/*aihua.shenah remove seconds,becasue of issues of performance.*/
        mUpdater.sendEmptyMessage(0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private Calendar mWorkCalendar;
    private void onTimeChanged() {
        if (mCalendar != null) {
            mCalendar.setToNow();
        }
        mWorkCalendar = Calendar.getInstance();
        mWorkCalendar.set(Calendar.SECOND, 0);
        mWorkCalendar.set(Calendar.MILLISECOND, 0);
        mTimeMillis = mWorkCalendar.getTimeInMillis();
        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;
        //Log.i(TAG, "second:" + second + " minute:" + minute + " hour:" + hour);
        mSeconds = second;
        mMinutes = minute;
        mHour = hour + mMinutes / 60.0f;
        mIsTimeChanged = true;
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

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mIsTimeChanged = true;
    }

    protected void onDraw(Canvas canvas) {

        // super.onDraw(canvas);
        boolean changed = mIsTimeChanged;
        if (changed) {
            mIsTimeChanged = false;
        }

        int availableWidth = getRight() - getLeft();
        int availableHeight = getBottom() - getTop();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();
        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min((float) availableWidth / (float) w,
                    (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);
//liuqipeng begin 画闹钟左右边的角
    	final Drawable dialNum = mDialNum;
        int ww = dialNum.getIntrinsicWidth();
        int hh = dialNum.getIntrinsicHeight();
/*
        if (availableWidth < ww || availableHeight < hh) {
            float scale = Math.min((float) availableWidth / (float) ww,
                    (float) availableHeight / (float) hh);
        }
*/
        if (changed) {
            dialNum.setBounds(x - (ww / 2), y - (hh / 2), x + (ww / 2), y + (hh / 2));
        }
        dialNum.draw(canvas);

		final Drawable dialLeft = mDialLeft;
		final Drawable dialRight = mDialRight;
		int wLeft = dialLeft.getIntrinsicWidth();
		int hLeft = dialLeft.getIntrinsicHeight();
		int wRight = dialRight.getIntrinsicWidth();
		int hRight = dialRight.getIntrinsicHeight();
		float xx=1.0f-getAlpha();
		int translate=(int)(44*xx);
		dialLeft.setBounds(x - 186 - wLeft + translate, y - (h / 2)+ translate, x - 186+ translate, y - (h / 2) + hLeft+translate);
		dialRight.setBounds(x + 186 -translate, y - (h / 2) + translate , x + 186 + wRight- translate, y - (h / 2) + hRight+translate);
		dialLeft.draw(canvas);
		dialRight.draw(canvas);
//liuqipeng
        canvas.save();

        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            //Log.i(TAG, "minuteHand w:" + w + " h:" + h);
            minuteHand.setBounds(x - (w / 2), y - h + handOffset, x + (w / 2), y + handOffset);
        }
        //minuteHand.draw(canvas);

        int hourWidth = getResources().getDimensionPixelSize(R.dimen.hour_hand_width);
        Paint hourPaint = new Paint();
        hourPaint.setStyle(Paint.Style.STROKE);
        hourPaint.setStrokeWidth(hourWidth);
        hourPaint.setColor(Color.WHITE);
        hourPaint.setAntiAlias(true);

        Paint secondPaint = new Paint();
        secondPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.connect_line_width));
        secondPaint.setColor(Color.WHITE);
        secondPaint.setAntiAlias(true);
        secondPaint.setAlpha(154);

        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.WHITE);
        circlePaint.setAntiAlias(true);
//liuqipeng begin 注销
       // canvas.drawCircle(x, y, hourWidth / 2.0f, circlePaint);
//liuqipeng

        int minuteH = getResources().getDimensionPixelSize(R.dimen.minute_hand_radius);
        int hourH = getResources().getDimensionPixelSize(R.dimen.hour_hand_radius);
        mHour = mHour >= 12.0f ? mHour - 12.0f : mHour;
        //Log.i(TAG, "second:" + mSeconds + " minute:" + mMinutes + " hour:" + mHour);
        float sinMinutes = (float) Math.sin(mMinutes / 30.0f * Math.PI);
        float cosMinutes = (float) Math.cos(mMinutes / 30.0f * Math.PI);
        float sinHour = (float) Math.sin(mHour / 6.0f * Math.PI);
        float cosHour = (float) Math.cos(mHour / 6.0f * Math.PI);
//liuqipeng begin 注销
        /*canvas.drawLine(x, y, x + minuteH * sinMinutes,
                y - minuteH * cosMinutes, hourPaint);

        canvas.drawCircle(x + minuteH * sinMinutes, y - minuteH * cosMinutes,
                hourWidth / 2.0f, circlePaint);

        canvas.drawLine(x, y, x + hourH * sinHour,
                y - hourH * cosHour, hourPaint);

        canvas.drawCircle(x + hourH * sinHour, y - hourH * cosHour,
                hourWidth / 2.0f, circlePaint);*/
//liuqipeng
//liuqipeng begin 画闹钟的dot，时针和分针
		final Drawable dotIndex=mDotIndex;
		final Drawable hourIndex=mHourIndex;
		final Drawable minuteIndex=mMinuteIndex;
		int wDot = dotIndex.getIntrinsicWidth();
        int hDot = dotIndex.getIntrinsicHeight();
		//设置动态透明度
		float currentAlpha=Math.abs(alreadyDrawAngle/183-1);
		dotIndex.setAlpha((int)(currentAlpha*250));
		hourIndex.setAlpha((int)(currentAlpha*250));
		minuteIndex.setAlpha((int)(currentAlpha*250));
		dotIndex.setBounds(x-(wDot/2),y-(hDot/2),x+(wDot/2),y+(hDot/2));
		dotIndex.draw(canvas);
		int wHour = hourIndex.getIntrinsicWidth();
        int hHour = hourIndex.getIntrinsicHeight();
		int wMinute = minuteIndex.getIntrinsicWidth();
        int hMinute = minuteIndex.getIntrinsicHeight();
		if(nowDrawClockAnimation&&alreadyDrawAngle!=0)
		Log.e("liuqipengangle", "currentangle"+alreadyDrawAngle);
		if(nowDrawClockAnimation){
			//画动画
			//画时针
			canvas.save();
			canvas.rotate((mHour / 6.0f*180)+alreadyDrawAngle,x,y);
			hourIndex.setBounds(x-(wHour/2),y-(hHour/2),x+(wHour/2),y+(hHour/2));
			hourIndex.draw(canvas);
			canvas.restore();
			//画分针
			canvas.save();
			canvas.rotate((mMinutes / 30.0f*180)+alreadyDrawAngle,x,y);
			minuteIndex.setBounds(x-(wMinute/2),y-(hMinute/2),x+(wMinute/2),y+(hMinute/2));
			minuteIndex.draw(canvas);
			canvas.restore();
		}
		else {
			//画时针
			canvas.save();
			canvas.rotate(mHour / 6.0f * 180,x,y);
			hourIndex.setBounds(x-(wHour/2),y-(hHour/2),x+(wHour/2),y+(hHour/2));
			hourIndex.draw(canvas);
			canvas.restore();
			//画分针
			canvas.save();
			canvas.rotate(mMinutes / 30.0f  * 180,x,y);
			minuteIndex.setBounds(x-(wMinute/2),y-(hMinute/2),x+(wMinute/2),y+(hMinute/2));
			minuteIndex.draw(canvas);
			canvas.restore();
		}
//liuqipeng

        //canvas.drawLine(x + minuteH * sinMinutes, y - minuteH * cosMinutes,
          //      x + hourH * sinHour, y - hourH * cosHour, secondPaint);

        // canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            //Log.i(TAG, "hourHand w:" + w + " h:" + h);
            hourHand.setBounds(x - (w / 2), y - h + handOffset, x + (w / 2), y + handOffset);
        }
//liuqipeng begin 注销
/*
        int secondPos = (int) (mWorkSecond / 60000.0f * 360.0f) / 30;
        final Drawable secondHand = mSecondHand;
        if (changed) {
            w = secondHand.getIntrinsicWidth();
            h = secondHand.getIntrinsicHeight();
            secondHand.setBounds(x - (w / 2), y - h, x + (w / 2), y
                    );
        }

        Paint pt = new Paint();
        pt.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.second_hand_width));
        pt.setAntiAlias(true);
        pt.setColor(Color.WHITE);

        int secondRadius = getResources().getDimensionPixelSize(R.dimen.second_hand_radius);

        float f1 = (float)(secondRadius * Math.sin(mWorkSecond / 30000.0f * Math.PI));
        float f2 = (float)(secondRadius * Math.cos(mWorkSecond / 30000.0f * Math.PI));
        int secondInterval = (int)(mWorkSecond / 60000.0f * 360.0f) % 30;
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

        if (scaled) {
            canvas.restore();
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }
            onTimeChanged();
            invalidate();
            //mListener.clockTick();
        }
    };

    // TY ZhangYan 20110118 add
    public void changeTimeOnWindow(Time time) {
        mCalendar = time;
        onTimeChanged();
        // invalidate();
    }

    private float mWorkSecond = 0;
    private long mCurrentTimeMillis,mTimeMillis;
    /*aihua.shenah remove seconds,becasue of issues of performance.*/
    @Override
    public boolean handleMessage(Message message) {
        if (mIsFlagUpdate) {
            mUpdater.removeMessages(0);
            mWorkCalendar = Calendar.getInstance();
            mCurrentTimeMillis = mWorkCalendar.getTimeInMillis();
            mWorkSecond = mCurrentTimeMillis - mTimeMillis;

            if (mCalendar != null) {
                mCalendar.setToNow();
            }
            mSeconds = mCalendar.second;
            mIsTimeChanged = true;
            invalidate();
            mUpdater.sendEmptyMessageDelayed(0, 10);
//liuqipeng begin
			if(nowDrawClockAnimation){
				alreadyDrawAngle=alreadyDrawAngle+6;//画一次增加一次
				if(alreadyDrawAngle>364){
					alreadyDrawAngle=0;
					nowDrawClockAnimation=false;
				}
			}
//liuqipeng
            //if(!isVisibleToUser())
              //setVisibility(View.VISIBLE);
        }
        return true;
    }
}
