package com.example.liuapidemos.viewpager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.liuapidemos.R;

public class ViewPagerMainActivity extends Activity implements OnClickListener {// 注意要实现接口
	ViewPager mViewPager;
	List<View> mList;
	List<String> mList2;
	View mView1;
	View mView2;
	View mView3;
	ImageView mImageView;
	LayoutInflater mLayoutInflater;
	PagerTabStrip mPagerTabStrip;
	Button mButton;
	int index;
	int num = 0;
	int cursor;// 光标宽度
	int offset;// 偏移位置
	int number;// 当前view编号
	TextView mTextView;
	TextView mTextView1;
	TextView mTextView2;
	TextView mTextView3;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager_main);
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		int width = metric.widthPixels; // 屏幕宽度（像素）
		int height = metric.heightPixels; // 屏幕高度（像素）
		float density = metric.density; // 屏幕密度（0.75 / 1.0 / 1.5）
		int densityDpi = metric.densityDpi; // 屏幕密度DPI（120 / 160 / 240）
		Log.e("fuck", "宽" + width + "\n" + height + "\n" + density + "\n"
				+ densityDpi);
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mList = new ArrayList<View>();
		mLayoutInflater = getLayoutInflater();
		mView1 = mLayoutInflater.inflate(R.layout.viewpager_layout1, null);
		mView2 = mLayoutInflater.inflate(R.layout.viewpager_layout2, null);
		mView3 = mLayoutInflater.inflate(R.layout.viewpager_layout3, null);

		mTextView = (TextView) mView2.findViewById(R.id.mybutton);
		mTextView.setText("shit");
		mList.add(mView1);
		mList.add(mView2);
		mList.add(mView3);
		mList2 = new ArrayList<String>();
		mList2.add("red");
		mList2.add("green");
		mList2.add("blue");
		mButton = (Button) findViewById(R.id.button1);
		index = 0;
		mImageView = (ImageView) findViewById(R.id.imageview);
		initImage();

		mButton.setOnClickListener(this);

		mPagerTabStrip = (PagerTabStrip) findViewById(R.id.pagertitle);
		mPagerTabStrip.setTabIndicatorColor(Color.MAGENTA);

		mTextView1 = (TextView) findViewById(R.id.red);
		mTextView2 = (TextView) findViewById(R.id.green);
		mTextView3 = (TextView) findViewById(R.id.blue);
		mTextView1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mViewPager.setCurrentItem(0); // ViewPager的page跳转
			}
		});
		mTextView2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mViewPager.setCurrentItem(1);
			}
		});
		mTextView3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mViewPager.setCurrentItem(2);
			}
		});
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				Log.e("调用PageSelected", "position为" + arg0);
				Log.e("调用PageSelected的时间", "" + System.currentTimeMillis());
				// TODO Auto-generated method stub
				int shift1 = 2 * offset + cursor;
				int shift2 = 2 * shift1;
				Animation mAnimation = null;// 如果不赋值null，switch default出去会报错
				switch (arg0) {
				case 0:
					if (number == 1) {
						mAnimation = new TranslateAnimation(shift1, 0, 0, 0);
					} else if (number == 2) {
						mAnimation = new TranslateAnimation(shift2, 0, 0, 0);
					}
					break;
				case 1:
					if (number == 0) {
						mAnimation = new TranslateAnimation(0, shift1, 0, 0);
					} else if (number == 2) {
						mAnimation = new TranslateAnimation(shift2, shift1, 0,
								0);
					}
					break;
				case 2:
					if (number == 0) {
						mAnimation = new TranslateAnimation(0, shift2, 0, 0);
					} else if (number == 1) {
						mAnimation = new TranslateAnimation(shift1, shift2, 0,
								0);
					}
					break;
				default:
					break;
				}
				number = arg0;
				mAnimation.setFillAfter(true);
				mAnimation.setDuration(300);
				mImageView.setAnimation(mAnimation);
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				Log.e("调用PageState", "state" + arg0);
				Log.e("调用PageState的时间", "" + System.currentTimeMillis());
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				Log.e("调用PageScrolled", "positon为" + arg0 + "\t" + "offset为"
						+ arg1 + "\t" + "像素为" + arg2);
			}

		});
		PagerAdapter mPagerAdapter = new PagerAdapter() {

			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				// TODO Auto-generated method stub
				return arg0 == arg1;
				// return true;
			}

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return mList.size();
			}

			@Override
			public void destroyItem(android.view.ViewGroup container,
					int position, Object object) {
				container.removeView(mList.get(position));
			}

			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				// TODO Auto-generated method stub
				container.addView(mList.get(position));
				return mList.get(position);
			}

			@Override
			public CharSequence getPageTitle(int position) {
				// TODO Auto-generated method stub
				return mList2.get(position);
			}
		};
		mViewPager.setAdapter(mPagerAdapter);
	}

	private void initImage() {
		// TODO Auto-generated method stub
		Bitmap mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.viewpager_pic1);
		cursor = mBitmap.getWidth();
		Matrix matrix = new Matrix();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		offset = (dm.widthPixels / mList.size() - cursor) / 2;
		matrix.postTranslate(offset, 0);
		mImageView.setImageMatrix(matrix);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.button1: {
			if (num % 4 == 0)
				index = 0;
			if (num % 4 == 1)
				index = 1;
			if (num % 4 == 2)
				index = 2;
			if (num % 4 == 3)
				index = 1;
			if (num == 4)
				num = 0;// 最好赋值让num归零。否则点击一次自加一次，点n次，n大于整形数的范围，就会报错。
			num++;
			mViewPager.setCurrentItem(index);// 该方法可以设定ViewPager当前在哪个View
		}
			break;

		default:
			break;
		}
	}

	class MyView extends View {

		public MyView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

	}
}
