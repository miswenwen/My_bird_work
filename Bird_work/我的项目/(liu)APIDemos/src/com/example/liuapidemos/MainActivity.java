package com.example.liuapidemos;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.liuapidemos.activitylog.ActivityLogMainActivity;
import com.example.liuapidemos.alarmclock.AlarmClockMainActivity;
import com.example.liuapidemos.alphatest.AlphaTestMainActivity;
import com.example.liuapidemos.asynctask.AsyncTaskMainActivity;
import com.example.liuapidemos.broadcast.BroadcastMainActivity;
import com.example.liuapidemos.canvaschange.CanvasChangeMainActivity;
import com.example.liuapidemos.checkboxtogglebtnswitchdefault.CheckBoxToggleBtnSwitchDefaultMainActivity;
import com.example.liuapidemos.custombutton.CustomButtonMainActivity;
import com.example.liuapidemos.custombuttonsec.CustomButtonSecMainActivity;
import com.example.liuapidemos.database.DatabaseMainActivity;
import com.example.liuapidemos.delayfinishact.DelayFinishActMainActivity;
import com.example.liuapidemos.digitalclock.DigitalClockMainActivity;
import com.example.liuapidemos.draggablewindow.DraggableWindowMainActivity;
import com.example.liuapidemos.drawlinebypath.DrawLineByPathMainActivity;
import com.example.liuapidemos.drawlinecanvasdrawline.DrawLineCanvasDrawLineMainActivity;
import com.example.liuapidemos.frameanim.FrameAnimMainActivity;
import com.example.liuapidemos.frameanimcustomview.FrameAnimCustomViewMainActivity;
import com.example.liuapidemos.handler.HandlerMainActivity;
import com.example.liuapidemos.matrix.MatrixMainActivity;
import com.example.liuapidemos.packagemanager.PackageManagerMainActivity;
import com.example.liuapidemos.packagemanagersec.PackageManagerSecMainActivity;
import com.example.liuapidemos.smallball.SmallBallMainActivity;
import com.example.liuapidemos.timerpicker.TimerPickerMainActivity;
import com.example.liuapidemos.valueanimator.ValueAnimatorMainActivity;
import com.example.liuapidemos.viewpager.ViewPagerMainActivity;

public class MainActivity extends Activity {
	private final int CUSTOM_BUTTON = 0;
	private final int CUSTOM_BUTTON_SEC =1;
	private final int PACKAGE_MANAGER = 2;
	private final int PACKAGE_MANAGER_SEC =3;
	private final int FRAME_ANIM = 4;
	private final int FRAME_ANIM_CUSTOM_VIEW =5;
	private final int CANVAS_CHANGE = 6;
	private final int DRAW_LINE_BY_PATH = 7;
	private final int DRAW_LINE_CANVAS_DRAW_LINE =8;
	private final int MATRIX =9;
	private final int SMALL_BALL =10;
	private final int VIEWPAGER =11;
	private final int DATABASE =12;
	private final int HANDLER =13;
	private final int ASYNCTASK =14;
	private final int CHKTOGGLESWITCHDEFAULT =15;
	private final int BROADCAST = 16;
	private final int TIMER_PICKER = 17;
	private final int ACTIVITY_LOG = 18;
	private final int DRAGGABLE_WINODW = 19;
	private final int VALUE_ANIMATOR = 20;
	private final int ALPHA_TEST = 21;
	private final int ALARM_CLOCK = 22;
	private final int DELAY_FINISH_ACT = 23;
	private final int DIGITAL_CLOCK = 24;

	private ListView mListView;
	private ArrayList<String> mList = new ArrayList<String>();
	private Intent mIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		mListView = (ListView) findViewById(R.id.my_list);
		dataInit();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.simple_list_item_1, mList);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				switch (position) {
				case CUSTOM_BUTTON:
					mIntent = new Intent(MainActivity.this,
							CustomButtonMainActivity.class);
					startActivity(mIntent);
					break;
				case ALARM_CLOCK:
					mIntent = new Intent(MainActivity.this,
							AlarmClockMainActivity.class);
					startActivity(mIntent);
					break;
				case BROADCAST:
					mIntent = new Intent(MainActivity.this,
							BroadcastMainActivity.class);
					startActivity(mIntent);
					break;
				case PACKAGE_MANAGER:
					mIntent = new Intent(MainActivity.this,
							PackageManagerMainActivity.class);
					startActivity(mIntent);
					break;			
				case FRAME_ANIM:
					mIntent = new Intent(MainActivity.this,
							FrameAnimMainActivity.class);
					startActivity(mIntent);
					break;	
				case CANVAS_CHANGE:
					mIntent = new Intent(MainActivity.this,
							CanvasChangeMainActivity.class);
					startActivity(mIntent);
					break;	
				case DRAW_LINE_BY_PATH:
					mIntent = new Intent(MainActivity.this,
							DrawLineByPathMainActivity.class);
					startActivity(mIntent);
					break;	
				case DRAW_LINE_CANVAS_DRAW_LINE:
					mIntent = new Intent(MainActivity.this,
							DrawLineCanvasDrawLineMainActivity.class);
					startActivity(mIntent);
					break;	
				case FRAME_ANIM_CUSTOM_VIEW:
					mIntent = new Intent(MainActivity.this,
							FrameAnimCustomViewMainActivity.class);
					startActivity(mIntent);
					break;	
				case MATRIX:
					mIntent = new Intent(MainActivity.this,
							MatrixMainActivity.class);
					startActivity(mIntent);
					break;	
				case SMALL_BALL:
					mIntent = new Intent(MainActivity.this,
							SmallBallMainActivity.class);
					startActivity(mIntent);
					break;	
				case VIEWPAGER:
					mIntent = new Intent(MainActivity.this,
							ViewPagerMainActivity.class);
					startActivity(mIntent);
					break;	
				case DATABASE:
					mIntent = new Intent(MainActivity.this,
							DatabaseMainActivity.class);
					startActivity(mIntent);
					break;	
				case HANDLER:
					mIntent = new Intent(MainActivity.this,
							HandlerMainActivity.class);
					startActivity(mIntent);
					break;	
				case ASYNCTASK:
					mIntent = new Intent(MainActivity.this,
							AsyncTaskMainActivity.class);
					startActivity(mIntent);
					break;			
				case CHKTOGGLESWITCHDEFAULT:
					mIntent = new Intent(MainActivity.this,
							CheckBoxToggleBtnSwitchDefaultMainActivity.class);
					startActivity(mIntent);
					break;	
				case PACKAGE_MANAGER_SEC:
					mIntent = new Intent(MainActivity.this,
							PackageManagerSecMainActivity.class);
					startActivity(mIntent);
				case TIMER_PICKER:
					mIntent = new Intent(MainActivity.this,
							TimerPickerMainActivity.class);
					startActivity(mIntent);
					break;	
				case ACTIVITY_LOG:
					mIntent = new Intent(MainActivity.this,
							ActivityLogMainActivity.class);
					startActivity(mIntent);
					break;		
				case DRAGGABLE_WINODW:
					mIntent = new Intent(MainActivity.this,
							DraggableWindowMainActivity.class);
					startActivity(mIntent);
					break;		
				case VALUE_ANIMATOR:
					mIntent = new Intent(MainActivity.this,
							ValueAnimatorMainActivity.class);
					startActivity(mIntent);
					break;		
				case ALPHA_TEST:
					mIntent = new Intent(MainActivity.this,
							AlphaTestMainActivity.class);
					startActivity(mIntent);
					break;			
				case CUSTOM_BUTTON_SEC:
					mIntent = new Intent(MainActivity.this,
							CustomButtonSecMainActivity.class);
					startActivity(mIntent);
					break;		
				case DELAY_FINISH_ACT:
					mIntent = new Intent(MainActivity.this,
							DelayFinishActMainActivity.class);
					startActivity(mIntent);
					break;
				case DIGITAL_CLOCK:
					mIntent = new Intent(MainActivity.this,
							DigitalClockMainActivity.class);
					startActivity(mIntent);
					break;
				default:
					break;
				}
			}
		});
	}

	private void dataInit() {
		// TODO Auto-generated method stub
		mList.add("自定义按钮1");
		mList.add("自定义按钮2");
		mList.add("查找本机所有应用及对应包名");
		mList.add("查找本机所有应用及对应包名2");
		mList.add("帧动画(drawable,xml文件加载若干图片)");
		mList.add("帧动画(自定义View裁剪长图片)");
		mList.add("自定义View画布坐标轴变化");
		mList.add("自定义View手指画线(canvas.drawPath)");
		mList.add("自定义View手指画线(canvas.drawLine)");
		mList.add("ImageView利用Matrix对象改变效果");
		mList.add("自定义View小球来回移动");
		mList.add("ViewPager带滑条(加载View对象)");
		mList.add("Database创建表");
		mList.add("Handler基本使用");
		mList.add("AsyncTask基本使用");
		mList.add("ChkToggleSwitch默认效果");
		mList.add("广播(BaseActivity,ActivityCollector)");
		mList.add("自定义PickerView(网上找的)");
		mList.add("Activity,View各方法执行打印log");
		mList.add("可拖动的Window");
		mList.add("属性动画基本使用");
		mList.add("子控件,父容器透明度的关系");
		mList.add("闹钟");
		mList.add("延迟退出Activity");
		mList.add("数字时钟简单实现");

	}

}
