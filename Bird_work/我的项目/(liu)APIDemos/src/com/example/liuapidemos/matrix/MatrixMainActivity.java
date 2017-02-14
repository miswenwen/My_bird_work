package com.example.liuapidemos.matrix;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import com.example.liuapidemos.R;

public class MatrixMainActivity extends Activity {
	ImageView mImageView;
	ImageView mImageView1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.matrix_main);
		mImageView = (ImageView) findViewById(R.id.imageview);
		Matrix matrix = new Matrix();
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		int desity = dm.densityDpi;
		// matrix.preTranslate(width/2, 0);
		// matrix.setTranslate(200, 0);
		matrix.setTranslate(width / 4, 0);
		matrix.postTranslate(width / 2, 0);
		Log.e("shit", "x" + dm.widthPixels / 2);
		mImageView.setImageMatrix(matrix);
		// Bitmap mBitmap2=BitmapFactory.dec
		Bitmap mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.matrix_pic2);
		Bitmap mBitmap1 = BitmapFactory.decodeResource(getResources(),
				R.drawable.matrix_pic1);

		mImageView1 = (ImageView) findViewById(R.id.imageview1);
		Matrix matrix1 = new Matrix();
		matrix1.setRotate(45, mBitmap1.getWidth() / 2, mBitmap1.getHeight() / 2
				+ mBitmap.getHeight());// 图片中心转45度
		matrix1.postScale(0.5f, 0.5f, mBitmap1.getWidth() / 2,
				mBitmap1.getHeight() / 2 + mBitmap.getHeight());// 图片中心缩放0.5倍

		mImageView1.setImageMatrix(matrix1);
		// mImageView.setImageAlpha(30);//设置View的透明度为0~255的值，0完全透明，255完全不透明。
	}

}
