package src.coordinate;

import java.awt.geom.Point2D;

/**
 * 緯度経度(EPSG:4326)と球面メルカトル座標(EPSG:3857)の変換などのユーティリティー
 * @author murase
 *
 */
public class LngLatMercatorUtility {
	
	/** 地球の半径 */
	private static int EARTH_RADIUS = 6378137;
	
	/** 緯度経度座標系 */
	private Point2D lnglat;
	/** 球面メルカトル座標系 */
	private Point2D mercator;
	
	
	//http://qiita.com/kochizufan/items/bf880c8d2b25385d4efe
	/**
	 * 緯度経度座標系から，球面メルカトル座標系へと変換
	 * @param aLngLat
	 * @return
	 */
	public static Point2D ConvertLngLatToMercator(Point2D aLngLat){
		return new Point2D.Double(
				EARTH_RADIUS*aLngLat.getX()*Math.PI/180, 
				EARTH_RADIUS*Math.log(Math.tan(Math.PI/360*(90+aLngLat.getY())))
			);
	}
	
	/**
	 * 球面メルカトル座標系から緯度経度座標系へと変換
	 * @param aMercator
	 * @return
	 */
	public static Point2D ConvertMercatorToLngLat(Point2D aMercator){
		return new Point2D.Double(
				180/(Math.PI*EARTH_RADIUS)*aMercator.getX(), 
				90+Math.atan(Math.pow(Math.E, aMercator.getY()/EARTH_RADIUS))*360/Math.PI);
	}
	
	/***
	 * 2点の緯度経度座標系から距離を求める
	 * Haversine formulaを使う
	 * https://en.wikipedia.org/wiki/Haversine_formula
	 * 球面3角法を使う
	 * http://www.astro.sci.yamaguchi-u.ac.jp/~kenta/eclipse/SphericalTriangle081106.pdf
	 */
	public static double calcDistanceFromLngLat(Point2D aLngLat1, Point2D aLngLat2){
		//Haversine formula
		return 2*EARTH_RADIUS*
				Math.asin(
					Math.sqrt(
						Math.pow(Math.sin(Math.toRadians((aLngLat2.getY()-aLngLat1.getY())/2)),2) +
						Math.cos(Math.toRadians(aLngLat1.getY()))*Math.cos(Math.toRadians(aLngLat2.getY()))*Math.pow(Math.sin(Math.toRadians((aLngLat2.getX()-aLngLat1.getX())/2)), 2)
					)
				);
		
		// 球面3角法.
//		return EARTH_RADIUS*Math.acos(
//					Math.sin(Math.toRadians(aLngLat1.getY()))*
//					Math.sin(Math.toRadians(aLngLat2.getY())+
//					Math.cos(Math.toRadians(aLngLat1.getY()))*
//					Math.cos(Math.toRadians(aLngLat2.getY()))*
//					Math.cos(Math.toRadians(aLngLat2.getX()-aLngLat1.getX()))));

	}
	
	/***
	 * 2点の球面メルカトル座標系から距離を求める
	 * @param aMercator1
	 * @param aMercator2
	 * @return
	 */
	public static double calcDistanceFromMercator(Point2D aMercator1, Point2D aMercator2){
		// 緯度経度に変換.
		Point2D lnglat1 = LngLatMercatorUtility.ConvertMercatorToLngLat(aMercator1);
		Point2D lnglat2 = LngLatMercatorUtility.ConvertMercatorToLngLat(aMercator2);
		// 緯度経度から距離を求める.
		return calcDistanceFromLngLat(lnglat1, lnglat2);
	}
	
}
