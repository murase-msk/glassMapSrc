package src.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.sun.org.apache.bcel.internal.generic.NEW;

import src.ElasticPoint;
import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.coordinate.LngLatMercatorUtility;

/**
 * glueの道路の変形，描画
 * @author murase
 *
 */
public class PaintGlueRoad {
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
	public ArrayList<ArrayList<Point2D>> transformedPoint = new ArrayList<>();
	
	public PaintGlueRoad(Point2D aCenterLngLat, int aFocusScale, int aContextScale, int aGlueInnerRadius, int aGlueOuterRadius, 
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
	
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	///////////////////道路描画について//////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////
	/**
	 * 伸縮した道路データの描画.
	 * @param __arc
	 */
	public void paintElasticRoadData(ArrayList<ArrayList<Point2D>> aPointSeries, ArrayList<Integer> __clazz){
		
		ArrayList<ArrayList<Point2D>> bluePath = new ArrayList<>(); 	// 自動車専用道路(高速道路など).
		ArrayList<ArrayList<Point2D>> greenPath = new ArrayList<>();	// 国道.
		ArrayList<ArrayList<Point2D>> redPath = new ArrayList<>();		// 主要地方道.
		ArrayList<ArrayList<Point2D>> orangePath = new ArrayList<>();	// 一般地方道.
		ArrayList<ArrayList<Point2D>> yellowPath = new ArrayList<>();	// 一般道(2車線以上).
		
        ArrayList<ArrayList<Point2D>> whitePath = new ArrayList<>();    // その他.
        ArrayList<ArrayList<Point2D>> narrowWhitePath = new ArrayList<>();
        ArrayList<ArrayList<Point2D>> grayPath = new ArrayList<>();
        ArrayList<ArrayList<Point2D>> faintGrayPath = new ArrayList<>();
        ArrayList<ArrayList<Point2D>> brownDashPath = new ArrayList<>();
        ArrayList<ArrayList<Point2D>> blueDashPath = new ArrayList<>();
        ArrayList<ArrayList<Point2D>> redDashPath = new ArrayList<>();
        ArrayList<ArrayList<Point2D>> wideRedDashPath = new ArrayList<>();

        ArrayList<ArrayList<Point2D>> railPath = new ArrayList<>();		// 電車.
		ArrayList<ArrayList<Point2D>> subwayPath = new ArrayList<>();	// 地下鉄.
		
		// 点を歪める準備.
		ElasticPoint elasticPoint = new ElasticPoint(
				_contextMercatorConvert.mercatorPerPixel.getX()*glueInnerRadius, 
				_contextMercatorConvert.mercatorPerPixel.getX()*glueOuterRadius, 
				Math.pow(2, focusScale-contextScale), 
				LngLatMercatorUtility.ConvertLngLatToMercator(centerLngLat));
		
		transformedPoint = new ArrayList<>();
		for(int i=0; i<aPointSeries.size(); i++){
			Point pXy;// あるセグメントにおける点.
			ArrayList<Point2D> path = new ArrayList<>();	// 1つのリンクの変形後のリンク形状(xy座標系).
			for(Point2D onePoint : aPointSeries.get(i)){
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
			transformedPoint.add(path);
			dispatchLineStyle(path, __clazz.get(i), bluePath, greenPath, redPath, orangePath, yellowPath, whitePath,
                    narrowWhitePath, grayPath, faintGrayPath, brownDashPath,
                    blueDashPath, redDashPath, wideRedDashPath, railPath, subwayPath);
		}
		
		paintEachLineStyle(bluePath, greenPath, redPath, orangePath, yellowPath, whitePath,
                narrowWhitePath, grayPath, faintGrayPath, brownDashPath,
                blueDashPath, redDashPath, wideRedDashPath, railPath, subwayPath);
	}

	/**
	 * 線の描画形式を道路クラスによって振り分ける.
	 * @param path
	 * @param __clazz
	 */
	private static void dispatchLineStyle(ArrayList<Point2D> path, int __clazz, 
			ArrayList<ArrayList<Point2D>> bluePath, 
			ArrayList<ArrayList<Point2D>> greenPath, 
			ArrayList<ArrayList<Point2D>> redPath, 
			ArrayList<ArrayList<Point2D>> orangePath, 
			ArrayList<ArrayList<Point2D>> yellowPath,
			ArrayList<ArrayList<Point2D>> whitePath,
            ArrayList<ArrayList<Point2D>> narrowWhitePath,
            ArrayList<ArrayList<Point2D>> grayPath,
            ArrayList<ArrayList<Point2D>> faintGrayPath,
            ArrayList<ArrayList<Point2D>> brownDashPath,
            ArrayList<ArrayList<Point2D>> blueDashPath,
            ArrayList<ArrayList<Point2D>> redDashPath,
            ArrayList<ArrayList<Point2D>> wideRedDashPath,
			ArrayList<ArrayList<Point2D>> railPath,
			ArrayList<ArrayList<Point2D>> subwayPath
			){

		switch(__clazz){
        case 11:// 水色(mortorway,mortorwa_link).
        case 12:
            bluePath.add(path);
            break;
        case 13://緑(trunk, trunk_link).
        case 14:
            greenPath.add(path);
            break;
        case 15:// 赤 (primary, primary_link).
        case 16:
            redPath.add(path);
            break;
        case 21:// オレンジ (secondary, secondary_link).
        case 22:
            orangePath.add(path);
            break;
        case 31:// 黄色 (tertiary).
            yellowPath.add(path);
            break;
        case 32:// 白 (residential, road, unclassified).
        case 41:
        case 42:
            whitePath.add(path);
            break;
        case 51://(service)
            narrowWhitePath.add(path);
            break;
        case 63:// (living street).
            grayPath.add(path);
            break;
        case 62://(pedestrian)
            faintGrayPath.add(path);
            break;
        case 71:// (track).
            brownDashPath.add(path);
            break;
        case 72://(path).
            redDashPath.add(path);
            break;
        case 81://青点線＋白線(cycleway)
            blueDashPath.add(path);
            break;
        case 91:// 赤点線(footway).
            redDashPath.add(path);
            break;
        case 92:// 太め赤白点線(step).
            wideRedDashPath.add(path);
            break;
        case 3:// 鉄道 白黒.
            railPath.add(path);
            break;
            // 地下鉄.
        case 2:
            subwayPath.add(path);
            break;
        default:
            whitePath.add(path);
            break;
		}
	}
	
	/**
	 * 道路クラスが大きい順に道路クラスごとに描画
	 * @param bluePath
	 * @param greenPath
	 * @param redPath
	 * @param orangePath
	 * @param yellowPath
	 * @param whitePath
	 */
	private void paintEachLineStyle(
			ArrayList<ArrayList<Point2D>> bluePath, 
			ArrayList<ArrayList<Point2D>> greenPath, 
			ArrayList<ArrayList<Point2D>> redPath, 
			ArrayList<ArrayList<Point2D>> orangePath, 
			ArrayList<ArrayList<Point2D>> yellowPath,
			ArrayList<ArrayList<Point2D>> whitePath,
			
            ArrayList<ArrayList<Point2D>> narrowWhitePath,
            ArrayList<ArrayList<Point2D>> grayPath,
            ArrayList<ArrayList<Point2D>> faintGrayPath,
            ArrayList<ArrayList<Point2D>> brownDashPath,
            ArrayList<ArrayList<Point2D>> blueDashPath,
            ArrayList<ArrayList<Point2D>> redDashPath,
            ArrayList<ArrayList<Point2D>> wideRedDashPath,
			
			ArrayList<ArrayList<Point2D>> railPath,
			ArrayList<ArrayList<Point2D>> subwayPath
		){
		// 道路クラスごとに描画.
		// 最初に描画する道路が後ろに来る.
		int mainRoadEdge = 8;//6;//8
		int mainRoadIn = 7;//5;//7
		
       for(ArrayList<Point2D> line: wideRedDashPath){
            float[] dash = { 2.f, 2.f };
            paintPath(line, 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(252, 127, 111), (float) 1., dash, (float) 0.);
        }
        for(ArrayList<Point2D> line: redDashPath){
            float[] dash = { 2.f, 2.f };
            paintPath(line, 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(252, 127, 111), (float) 1., dash, (float) 0.);
        }
        for(ArrayList<Point2D> line: blueDashPath){
            float[] dash = { 4.f, 4.f };
            paintPath(line, 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(0, 0, 255), (float) 1., dash, (float) 0.);
        }
        
        
        for(ArrayList<Point2D> line: brownDashPath){
            float[] dash = { 3.f, 3.f };
            paintPath(line, 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(164, 134, 46), (float) 1., dash, (float) 0.);
        }
        for(ArrayList<Point2D> line: faintGrayPath){
            paintPath(line, 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(189,189,189));// 枠.
            paintPath(line, 3, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(237,237,237));// 中の線.
        }
        for(ArrayList<Point2D> line: grayPath){
            paintPath(line, mainRoadEdge, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(189,189,188));// 枠.
            paintPath(line, mainRoadIn, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(204,204,204));// 中の線.
        }
        for(ArrayList<Point2D> line: narrowWhitePath){
            paintPath(line, 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(189,189,188));// 枠.
            paintPath(line, 3, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(255,255,255));// 中の線.
        }
	        
		for(ArrayList<Point2D> whiteLine: whitePath){
			paintPath(whiteLine, 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(189,189,188));// 枠.
			paintPath(whiteLine, 3, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(255,255,255));// 中の線.
		}
		// 2車線道路.
		for(ArrayList<Point2D>yellowLine: yellowPath){
			paintPath(yellowLine, mainRoadEdge, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(189,189,188));
			paintPath(yellowLine, mainRoadIn, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(255,255,255));
		}
		//.
		for(ArrayList<Point2D>orangeLine: orangePath){
			paintPath(orangeLine, mainRoadEdge, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(204,102,0));
			paintPath(orangeLine, mainRoadIn, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(248,214,170));
		}
		// 地方主要道.
		for(ArrayList<Point2D>redLine: redPath){
			paintPath(redLine, mainRoadEdge, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(211,147,148));
			paintPath(redLine, mainRoadIn, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(252,214,164));
		}
		// 国道.
		for(ArrayList<Point2D>greenLine: greenPath){
			paintPath(greenLine, mainRoadEdge, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(213,115,89));
			paintPath(greenLine, mainRoadIn, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(248,177,156));
		}
		// 路線.
		for(ArrayList<Point2D>railLine: railPath){
			float[] dash = { 12.f, 12.f };
			paintPath(railLine, 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(140,140,140));
			paintPath(railLine, 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(255,255,255));
			paintPath(railLine, 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(140, 140, 140), (float) 1., dash, (float) 0.);
		}
		// 地下鉄.
		for(ArrayList<Point2D>subwayLine: subwayPath){
			float[] dash = { 6.f, 6.f };
			paintPath(subwayLine, 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(153, 153, 153), (float) 1., dash, (float) 0.);
		}
		// 高速道路.
		for(ArrayList<Point2D>blueLine: bluePath){
			paintPath(blueLine, mainRoadEdge, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, new Color(0,0,0));
			paintPath(blueLine, mainRoadIn, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND, new Color(231,146,162));
		}
	}
	
	
	
	
	/**
	 * パスの描画
	 * @param path
	 * @param aPathWidth
	 * @param aColor
	 */
	private void paintPath(ArrayList<Point2D> path,int aPathWidth, int CAP_BUTT,int JOIN_BEVEL, Color aColor){
		_graphics2d.setStroke(new BasicStroke(aPathWidth, CAP_BUTT, JOIN_BEVEL));
		drawPath(path, aPathWidth, aColor);
	}
	private void paintPath(ArrayList<Point2D> path,int aPathWidth, int CAP_BUTT,int JOIN_BEVEL, Color aColor, float miterlimit, float[] dash, float dash_phase){
		_graphics2d.setStroke(new BasicStroke(aPathWidth, CAP_BUTT, JOIN_BEVEL, miterlimit, dash, dash_phase));
		drawPath(path, aPathWidth, aColor);
	}
	private void drawPath(ArrayList<Point2D> path,int aPathWidth, Color aColor){
		_graphics2d.setPaint(aColor);
		GeneralPath generalPath = new GeneralPath();
		for(int i=0; i<path.size(); i++){
			if(i==0){
				generalPath.moveTo(path.get(i).getX(), path.get(i).getY());
			}
			generalPath.lineTo(path.get(i).getX(), path.get(i).getY());
		}
		_graphics2d.draw(generalPath);

	}
	
	////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
}
