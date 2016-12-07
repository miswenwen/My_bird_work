package com.example.liuunitconvert;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetricsInt;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 
 * @author lqp
 * 
 */
public class PickerView extends View {
	// 可编辑属性
	private int num = 7;// view中显示数据项个数
	private float minTextSize = 20;// 文字最小
	private float maxTextSize = 120;// 文字最大
	private int textColor = Color.BLACK;// 字体颜色
	private int centerTextColor = Color.BLUE;// 选中框的文字颜色
	private int otherPartColor = Color.WHITE;// 其他部分的颜色
	private int lineColor = Color.BLACK;// 分割线颜色
	private int speed = 1;// 手指抬起后是继续前进还是回退的速度
	private float devideLineLength = 300;// 设置分割线长度，2×devideLineLength
	// 成员变量
	double zz;
	private Handler mHandler;
	private TimerTask mTask;
	private Timer mTimer;
	private float mCurrentLocation;// 当前view竖立中心的Text对应位置(浮点数)，用于动态设置字号大小，透明度，渐变字体颜色
	private int mCurrentItem;
	private int mCurrentPosition;// 当前view竖立中心选中框的内容对应ArrayList的postion,用于返回,与集合奇偶，mCurrentItem有关
	private float itemError;// 当手指抬起后，最近的item距离选框中心线的误差，用于继续绘画出继续前进或者回退的动画
	private ArrayList<String> strings = new ArrayList<String>();// 显示的数据集合
	private int midStringNum;// 默认选中数据集合的中间项
	private float viewHeight;
	private float viewWidth;
	private float itemHeight;// 单项的高度，view的高度/num
	private Paint mPaint;
	private float offset = 0;// 滑动距离，相对与view的竖立中心线
	private float x = 0;
	private float y = 0;
	private ArgbEvaluator evaluator;

	float positionOffset;// 大小在0~1 用于计算varible的各项属性
	private boolean isSetChecked=false;//是否调用了setChecked方法
	private int position;//调用了setChecked方法后，设置positon
	private int varibleTextAlpha;// 与offset，minTextSize，maxTextSize关联的字体透明度
	private float varibleTextSize;// 与offset，minTextSize，maxTextSize关联的字体大小
	private int varibleTextColor;// 与offset，minTextSize，maxTextSize关联的字体颜色

	public PickerView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public PickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		strings.add("liu0");
		strings.add("liu1");
		strings.add("liu2");
		strings.add("liu3");
		strings.add("liu4");
		strings.add("liu5");
		strings.add("liu6");
		strings.add("liu7");
		strings.add("liu8");
		strings.add("liu9");
		strings.add("liu10");
		// Collections.reverse(strings);
		minTextSize = DensityUtil.dip2px(context,7);
		maxTextSize =  DensityUtil.dip2px(context, 40);
		devideLineLength = DensityUtil.dip2px(context,100);
		/**
		 * 总个数为奇数:显示中间一个，比如总数3个，3/2=1。list.get(1)实际对应的第二个元素，所以在中间
		 * 总个数为偶数:上部分要比下部分多显示一个
		 */
		mCurrentPosition=strings.size() / 2;
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTimer = new Timer();
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (Math.abs(offset - (mCurrentItem - midStringNum)
						* itemHeight)
						/ itemHeight > 0.001) {
					offset = offset + (itemError) / (20/speed);
					mCurrentLocation = offset / itemHeight + midStringNum;
					invalidate();
				}
			}
		};
	}

	public PickerView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		viewHeight = getMeasuredHeight();
		viewWidth = getMeasuredWidth();
		itemHeight = viewHeight / num;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		/**
		 * 在我们设置调用setChecked后，因为Activity中虽然获取了PickerView的实例，但是仅仅是执行了构造方法。
		 * 构造方法里面是获取不到view的宽，高的。
		 * view的宽高只有去onMeasure后才能获取到。
		 * 所以有关于itemHeight的计算只能放置到onDraw里面来。
		 */
		if(isSetChecked){
		offset = -(position - midStringNum) * itemHeight;
		mCurrentPosition=position;
		isSetChecked=false;
		}
		midStringNum = strings.size() / 2;
		mCurrentItem = midStringNum + Math.round(offset / itemHeight);
		mCurrentLocation = midStringNum + offset / itemHeight;
		mPaint.setTextAlign(Align.CENTER);
		mPaint.setColor(lineColor);
		// 画两根线
		canvas.drawLine(viewWidth / 2 - devideLineLength, viewHeight / 2
				- itemHeight / 2, viewWidth / 2 + devideLineLength, viewHeight
				/ 2 - itemHeight / 2, mPaint);
		canvas.drawLine(viewWidth / 2 - devideLineLength, viewHeight / 2
				+ itemHeight / 2, viewWidth / 2 + devideLineLength, viewHeight
				/ 2 + itemHeight / 2, mPaint);
		/**
		 * 画文字 计算字体颜色 计算字体大小
		 */
		for (int i = 0; i <= strings.size() - 1; i++) {
			evaluator = new ArgbEvaluator();
			if (strings.size() % 2 == 0) {
				positionOffset = (float) (1 / (Math.pow(
						Math.abs(strings.size() - i - mCurrentLocation), 2) + 1));
			} else {
				positionOffset = (float) (1 / (Math.pow(
						Math.abs(strings.size() - 1 - i - mCurrentLocation), 2) + 1));
			}
			varibleTextColor = (Integer) evaluator.evaluate(positionOffset,
					otherPartColor, centerTextColor);
			mPaint.setColor(varibleTextColor);
			varibleTextSize = (float) (minTextSize + (maxTextSize - minTextSize)
					* positionOffset);
			mPaint.setTextSize(varibleTextSize);
			FontMetricsInt newfmi = mPaint.getFontMetricsInt();
			float newbaseline = (float) (viewHeight / 2 - (newfmi.bottom / 2.0 + newfmi.top / 2.0));
			canvas.drawText(strings.get(i), viewWidth / 2, newbaseline
					+ itemHeight * (i - midStringNum) + offset, mPaint);
		}

	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			x = event.getX();
			y = event.getY();
			/**
			 * 注意：这三行非常重要 不能重复schedule同一个任务，否则会报错
			 * 这里的代码配合ACTION_UP里面的代码。实现了每次up的时候计时器开始干事情，帮助回滚或者继续滑动。
			 * 而在ACTION_DOWN的时候就让计时器停下。
			 * 所以在ACTION_MOVE的时候是没有计时器的程序执行来重绘的，只是ACTION_MOVE里面的代码在重绘。
			 */
			if (mTask != null) {
				mTask.cancel();
				mTask = null;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			offset = offset + event.getY() - y;
			y = event.getY();
			invalidate();
			/**
			 * 设定滑动的边界,要分两种情况 集合元素个数为奇数和为偶数
			 */
			int index = strings.size() / 2;// 别把这行直接加进去，否则奇数时会多出0.5
			float bound = (float) (itemHeight * index);
			if (strings.size() % 2 == 1) {
				if (Math.abs(offset) > bound) {
					if (offset > 0) {
						offset = bound;
					} else {
						offset = -bound;
					}
				}
			} else {
				if (offset > bound) {
					offset = bound;
				}
				if (offset < -bound + itemHeight) {
					offset = -bound + itemHeight;
				}
			}
			mCurrentLocation = offset / itemHeight + midStringNum;
			/**
			 * 下面这两行同时在ACTION_DOWN里面设置的理由：
			 * 有可能两个手指在操作，一个手指滑动pickerview(未抬起，也就是没有触发ACTION_UP)，另一个手指点击button并返回pickerview.getCurrentPosition.
			 * 为了返回准确的currentpositon，这里也要进行设置
			 */
			mCurrentItem = Math.round(offset / itemHeight) + midStringNum;
			mCurrentPosition = midStringNum - (mCurrentItem - midStringNum);
			break;
		case MotionEvent.ACTION_UP:
			invalidate();
			/**
			 * 注意点： 1.当你手指下滑的时候，如果从中心线来算，你的偏移值是正的。重绘text的时候
			 * canvas.drawText(String text, float x, float y, Paint paint)
			 * y值是加上offset. 假设你是a1,a2----a10向下排列，你向下偏移了两个位置。
			 * 如果原来中心显示的是a5,那么现在显示的不是a7,而是a3;而a5也到了原来a7的位置。
			 * 所以获取正确的ArrayList集合的对应位置用下面代码
			 */
			mCurrentItem = Math.round(offset / itemHeight) + midStringNum;// 判断当前应该是在哪个位置，然后滑动到这个位置
			mCurrentPosition = midStringNum - (mCurrentItem - midStringNum);// 将偏差数反向就可以了
			Log.e("current", "text:" + strings.get(mCurrentPosition)
					+ "\n mCurrentPosition" + mCurrentPosition);
			itemError = (Math.round(offset / itemHeight)) * itemHeight - offset;
			mTask = new TimerTask() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mHandler.sendEmptyMessage(0);
				}
			};
			mTimer.schedule(mTask, 0, 10);

			break;
		default:
			mCurrentItem = Math.round(offset / itemHeight) + midStringNum;
			mCurrentPosition = midStringNum - (mCurrentItem - midStringNum);
			if (mTask != null) {
				mTask.cancel();
				mTask = null;
			}
			break;
		}

		return true;
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		mTimer.cancel();
	}

	public void setData(ArrayList<String> dataList) {
		strings = dataList;
		midStringNum = strings.size() / 2;
		mCurrentItem = midStringNum;
		mCurrentLocation = midStringNum;
		offset = 0;
		invalidate();
	}

	public void setChecked(int position) {
		isSetChecked=true;
		this.position=position;
		invalidate();
	}
	public int getCurrentPosition(){
		/**
		 * 当前位置
		 * 1.没设置具体位置时，在构造方法里面设置为ArrayList中间postion
		 * 2.设置具体位置后，当前位置为设置的position
		 * 3.每次手指滑动抬起（准确说是除了手指按下和移动外的其他所有MotionEvent类型）后，position位置改变
		 * 
		 */
		if(isSetChecked){
			mCurrentPosition=position;
		}
		return mCurrentPosition;
	}
}
