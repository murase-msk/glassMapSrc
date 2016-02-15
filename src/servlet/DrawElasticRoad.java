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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import src.ElasticPoint;
import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.coordinate.LngLatMercatorUtility;
import src.db.getData.OsmLineDataGeom;
import src.db.getData.OsmPolygonDataGeom;
import src.db.getData.OsmRoadDataGeom;
import src.paint.PaintGluePolygon;
import src.paint.PaintGlueRoad;

/**
 * 伸縮する道路を描画
 * http://133.68.13.112:8080/EmmaGlueMuraseOriginal/MainServlet?type=DrawElasticRoad&centerLngLat=136.9309671669116,35.15478942665804&focus_zoom_level=17&context_zoom_level=15&glue_inner_radius=200&glue_outer_radius=300&roadType=car
 * @author murase
 *
 */
public class DrawElasticRoad {
	
	/** 地図の大きさ */
	public Point windowSize = new Point(700, 700);
	/** 初期の緯度経度Point2D形式 */
	private  Point2D.Double centerLngLat = new Point2D.Double(136.9309671669116, 35.15478942665804);// 鶴舞公園.
	/** focusのスケール */
	private int focusScale = 17;
	/** contextのスケール */
	private int contextScale = 15;
	/** glue内側の半径(pixel) */
	private int glueInnerRadius=200;
	/** glue外側の半径(pixel) */
	private int glueOuterRadius=300;
	
	/** 道路の種類(car, bikeFoot) */
	public String roadType = "car";
	public String option = "";
	/** ポリゴンの描画有無 */
	public boolean isDrawPolygon = false;
	
	// 中心点からglue内側の長さ.
	public double glueInnerRadiusMeter;
	// 中心点からglue外側の長さ.
	public double glueOuterRadiusMeter;
	
	
	Graphics2D _graphics2d;
	/** focus */
	public GetLngLatOsm _getLngLatOsmFocus;
	/** focus領域の緯度経度xy変換 */
	public ConvertLngLatXyCoordinate _convertFocus;
	/** context */
	public GetLngLatOsm _getLngLatOsmContext;
	/** context領域の緯度経度xy変換 */
	public ConvertLngLatXyCoordinate _convertContext;
	/** メルカトル座標系xy変換 */
	public ConvertMercatorXyCoordinate _contextMercatorConvert;
//	public Point2D _upperLeftLngLat;
//	public Point2D _lowerRightLngLat;
	
	
	// ほしい値.
	/** 選択されたリンクの形状(緯度経度) */
	public ArrayList<ArrayList<Point2D>> _selectedRoadPath = new ArrayList<>();
	/** 選択されたリンクのクラス */
	public ArrayList<Integer> _selectedRoadClazz = new ArrayList<>();
	/** 選択されたのリンクID */
	public ArrayList<Integer> _selectedLinkId = new ArrayList<>();
	/** 選択されたリンクで変形後の道路形状(xy座標) */
	public ArrayList<ArrayList<Point2D>> _selectedTransformedPoint = new ArrayList<>();
	
	//http://133.68.13.112:8080/EmmaGlueMuraseOriginal/MainServlet?type=DrawElasticRoad&centerLngLat=136.9309671669116,35.15478942665804&focus_zoom_level=17&context_zoom_level=15&glue_inner_radius=200&glue_outer_radius=300
	public DrawElasticRoad(HttpServletRequest request, HttpServletResponse response) {
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
		option = request.getParameter("option") == null ? "" : request.getParameter("option");
		if(option.equals("vector")){	// 選択されたベクタの道路データを返す.
			drawImage();
			createVectorResponse(request, response);
			return;
		}
		
		isDrawPolygon = request.getParameter("isDrawPolygon") == null ? false : request.getParameter("isDrawPolygon").equals("true")?true:false;
		try{
			OutputStream out=response.getOutputStream();
			ImageIO.write( drawImage(), "png", out);
			
			out.flush();
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	
	/** ベクター用のレスポンス */
	public void createVectorResponse(HttpServletRequest request, HttpServletResponse response){
		// レスポンスの作成.
		try{
			response.setContentType("text/xml; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("<?xml version = \"1.0\" encoding = \"UTF-8\"?>");
			out.println("<data>");
			for(int i=0; i<_selectedLinkId.size(); i++){
				out.println("<oneLink>");
					out.println("<selectedLinkId>");
						out.println(_selectedLinkId.get(i));
					out.println("</selectedLinkId>");
					out.println("<selectedTransformedPoint>");
						for(int j=0; j<_selectedTransformedPoint.get(i).size(); j++){
							out.print("<xy>");
							out.print(_selectedTransformedPoint.get(i).get(j).getX());
							out.print(",");
							out.print(_selectedTransformedPoint.get(i).get(j).getY());
							out.print("</xy>");
						}
					out.println("</selectedTransformedPoint>");
					out.println("<selectedTransformedLngLat>");
						for(int j=0; j<_selectedRoadPath.get(i).size(); j++){
							out.println("<lngLat>");
							out.print(_selectedRoadPath.get(i).get(j).getX());
							out.print(",");
							out.print(_selectedRoadPath.get(i).get(j).getY());
							out.println("</lngLat>");
						}
					out.println("</selectedTransformedLngLat>");
					out.println("<roadClazz>");
					out.println(_selectedRoadClazz.get(i));
					out.println("</roadClazz>");
				out.println("</oneLink>");
			}
			out.println("</data>");
			out.close();
		}catch(IOException e){
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
		//_graphics2d.setBackground(new Color(172,208,158));	// 背景指定(モリコロパーク用).
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
		
		
		// 道路データの取得.
		ArrayList<ArrayList<Point2D>> roadPath = new ArrayList<>();
		ArrayList<Integer> clazzList = new ArrayList<>();
		OsmRoadDataGeom osmRoadDataGeom = new OsmRoadDataGeom();
		osmRoadDataGeom.startConnection();
		//////////////////////////////////
		// 道路データを取得する.///////////////
		//////////////////////////////////
		osmRoadDataGeom.insertOsmRoadData(_getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat, roadType, "");
		// 道路の描画.
		roadPath.addAll(osmRoadDataGeom._arc2);
		clazzList.addAll(osmRoadDataGeom._clazz);
		if(option.equals("vector")){
			_selectedRoadPath = new ArrayList<>();
			_selectedRoadClazz = new ArrayList<>();
			_selectedLinkId = new ArrayList<>();
			for(int i=0; i<osmRoadDataGeom._linkId.size(); i++){
				_selectedRoadClazz.add(osmRoadDataGeom._clazz.get(i));
				_selectedLinkId.add(osmRoadDataGeom._linkId.get(i));
				ArrayList<Point2D> oneArc = new ArrayList<>();
				for(int j=0; j<osmRoadDataGeom._arc2.get(i).size(); j++){
					oneArc.add(osmRoadDataGeom._arc2.get(i).get(j));
				}
				_selectedRoadPath.add(oneArc);
			}
		}
		//////////////////////////////////
		// 鉄道データの取得.//////////////////
		//////////////////////////////////
		osmRoadDataGeom.insertOsmRoadData(_getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat, "rail", "");
		// 鉄道の描画.
		roadPath.addAll(osmRoadDataGeom._arc2);
		clazzList.addAll(osmRoadDataGeom._clazz);
		osmRoadDataGeom.endConnection();
		
		// 地下鉄の取得.
		OsmLineDataGeom osmLineDataGeom = new OsmLineDataGeom();
		osmLineDataGeom.startConnection();
		osmLineDataGeom.insertLineDataSpecificColumn("railway", "subway", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);
		osmLineDataGeom.endConnection();
		roadPath.addAll(osmLineDataGeom._arc);
		clazzList.addAll(osmLineDataGeom._clazz);
		
		// その他の地形の描画.
		if(isDrawPolygon){
			ArrayList<ArrayList<Point2D>> polygonPath = new ArrayList<>();
			ArrayList<String> polygonType = new ArrayList<>();
			
			OsmPolygonDataGeom osmPolygonDataGeom = new OsmPolygonDataGeom();
			osmPolygonDataGeom.startConnection();
			// 上の方ほど下に描画される.
			osmPolygonDataGeom.addFacilityPolygon("leisure", "garden", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 
			osmPolygonDataGeom.addFacilityPolygon("leisure", "pitch", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 
			osmPolygonDataGeom.addFacilityPolygon("amenity", "parking", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 駐車場
			osmPolygonDataGeom.addFacilityLine("amenity", "parking", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 駐車場.
			osmPolygonDataGeom.addFacilityPolygon("landuse", "reservoir", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 池.
			osmPolygonDataGeom.addFacilityLine("landuse", "reservoir", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 池.
//			osmPolygonDataGeom.addFacilityPolygon("natural", "water", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 池.
//			osmPolygonDataGeom.addFacilityLine("natural", "water", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 池.
			osmPolygonDataGeom.addFacilityPolygon("landuse", "forest", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 人工林の描画.
			osmPolygonDataGeom.addFacilityLine("landuse", "forest", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 人工林の描画.
			osmPolygonDataGeom.addFacilityPolygon("landuse", "grass", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 芝地の描画.
			osmPolygonDataGeom.addFacilityLine("landuse", "grass", _getLngLatOsmContext._upperLeftLngLat, _getLngLatOsmContext._lowerRightLngLat);	// 芝地の描画.
			
			osmPolygonDataGeom.endConnection();
			polygonPath.addAll(osmPolygonDataGeom._facilityLocation);
			polygonType.addAll(osmPolygonDataGeom._facilityType);
			
			PaintGluePolygon paintGluePolygon = new PaintGluePolygon(centerLngLat, focusScale, contextScale, glueInnerRadius, glueOuterRadius, glueInnerRadiusMeter, glueOuterRadiusMeter, _graphics2d, _convertFocus, _convertContext, _contextMercatorConvert);
			paintGluePolygon.paintElasticPolygon(polygonPath, polygonType);	// ポリゴンの描画.
		}

		// 最後に描画.
		// glue部分だけ描画
		PaintGlueRoad paintGlueRoad = new PaintGlueRoad(centerLngLat, focusScale, contextScale, glueInnerRadius, glueOuterRadius, glueInnerRadiusMeter, glueOuterRadiusMeter, _graphics2d, _convertFocus, _convertContext, _contextMercatorConvert);
		paintGlueRoad.paintElasticRoadData(roadPath, clazzList);
		_selectedTransformedPoint = paintGlueRoad.transformedPoint;
		
		BasicStroke wideStroke = new BasicStroke(3);
		_graphics2d.setStroke(wideStroke);
//		// glueの枠線の描画.
//		_graphics2d.setColor(Color.red);
//		// 中心点.
//		_graphics2d.drawOval(windowSize.x/2-2, windowSize.x/2-2, 4, 4);
//		// glue領域内側想定範囲.
//		_graphics2d.drawOval(windowSize.x/2-glueInnerRadius, windowSize.x/2-glueInnerRadius, glueInnerRadius*2, glueInnerRadius*2);
//		// glue領域外側想定範囲.
//		_graphics2d.drawOval(windowSize.x/2-glueOuterRadius, windowSize.x/2-glueOuterRadius, glueOuterRadius*2, glueOuterRadius*2);
		
		return bfImage;
	}


}
