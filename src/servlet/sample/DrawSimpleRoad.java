package servlet.sample;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.db.getData.OsmRoadDataGeom;

/**
 * 単純に道路の描画
 * @author murase
 *
 */
public class DrawSimpleRoad {
	/** 地図パネルの横幅. */
	public static  int WINDOW_WIDTH = 700;
	/** 地図パネルの高さ. */
	public static  int WINDOW_HEIGHT = 700;
	/** 初期の経度. */
	private static final double DEFAULT_LNG = 136.9309671669116;	// 鶴舞公園.
	/** 初期の緯度. */
	private static final double DEFAULT_LAT = 35.15478942665804;	// 鶴舞公園.
	/** 初期のスケール. */
	private static final int DEFAULT_SCALE = 15;
	
	
	Graphics2D _graphics2d;
	public GetLngLatOsm _getLngLatOsm;
	public ConvertLngLatXyCoordinate _convert;
	public Point2D _upperLeftLngLat;
	public Point2D _lowerRightLngLat;
	
	public DrawSimpleRoad(HttpServletRequest request, HttpServletResponse response) {
		try{
			OutputStream out=response.getOutputStream();
			//BufferedImage img = emgd.getEmGlueImage(param);
			ImageIO.write( drawImage(), "png", out);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 道路データの取得しbufferedimageの作成
	 * @return
	 */
	private BufferedImage drawImage(){
		BufferedImage bfImage=null;
		bfImage=new BufferedImage( WINDOW_WIDTH, WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		_graphics2d = (Graphics2D) bfImage.getGraphics();
		
		_graphics2d.setColor(Color.black);
		_graphics2d.drawLine(0,0,10,10);
		System.out.println("ok");
		
		
		// 中心座標とスケールから 左上と右下の座標取得.
		_getLngLatOsm = new GetLngLatOsm(new Point2D.Double(DEFAULT_LNG, DEFAULT_LAT), DEFAULT_SCALE, new Point(WINDOW_WIDTH, WINDOW_HEIGHT));
		_upperLeftLngLat = _getLngLatOsm._upperLeftLngLat;
		_lowerRightLngLat = _getLngLatOsm._lowerRightLngLat;
		// 緯度経度とXy座標の変換用インスタンス.
		_convert = new ConvertLngLatXyCoordinate((Point2D.Double)_upperLeftLngLat,
				(Point2D.Double)_lowerRightLngLat, new Point(WINDOW_WIDTH, WINDOW_HEIGHT));
		
		// 道路データの取得.
		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
		osmRoadDataGeom.startConnection();
		osmRoadDataGeom.insertOsmRoadData(_upperLeftLngLat, _lowerRightLngLat,"car", "");
		osmRoadDataGeom.endConnection();
		
		// 道路の描画.
		paintRoadData(osmRoadDataGeom._arc);
		
		
		
		_graphics2d.setColor(Color.red);
		// 中心点.
		_graphics2d.drawOval(348, 348, 4, 4);
		// glue領域内側想定範囲.
		_graphics2d.drawOval(300, 300, 100, 100);
		// glue領域外側想定範囲.
		_graphics2d.drawOval(250, 250, 200, 200);
		
		return bfImage;
	}
	
	
	// 道路データの描画.
	public void paintRoadData(ArrayList<ArrayList<Line2D>> _arc){
		for(ArrayList<Line2D> arrArc : _arc){
			for(Line2D arc : arrArc){
				paint2dLine(_convert.convertLngLatToXyCoordinateLine2D(arc),
					Color.black, (float)3);
				// 点の描画.
				paint2dEllipse((Point2D)_convert.convertLngLatToXyCoordinate(arc.getP1()), Color.black, 6);
				paint2dEllipse((Point2D)_convert.convertLngLatToXyCoordinate(arc.getP2()), Color.black, 6);
			}
		}
	}
	
	// 線分の描画.
	private void paint2dLine(Line2D aLine, Color aColor, float aLineWidth){
		Line2D linkLine = aLine;
		// 線の幅.
		BasicStroke wideStroke = new BasicStroke(aLineWidth);
		_graphics2d.setStroke(wideStroke);
		_graphics2d.setPaint(aColor);
		_graphics2d.draw(linkLine);
	}
	
	// 円.
	private void paint2dEllipse(Point2D aCenterPointDouble, Color aColor, int aMarkerSize){
		_graphics2d.setPaint(aColor);
		Ellipse2D.Double ellipse = new Ellipse2D.Double(aCenterPointDouble.getX() - aMarkerSize/2,
				aCenterPointDouble.getY() - aMarkerSize/2, aMarkerSize, aMarkerSize);
		_graphics2d.fill(ellipse);	// 内部塗りつぶし.
		BasicStroke wideStroke = new BasicStroke(1.0f);
		_graphics2d.setStroke(wideStroke);
		_graphics2d.setPaint(Color.black);
		_graphics2d.draw(ellipse);	// 輪郭の描画.
	}
	

}
