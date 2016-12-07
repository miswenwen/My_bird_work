package com.example.liuunitconvert;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import android.util.Log;

public class UnitConvertUtil {

	/**
	 * 结果： 输入大于1的时候，有效位为输入实际位数。可以到算到无限大。 输入小于1的时候，此时输入的数由于用了科学计数法，有效位被控制在15位。
	 * 小于1的结果分两种情况，如果大于0.00001，就直接显示，反之用科学计数法，小数点后最多保留15位显示。 
	 * 大于1的结果有两种（可选）：
	 * 一是用科学计数法，小数点后保留15位表示。当实际结果大于15位的时候，15位以后的数字全部成为0.
	 * 二是每隔3位，放个点。这样的好处是结果最大位数没有受限制，而且大于1的数如果有小数点，则小数点最多保留15位。 注意点:
	 * (1)除数（也可理解成输入数以及中间计算数）在小于1时，要先转换成科学技术法表示。
	 * 理由，由于存在除法除不尽，会对小数保留位处理。当输入数太小，比如0.0000005米/s换算成米/s。
	 * 0.0000005/1.如果小数保留位设置为5，得到结果会是0. 
	 * (2)在被除数大于10的时候，除法分两次除。
	 * 理由，由于存在除法除不尽，会对小数保留位处理。但是当换算单位差距过大，比如1米/s换算成光速。 1m/s=1/30 0000 000.
	 * 如果小数保留位设置为5，得到结果会是0. 对被除数大于10的都处理成科学计数法。小于10的不处理。
	 * 
	 * 1.先换算成a.b...*10^n的形式 
	 * 2.第一次除以a.b....,可能除不尽。需要对除法做处理，小数保留多少位。1/3=0.33333
	 * 3.第二次除以10^n,一定能除尽。不对小数保留位数作处理。0.33333/10^8=3.3333*10^-9
	 * (3)去除结果中多余的0,并且每三位加个点间隔。
	 * (4)由于计算了两次，公式问题。可能会导致1s=1000 000.00000001us这种情况。
	 * 针对这类情况，暂时能想到的只能当成两两配对的当特殊情况给出公式。
	 */
	public static final int Length = 0;
	public static final int Area = 1;
	public static final int Volume = 2;
	public static final int Temperature = 3;
	public static final int Speed = 4;
	public static final int Time = 5;
	public static final int Mass = 6;

	// 长度
	public static String computeConvertResult(String inputNum, int preUnit,
			int aftUnit, int type) {
		// TODO Auto-generated method stub
		Log.e("computeConvertResult", inputNum);
		String result = null;
		String mid = null;
		String index = null;
		/**
		 * 分两次计算 1.计算preUnit到标准单位 2.计算标准单位到aftUnit 例子： 如千米到纳米，那么千米--米，米--纳米
		 */
		if (preUnit == aftUnit) {
			result = inputNum;
		} else {
			switch (type) {
			case Length:
				if (Double.valueOf(inputNum) > 1) {
					mid = computeLength(inputNum, preUnit, true);
				} else {
					mid = computeLength(Arith.getScientificNum(inputNum),
							preUnit, true);
					mid = Arith.mul(mid, Arith.getScientificIndex(inputNum));
				}
				if (Double.valueOf(mid) > 1) {
					result = computeLength(mid, aftUnit, false);
				} else {
					result = computeLength(Arith.getScientificNum(mid), aftUnit,
							false);
					result = Arith.mul(result, Arith.getScientificIndex(mid));
				}
				break;
			case Area:
				if (Double.valueOf(inputNum) > 1) {
					mid = computeArea(inputNum, preUnit, true);
				} else {
					mid = computeArea(Arith.getScientificNum(inputNum),
							preUnit, true);
					mid = Arith.mul(mid, Arith.getScientificIndex(inputNum));
				}
				if (Double.valueOf(mid) > 1) {
					result = computeArea(mid, aftUnit, false);
				} else {
					result = computeArea(Arith.getScientificNum(mid), aftUnit,
							false);
					result = Arith.mul(result, Arith.getScientificIndex(mid));
				}
				break;
			case Volume:
				if (Double.valueOf(inputNum) > 1) {
					mid = computeVolume(inputNum, preUnit, true);
				} else {
					mid = computeVolume(Arith.getScientificNum(inputNum),
							preUnit, true);
					mid = Arith.mul(mid, Arith.getScientificIndex(inputNum));
				}
				if (Double.valueOf(mid) > 1) {
					result = computeVolume(mid, aftUnit, false);
				} else {
					result = computeVolume(Arith.getScientificNum(mid), aftUnit,
							false);
					result = Arith.mul(result, Arith.getScientificIndex(mid));
				}
				break;
			case Temperature:
				//由于温度不仅仅是乘除，故不像其它方式过多处理。
				mid = computeTemperature(inputNum, preUnit, true);
				result = computeTemperature(mid, aftUnit, false);
				break;
			case Speed:
				if (Double.valueOf(inputNum) > 1) {
					mid = computeSpeed(inputNum, preUnit, true);
				} else {
					mid = computeSpeed(Arith.getScientificNum(inputNum),
							preUnit, true);
					mid = Arith.mul(mid, Arith.getScientificIndex(inputNum));
				}
				if (Double.valueOf(mid) > 1) {
					result = computeSpeed(mid, aftUnit, false);
				} else {
					result = computeSpeed(Arith.getScientificNum(mid), aftUnit,
							false);
					result = Arith.mul(result, Arith.getScientificIndex(mid));
				}
				break;
			case Time:
				if (Double.valueOf(inputNum) > 1) {
					mid = computeTime(inputNum, preUnit, true);
				} else {
					mid = computeTime(Arith.getScientificNum(inputNum),
							preUnit, true);
					mid = Arith.mul(mid, Arith.getScientificIndex(inputNum));
				}
				if (Double.valueOf(mid) > 1) {
					result = computeTime(mid, aftUnit, false);
				} else {
					result = computeTime(Arith.getScientificNum(mid), aftUnit,
							false);
					result = Arith.mul(result, Arith.getScientificIndex(mid));
				}
				break;
			case Mass:
				if (Double.valueOf(inputNum) > 1) {
					mid = computeMass(inputNum, preUnit, true);
				} else {
					mid = computeMass(Arith.getScientificNum(inputNum),
							preUnit, true);
					mid = Arith.mul(mid, Arith.getScientificIndex(inputNum));
				}
				if (Double.valueOf(mid) > 1) {
					result = computeMass(mid, aftUnit, false);
				} else {
					result = computeMass(Arith.getScientificNum(mid), aftUnit,
							false);
					result = Arith.mul(result, Arith.getScientificIndex(mid));
				}
				break;

			default:
				break;
			}
		}
		// 去除多余0，始终不用科学计数法表示
		result = new BigDecimal(result).stripTrailingZeros().toPlainString();
		// 去除多余0，数字较大时科学计数法(而且不稳定)表示
		// result = new BigDecimal(result).stripTrailingZeros().toString();
		/**
		 * 当result超过double类型大小范围时，比如一个很大的数。Double.valueOf(result)它后面很多位会变为0.
		 * 这是个隐患。 所以不要new
		 * DecimalFormat(",###.###############").format(Double.valueOf(result));
		 * 而应该是 new DecimalFormat(",###.###############").format(new
		 * BigDecimal(result));
		 */

		if (Math.abs(Double.valueOf(result)) >= 1) {
			result = new DecimalFormat(",###.###############")
					.format(new BigDecimal(result));
		} else if (Math.abs(Double.valueOf(result)) <0.00001&&Double.valueOf(result)!=0){
			DecimalFormat mDecimalFormat = new DecimalFormat(
					"#.###############E0");
			result = mDecimalFormat.format(new BigDecimal(result));
		}
		return result;
	}

	// 下面有7个compute类，因为unit都是对应的单位集合中的index。所以要一一对应
	private static String computeLength(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为米
		 */
		String result = "1";
		if (isToStandardUnit) {
			switch (unit) {
			//0:千米 y=1000x;x=y/1000
			case 0:
				result=Arith.mul(inputNum,"1000");
				break;
				//1:米 y=x;x=y
			case 1:
				result=inputNum;
				break;
				//2:分米 y=0.1x;x=10y
			case 2:
				result=Arith.mul(inputNum,"0.1");
				break;
				//3:厘米 y=0.01x;x=100y;
			case 3:
				result=Arith.mul(inputNum,"0.01");
				break;
				//4:毫米 y=0.001x;x=1000y
			case 4:
				result=Arith.mul(inputNum,"0.001");
				break;
				//5:微米 y=0.000001x;x=1000000y
			case 5:
				result=Arith.mul(inputNum,"0.000001");
				break;
				//6:纳米 y=x/1000000000;x=1000000000y;
			case 6:
				result=Arith.div(inputNum,"1000000000");
				break;
				//7:皮米 y=x/1000000000000;x=1000000000000y
			case 7:
				result=Arith.div(inputNum,"1000000000000");
				break;
				//8:海里 y=1852x;x=y/1852
			case 8:
				result=Arith.mul(inputNum,"1852");
				break;
				//9:英里 y=1609.344x;x=y/1609.344
			case 9:
				result=Arith.mul(inputNum,"1609.344");
				break;
				//10:弗隆 y=201.168x;x=y/201.168
			case 10:
				result=Arith.mul(inputNum,"201.168");
				break;
				//11:英寻 y=1.8288x;x=y/1.8288
			case 11:
				result=Arith.mul(inputNum,"1.8288");
				break;
				//12:码 y=0.9144x;x=y/0.9144
			case 12:
				result=Arith.mul(inputNum,"0.9144");
				break;
				//13:英尺 y=0.3048x;x=y/0.3048
			case 13:
				result=Arith.mul(inputNum,"0.3048");
				break;
				//14:英寸 y=0.0254x;x=y/0.0254
			case 14:
				result=Arith.mul(inputNum,"0.0254");
				break;
				//15:公里 y=1000x;x=y/1000
			case 15:
				result=Arith.mul(inputNum,"1000");
				break;
				//16:里 y=500x;x=y/500
			case 16:
				result=Arith.mul(inputNum,"500");
				break;
				//17:丈 y=10x/3;x=3y/10
			case 17:
				result=Arith.div(Arith.mul(inputNum,"10"),"3");
				break;
				//18:尺	y=1x/3;x=3y
			case 18:
				result=Arith.div(inputNum,"3");
				break;
				//19:寸	y=1x/30;x=30y
			case 19:
				result=Arith.div(Arith.mul(inputNum,"3"),"10");
				break;
				//20:分	y=1x/300;x=300y
			case 20:
				result=Arith.div(Arith.mul(inputNum,"3"),"100");
				break;
				//21:厘	y=1x/3000;x=3000y
			case 21:
				result=Arith.div(Arith.mul(inputNum,"3"),"1000");
				break;
				//22:毫	y=1x/30000;x=30000y
			case 22:
				result=Arith.div(Arith.mul(inputNum,"3"),"10000");
				break;
				//23:秒差距 y=3.08568×10^16x;x=y/3.08568×10^16
			case 23:
				result=Arith.mul(Arith.mul(inputNum,"3.08568"),"10000000000000000");
				break;
				//24:月球距离 y=3.84×10^8x;x=y/3.84×10^8
			case 24:
				result=Arith.mul(Arith.mul(inputNum,"3.84"),"100000000");
				break;
				//25:天文单位  y=149597870700x;x=y/149597870700	
			case 25:
				result=Arith.mul(inputNum,"149597870700");
				break;
				//26:光年单位 y=9460730472580800x;x=y/9460730472580800
			case 26:
				result=Arith.mul(inputNum,"9460730472580800");
				break;

			default:
				break;
			}
		} else {
			switch (unit) {
			//0:千米 y=1000x;x=y/1000
			case 0:
				result=Arith.div(inputNum,"1000");
				break;
				//1:米 y=x;x=y
			case 1:
				result=inputNum;
				break;
				//2:分米 y=0.1x;x=10y
			case 2:
				result=Arith.mul(inputNum,"10");
				break;
				//3:厘米 y=0.01x;x=100y;
			case 3:
				result=Arith.mul(inputNum,"100");
				break;
				//4:毫米 y=0.001x;x=1000y
			case 4:
				result=Arith.mul(inputNum,"1000");
				break;
				//5:微米 y=0.000001x;x=1000000y
			case 5:
				result=Arith.mul(inputNum,"1000000");
				break;
				//6:纳米 y=x/1000000000;x=1000000000y;
			case 6:
				result=Arith.mul(inputNum,"1000000000");
				break;
				//7:皮米 y=x/1000000000000;x=1000000000000y
			case 7:
				result=Arith.mul(inputNum,"1000000000000");
				break;
				//8:海里 y=1852x;x=y/1852
			case 8:
				result=Arith.div(Arith.div(inputNum,"1.852"),"1000");
				break;
				//9:英里 y=1609.344x;x=y/1609.344
			case 9:
				result=Arith.div(Arith.div(inputNum,"1.609344"),"1000");
				break;
				//10:弗隆 y=201.168x;x=y/201.168
			case 10:
				result=Arith.div(Arith.div(inputNum,"2.01168"), "100");
				break;
				//11:英寻 y=1.8288x;x=y/1.8288
			case 11:
				result=Arith.div(inputNum,"1.8288");
				break;
				//12:码 y=0.9144x;x=y/0.9144
			case 12:
				result=Arith.div(inputNum,"0.9144");
				break;
				//13:英尺 y=0.3048x;x=y/0.3048
			case 13:
				result=Arith.div(inputNum,"0.3048");
				break;
				//14:英寸 y=0.0254x;x=y/0.0254
			case 14:
				result=Arith.div(inputNum,"0.0254");
				break;
				//15:公里 y=1000x;x=y/1000
			case 15:
				result=Arith.div(inputNum,"1000");
				break;
				//16:里 y=500x;x=y/500
			case 16:
				result=Arith.div(inputNum,"500");
				break;
				//17:丈 y=10x/3;x=3y/10
			case 17:
				result=Arith.div(Arith.mul(inputNum,"3"),"10");
				break;
				//18:尺	y=1x/3;x=3y
			case 18:
				result=Arith.mul(inputNum,"3");
				break;
				//19:寸	y=1x/30;x=30y
			case 19:
				result=Arith.mul(inputNum,"30");
				break;
				//20:分	y=1x/300;x=300y
			case 20:
				result=Arith.mul(inputNum,"300");
				break;
				//21:厘	y=1x/3000;x=3000y
			case 21:
				result=Arith.mul(inputNum,"3000");
				break;
				//22:毫	y=1x/30000;x=30000y
			case 22:
				result=Arith.mul(inputNum,"30000");
				break;
				//23:秒差距 y=3.08568×10^16x;x=y/3.08568×10^16
			case 23:
				result=Arith.div(Arith.div(inputNum,"3.08568"),"10000000000000000");
				break;
				//24:月球距离 y=3.84×10^8x;x=y/3.84×10^8
			case 24:
				result=Arith.div(Arith.div(inputNum,"3.84"),"100000000");
				break;
				//25:天文单位  y=149597870700x;x=y/149597870700	
			case 25:
				result=Arith.div(Arith.div(inputNum,"1.495978707"),"100000000000");
				break;
				//26:光年单位 y=9460730472580800x;x=y/9460730472580800
			case 26:
				result=Arith.div(Arith.div(inputNum,"9.4607304725808"),"1000000000000000");
				break;

			default:
				break;
			}
		}
		return result;
	}

	private static String computeArea(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为平方米
		 */
		String result = "1";
		if (isToStandardUnit) {
			switch (unit) {
			//0:平方千米 y=10^6x;x=y/10^6
			case 0:
				result=Arith.mul(inputNum, "1000000");
				break;
				//1:公顷 y=10000x;x=y/10000
			case 1:
				result=Arith.mul(inputNum, "10000");
				break;
				//2:公亩 y=100x;x=y/100
			case 2:
				result=Arith.mul(inputNum, "100");
				break;
				//3:平方米 y=x;x=y
			case 3:
				result=inputNum;
				break;
				//4:平方分米 y=x/100;x=y*100
			case 4:
				result=Arith.div(inputNum, "100");
				break;
				//5:平方厘米 y=x/10000;x=10000y
			case 5:
				result=Arith.div(inputNum, "10000");
				break;
				//6:平方毫米 y=x/10^6;x=10^6*y
			case 6:
				result=Arith.div(inputNum, "1000000");
				break;
				//7:平方微米 y=x/10^12;x=10^12y
			case 7:
				result=Arith.div(inputNum, "1000000000000");
				break;
				//8:英亩  y=4046.864798x;x=y/4046.864798
			case 8:
				result=Arith.mul(inputNum, "4046.864798");
				break;
				//9:平方英里 y=2589998.11x;x=y/2589998.11
			case 9:
				result=Arith.mul(inputNum, "2589998.11");
				break;
				//10:平方码 y=0.83612736x;x=y/0.83612736
			case 10:
				result=Arith.mul(inputNum, "0.83612736");
				break;
				//11:平方英尺  y=0.09290304x;x=y/0.09290304
			case 11:
				result=Arith.mul(inputNum, "0.09290304");
				break;
				//12:平方英寸 y=0.00064516x;x=y/0.00064516
			case 12:
				result=Arith.mul(inputNum, "0.00064516");
				break;
				//13:平方竿 y=25.2928526x;x=y/25.2928526
			case 13:
				result=Arith.mul(inputNum, "25.2928526");
				break;	
				//14:顷 y=1000000x/15;x=15y/1000000
			case 14:
				result=Arith.div(Arith.mul(inputNum, "100000"), "1.5");
				break;
				//15:亩 y=10000x/15;x=15y/10000
			case 15:
				result=Arith.div(Arith.mul(inputNum, "1000"), "1.5");
				break;
				//16:平方尺 y=x/9;x=9y
			case 16:
				result=Arith.div(inputNum, "9");
				break;
				//17:平方寸 y=x/900;x=900y
			case 17:
				result=Arith.div(Arith.div(inputNum, "9"), "100");
				break;
				//18:平方公里  y=10^6x;x=y/10^6
			case 18:
				result=Arith.mul(inputNum, "1000000");
				break;

			default:
				break;
			}
		} else {
			switch (unit) {
			//0:平方千米 y=10^6x;x=y/10^6
			case 0:
				result=Arith.div(inputNum, "1000000");
				break;
				//1:公顷 y=10000x;x=y/10000
			case 1:
				result=Arith.div(inputNum, "10000");
				break;
				//2:公亩 y=100x;x=y/100
			case 2:
				result=Arith.div(inputNum, "100");
				break;
				//3:平方米 y=x;x=y
			case 3:
				result=inputNum;
				break;
				//4:平方分米 y=x/100;x=y*100
			case 4:
				result=Arith.mul(inputNum, "100");
				break;
				//5:平方厘米 y=x/10000;x=10000y
			case 5:
				result=Arith.mul(inputNum, "10000");
				break;
				//6:平方毫米 y=x/10^6;x=10^6*y
			case 6:
				result=Arith.mul(inputNum, "1000000");
				break;
				//7:平方微米 y=x/10^12;x=10^12y
			case 7:
				result=Arith.mul(inputNum, "1000000000000");
				break;
				//8:英亩  y=4046.864798x;x=y/4046.864798
			case 8:
				result=Arith.div(Arith.div(inputNum, "4.046864798"), "1000");
				break;
				//9:平方英里 y=2589998.11x;x=y/2589998.11
			case 9:
				result=Arith.div(Arith.div(inputNum, "2.58999811"), "1000000");
				break;
				//10:平方码 y=0.83612736x;x=y/0.83612736
			case 10:
				result=Arith.div(inputNum, "0.83612736");
				break;
				//11:平方英尺  y=0.09290304x;x=y/0.09290304
			case 11:
				result=Arith.div(inputNum, "0.09290304");
				break;
				//12:平方英寸 y=0.00064516x;x=y/0.00064516
			case 12:
				result=Arith.div(inputNum, "0.00064516");
				break;
				//13:平方竿 y=25.2928526x;x=y/25.2928526
			case 13:
				result=Arith.div(result=Arith.div(inputNum, "2.52928526"), "10");
				break;	
				//14:顷 y=1000000x/15;x=15y/1000000
			case 14:
				result=Arith.div(Arith.mul(inputNum, "15"), "1000000");
				break;
				//15:亩 y=10000x/15;x=15y/10000
			case 15:
				result=Arith.div(Arith.mul(inputNum, "15"), "10000");
				break;
				//16:平方尺 y=x/9;x=9y
			case 16:
				result=Arith.mul(inputNum, "9");
				break;
				//17:平方寸 y=x/900;x=900y
			case 17:
				result=Arith.mul(inputNum, "900");
				break;
				//18:平方公里  y=10^6x;x=y/10^6
			case 18:
				result=Arith.div(inputNum, "1000000");
				break;

			default:
				break;
			}
		}
		return result;
	}

	private static String computeVolume(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为立方米
		 */
		String result = "1";
		if (isToStandardUnit) {
			switch (unit) {
			//0:立方米 y=x;x=y
			case 0:
				result=inputNum;
				break;
				//1:立方分米 y=x/1000;x=1000x;
			case 1:
				result=Arith.div(inputNum, "1000");
				break;
				//2:立方厘米 y=x/1000000;x=1000000y
			case 2:
				result=Arith.div(inputNum, "1000000");
				break;
				//3:立方毫米 y=x/1000000000;x=1000000000y
			case 3:
				result=Arith.div(inputNum, "1000000000");
				break;
				//4:公石 y=0.1x;x=10y
			case 4:
				result=Arith.mul(inputNum, "0.1");
				break;
				//5:升 y=0.001x;y=1000x;
			case 5:
				result=Arith.mul(inputNum, "0.001");
				break;
				//6:分升 y=0.0001x;y=10000x;
			case 6:
				result=Arith.mul(inputNum, "0.0001");
				break;
				//7:厘升 y=0.00001x;x=100000y
			case 7:
				result=Arith.mul(inputNum, "0.00001");
				break;
				//8:毫升 y=0.000001x;x=1000000y
			case 8:
				result=Arith.mul(inputNum, "0.000001");
				break;
				//9:立方英尺 y=0.02831685x;x=y/0.02831685
			case 9:
				result=Arith.mul(inputNum, "0.02831685");
				break;
				//10:立方英寸 y=0.000016387064x;x=y/0.000016387064
			case 10:
				result=Arith.mul(inputNum, "0.000016387064");
				break;
				//11:立方码 y=0.76455486x;x=y/0.76455486
			case 11:
				result=Arith.mul(inputNum, "0.76455486");
				break;
				//12:亩英尺 y=1233.4818375x;x=y/1233.4818375
			case 12:
				result=Arith.mul(inputNum, "1233.4818375");
				break;

			default:
				break;
			}
		} else {
			switch (unit) {
			//0:立方米 y=x;x=y
			case 0:
				result=inputNum;
				break;
				//1:立方分米 y=x/1000;x=1000x;
			case 1:
				result=Arith.mul(inputNum, "1000");
				break;
				//2:立方厘米 y=x/1000000;x=1000000y
			case 2:
				result=Arith.mul(inputNum, "1000000");
				break;
				//3:立方毫米 y=x/1000000000;x=1000000000y
			case 3:
				result=Arith.mul(inputNum, "1000000000");
				break;
				//4:公石 y=0.1x;x=10y
			case 4:
				result=Arith.mul(inputNum, "10");
				break;
				//5:升 y=0.001x;y=1000x;
			case 5:
				result=Arith.mul(inputNum, "1000");
				break;
				//6:分升 y=0.0001x;y=10000x;
			case 6:
				result=Arith.mul(inputNum, "10000");
				break;
				//7:厘升 y=0.00001x;x=100000y
			case 7:
				result=Arith.mul(inputNum, "100000");
				break;
				//8:毫升 y=0.000001x;x=1000000y
			case 8:
				result=Arith.mul(inputNum, "1000000");
				break;
				//9:立方英尺 y=0.02831685x;x=y/0.02831685
			case 9:
				result=Arith.div(inputNum, "0.02831685");
				break;
				//10:立方英寸 y=0.000016387064x;x=y/0.000016387064
			case 10:
				result=Arith.div(inputNum, "0.000016387064");
				break;
				//11:立方码 y=0.76455486x;x=y/0.76455486
			case 11:
				result=Arith.div(inputNum, "0.76455486");
				break;
				//12:亩英尺 y=1233.4818375x;x=y/1233.4818375
			case 12:
				result=Arith.div(Arith.div(inputNum, "1.2334818375"), "1000");
				break;

			default:
				break;
			}
		}
		return result;
	}

	private static String computeTemperature(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为摄氏度℃ x:摄氏度℃
		 */
		String result = "1";
		if (isToStandardUnit) {
			// preUnit到摄氏度℃
			switch (unit) {
			// 0:摄氏度
			case 0:
				result = inputNum;
				break;
			// 1:华氏度 y=1.8x+32---x=(y-32)/1.8
			case 1:
				result = Arith.div(Arith.sub(inputNum, "32"), "1.8");
				break;
			// 2:开尔文 y=x+273.15----x=y-273.15
			case 2:
				result = Arith.sub(inputNum, "273.15");
				break;
			// 兰氏度 y=1.8x+32+459.67---x=(y-491.67)/1.8
			case 3:
				result = Arith.div(Arith.sub(inputNum, "491.67"), "1.8");
				break;
			// 列氏度 y=0.8x---x=y/0.8
			case 4:
				result = Arith.div(inputNum, "0.8");
				break;

			default:
				break;
			}
		} else {
			// 摄氏度℃到aftUnit
			switch (unit) {
			// 0:摄氏度
			case 0:
				result = inputNum;
				break;
			// 1:华氏度 y=1.8x+32---x=(y-32)/1.8
			case 1:
				result = Arith.add(Arith.mul(inputNum, "1.8"), "32");
				break;
			// 2:开尔文 y=x+273.15----x=y-273.15
			case 2:
				result = Arith.add(inputNum, "273.15");
				break;
			// 兰氏度 y=1.8x+32+459.67---x=(y-491.67)/1.8
			case 3:
				result = Arith.add(Arith.mul(inputNum, "1.8"), "491.67");
				break;
			// 列氏度 y=0.8x---x=y/0.8
			case 4:
				result = Arith.mul(inputNum, "0.8");
				break;

			default:
				break;
			}
		}
		return result;
	}

	private static String computeSpeed(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为米/秒 方程式左边为1;
		 */
		String result = "1";
		if (isToStandardUnit) {
			switch (unit) {
			// 0:光速 y=299792458x;x=y/299792458
			case 0:
				result = Arith.mul(inputNum, "299792458");
				break;
			// 1:马赫 y=340.3x;x=y/340.3
			case 1:
				result = Arith.mul(inputNum, "340.3");
				break;
			// 2:米/秒 y=x;x=y
			case 2:
				result = inputNum;
				break;
			// 3:千米/小时 y=1000x/3600;x=y*3.6
			case 3:
				result = Arith.div(inputNum, "3.6");
				break;
			// 4:千米/秒 y=1000x;x=y/1000;
			case 4:
				result = Arith.mul(inputNum, "1000");
				break;
			// 5:海里/小时 y=1852x/3600;x=3600y/1852
			case 5:
				result = Arith.div(Arith.mul(inputNum, "1.852"), "3.6");
				break;
			// 6:英里/小时 y=1609.344x/3600;x=3600y/1609.344
			case 6:
				result = Arith.div(Arith.mul(inputNum, "1.609344"), "3.6");
				break;
			// 英尺/秒 y=0.3048x;x=y/0.3048
			case 7:
				result = Arith.mul(inputNum, "0.3048");
				break;
			// 8:英寸/秒 y=(2.54/100)x;x=y/0.0254
			case 8:
				result = Arith.mul(inputNum, "0.0254");
				break;

			default:
				break;
			}
		} else {
			switch (unit) {
			// 0:光速 y=299792458x;x=y/299792458
			case 0:
				result = Arith.div(Arith.div(inputNum, "2.99792458"),
						"100000000");
				break;
			// 1:马赫 y=340.3x;x=y/340.3
			case 1:
				result = Arith.div(Arith.div(inputNum, "3.403"), "100");
				break;
			// 2:米/秒 y=x;x=y
			case 2:
				result = inputNum;
				break;
			// 3:千米/小时 y=1000x/3600;x=y*3.6
			case 3:
				result = Arith.mul(inputNum, "3.6");
				break;
			// 4:千米/秒 y=1000x;x=y/1000;
			case 4:
				result = Arith.div(inputNum, "1000");
				break;
			// 5:海里/小时 y=1852x/3600;x=3600y/1852
			case 5:
				result = Arith.div(Arith.mul(inputNum, "3.6"), "1.852");
				break;
			// 6:英里/小时 y=1609.344x/3600;x=3600y/1609.344
			case 6:
				result = Arith.div(Arith.mul(inputNum, "3.6"), "1.609344");
				break;
			// 英尺/秒 y=0.3048x;x=y/0.3048
			case 7:
				result = Arith.div(inputNum, "0.3048");
				break;
			// 8:英寸/秒 y=(2.54/100)x;x=y/0.0254
			case 8:
				result = Arith.div(inputNum, "0.0254");
				break;

			default:
				break;
			}
		}
		return result;
	}

	private static String computeTime(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为小时 x: 秒
		 */
		String result = "1";
		if (isToStandardUnit) {
			switch (unit) {
			// 0:年 y=365*24*3600x---x=y/31536000
			case 0:
				result = Arith.mul(inputNum, "31536000");
				break;
			// 1:周 y=7*24*3600x---x=y/604800
			case 1:
				result = Arith.mul(inputNum, "604800");
				break;
			// 2:天 y=24*3600x---x=y/86400
			case 2:
				result = Arith.mul(inputNum, "86400");
				break;
			// 3.小时 y=3600x---x=y/3600
			case 3:
				result = Arith.mul(inputNum, "3600");
				break;
			// 4.分 y=60x---x=y/60
			case 4:
				result = Arith.mul(inputNum, "60");
				break;
			// 5.秒 y=x---x=y
			case 5:
				result = inputNum;
				break;
			// 6.毫秒 y=x/10^3---x=y*1000
			case 6:
				result = Arith.div(inputNum, "1000");
				break;
			// 7.微秒 y=x/10^6---x=y*1000 000
			case 7:
				result = Arith.div(inputNum,"1000000");
				break;
			// 8.皮秒 y=x/10^12---x=y*1000 000 000 000
			case 8:
				result = Arith.div(inputNum,
						"1000000000000");
				break;
			default:
				break;
			}
		} else {
			switch (unit) {
			// 0:年 y=365*24*3600x---x=y/31536000
			case 0:
				result = Arith.div(Arith.div(inputNum, "3.1536"), "10000000");
				break;
			// 1:周 y=7*24*3600x---x=y/604800
			case 1:
				result = Arith.div( Arith.div(inputNum, "6.048"), "100000");
				break;
			// 2:天 y=24*3600x---x=y/86400
			case 2:
				result = Arith.div(Arith.div(inputNum, "8.64"), "10000");
				break;
			// 3.小时 y=3600x---x=y/3600
			case 3:
				result = Arith.div(Arith.div(inputNum, "3.6"), "1000");
				break;
			// 4.分 y=60x---x=y/60
			case 4:
				result = Arith.div(Arith.div(inputNum, "6"), "10");
				break;
			// 5.秒 y=x---x=y
			case 5:
				result = inputNum;
				break;
			// 6.毫秒 y=x/10^3---x=y*1000
			case 6:
				result = Arith.mul(inputNum, "1000");
				break;
			// 7.微秒 y=x/10^6---x=y*1000 000
			case 7:
				result = Arith.mul(inputNum,"1000000");
				break;
			// 8.皮秒 y=x/10^12---x=y*1000 000 000 000
			case 8:
				result = Arith.mul(inputNum,
						"1000000000000");
				break;
			default:
				break;
			}
		}
		return result;
	}

	private static String computeMass(String inputNum, int unit,
			boolean isToStandardUnit) {
		/**
		 * 标准单位设置为克
		 */
		String result = "1";
		if (isToStandardUnit) {
			switch (unit) {
			//0:吨 y=1000000x;x=y/1000000
			case 0:
				result=Arith.mul(inputNum, "1000000");
				break;
				//1:千克 y=1000x;x=y/1000
			case 1:
				result=Arith.mul(inputNum, "1000");
				break;
				//2:克 y=x;x=y
			case 2:
				result=inputNum;
				break;
				//3:毫克 y=x/1000;x=1000y;
			case 3:
				result=Arith.div(inputNum, "1000");
				break;
				//4:微克 y=x/1000000;x=1000000y;
			case 4:
				result=Arith.div(inputNum, "1000000");
				break;
				//5:公担 y=100000x;x=y/100000
			case 5:
				result=Arith.mul(inputNum, "100000");
				break;
				//6:磅 y=453.59237x;x=y/453.59237
			case 6:
				result=Arith.mul(inputNum, "453.59237");
				break;
				//7:盎司 y=453.59237x/16;x=16y/453.59237 1/16磅
			case 7:
				result=Arith.div(Arith.mul(inputNum, "45.359237"), "1.6");
				break;
				//8:克拉 y=0.2x;x=5y;
			case 8:
				result=Arith.mul(inputNum, "0.2");
				break;
				//9:格令 y=453.59237x/7000;x=7000y/453.59237
			case 9:
				result=Arith.div(Arith.mul(inputNum, "0.45359237"), "7");
				break;
				//10:长吨 y=1016046.91x;x=y/1016046.91 2205磅
			case 10:
				result=Arith.mul(inputNum, "1016046.91");
				break;
				//11:短吨 y=907184.74x;x=y/907184.74 2000磅
			case 11:
				result=Arith.mul(inputNum, "907184.74");
				break;
				//12:英担 y=50802.3454x;x=y/50802.3454  112磅
			case 12:
				result=Arith.mul(inputNum, "50802.3454");
				break;
				//13:美担 y=45359.237x;x=y/45359.237	100磅
			case 13:
				result=Arith.mul(inputNum, "45359.237");
				break;
				//14:英石 y=6350.29318x;x=y/6350.29318 14磅
			case 14:
				result=Arith.mul(inputNum, "6350.29318");
				break;
				//15:打兰 y=453.59237x/256;x=256y453.59237   1/256磅
			case 15:
				result=Arith.div(Arith.mul(inputNum, "4.5359237"), "2.56");
				break;
				//16:担  y=50000x;x=y/50000
			case 16:
				result=Arith.mul(inputNum, "50000");
				break;
				//17:斤 y=500x;x=y/500
			case 17:
				result=Arith.mul(inputNum, "500");
				break;
				//18:钱 y=5x;x=y/5
			case 18:
				result=Arith.mul(inputNum, "5");
				break;
				//19:两 y=50x;x=y/50
			case 19:
				result=Arith.mul(inputNum, "50");
				break;

			default:
				break;
			}
		} else {
			switch (unit) {
			//0:吨 y=1000000x;x=y/1000000
			case 0:
				result=Arith.div(inputNum, "1000000");
				break;
				//1:千克 y=1000x;x=y/1000
			case 1:
				result=Arith.div(inputNum, "1000");
				break;
				//2:克 y=x;x=y
			case 2:
				result=inputNum;
				break;
				//3:毫克 y=x/1000;x=1000y;
			case 3:
				result=Arith.mul(inputNum, "1000");
				break;
				//4:微克 y=x/1000000;x=1000000y;
			case 4:
				result=Arith.mul(inputNum, "1000000");
				break;
				//5:公担 y=100000x;x=y/100000
			case 5:
				result=Arith.div(inputNum, "100000");
				break;
				//6:磅 y=453.59237x;x=y/453.59237
			case 6:
				result=Arith.div(Arith.div(inputNum, "4.5359237"), "100");
				break;
				//7:盎司 y=453.59237x/16;x=16y/453.59237 1/16磅
			case 7:
				result=Arith.div(Arith.mul(inputNum, "0.16"), "4.5359237");
				break;
				//8:克拉 y=0.2x;x=5y;
			case 8:
				result=Arith.mul(inputNum, "5");
				break;
				//9:格令 y=453.59237x/7000;x=7000y/453.59237
			case 9:
				result=Arith.div(Arith.mul(inputNum, "70"), "4.5359237");
				break;
				//10:长吨 y=1016046.91x;x=y/1016046.91 2205磅
			case 10:
				result=Arith.div(Arith.div(inputNum, "1.01604691"), "1000000");
				break;
				//11:短吨 y=907184.74x;x=y/907184.74 2000磅
			case 11:
				result=Arith.div(Arith.div(inputNum, "9.0718474"), "100000");
				break;
				//12:英担 y=50802.3454x;x=y/50802.3454  112磅
			case 12:
				result=Arith.div(Arith.div(inputNum, "5.08023454"), "10000");
				break;
				//13:美担 y=45359.237x;x=y/45359.237	100磅
			case 13:
				result=Arith.div(Arith.div(inputNum, "4.5359237"), "10000");
				break;
				//14:英石 y=6350.29318x;x=y/6350.29318 14磅
			case 14:
				result=Arith.div(Arith.div(inputNum, "6.35029318"), "1000");
				break;
				//15:打兰 y=453.59237x/256;x=256y/453.59237   1/256磅
			case 15:
				result=Arith.div(Arith.mul(inputNum, "2.56"), "4.5359237");
				break;
				//16:担  y=50000x;x=y/50000
			case 16:
				result=Arith.div(inputNum, "50000");
				break;
				//17:斤 y=500x;x=y/500
			case 17:
				result=Arith.div(inputNum, "500");
				break;
				//18:钱 y=5x;x=y/5
			case 18:
				result=Arith.div(inputNum, "5");
				break;
				//19:两 y=50x;x=y/50
			case 19:
				result=Arith.div(inputNum, "50");
				break;

			default:
				break;
			}
		}
		return result;
	}
}
