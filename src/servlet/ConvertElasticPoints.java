package servlet;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.coordinate.ConvertElasticPointGlue;
import src.coordinate.LngLatMercatorUtility;
import sun.launcher.resources.launcher;

/**
 * 点(複数)をglueに描画できる形に変換する
 * http://133.68.13.112:8080/EmmaGlueMuraseOriginal/MainServlet?type=ConvertElasticPoints&centerLngLat=136.9288336363183,35.158167325045824&points=136.92588320639172,35.15937778672364,136.9266127672479,35.15893921573326,136.9270526495242,35.15869361494546,136.92767492201676,35.15823749722958,136.92857614424028,35.15764103174276,136.92920914557283,35.15704456188256,136.92994943525997,35.15660597831222,136.93057170775256,35.15601827262135,136.93055025008164,35.156000729102885&focus_zoom_level=17&context_zoom_level=15&glue_inner_radius=200&glue_outer_radius=300
 * @author murase
 *
 */
public class ConvertElasticPoints {
	/** 地図の大きさ */
	private Point windowSize = new Point(700, 700);
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
	
	/** 中心点からglue内側の長さ(メートル) */
	private double glueInnerRadiusMeter;
	/** 中心点からglue外側の長さ(メートル)  */
	private double glueOuterRadiusMeter;
	
	/** 描画用 */
	Graphics2D _graphics2d;
	/** focusの端点の緯度経度を求める */
	private GetLngLatOsm _getLngLatOsmFocus;
	/** focus領域の緯度経度xy変換 */
	private ConvertLngLatXyCoordinate _convertFocus;
	/** contextの端点の緯度経度を求める */
	private GetLngLatOsm _getLngLatOsmContext;
	/** context領域の緯度経度xy変換 */
	private ConvertLngLatXyCoordinate _convertContext;
	/** メルカトル座標系xy変換 */
	private ConvertMercatorXyCoordinate _contextMercatorConvert;
	
	
	public ConvertElasticPoints(HttpServletRequest request, HttpServletResponse response){
		
		// 必須パラメータがあるか.
		if(request.getParameter("centerLngLat")==null ||
				request.getParameter("points")==null ||
				request.getParameter("focus_zoom_level")==null ||
				request.getParameter("context_zoom_level")==null ||
				request.getParameter("glue_inner_radius")==null ||
				request.getParameter("glue_outer_radius")==null
				){
			ErrorMsg.errorResponse(request, response, "必要なパラメータがありません");
			return;
		}
		// パラメータの受け取り.
		String[] pointString = request.getParameter("points").split(",");
		ArrayList<Point2D> points = new ArrayList<>();
		for(int i=0; i<pointString.length; i+=2){
			points.add(new Point2D.Double(Double.parseDouble(pointString[i]), Double.parseDouble(pointString[i+1])));
		}
		centerLngLat = new Point2D.Double(
				Double.parseDouble(request.getParameter("centerLngLat").split(",")[0]), 
				Double.parseDouble(request.getParameter("centerLngLat").split(",")[1]));
		focusScale = Integer.parseInt(request.getParameter("focus_zoom_level"));
		contextScale = Integer.parseInt(request.getParameter("context_zoom_level"));
		glueInnerRadius = Integer.parseInt(request.getParameter("glue_inner_radius"));
		glueOuterRadius = Integer.parseInt(request.getParameter("glue_outer_radius"));
		windowSize = new Point(glueOuterRadius*2, glueOuterRadius*2);
		

		ArrayList<Point2D> convertedPoints = convertTransformedPoints(points);
		
		
		// レスポンスの作成.
		response.setContentType("text/xml");
		try{
			PrintWriter out = response.getWriter();
			out.println("<?xml version = \"1.0\" encoding = \"UTF-8\"?>");
			out.println("<data>");
			for(int i=0; i<convertedPoints.size(); i++){
				out.println("<xy>");
				out.println("<x>");
				out.println((int)convertedPoints.get(i).getX());
				out.println("</x>");
				out.println("<y>");
				out.println((int)convertedPoints.get(i).getY());
				out.println("</y>");
				out.println("</xy>");
			}
			out.println("</data>");
			
			
			//out.flush(); //(4)データ返信の終了
			out.close();
			
			System.out.println("finish");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public ConvertElasticPoints(Point2D aCenterLngLat, int aFocusScale, int aContextScale, 
			int aGlueInnerRadius, int aGlueOuterRadius, Point aWindowSize){
		centerLngLat = aCenterLngLat;
		focusScale = aFocusScale;
		contextScale = aContextScale;
		glueInnerRadius = aGlueInnerRadius;
		glueOuterRadius = aGlueOuterRadius;
		windowSize = aWindowSize;
	}
	
	/**
	 * glue用の座標に変換
	 * @param points
	 */
	public ArrayList<Point2D> convertTransformedPoints(ArrayList<Point2D> points){
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
		
		ConvertElasticPointGlue convertGlue = new ConvertElasticPointGlue(glueInnerRadius, glueOuterRadius, glueInnerRadiusMeter, glueOuterRadiusMeter,
				focusScale, contextScale, centerLngLat, _convertFocus, _convertContext, _contextMercatorConvert);
		
		// 変換.
		ArrayList<Point2D> convertedPoints= new ArrayList<>();
		for(int i=0; i<points.size(); i++){
			convertedPoints.add(convertGlue.convertLngLatGlueXy(points.get(i)));
		}
		return convertedPoints;
	}
	
	
	
}
