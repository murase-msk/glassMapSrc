package src.coordinate;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

/**
 * 中心緯度経度，ズームレベル，画像サイズから端点の緯度経度を求める
 * @author murase
 *
 */
public class GetLngLatOsm {
	
	/** 左上の緯度経度 */
	public Point2D _upperLeftLngLat;
	/** 右上の緯度経度 */
	public Point2D _lowerRightLngLat;
	
	/** 1枚のタイル画像の大きさ(256ピクセルx256ピクセル) */
	private static int oneTileImageSize=256;
	
	public GetLngLatOsm(Point2D aCenterLngLat, int aZoom, Point ImageSize){
		
		Point tileXy;	// タイル座標.
		tileXy = getTileNumber(aCenterLngLat.getY(), aCenterLngLat.getX(), aZoom);
//		System.out.println("タイル番号"+tileXy);
		Point2D onePixelLngLat;
		BoundingBox bbox = tile2boundingBox((int)tileXy.getX(), (int)tileXy.getY(), aZoom);
//		System.out.println("タイルの端点"+bbox);
		onePixelLngLat = new Point2D.Double(((double)bbox.east-bbox.west)/oneTileImageSize, ((double)bbox.north-bbox.south)/oneTileImageSize);
		
		_upperLeftLngLat = new Point2D.Double(aCenterLngLat.getX() - (ImageSize.x/2)*onePixelLngLat.getX(), aCenterLngLat.getY()+(ImageSize.y/2)*onePixelLngLat.getY());
		_lowerRightLngLat = new Point2D.Double(aCenterLngLat.getX() + (ImageSize.x/2)*onePixelLngLat.getX(), aCenterLngLat.getY()-(ImageSize.y/2)*onePixelLngLat.getY());
		
		
		
//		System.out.println("upperLeftLngLat"+_upperLeftLngLat);
//		System.out.println("lowerRightLngLat"+_lowerRightLngLat);
	}
	
	
	
	
	/**
	 * 
	 * 緯度経度(degree)とズームレベルからタイル座標を求める
	 * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
	 * 
	 * @param lat
	 * @param lon
	 * @param zoom
	 * @return
	 */
	 private static Point getTileNumber(final double lat, final double lon, final int zoom) {
		int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
		int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
		if (xtile < 0)
		xtile=0;
		if (xtile >= (1<<zoom))
			xtile=((1<<zoom)-1);
		if (ytile < 0)
			ytile=0;
		if (ytile >= (1<<zoom))
			ytile=((1<<zoom)-1);
		return new Point(xtile, ytile) ;
	}
	
	 
	 class BoundingBox {
		double north;
		double south;
		double east;
		double west;   
	}
	/**
	 * タイル番号からそのタイル(256x256)の端点の座標(degree)を求める
	 * http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @return
	 */
	private BoundingBox tile2boundingBox(final int x, final int y, int zoom) {
	//	zoom = 16;
//		System.out.println("x"+x+"  y"+y+"   z"+zoom);
		BoundingBox bb = new BoundingBox();
		bb.north = tile2lat(y, zoom);
		bb.south = tile2lat(y + 1, zoom);
		bb.west = tile2lon(x, zoom);
		bb.east = tile2lon(x + 1, zoom);
//		System.out.println("north "+bb.north);
//		System.out.println("south "+bb.south);
//		System.out.println("east "+bb.east);
//		System.out.println("west "+bb.west);
		return bb;
	}
	
	private static double tile2lon(int x, int z) {
		return ((double)x / Math.pow(2.0, z)) * 360.0 - 180;
	}
	
	private static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
	 
	 
	 
	 
	 
}
