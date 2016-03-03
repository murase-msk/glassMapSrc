package servlet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import src.ElasticPoint;
import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.coordinate.LngLatMercatorUtility;
import src.db.getData.OsmLineDataGeom;
import src.db.getData.OsmRoadDataGeom;
import src.db.getData.OsmStrokeDataGeom;
import src.paint.PaintGlueRoad;

/**
 * 色を付け他ストロークを使う
 * @author murase
 *
 */
public class DrawElasticStroke_v2 {
	
	/** 地図の大きさ */
	public Point windowSize = new Point(700, 700);
	/** 初期の緯度経度Point2D形式 */
	private  Point2D centerLngLat = new Point2D.Double(136.9309671669116, 35.15478942665804);// 鶴舞公園.
	/** focusのスケール */
	private int focusScale = 17;
	/** contextのスケール */
	private int contextScale = 15;
	/** glue内側の半径(pixel) */
	private int glueInnerRadius=200;
	/** glue外側の半径(pixel) */
	private int glueOuterRadius=300;
	
	/** 描画するストロークの数 */
	private static final int STROKE_NUM = 30;
	
	/** 道路の種類(car, bikeFoot) */
	public String roadType = "car";
	
	/** 中心点からglue内側の長さ(メートル) */
	public double glueInnerRadiusMeter;
	/** 中心点からglue外側の長さ(メートル)  */
	public double glueOuterRadiusMeter;
	
	/** 描画用 */
	Graphics2D _graphics2d;
	/** focusの端点の緯度経度を求める */
	public GetLngLatOsm _getLngLatOsmFocus;
	/** focus領域の緯度経度xy変換 */
	public ConvertLngLatXyCoordinate _convertFocus;
	/** contextの端点の緯度経度を求める */
	public GetLngLatOsm _getLngLatOsmContext;
	/** context領域の緯度経度xy変換 */
	public ConvertLngLatXyCoordinate _convertContext;
	/** メルカトル座標系xy変換 */
	public ConvertMercatorXyCoordinate _contextMercatorConvert;

	
	public DrawElasticStroke_v2(HttpServletRequest request, HttpServletResponse response) {
		
		// 必須パラメータがあるか.
		if(request.getParameter("centerLngLat")==null ||
				request.getParameter("focus_zoom_level")==null ||
				request.getParameter("context_zoom_level")==null ||
				request.getParameter("glue_inner_radius")==null ||
				request.getParameter("glue_outer_radius")==null
				){
			ErrorMsg.errorResponse(request, response, "必要なパラメータがありません");
			return;
		}
		// パラメータの受け取り.
		centerLngLat = new Point2D.Double(
				Double.parseDouble(request.getParameter("centerLngLat").split(",")[0]), 
				Double.parseDouble(request.getParameter("centerLngLat").split(",")[1]));
		focusScale = Integer.parseInt(request.getParameter("focus_zoom_level"));
		contextScale = Integer.parseInt(request.getParameter("context_zoom_level"));
		glueInnerRadius = Integer.parseInt(request.getParameter("glue_inner_radius"));
		glueOuterRadius = Integer.parseInt(request.getParameter("glue_outer_radius"));
		windowSize = new Point(glueOuterRadius*2, glueOuterRadius*2);
		roadType = request.getParameter("roadType") == null  ? "car" : request.getParameter("roadType");
		try{
			OutputStream out=response.getOutputStream();
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
		bfImage=new BufferedImage( windowSize.x, windowSize.y, BufferedImage.TYPE_INT_ARGB);
		_graphics2d = (Graphics2D) bfImage.getGraphics();
		_graphics2d.setBackground(new Color(241,238,232));	// 背景指定.
		_graphics2d.clearRect(0, 0, windowSize.x, windowSize.y);
		// アンチエイリアス設定：遅いときは次の行をコメントアウトする.
		_graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		//focus用の緯度経度xy変換
		_getLngLatOsmFocus = new GetLngLatOsm(centerLngLat, focusScale, windowSize);
		_convertFocus = new ConvertLngLatXyCoordinate((Point2D.Double)_getLngLatOsmFocus._upperLeftLngLat,
				(Point2D.Double)_getLngLatOsmFocus._lowerRightLngLat, windowSize);
		//context用の緯度経度xy変換
		_getLngLatOsmContext = new GetLngLatOsm(centerLngLat, contextScale, windowSize);
		_convertContext = new ConvertLngLatXyCoordinate((Point2D.Double)_getLngLatOsmContext._upperLeftLngLat,
				(Point2D.Double)_getLngLatOsmContext._lowerRightLngLat, windowSize);
		glueInnerRadiusMeter = glueInnerRadius*_convertFocus.meterPerPixel.getX();
		glueOuterRadiusMeter = glueOuterRadius*_convertContext.meterPerPixel.getX();
		// contextでのメルカトル座標系xy変換.
		_contextMercatorConvert = new ConvertMercatorXyCoordinate(
				LngLatMercatorUtility.ConvertLngLatToMercator((Point2D.Double)_getLngLatOsmContext._upperLeftLngLat), 
				LngLatMercatorUtility.ConvertLngLatToMercator((Point2D.Double)_getLngLatOsmContext._lowerRightLngLat), windowSize);
		
		// 描画する道路などのデータ.
		ArrayList<ArrayList<Point2D>> roadPath = new ArrayList<>();
		// その時の道路クラス.
		ArrayList<Integer> clazzList = new ArrayList<>();

		//////////////////////////////////////////////
		// ストローク取得.
		OsmStrokeDataGeom osmStrokeDataGeom = new OsmStrokeDataGeom();
		osmStrokeDataGeom.startConnection();
		// 左上の座標(xy座標((0, 0)になるはず)).
		Point upperLeftOuterGlueXY = new Point(windowSize.x/2-glueOuterRadius, windowSize.x/2-glueOuterRadius);
		// 右下の座標(xy座標((windowsize, windowsize)になるはず).
		Point LowerRightOuterGlueXY = new Point(windowSize.x/2-glueOuterRadius + glueOuterRadius*2, windowSize.x/2-glueOuterRadius + glueOuterRadius*2);
		// context(全体)にかかるストロークを取り出す.
		osmStrokeDataGeom.insertStrokeData(_convertContext.convertXyCoordinateToLngLat(upperLeftOuterGlueXY), _convertContext.convertXyCoordinateToLngLat(LowerRightOuterGlueXY));
		osmStrokeDataGeom.endConnection();
		// 上位30本だけ取得.
		ArrayList<ArrayList<Point2D>> topN_strokes = new ArrayList<>();
		ArrayList<Integer> topN_strokeClazz = new ArrayList<>();
		for(int i=0; i<30; i++){
			topN_strokes.add(osmStrokeDataGeom._strokeArcPoint.get(i));
			topN_strokeClazz.add(osmStrokeDataGeom._strokeClazz.get(i));
		}
		
		roadPath.addAll(topN_strokes);
		clazzList.addAll(topN_strokeClazz);
		//roadPath.addAll(osmStrokeDataGeom._strokeArcPoint);
		//clazzList.addAll(osmStrokeDataGeom._strokeClazz);
		
		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
		osmRoadDataGeom.startConnection();
		//////////////////////////////////
		// 高速道路を取得.///////////////
		//////////////////////////////////
		osmRoadDataGeom.insertOsmRoadData(_getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat, roadType, " clazz <=12");
		roadPath.addAll(osmRoadDataGeom._arc2);
		clazzList.addAll(osmRoadDataGeom._clazz);
		//////////////////////////////////
		// 鉄道データの取得.//////////////////
		//////////////////////////////////
		osmRoadDataGeom.insertOsmRoadData(_getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat, "rail", "");
		// 鉄道の描画.
		roadPath.addAll(osmRoadDataGeom._arc2);
		clazzList.addAll(osmRoadDataGeom._clazz);
//		paintElasticRoadData(osmRoadDataGeom._arc2, osmRoadDataGeom._clazz);
		osmRoadDataGeom.endConnection();
		
		// 地下鉄の取得.
		OsmLineDataGeom osmLineDataGeom = new OsmLineDataGeom();
		osmLineDataGeom.startConnection();
		osmLineDataGeom.insertLineDataSpecificColumn("railway", "subway", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);
		osmLineDataGeom.endConnection();
		roadPath.addAll(osmLineDataGeom._arc);
		clazzList.addAll(osmLineDataGeom._clazz);

		
		// glue部分だけ描画
				PaintGlueRoad paintGlueRoad = new PaintGlueRoad(centerLngLat, focusScale, contextScale, glueInnerRadius, glueOuterRadius, glueInnerRadiusMeter, glueOuterRadiusMeter, _graphics2d, _convertFocus, _convertContext, _contextMercatorConvert);
				paintGlueRoad.paintElasticRoadData(roadPath, clazzList);
		
		//////////////////////////////////////////////
//		// 道路データの取得.
//		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
//		osmRoadDataGeom.startConnection();
//		// 矩形範囲内の道路データを取得する.
//		osmRoadDataGeom.insertOsmRoadData(_getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat, roadType);
//		osmRoadDataGeom.__arc = osmRoadDataGeom._arc;
//		osmRoadDataGeom.endConnection();
//		// // focus, contextの道路の描画.
//		paintRoadData(osmRoadDataGeom.__arc);
		//////////////////////////////////////////////
		
		BasicStroke wideStroke = new BasicStroke(3);
		_graphics2d.setStroke(wideStroke);
		// glueの枠線の描画.
		_graphics2d.setColor(Color.red);
		// 中心点.
		_graphics2d.drawOval(windowSize.x/2-2, windowSize.x/2-2, 4, 4);
		// glue領域内側想定範囲.
		_graphics2d.drawOval(windowSize.x/2-glueInnerRadius, windowSize.x/2-glueInnerRadius, glueInnerRadius*2, glueInnerRadius*2);
		// glue領域外側想定範囲.
		_graphics2d.drawOval(windowSize.x/2-glueOuterRadius, windowSize.x/2-glueOuterRadius, glueOuterRadius*2, glueOuterRadius*2);
		
		return bfImage;
	}


	
}
