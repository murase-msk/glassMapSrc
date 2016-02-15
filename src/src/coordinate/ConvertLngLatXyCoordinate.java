package src.coordinate;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.Point;

import java.util.ArrayList;

import src.db.getData.OsmRoadDataGeom;


/**
 * 緯度経度とアプレット座標の変換に関するクラス
 * @author murase
 *
 */
public class ConvertLngLatXyCoordinate {
	/** 左上の経度, 左上の緯度. */
	private Point2D _upperLeftLngLat;
	/** 右下の経度, 右下の緯度. */
	private Point2D _lowerRightLngLat;
	/** ウインドウサイズ */
	private Point _windowSize;
	
	/** 経度1,緯度1あたりのピクセル数 */
	public Point2D pixelPerLngLat;
	/** 1ピクセルあたりの緯度経度 */
	public Point2D lnglatPerPixel;
	/** 1ピクセルあたりの長さ(メートル) */
	public Point2D meterPerPixel;
	/** 緯度経度1あたりの長さ(メートル) */
//	public Point2D meterPerLngLat;
	
	/**
	 * 
	 * @param aUpperLeftLngLat	// 左上の緯度経度.
	 * @param aLowerRightLngLat	// 右下の緯度経度.
	 * @param aWindowSize		// ウインドウサイズ(x,y 縦横の長さ).
	 */
	public ConvertLngLatXyCoordinate(Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat,
			Point aWindowSize){
		_upperLeftLngLat = aUpperLeftLngLat;
		_lowerRightLngLat = aLowerRightLngLat;
		_windowSize = aWindowSize;
		lnglatPerPixel = new Point2D.Double(Math.abs(_upperLeftLngLat.getX()-_lowerRightLngLat.getX())/(_windowSize.getY())
		,Math.abs(_upperLeftLngLat.getY()-_lowerRightLngLat.getY())/(_windowSize.getY()));
		pixelPerLngLat = new Point2D.Double((_windowSize.getY()/Math.abs(_upperLeftLngLat.getX()-_lowerRightLngLat.getX()))
				,(_windowSize.getY())/Math.abs(_upperLeftLngLat.getY()-_lowerRightLngLat.getY()));
		meterPerPixel = new Point2D.Double(
				LngLatMercatorUtility.calcDistanceFromLngLat(_upperLeftLngLat, new Point2D.Double(_upperLeftLngLat.getX()+lnglatPerPixel.getX(), _upperLeftLngLat.getY())),
				LngLatMercatorUtility.calcDistanceFromLngLat(_upperLeftLngLat, new Point2D.Double(_upperLeftLngLat.getX(), _upperLeftLngLat.getY()+lnglatPerPixel.getY()))
				);
//		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();;
//		osmRoadDataGeom.startConnection();
//		meterPerPixel = osmRoadDataGeom.calcMeterPerPixel(
//				new Point2D.Double((_upperLeftLngLat.getX()+_lowerRightLngLat.getX())/2, (_upperLeftLngLat.getY()+_lowerRightLngLat.getY())/2), 
//				lnglatPerPixel);
//		meterPerLngLat = new Point2D.Double((meterPerPixel.getX()*pixelPerLngLat.getX()), (meterPerPixel.getY()*pixelPerLngLat.getY()));
//		osmRoadDataGeom.endConnection();
	}
	
	/**
	 * 緯度経度からアプレット内座標に変換.
	 * @param aLngLat 緯度経度
	 * @return　アプレット座標
	 */
	public Point convertLngLatToXyCoordinate(Point2D aLngLat){
		double width = _lowerRightLngLat.getX() - _upperLeftLngLat.getX();	// 右端から左端までの経度の差（経度であらわされている）.
		double hight = _upperLeftLngLat.getY() - _lowerRightLngLat.getY() ;	// 上端から下端までの緯度の差（緯度であらわされている）.
		double widthBase = _windowSize.x/width;				// widthBaseの逆数がアプレット1ドットあたりの経度の増加幅.
		double heightBase = _windowSize.y/hight;			// heightBaseの逆数がアプレット1ドットあたりの緯度の増加幅.
		
		int xtoi = (int)((aLngLat.getX() - _upperLeftLngLat.getX())*widthBase);	// アプレット内のｘ座標.
		int ytoi = (int)((aLngLat.getY() - _lowerRightLngLat.getY())*heightBase);	// アプレット内のy座標.
		return(new Point(xtoi, _windowSize.y - ytoi));	// 戻り値のY軸は反転させる必要がある.
	}
	
	/**
	 * 緯度経度からアプレット内座標に変換.
	 * @param aLngLatArray 緯度経度
	 * @return　アプレット座標
	 */
	public ArrayList<Point> convertLngLatToXyCoordinate(ArrayList<Point2D> aLngLatArray){
		Point lnglat;
		ArrayList<Point> XyCoordinateArray = new ArrayList<Point>();;
		for (int i=0; i<aLngLatArray.size(); i++) {
			lnglat = convertLngLatToXyCoordinate(aLngLatArray.get(i));
			XyCoordinateArray.add(new Point(lnglat.x,lnglat.y));
		}
		return XyCoordinateArray;
	}
	
	/**
	 * アプレット内座標から緯度経度に変換
	 * @param aXyCoordinate　アプレット内座標
	 * @return 緯度経度
	 */
	public Point2D.Double convertXyCoordinateToLngLat(Point aXyCoordinate){
		//aXyCoordinate.y = MapPanel.WINDOW_HEIGHT - aXyCoordinate.y;	// Y軸の反転.
		int XyCoordinateX = aXyCoordinate.x;
		int XyCoordinateY = _windowSize.y - aXyCoordinate.y;	// Y軸の反転.
		double width = _lowerRightLngLat.getX() - _upperLeftLngLat.getX();	// 右端から左端までの経度の差（経度であらわされている）.
		double hight = _upperLeftLngLat.getY() - _lowerRightLngLat.getY() ;	// 上端から下端までの緯度の差（緯度であらわされている）.
		double widthBase = _windowSize.x/width;			// widthBaseの逆数がアプレット1ドットあたりの経度の増加幅.
		double heightBase = _windowSize.y/hight;		// heightBaseの逆数がアプレット1ドットあたりの緯度の増加幅.
		
		return(new Point2D.Double((XyCoordinateX/widthBase)+_upperLeftLngLat.getX(),
				(XyCoordinateY/heightBase)+_lowerRightLngLat.getY()));
	}
	
	/**
	 * アプレット内座標から緯度経度に変換.
	 * @param aXyCoordinateArray　アプレット座標
	 * @return 緯度経度
	 */
	public ArrayList<Point2D> convertXyCoordinateToLngLat(ArrayList<Point> aXyCoordinateArray){
		Point2D XyCoordinate;
		ArrayList<Point2D> lnglatArray = new ArrayList<Point2D>();
		for (int i=0; i<aXyCoordinateArray.size(); i++) {
			XyCoordinate = convertXyCoordinateToLngLat(aXyCoordinateArray.get(i));
			lnglatArray.add(new Point2D.Double(XyCoordinate.getX(), XyCoordinate.getY()));
		}
		return lnglatArray;
	}
	
	/**
	 * 緯度経度からアプレット内座標に変換.
	 * @param aLine2Double
	 * @return
	 */
	public Line2D convertLngLatToXyCoordinateLine2D(Line2D aLine2Double){
		Line2D line2d = new Line2D.Double(convertLngLatToXyCoordinate((Point2D)aLine2Double.getP1()),
				convertLngLatToXyCoordinate((Point2D)aLine2Double.getP2()));
		return line2d;
	}
	public ArrayList<Line2D> convertLngLatToXyCoordinateLine2D(ArrayList<Line2D> aLine2dArrayList){
		ArrayList<Line2D> line2dArrayList = new ArrayList<>();
		for(int i=0; i<aLine2dArrayList.size(); i++){
			line2dArrayList.add(convertLngLatToXyCoordinateLine2D(aLine2dArrayList.get(i)));
		}
		return line2dArrayList;
	}
	
	
}
