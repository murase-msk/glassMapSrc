package src.coordinate;

import java.awt.Point;
import java.awt.geom.Point2D;

import src.ElasticPoint;

/**
 * glueのxy座標に変換
 * @author murase
 *
 */
public class ConvertElasticPointGlue {
	
	public ElasticPoint _elasticPoint;
	public double _glueInnerRadiusMeter;
	public double _glueOuterRadiusMeter;
	public Point2D _centerLngLat;
	public ConvertLngLatXyCoordinate _convertFocus;
	public ConvertLngLatXyCoordinate _convertContext;
	public ConvertMercatorXyCoordinate _contextMercatorConvert;
	
	
	public ConvertElasticPointGlue(int glueInnerRadius, Integer glueOuterRadius, double glueInnerRadiusMeter, double glueOuterRadiusMeter, int focusScale, int contextScale, Point2D centerLngLat,
			ConvertLngLatXyCoordinate convertFocus, ConvertLngLatXyCoordinate convertContext, ConvertMercatorXyCoordinate convertMercator){
		_glueInnerRadiusMeter = glueInnerRadiusMeter;
		_glueOuterRadiusMeter = glueOuterRadiusMeter;
		_centerLngLat = centerLngLat;
		_convertFocus = convertFocus;
		_convertContext = convertContext;
		_contextMercatorConvert = convertMercator;
		// 点を歪める準備.
				_elasticPoint = new ElasticPoint(
						_contextMercatorConvert.mercatorPerPixel.getX()*glueInnerRadius, 
						_contextMercatorConvert.mercatorPerPixel.getX()*glueOuterRadius, 
						Math.pow(2, focusScale-contextScale), 
						LngLatMercatorUtility.ConvertLngLatToMercator(centerLngLat));
	}
	
	/**
	 * 指定の点を歪める(xy座標になる)
	 * @param onePoint 緯度経度座標
	 */
	public Point convertLngLatGlueXy(Point2D onePoint){
		Point pXy;// あるセグメントにおける点.
		// 2点の緯度経度から中心までの距離(メートル)を求める.
		double pMeter = LngLatMercatorUtility.calcDistanceFromLngLat(_centerLngLat, onePoint);
		// p1について.
		if(pMeter < _glueInnerRadiusMeter){	// focus領域にある.
			pXy = _convertFocus.convertLngLatToXyCoordinate(onePoint);
		}else if ( _glueInnerRadiusMeter < pMeter && pMeter < _glueOuterRadiusMeter){// glue領域にある.
//			System.out.println("$$$$$$$$$$");
//			System.out.println(_glueInnerRadiusMeter);
//			System.out.println(_glueOuterRadiusMeter);
			
			// glue内側から見て何パーセントの位置にあるか(0~1).
			double glueRatio = (pMeter-_glueInnerRadiusMeter)/(_glueOuterRadiusMeter - _glueInnerRadiusMeter);
			Point2D elasticPointMercator = _elasticPoint.calcElasticPoint(LngLatMercatorUtility.ConvertLngLatToMercator(onePoint), glueRatio);
			pXy = _contextMercatorConvert.convertMercatorToXyCoordinate(elasticPointMercator);
		}else{// context領域にある.
			pXy = _convertContext.convertLngLatToXyCoordinate(onePoint);
		}
		return pXy;
	}
}
