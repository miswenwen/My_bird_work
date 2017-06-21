/*
 * Author:Wang Lei
 */

package com.bird.ninekeylock;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.bird.accesscontrol.R;
import com.bird.widget.MyView;

public class NineKeyLockView extends MyView {
	private static final int MAX_TIMER_COUNT = 50;
	private boolean isCanInput = true;
	private boolean isShowGreen = true;
	private boolean makePatternVisible = true;
	private int mBitmapHeight;
	private int mBitmapWidth;
	private int timerCount = MAX_TIMER_COUNT;
	private float mX = -1;
	private float mY = -1;
	private ArrayList<Integer> mPattern = new ArrayList<Integer>();
	private Bitmap mBitmapArrowGreenUp;
	private Bitmap mBitmapArrowRedUp;
	private Bitmap mBitmapBtnDefault;
	private Bitmap mBitmapBtnTouched;
	private Bitmap mBitmapCircleDefault;
	private Bitmap mBitmapCircleGreen;
	private Bitmap mBitmapCircleRed;
	private Matrix mMatrix = new Matrix();
	private NineKeyLockListener mListener = null;
	private Paint mPaint = new Paint();
	private Paint mPathPaint = new Paint();
	private Path mPath = new Path();

	public NineKeyLockView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPathPaint.setAntiAlias(true);
		mPathPaint.setDither(true);
		mPathPaint.setColor(Color.WHITE);
		mPathPaint.setAlpha(0x80);
		mPathPaint.setStyle(Paint.Style.STROKE);
		mPathPaint.setStrokeJoin(Paint.Join.ROUND);
		mPathPaint.setStrokeCap(Paint.Cap.ROUND);

        // lot's of bitmaps!
        mBitmapBtnDefault = getBitmapFor(R.drawable.btn_code_lock_default_holo);
        mBitmapBtnTouched = getBitmapFor(R.drawable.btn_code_lock_touched_holo);
        mBitmapCircleDefault = getBitmapFor(R.drawable.indicator_code_lock_point_area_default_holo);
        mBitmapCircleGreen = getBitmapFor(R.drawable.indicator_code_lock_point_area_green_holo);
        mBitmapCircleRed = getBitmapFor(R.drawable.indicator_code_lock_point_area_red_holo);

        mBitmapArrowGreenUp = getBitmapFor(R.drawable.indicator_code_lock_drag_direction_green_up);
        mBitmapArrowRedUp = getBitmapFor(R.drawable.indicator_code_lock_drag_direction_red_up);

        // bitmaps have the size of the largest bitmap in this group
        final Bitmap bitmaps[] = { mBitmapBtnDefault, mBitmapBtnTouched, mBitmapCircleDefault,
                mBitmapCircleGreen, mBitmapCircleRed };

        for (Bitmap bitmap : bitmaps) {
            mBitmapWidth = Math.max(mBitmapWidth, bitmap.getWidth());
            mBitmapHeight = Math.max(mBitmapHeight, bitmap.getHeight());
        }
	}

	private Bitmap getBitmapFor(int resId)
	{
		return BitmapFactory.decodeResource(getResources(), resId);
	}

	private int getShorter()
	{
		int width = getWidth()-getPaddingLeft()-getPaddingRight();
		int height = getHeight()-getPaddingTop()-getPaddingBottom();
		
		return Math.min(width, height);
	}

	private int getColumnHit(float x)
	{
		int shorter = getShorter();
		int i = 0;
		int a = 10*shorter;
		int b = 3*shorter;
		int c = 15*getWidth()-a;
		int d, left, right;
		int result = -1;

		for (i=0; i<3; i++)
		{
			d = c+i*a;
			left = (d-b)/30;
			right = (d+b)/30;

			if (x>=left && x<=right)
			{
				result = i;
				break;
			}
			
			if (x<left)
			{
				break;
			}
		}

		return result;
	}

	private int getRowHit(float y)
	{
		int shorter = getShorter();
		int i = 0;
		int a = 10*shorter;
		int b = 3*shorter;
		int c = 15*getHeight()-a;
		int d, top, bottom;
		int result = -1;

		for (i=0; i<3; i++)
		{
			d = c+i*a;
			top = (d-b)/30;
			bottom = (d+b)/30;

			if (y>=top && y<=bottom)
			{
				result = i;
				break;
			}
			
			if (y<top)
			{
				break;
			}
		}

		return result;
	}
	
	private int checkForNewHit(float x, float y)
	{
		int i = 0;
		int column = -1;
		int row = -1;
		int shorter = getShorter();
		int a = 10*shorter;
		int b = 3*shorter;
		int c = 15*getWidth()-a;
		int d = 15*getHeight()-a;
		int e, f;
		int left, top, right, bottom;
		int result = -1;
		
		for (i=0; i<3; i++)
		{
			if (column==-1)
			{
				e = c+i*a;
				left = (e-b)/30;
				right = (e+b)/30;
				
				if (x>=left && x<=right)
				{
					column = i;
				}
				
				if (x<left)
				{
					break;
				}
			}
			
			if (row==-1)
			{
				f = d+i*a;
				top = (f-b)/30;
				bottom = (f+b)/30;
				
				if (y>=top && y<=bottom)
				{
					row = i;
				}
				
				if (y<top)
				{
					break;
				}
			}

			if (column!=-1 && row!=-1)
			{
				result = getHit(column, row);
				break;
			}
		}

		return result;
	}

	private int getColumn(int hit)
	{
		return hit/3%3;
	}
	
	private int getRow(int hit)
	{
		return hit%3;
	}
	
	private int getHit(int column, int row)
	{
		return column*3+row;
	}

	private float getHitX(int hit)
	{
		return (3*getWidth()+(getColumn(hit)-1)*2*getShorter())/6f;
	}
	
	private float getHitY(int hit)
	{
		return (3*getHeight()+(getRow(hit)-1)*2*getShorter())/6f;
	}

	private void detectAndAddHit(float x, float y)
	{
		int newHit = checkForNewHit(x, y);
		int mPatternSize = getPatternSize();
		
		if (newHit != -1 && !mPattern.contains(newHit))
		{
			int newHitColumn = getColumn(newHit);
			int newHitRow = getRow(newHit);

			if (mPatternSize>0)
			{
				int lastHit = mPattern.get(mPatternSize-1);
				int lastHitColumn = getColumn(lastHit);
				int lastHitRow = getRow(lastHit);
				
				if (Math.abs(newHitColumn-lastHitColumn)!=1 && Math.abs(newHitRow-lastHitRow)!=1)
				{
					int centerHitColumn = (newHitColumn+lastHitColumn)>>1;
					int centerHitRow = (newHitRow+lastHitRow)>>1;
					int centerHit = getHit(centerHitColumn, centerHitRow);
					
					if (!mPattern.contains(centerHit))
					{
						mPattern.add(centerHit);
					}
				}
			}
			
			mPattern.add(newHit);
			
			if (mListener != null)
			{
				mListener.onAddHit(this);
			}
		}
		
		if (mListener != null && mPatternSize==0 && getPatternSize()>0)
		{
			mListener.beforeInput(this);
		}
	}

	@Override
	protected void OnUpdateStart() {
		startCheck();
	}

	@Override
	protected void OnUpdate() {
		if (timerCount>0)
		{
			timerCount--;
		}
		else
		{
			stopTimer();
		}
	}

	@Override
	protected void OnUpdateStop() {
		stopCheck();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		int shorter = getShorter();
		int i;
		int mPatternSize = getPatternSize();
		int curHit, nextHit;
		Bitmap cicle;
		Bitmap btn;
		Bitmap arrow;
		float scaleX = (4*shorter)/(15f*mBitmapWidth);
		float scaleY = (4*shorter)/(15f*mBitmapHeight);
		float hitX, hitY;

		if ((makePatternVisible || !isShowGreen) && mPatternSize>0)
		{
			curHit = mPattern.get(0);
			mPathPaint.setStrokeWidth(shorter/60f);
			mPath.reset();
			mPath.moveTo(getHitX(curHit), getHitY(curHit));

			for (i=1; i<mPatternSize; i++)
			{
				curHit = mPattern.get(i);
				mPath.lineTo(getHitX(curHit), getHitY(curHit));
			}

			if (mX != -1 && mY != -1)
			{
				mPath.lineTo(mX, mY);
			}

			canvas.drawPath(mPath, mPathPaint);
		}

		for (curHit=0; curHit<9; curHit++)
		{
			hitX = getHitX(curHit);
			hitY = getHitY(curHit);
			mMatrix.reset();
			mMatrix.postTranslate(hitX - mBitmapWidth/2f, hitY - mBitmapHeight/2f);
			mMatrix.postScale(scaleX, scaleY, hitX, hitY);

			if ((!makePatternVisible && isShowGreen) || !mPattern.contains(curHit))
			{
				cicle = mBitmapCircleDefault;
				btn = mBitmapBtnDefault;
			}
			else if (isShowGreen)
			{
				cicle = mBitmapCircleGreen;
				
				if (mX != -1 && mY != -1)
				{
					btn = mBitmapBtnTouched;
				}
				else
				{
					btn = mBitmapBtnDefault;
				}
			}
			else
			{
				cicle = mBitmapCircleRed;
				btn = mBitmapBtnDefault;
			}

			canvas.drawBitmap(cicle, mMatrix, mPaint);
			canvas.drawBitmap(btn, mMatrix, mPaint);
		}

		if (makePatternVisible || !isShowGreen)
		{
			if (isShowGreen)
			{
				arrow = mBitmapArrowGreenUp;
			}
			else
			{
				arrow = mBitmapArrowRedUp;
			}
	
			for (i=0; i<mPatternSize-1; i++)
			{
				curHit = mPattern.get(i);
				nextHit = mPattern.get(i+1);
				hitX = getHitX(curHit);
				hitY = getHitY(curHit);
				
				mMatrix.reset();
				mMatrix.postTranslate(hitX - arrow.getWidth()/2f, hitY - mBitmapHeight/2f);
				mMatrix.postScale(scaleX, scaleY, hitX, hitY);
				mMatrix.postRotate((float)Math.toDegrees(Math.atan2(getRow(nextHit) - getRow(curHit), getColumn(nextHit) - getColumn(curHit))) + 90f, hitX, hitY);
				canvas.drawBitmap(arrow, mMatrix, mPaint);
			}
		}
	}

	private void touch_start(float x, float y) {
		stopTimer();
		mPattern.clear();
		detectAndAddHit(x, y);
		mX = x;
		mY = y;
		isShowGreen = true;
	}

	private void touch_move(float x, float y) {
		detectAndAddHit(x, y);
		mX = x;
		mY = y;
	}

	private void touch_up(float x, float y) {
		mX = -1;
		mY = -1;
		
		if (mListener != null && getPatternSize()>0)
		{
			mListener.afterInput(this);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		boolean result = false;
		
		float x = event.getX();
		float y = event.getY();
		
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				if (isCanInput)
				{
					touch_start(x, y);
					invalidate();
					result = true;
				}

				break;

			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				break;

			case MotionEvent.ACTION_UP:
				touch_up(x, y);
				invalidate();
				break;

			default:
				break;
		}
		
		if (!result)
		{
			result = super.onTouchEvent(event);
		}

		return result;
	}
	
	public void checkPassed(boolean passed)
	{
		if (!passed)
		{
			isShowGreen = false;
			invalidate();
			startTimer();
		}
		else
		{
			isCanInput = false;
		}
		
		if (mListener != null)
		{
			mListener.beforeCheck(this, isShowGreen);
		}
	}

	public int getPatternSize()
	{
		return mPattern.size();
	}

	public byte[] getPassword()
	{
		byte[] result = null;
		final int mPatternSize = getPatternSize();
		byte[] res = new byte[mPatternSize];

		for (int i = 0; i < mPatternSize; i++)
		{
			res[i] = mPattern.get(i).byteValue();
		}

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hash = md.digest(res);
			result = hash;
		} catch (NoSuchAlgorithmException nsa) {
			result = res;
        }

		return result;
	}
	
	public void setListener(NineKeyLockListener listener)
	{
		mListener = listener;
	}
	
	private void startCheck()
	{
		timerCount = MAX_TIMER_COUNT;
		
		invalidate();
	}
	
	private void stopCheck()
	{
		if (mListener != null)
		{
			mListener.afterCheck(this, isShowGreen);
		}
	
		mPattern.clear();
		isShowGreen = true;
			
		invalidate();
	}
	
	public void reset()
	{
		if (isCanInput)
		{
			stopTimer();
		}
		else
		{
			stopCheck();
			isCanInput = true;
		}
	}
	
	public void setMakePatternVisible(boolean value)
	{
		makePatternVisible = value;
	}
}