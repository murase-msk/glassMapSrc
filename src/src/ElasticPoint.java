package src;

import java.awt.geom.Point2D;

/**
 * glue領域の点を変形させる処理に関するクラス
 * @author murase
 *
 */
public class ElasticPoint {
	
	/** focus-glue間の滑らかさ(0～1) */
	private static double FOCUS_SIDE_SMOOTH_RATIO = 0.4;
	/** context-glue間の滑らかさ(0～1) */
	private static double CONTEXT_SIDE_SMOOTH_RATIO = 0.2;
	
	// 中心からglue内側までの長さ(球面メルカトル座標系).
	private double _glueInnerLengthMercator;
	// 中心からglue外側までの長さ(球面メルカトル座標系).
	private double _glueOuterLengthMercator;
	// 拡大率.
	private double _magnifyingPower;
	// 中心点の座標(球面メルカトル座標系).
	private Point2D _centerPointMercator;
	
	
	// 開始点，終了点，制御点12(中心点を原点とした球面メルカトル座標系).
	private Point2D _startPoint, _endPoint, _controlPoint1, _controlPoint2;
	// 中心から対象の点までの長さ(球面メルカトル座標系).
	private double _pointLengthMercator;
	
	// 対称点の座標(球面メルカトル座標系).
	//private Point2D _pointMercator;
	
	/**
	 * コンストラクタ　最初の設定
	 * @param aGlueInnerLengthMercator 中心からglue内側までの長さ(球面メルカトル座標系).
	 * @param aGlueOuterLengthMercator 中心からglue外側までの長さ(球面メルカトル座標系).
	 * @param aMagnifyingPower 拡大率.
	 * @param aCenterPointMercator 中心点の座標(球面メルカトル座標系).
	 */
	public ElasticPoint(double aGlueInnerLengthMercator, double aGlueOuterLengthMercator, double aMagnifyingPower, Point2D aCenterPointMercator) {
		_glueInnerLengthMercator = aGlueInnerLengthMercator;
		_glueOuterLengthMercator = aGlueOuterLengthMercator;
		_magnifyingPower = aMagnifyingPower;
		_centerPointMercator = aCenterPointMercator;

		// 開始点，終了点，制御点の計算.
		_startPoint = new Point2D.Double(_glueInnerLengthMercator/_magnifyingPower, _glueInnerLengthMercator);
		_endPoint = new Point2D.Double(_glueOuterLengthMercator, _glueOuterLengthMercator);
		double yd = _endPoint.getY() - _startPoint.getY();
		double ydF = yd * _magnifyingPower;
		_controlPoint1 = new Point2D.Double(_startPoint.getX()+ydF*FOCUS_SIDE_SMOOTH_RATIO, _startPoint.getY()+yd*FOCUS_SIDE_SMOOTH_RATIO);
		_controlPoint2 = new Point2D.Double(_endPoint.getX()-yd*CONTEXT_SIDE_SMOOTH_RATIO, _endPoint.getY()-yd*CONTEXT_SIDE_SMOOTH_RATIO);
		
//		System.out.println(_magnifyingPower);
//		System.out.println(_glueInnerLengthMercator);
//		System.out.println(_glueOuterLengthMercator);
//		System.out.println(_startPoint);
//		System.out.println(_endPoint);
//		System.out.println(_controlPoint1);
//		System.out.println(_controlPoint2);
//		System.exit(0);
	}
	
	
	/**
	 * 点を変形
	 * @param aPoint ある点の座標(メルカトル座標系)
	 * @param aOuterRatio glueの中でもどれくらいの位置にあるか(0~1)(0:glueの内側境界，1:glueの外側境界)
	 * @return 中心からどれだけ離れた位置に移動させるか(メルカトル座標系で)
	 */
	public Point2D calcElasticPoint(Point2D aPoint, double aOuterRatio){
		// 中心から対称点までの長さ.
		_pointLengthMercator = Math.hypot(_centerPointMercator.getX()-aPoint.getX(), _centerPointMercator.getY()-aPoint.getY());
		// 中心からどれだけ離れた位置に移動するか.
		double movedLength = bezierCurve(aOuterRatio, _startPoint, _controlPoint1, _controlPoint2, _endPoint).getY();
		// 元の点に対して移動した点の増加幅.
		double nobi = movedLength/_pointLengthMercator;
//		System.out.println(aOuterRatio);
//		System.out.println("##############");
//		System.out.println(_pointLengthMercator);
//		System.out.println(movedLength);
//		System.out.println(nobi);
//		System.exit(0);
		return new Point2D.Double(
				(aPoint.getX()-_centerPointMercator.getX())*nobi+_centerPointMercator.getX(), 
				(aPoint.getY()-_centerPointMercator.getY())*nobi+_centerPointMercator.getY());
	}
	
	
	
	// 3次ベジェ曲線の数式
	// http://geom.web.fc2.com/geometry/bezier/cubic.html
	// 中学生でもわかるベジェ曲線
	// http://blog.sigbus.info/2011/10/bezier.html
	// Flashゲーム講座 & アクションスクリプトサンプル集
	// http://hakuhin.jp/as/curve.html
	/**
	 * ベジェ曲線の計算(再配置関数)
	 * @param t ベジェ曲線のパラメータ
	 * @param p1　開始座標
	 * @param p2　制御座標１
	 * @param p3　制御座標２
	 * @param p4　終了座標
	 * @return
	 */
	private Point2D bezierCurve(double t, Point2D p1, Point2D p2, Point2D p3, Point2D p4){
		double t2=1-t;
		return new Point2D.Double(
				t2*t2*t2*p1.getX()+3*t2*t2*t*p2.getX()+3*t2*t*t*p3.getX()+t*t*t*p4.getX(),
				t2*t2*t2*p1.getY()+3*t2*t2*t*p2.getY()+3*t2*t*t*p3.getY()+t*t*t*p4.getY());
	}
	

}
