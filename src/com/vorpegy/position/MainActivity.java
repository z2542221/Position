package com.vorpegy.position;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.vorpegy.position.LocationApplication.onRangeBeaconsInRegionListener;

public class MainActivity extends Activity implements
		onRangeBeaconsInRegionListener {
	private MyView mv;
	private ImageView img;
	// a, b, c, d分别为当前位置到四个ibeacon的距离
	private int a, b, c, d;
	// pa, pb, pc, pd为房间四个点的坐标 pn为当前定位坐标
	private Point pa, pb, pc, pd, pn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mv = new MyView(this);
		setContentView(mv);
		mv.init();
		hd.postDelayed(r, 2000);
		init();
	}

	private void init() {
		img = new ImageView(this);
		img.setBackgroundResource(R.drawable.a);
		pa = new Point(100, 50);
		pb = new Point(50, 100);
		pc = new Point(0, 50);
		pd = new Point(50, 0);
		pn = new Point(0, 0);
	}

	Handler hd = new Handler();
	Runnable r = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mv.removeMarker(img);
			if (a != 0 && b != 0 && c != 0 && d != 0)
				pn = calculate(a, b, c, d);
			mv.addMarker(img, pn.x, pn.y, -0.5f, -0.5f);
			hd.postDelayed(this, 1000);
		}
	};

	@Override
	public void onRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
		// TODO Auto-generated method stub
		if (beacons != null) {
			for (Beacon beacon : beacons) {
				int minor = beacon.getId3().toInt();
				if (minor == 1) {
					a = calculateAccuracy(beacon.getRssi());
				}
				if (minor == 2) {
					b = calculateAccuracy(beacon.getRssi());
				}
				if (minor == 3) {
					c = calculateAccuracy(beacon.getRssi());
				}
				if (minor == 4) {
					d = calculateAccuracy(beacon.getRssi());
				}
			}

		}
	}

	@Override
	protected void onResume() {

		super.onResume();
		((LocationApplication) this.getApplication())
				.setOnRangeBeaconsInRegionListener(this);

	}

	@Override
	protected void onPause() {
		super.onPause();
		((LocationApplication) this.getApplication())
				.setOnRangeBeaconsInRegionListener(null);

	}

	/**
	 * 
	 * @param rssi
	 *            传输距离损耗模型
	 * @return
	 */
	private int calculateAccuracy(float rssi) {
		int txPower = -58;
		if (rssi == 0) {
			return (int) -1.0; // if we cannot determine accuracy, return -1.
		}
		double ratio = rssi * 1.0 / txPower;
		if (ratio < 1.0) {
			return (int) (Math.pow(ratio, 10) * 10);
		} else {
			return (int) (((0.89976) * Math.pow(ratio, 7.7095) + 0.111) * 10);

		}
	}

	/**
	 * 用于计算
	 * 
	 * @param distance1为到已知点的距离
	 * @param distance2为到已知点的距离
	 * @param p1x已知点1的x坐标
	 * @param p1y已知点1的y坐标
	 * @param p2x已知点2的x坐标
	 * @param p2y已知点2的y坐标
	 *            a,b,c为公式y=ax^+bx+c的系数
	 * @return
	 */
	private ArrayList<Integer> getpoint(int distance1, int distance2, int p1x,
			int p1y, int p2x, int p2y) {
		double k, kb;
		double a, b, c;
		double x1, y1, x2, y2;
		// list用于保存两组坐标点
		ArrayList<Integer> list = new ArrayList<Integer>();
		// l为两个已知点的纵坐标差值 由于后面会用到除法 为防止报错 如果差值为0则改为1
		int l = p2y - p1y;
		if (l == 0)
			l = 1;
		k = (p1x - p2x) / l;
		kb = (distance1 * distance1 - distance2 * distance2 + p2x * p2x + p2y
				* p2y - p1x * p1x - p1y * p1y)
				/ (2 * l);
		a = 1 + k * k;
		b = 2 * k * (kb - p1y) - 2 * p1x;
		c = p1x * p1x + (kb - p1y) * (kb - p1y) - distance1 * distance1;
		// b^-4ac判断根的个数 小于0则返回空list
		if ((b * b - 4 * a * c) < 0) {
			return list;
		}
		// x1,x2,y1,y2为返回的坐标点
		x1 = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
		x2 = (-b - Math.sqrt(b * b - 4 * a * c)) / (a * a);
		y1 = k * x1 + kb;
		y2 = k * x2 + kb;
		list.add((int) x1);
		list.add((int) y1);
		list.add((int) x2);
		list.add((int) y2);
		return list;
	}

	/**
	 * 此方法返回经过三角定位计算后的坐标 p为异常时返回的坐标 p=（0，0）
	 * 
	 * @param a
	 *            =相对于ibeacon1的距离
	 * @param b
	 *            =相对于ibeacon2的距离
	 * @param c
	 *            =相对于ibeacon3的距离
	 * @param d
	 *            =相对于ibeacon4的距离
	 * @return
	 */
	private Point calculate(int a, int b, int c, int d) {
		Point p = new Point(0, 0);
		// list1,list2，list3，list4用于保存两组坐标值
		ArrayList<Integer> list1 = new ArrayList<Integer>();
		ArrayList<Integer> list2 = new ArrayList<Integer>();
		ArrayList<Integer> list3 = new ArrayList<Integer>();
		ArrayList<Integer> list4 = new ArrayList<Integer>();
		// listpoint用于保存4组离中心点最近的坐标值
		ArrayList<List<Integer>> listpoint = new ArrayList<List<Integer>>();
		// prox1,prox2,prox3,prox4用于表示到中心点最近坐标
		// 由于有四条边所以prox1,proy1,prox2,proy2复用四次
		int prox1, proy1, prox2, proy2;
		// p1,p2,p3,p4用于保存四个离中心点最近的坐标
		Point p1 = new Point();
		Point p2 = new Point();
		Point p3 = new Point();
		Point p4 = new Point();
		// p5,p6为两三角形质心 由于四边形可分解成两个三角形
		// 由于有两个三角形 p5，p6复用一次
		Point p5 = new Point();
		Point p6 = new Point();
		// p7，p8为p5，p6两质心的中心点
		Point p7 = new Point();
		Point p8 = new Point();
		// p9为最终返回的坐标
		Point p9 = new Point();
		// 经过计算后的坐标值
		// a，b,c,d为位置点到已知点pa,pb,pc,pd的距离
		// 只计算相邻的两条边 相对的两条边由于误差的存在会出现无解的情况
		list1 = getpoint(a, b, pa.x, pa.y, pb.x, pb.y);
		list2 = getpoint(a, d, pa.x, pa.y, pd.x, pd.y);
		list3 = getpoint(b, c, pb.x, pb.y, pc.x, pc.y);
		list4 = getpoint(c, d, pc.x, pc.y, pd.x, pd.y);
		// 之所以判断大小是因为此处容易异常
		if (list1.size() == 4)
			listpoint.add(list1);
		if (list2.size() == 4)
			listpoint.add(list2);
		if (list3.size() == 4)
			listpoint.add(list3);
		if (list4.size() == 4)
			listpoint.add(list4);
		if (listpoint.size() < 3)
			return p;
		// 如果有四个点 则四点组成四边形 再分割成两个三角形 分别计算两个三角形的质心
		// 取两个质心的中点为最终定位坐标
		if (listpoint.size() == 4) {
			prox1 = list1.get(0) - 50;
			proy1 = list1.get(1) - 50;
			prox2 = list1.get(2) - 50;
			proy2 = list1.get(3) - 50;
			// 两点间距离公式
			if (Math.sqrt(prox1 * prox1 + proy1 * proy1) > Math.sqrt(prox2
					* prox2 + proy2 * proy2)) {
				p1.x = list1.get(2);
				p1.y = list1.get(3);
			} else {
				p1.x = list1.get(0);
				p1.y = list1.get(1);
			}
			prox1 = list2.get(0) - 50;
			proy1 = list2.get(1) - 50;
			prox2 = list2.get(2) - 50;
			proy2 = list2.get(3) - 50;
			if (Math.sqrt(prox1 * prox1 + proy1 * proy1) > Math.sqrt(prox2
					* prox2 + proy2 * proy2)) {
				p2.x = list2.get(2);
				p2.y = list2.get(3);
			} else {
				p2.x = list2.get(0);
				p2.y = list2.get(1);
			}
			prox1 = list3.get(0) - 50;
			proy1 = list3.get(1) - 50;
			prox2 = list3.get(2) - 50;
			proy2 = list3.get(3) - 50;
			if (Math.sqrt(prox1 * prox1 + proy1 * proy1) > Math.sqrt(prox2
					* prox2 + proy2 * proy2)) {
				p3.x = list3.get(2);
				p3.y = list3.get(3);
			} else {
				p3.x = list3.get(0);
				p3.y = list3.get(1);
			}
			prox1 = list4.get(0) - 50;
			proy1 = list4.get(1) - 50;
			prox2 = list4.get(2) - 50;
			proy2 = list4.get(3) - 50;
			if (Math.sqrt(prox1 * prox1 + proy1 * proy1) > Math.sqrt(prox2
					* prox2 + proy2 * proy2)) {
				p4.x = list4.get(2);
				p4.y = list4.get(3);
			} else {
				p4.x = list4.get(0);
				p4.y = list4.get(1);
			}

			p5.x = (p1.x + p2.x + p3.x) / 3;
			p5.y = (p1.y + p2.y + p3.y) / 3;
			p6.x = (p1.x + p3.x + p4.x) / 3;
			p6.y = (p1.y + p3.y + p4.y) / 3;
			p7.x = (p5.x + p6.x) / 2;
			p7.y = (p5.y + p6.y) / 2;
			// System.out.println(p7);
			p5.x = (p1.x + p2.x + p4.x) / 3;
			p5.y = (p1.y + p2.y + p4.y) / 3;
			p6.x = (p2.x + p3.x + p4.x) / 3;
			p6.y = (p2.y + p3.y + p4.y) / 3;
			p8.x = (p5.x + p6.x) / 2;
			p8.y = (p5.y + p6.y) / 2;
			// p9取两质心的中心点的中心点
			// * 10.8和* 17.16 + 34.32用于转换成手机屏幕坐标
			p9.x = (int) (((p7.x + p8.x) / 2) * 10.8);
			p9.y = 1716 - (int) (((p7.y + p8.y) / 2) * 17.16);
		}
		// System.out.println(p7);
		// System.out.println(getpoint(a, b, pa.x, pa.y, pb.x, pb.y) +
		// "-------->"
		// + p1);
		// System.out.println(getpoint(a, d, pa.x, pa.y, pd.x, pd.y) +
		// "-------->"
		// + p2);
		// System.out.println(getpoint(b, c, pb.x, pb.y, pc.x, pc.y) +
		// "-------->"
		// + p3);
		// System.out.println(getpoint(c, d, pc.x, pc.y, pd.x, pd.y) +
		// "-------->"
		// + p4);
		d(p9);
		// 不能超过地图的最大坐标
		if (p9.x > 1080 || p9.x < 1 || p9.y > 1716 || p9.y < 1)
			p9 = p;
		return p9;
	}

	private void d(Object obj) {
		Log.d("MainActivity", String.valueOf(obj));
	}

}
