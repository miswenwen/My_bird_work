package com.example.birdsalesstatistics;

import internal.org.apache.http.entity.mime.MultipartEntity;
import internal.org.apache.http.entity.mime.content.StringBody;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import Decoder.BASE64Encoder;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

public class SalesService extends Service {
	// private DataBinder mBinder = new DataBinder();

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.d("liuqipeng", "onCreate executed");

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.d("liuqipeng", "onStartCommand executed");
		doSales();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d("liuqipeng", "onDestroy executed");
	}

	private void doSales() {
		// TODO Auto-generated method stub
		new ReadHttpGet()
				.execute("http://47.88.23.146:8080/device/device/addDevice.do");
	}

	class ReadHttpGet extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... params) {
			// trim()方法去除首尾空格
			// String modelNumberStr =
			// et_modelNumber.getText().toString().trim();
			// String markStr = et_mark.getText().toString().trim();
			// String remarkStr = et_remark.getText().toString().trim();
			String modelNumberStr = "666";
			String markStr = "zzz";
			String remarkStr = "liu";
			// 拼接成json串
			Devices devices = new Devices();
			devices.setMark(markStr);
			devices.setModelNumber(modelNumberStr);
			devices.setRemark(remarkStr);
			String jsonProgram = new Gson().toJson(devices);
			Log.e("liuqipeng", "jsonProgram" + jsonProgram);// liuqipeng add
			// 给json加密
			BASE64Encoder encode = new BASE64Encoder();

			String base64 = encode.encode(jsonProgram.getBytes());
			Log.e("liuqipeng", "base64" + base64);// liuqipeng add
			HttpPost httpRequest = new HttpPost(params[0].toString());
			try {
				HttpClient httpClient = new DefaultHttpClient();

				MultipartEntity reqEntity = new MultipartEntity();
				reqEntity.addPart("program",
						new StringBody(base64, Charset.forName("UTF-8")));

				httpRequest.setEntity(reqEntity);
				// post请求
				HttpResponse httpResponse = httpClient.execute(httpRequest);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String strResult = EntityUtils.toString(httpResponse
							.getEntity());
					System.out.println("Result返回-" + strResult);
					Log.e("liuqipeng", "result==strResult" + strResult);// liuqipeng
																		// add
					return strResult;

				} else {
					Log.e("liuqipeng", "result==请求出错");// liuqipeng add
					return "请求出错";
				}
			} catch (ClientProtocolException e) {
				Log.e("liuqipeng", "ClientProtocolException" + e);// liuqipeng
																	// add
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("liuqipeng", "IOException" + e);// liuqipeng add
			}
			Log.e("liuqipeng", "result==null");// liuqipeng add
			return null;
		}

		@Override
		protected void onCancelled(Object result) {
			// TODO Auto-generated method stub
			// super.onCancelled(result);
		}

		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			// 返回信息
			Gson gson = new Gson();
			if (result == null) {
				Log.e("liuqipeng", "result==null,onPostExecute,连接不上服务器");// liuqipeng add
																			// add
				return;
			}
			ResponseModel model = gson.fromJson(result.toString(),
					ResponseModel.class);

			if (model.getStatus() == 1) {
				Toast.makeText(getApplicationContext(), "激活成功", 0).show();
				SharedPreferences.Editor mEditor=getSharedPreferences("data", MODE_PRIVATE).edit();
				mEditor.putBoolean("firstdo", false);
				mEditor.commit();
			} else {
				Toast.makeText(getApplicationContext(), model.getMsg(), 0)
						.show();
			}
			stopSelf();//liuqipeng add
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			// super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}
	}
	// class DataBinder extends Binder {
	//
	// }
}
