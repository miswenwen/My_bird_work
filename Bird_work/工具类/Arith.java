package com.example.liuunitconvert;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import android.util.Log;

public class Arith {
	//除法保留小数点后位数
	private static final int DEF_DIV_SCALE = 15;
	//科学计数法后保留小数点15位
	private static DecimalFormat mDecimalFormat=new DecimalFormat("#.###############E0");

	// 构造器私有
	private Arith() {
	}
	//输入数字转化为科学计数法
	//获取科学计数法的数字
	 public static String getScientificNum(String inputNumber){
		 String result=null;
		 //科学计数法后保留15位
		 result=mDecimalFormat.format(new BigDecimal(inputNumber));
		 int index=result.indexOf("E");
		 //获取数字
		 result=result.substring(0,index);
		 return result;
	 }
	 //获取科学计数法的指数
	 public static String getScientificIndex(String inputNumber){
		 String result=null;
		 //科学计数法后保留15位
		 result=mDecimalFormat.format(new BigDecimal(inputNumber));
		 int index=result.indexOf("E");;
		 //获取指数
		 result=result.substring(index+1);
		 Log.e("result", result);
		 double mid=Math.pow(10.0, Double.valueOf(result));
		 result=new BigDecimal(String.valueOf(mid)).toPlainString();
		 return result;
	 }
	// 针对double类型，加减乘除
	public static double add(double v1, double v2) {
		BigDecimal b1 = BigDecimal.valueOf(v1);
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.add(b2).doubleValue();
	}

	public static double sub(double v1, double v2) {
		BigDecimal b1 = BigDecimal.valueOf(v1);
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.subtract(b2).doubleValue();
	}

	public static double mul(double v1, double v2) {
		BigDecimal b1 = BigDecimal.valueOf(v1);
		BigDecimal b2 = BigDecimal.valueOf(v2);
		return b1.multiply(b2).doubleValue();
	}

	public static double div(double v1, double v2) {
		BigDecimal b1 = BigDecimal.valueOf(v1);
		BigDecimal b2 = BigDecimal.valueOf(v2);
		// 精确到小数点后多少位，四舍五入。因为除不尽的时候会报错
		//下面这么写：除的尽就除，除数可以无限小。除不尽的时候再保留小数点
		try {
			result = b1.divide(b2).doubleValue();
		} catch (Exception e) {
			result= b1.divide(b2, DEF_DIV_SCALE, BigDecimal.ROUND_HALF_UP)
					.doubleValue();
		}
		return result;
	}

	// 针对String类型，加减乘除
	public static String add(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.add(b2).toString();
	}

	public static String sub(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.subtract(b2).toString();
	}

	public static String mul(String v1, String v2) {
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		return b1.multiply(b2).toString();
	}

	public static String div(String v1, String v2) {
		String result;
		BigDecimal b1 = new BigDecimal(v1);
		BigDecimal b2 = new BigDecimal(v2);
		// 精确到小数点后多少位，四舍五入。因为除不尽的时候会报错
		//下面这么写：除的尽就除，除数可以无限小。除不尽的时候再保留小数点
		try {
			result = b1.divide(b2).toString();
		} catch (Exception e) {
			result= b1.divide(b2, DEF_DIV_SCALE, BigDecimal.ROUND_HALF_UP)
					.toString();
		}
		return result;
	}

}
