package src.paint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.sun.org.apache.bcel.internal.generic.NEW;

import src.ElasticPoint;
import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.LngLatMercatorUtility;

public class PaintGluePolygon {
	/** 地図の大きさ */
//	private Point windowSize = new Point(700, 700);
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
//	private GetLngLatOsm _getLngLatOsmFocus;
	/** focus領域の緯度経度xy変換 */
	private ConvertLngLatXyCoordinate _convertFocus;
	/** contextの端点の緯度経度を求める */
//	private GetLngLatOsm _getLngLatOsmContext;
	/** context領域の緯度経度xy変換 */
	private ConvertLngLatXyCoordinate _convertContext;
	/** メルカトル座標系xy変換 */
	private ConvertMercatorXyCoordinate _contextMercatorConvert;
	
	// 出力結果.
	/** 変形後の緯度経度  */
	public ArrayList<ArrayList<Point2D>> _transformedPoint = new ArrayList<>();
	public ArrayList<String> _polygonType;
	
	public PaintGluePolygon(Point2D aCenterLngLat, int aFocusScale, int aContextScale, int aGlueInnerRadius, int aGlueOuterRadius, 
			double aGlueInnerRadiusMeter, double aGlueOuterRadiusMeter, Graphics2D aGraphics2d, 
			ConvertLngLatXyCoordinate aConvertFocus, ConvertLngLatXyCoordinate aConvertContext, ConvertMercatorXyCoordinate aContextMercatorConvert){
		centerLngLat = aCenterLngLat;
		focusScale = aFocusScale;
		contextScale = aContextScale;
		glueInnerRadius = aGlueInnerRadius;
		glueOuterRadius = aGlueOuterRadius;
		glueInnerRadiusMeter = aGlueInnerRadiusMeter;
		glueOuterRadiusMeter = aGlueOuterRadiusMeter;
		_graphics2d = aGraphics2d;
		_convertFocus = aConvertFocus;
		_convertContext = aConvertContext;
		_contextMercatorConvert = aContextMercatorConvert;
	}
	
	/**
	 * ポリゴンの描画
	 * @param aPointSeries
	 * @param __clazz
	 */
	public void paintElasticPolygon(ArrayList<ArrayList<Point2D>> aPolygon, ArrayList<String> aPolygonType){
		_polygonType = aPolygonType;
		// 点を歪める準備.
		ElasticPoint elasticPoint = new ElasticPoint(
				_contextMercatorConvert.mercatorPerPixel.getX()*glueInnerRadius, 
				_contextMercatorConvert.mercatorPerPixel.getX()*glueOuterRadius, 
				Math.pow(2, focusScale-contextScale), 
				LngLatMercatorUtility.ConvertLngLatToMercator(centerLngLat));
		
		_transformedPoint = new ArrayList<>();
		for(int i=0; i<aPolygon.size(); i++){
			Point pXy;// あるセグメントにおける点.
			ArrayList<Point2D> path = new ArrayList<>();	// 1つのリンクの変形後のリンク形状(xy座標系).
			for(Point2D onePoint : aPolygon.get(i)){
				// 2点の緯度経度から中心までの距離(メートル)を求める.
				double pMeter = LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, onePoint);
				// p1について.
				if(pMeter < glueInnerRadiusMeter){	// focus領域にある.
					pXy = _convertFocus.convertLngLatToXyCoordinate(onePoint);
				}else if ( glueInnerRadiusMeter < pMeter && pMeter < glueOuterRadiusMeter){// glue領域にある.
					// glue内側から見て何パーセントの位置にあるか(0~1).
					double glueRatio = (pMeter-glueInnerRadiusMeter)/(glueOuterRadiusMeter - glueInnerRadiusMeter);
					Point2D elasticPointMercator = elasticPoint.calcElasticPoint(LngLatMercatorUtility.ConvertLngLatToMercator(onePoint), glueRatio);
					pXy = _contextMercatorConvert.convertMercatorToXyCoordinate(elasticPointMercator);
				}else{// context領域にある.
					pXy = _convertContext.convertLngLatToXyCoordinate(onePoint);
				}
				path.add(pXy);
//				paint2dLine(new Line2D.Double(p1Xy, p2Xy), Color.pink, (float)3, __clazz.get(i));
			}
			_transformedPoint.add(path);
		}
		
		paintPolyStyle();
	}
	
	/**
	 * ポリゴン描画
	 */
	public void paintPolyStyle(){
		for(int i=0; i<_transformedPoint.size(); i++){
			switch(_polygonType.get(i)){
				case "forest":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(160, 206, 133));
					break;
				case "grass":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(206, 235, 167));
					break;
				case "water":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(181, 208, 208));
					break;
				case "reservoir":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(181, 208, 208));
					break;
				case "parking":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(246, 238, 183));
					break;
				case "garden":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(206, 235, 167));
					break;
				case "pitch":
					paintPolygon(_graphics2d, _transformedPoint.get(i), new Color(138, 210, 175));
					break;
				default:
					break;
			}
		}
	}
	
	
	// 多角形.
	private static void paintPolygon(Graphics2D g, ArrayList<Point2D> aPointArrayList, Color aColor){
		int[] xPoints = new int[aPointArrayList.size()];
		int[] yPoints = new int[aPointArrayList.size()];
		
		for(int i=0; i<aPointArrayList.size(); i++){
			xPoints[i] = (int)aPointArrayList.get(i).getX();
			yPoints[i] = (int)aPointArrayList.get(i).getY(); 
		}
		
		Polygon polygon = new Polygon(xPoints, yPoints, xPoints.length);
		g.setPaint(aColor);
		g.draw(polygon);
		g.setPaint(aColor);
		g.fill(polygon);
	}
	
}
