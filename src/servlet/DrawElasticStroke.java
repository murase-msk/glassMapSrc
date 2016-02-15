package servlet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
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
import src.db.getData.OsmRoadDataGeom;
import src.db.getData.OsmStrokeDataGeom;

public class DrawElasticStroke {
	
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

	
	public DrawElasticStroke(HttpServletRequest request, HttpServletResponse response) {
		
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
		
		//////////////////////////////////////////////
		// ストローク取得.
		OsmStrokeDataGeom osmStrokeDataGeom = new OsmStrokeDataGeom();
		osmStrokeDataGeom.startConnection();
		Point upperLeftOuterGlueXY = new Point(windowSize.x/2-glueOuterRadius, windowSize.x/2-glueOuterRadius);
		Point LowerRightOuterGlueXY = new Point(windowSize.x/2-glueOuterRadius + glueOuterRadius*2, windowSize.x/2-glueOuterRadius + glueOuterRadius*2);
		osmStrokeDataGeom.insertStrokeData(_convertContext.convertXyCoordinateToLngLat(upperLeftOuterGlueXY), _convertContext.convertXyCoordinateToLngLat(LowerRightOuterGlueXY));
		osmStrokeDataGeom.endConnection();
		// glue部分だけ先に描画
		paintGlueStroke(osmStrokeDataGeom._strokeArc);
		//////////////////////////////////////////////
		
		//////////////////////////////////////////////
		// 道路データの取得.
		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
		osmRoadDataGeom.startConnection();
		// 矩形範囲内の道路データを取得する.
		osmRoadDataGeom.insertOsmRoadData(_getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat, roadType, "");
		osmRoadDataGeom.__arc = osmRoadDataGeom._arc;
		osmRoadDataGeom.endConnection();
		// // focus, contextの道路の描画.
		paintRoadData(osmRoadDataGeom.__arc);
		//////////////////////////////////////////////
		
		_graphics2d.setColor(Color.red);
		// 中心点.
		_graphics2d.drawOval(windowSize.x/2-2, windowSize.x/2-2, 4, 4);
		// glue領域内側想定範囲.
		_graphics2d.drawOval(windowSize.x/2-glueInnerRadius, windowSize.x/2-glueInnerRadius, glueInnerRadius*2, glueInnerRadius*2);
		// glue領域外側想定範囲.
		_graphics2d.drawOval(windowSize.x/2-glueOuterRadius, windowSize.x/2-glueOuterRadius, glueOuterRadius*2, glueOuterRadius*2);
		
		return bfImage;
	}


	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	///////////////////道路描画について//////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/***
	 * glue部分のストロークの描画
	 */
	public void paintGlueStroke(ArrayList<ArrayList<Line2D>> __arc){
		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
		osmRoadDataGeom.startConnection();
		Point p1Xy;
		Point p2Xy;
		
		// 点を歪める準備.
		ElasticPoint elasticPoint = new ElasticPoint(
				_contextMercatorConvert.mercatorPerPixel.getX()*glueInnerRadius, 
				_contextMercatorConvert.mercatorPerPixel.getX()*glueOuterRadius, 
				Math.pow(2, focusScale-contextScale), 
				LngLatMercatorUtility.ConvertLngLatToMercator(centerLngLat));
		
		//for(ArrayList<Line2D> arrArc : __arc){
		for(int i=0; i<STROKE_NUM; i++){	// 上位30本だけ.
			for(Line2D arc : __arc.get(i)){
				// 2点の緯度経度から中心までの距離(メートル)を求める.
				double p1Meter = LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, arc.getP1());
				double p2Meter = LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, arc.getP2());
				boolean p1GlueFlg = false;// p1がglue領域にあるか.
				// p1について.
				if(p1Meter < glueInnerRadiusMeter){	// focus領域にある.
					p1Xy = _convertFocus.convertLngLatToXyCoordinate(arc.getP1());
//					continue;
				}else if ( glueInnerRadiusMeter < p1Meter && p1Meter < glueOuterRadiusMeter){// glue領域にある.
					// glue内側から見て何パーセントの位置にあるか(0~1).
					double glueRatio = (p1Meter-glueInnerRadiusMeter)/(glueOuterRadiusMeter - glueInnerRadiusMeter);
					Point2D elasticPointMercator = elasticPoint.calcElasticPoint(LngLatMercatorUtility.ConvertLngLatToMercator(arc.getP1()), glueRatio);
					p1Xy = _contextMercatorConvert.convertMercatorToXyCoordinate(elasticPointMercator);
					p1GlueFlg = true;
				}else{// context領域にある.
					p1Xy = _convertContext.convertLngLatToXyCoordinate(arc.getP1());
//					continue;
				}
				// p2について.
				if(p2Meter < glueInnerRadiusMeter){	// focus領域にある.
					p2Xy = _convertFocus.convertLngLatToXyCoordinate(arc.getP2());
					if(p1GlueFlg == false){
						continue;
					}
				}else if ( glueInnerRadiusMeter < p2Meter && p2Meter < glueOuterRadiusMeter){// glue領域にある.
					// glue内側から見て何パーセントの位置にあるか(0~1).
					double glueRatio = (p2Meter-glueInnerRadiusMeter)/(glueOuterRadiusMeter - glueInnerRadiusMeter);
					Point2D elasticPointMercator = elasticPoint.calcElasticPoint(LngLatMercatorUtility.ConvertLngLatToMercator(arc.getP2()), glueRatio);
					p2Xy = _contextMercatorConvert.convertMercatorToXyCoordinate(elasticPointMercator);
//					continue;
				}else{// context領域にある.
					p2Xy = _convertContext.convertLngLatToXyCoordinate(arc.getP2());
					if(p1GlueFlg == false){
						continue;
					}
				}
				paint2dLine(new Line2D.Double(p1Xy, p2Xy), Color.pink, (float)3);
			}
		}
		osmRoadDataGeom.endConnection();
	}
	
	/**
	 * 道路データの描画.
	 * @param __arc
	 */
	public void paintRoadData(ArrayList<ArrayList<Line2D>> __arc){
		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
		osmRoadDataGeom.startConnection();
		Point p1Xy;
		Point p2Xy;
		for(ArrayList<Line2D> arrArc : __arc){
			for(Line2D arc : arrArc){
				// 2点の緯度経度から中心までの距離(メートル)を求める.
				
				double p1Meter = LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, arc.getP1());
				double p2Meter = LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, arc.getP2());
				// p1について.
				if(p1Meter < glueInnerRadiusMeter){	// focus領域にある.
					p1Xy = _convertFocus.convertLngLatToXyCoordinate(arc.getP1());
//					continue;
				}else if ( glueInnerRadiusMeter < p1Meter && p1Meter < glueOuterRadiusMeter){// glue領域にある.
					// glue内側から見て何パーセントの位置にあるか.
//					int glueRatio = (int)((p1Meter-glueInnerRadiusMeter)/(glueOuterRadiusMeter - glueInnerRadiusMeter)*100);
//					p1Xy = _arrayConvert.get(glueRatio).convertLngLatToXyCoordinate(arc.getP1());
					continue;
				}else{// context領域にある.
					p1Xy = _convertContext.convertLngLatToXyCoordinate(arc.getP1());
//					continue;
				}
				// p2について.
				if(p2Meter < glueInnerRadiusMeter){	// focus領域にある.
					p2Xy = _convertFocus.convertLngLatToXyCoordinate(arc.getP2());
//					continue;
				}else if ( glueInnerRadiusMeter < p2Meter && p2Meter < glueOuterRadiusMeter){// glue領域にある.
					// glue内側から見て何パーセントの位置にあるか.
//					int glueRatio = (int)((p2Meter-glueInnerRadiusMeter)/(glueOuterRadiusMeter - glueInnerRadiusMeter)*100);
//					p2Xy = _arrayConvert.get(glueRatio).convertLngLatToXyCoordinate(arc.getP2());
					continue;
				}else{// context領域にある.
					p2Xy = _convertContext.convertLngLatToXyCoordinate(arc.getP2());
//					continue;
				}
				paint2dLine(new Line2D.Double(p1Xy, p2Xy), Color.pink, (float)3);
			}
		}
		osmRoadDataGeom.endConnection();
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

	
}
