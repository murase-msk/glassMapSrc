package src.drawGlue_v2;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;

import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.coordinate.LngLatMercatorUtility;
import src.db.getData.OsmStrokeDataGeom;

/**
 * DrawGlue_v2のストローク選択アルゴリズム
 * @author murase
 *
 */
public class StrokeSelectionAlgorithm_DrawGlue_v2 {
//	/** 地図の大きさ */
//	private Point windowSize = new Point(700, 700);
	/** 初期の緯度経度Point2D形式 */
	private  Point2D centerLngLat;// 鶴舞公園.
//	/** focusのスケール */
//	private int focusScale = 17;
//	/** contextのスケール */
//	private int contextScale = 15;
	/** glue内側の半径(pixel) */
	private int glueInnerRadius=200;
	/** glue外側の半径(pixel) */
	private int glueOuterRadius=300;
	
//	/** 描画するストロークの数 */
//	private static final int STROKE_NUM = 30;
	
//	/** 道路の種類(car, bikeFoot) */
//	private String roadType = "car";
	
	/** 中心点からglue内側の長さ(メートル) */
	private double glueInnerRadiusMeter;
	/** 中心点からglue外側の長さ(メートル)  */
	private double glueOuterRadiusMeter;
	
	/** 描画用 */
	private Graphics2D _graphics2d;
//	/** focusの端点の緯度経度を求める */
//	private GetLngLatOsm _getLngLatOsmFocus;
//	/** focus領域の緯度経度xy変換 */
//	private ConvertLngLatXyCoordinate _convertFocus;
//	/** contextの端点の緯度経度を求める */
//	private GetLngLatOsm _getLngLatOsmContext;
//	/** context領域の緯度経度xy変換 */
//	private ConvertLngLatXyCoordinate _convertContext;
//	/** メルカトル座標系xy変換 */
//	private ConvertMercatorXyCoordinate _contextMercatorConvert;
	
	// 出力結果.
	/** 選択されたストロークID */
	public ArrayList<Integer> strokeId;
	/** 選択された道路 */
	public ArrayList<ArrayList<Point2D>> roadPath;
	/** 選択された道路クラス */
	public ArrayList<Integer> clazzList;
	
	public StrokeSelectionAlgorithm_DrawGlue_v2(Point2D aCenterLngLat, int aGlueInnerRadius, int aGlueOuterRadius, double aGlueInnerRadiusMeter, double aGlueOuterRadiusMeter, Graphics2D aGraphics2d){
		centerLngLat = aCenterLngLat;
		glueInnerRadius = aGlueInnerRadius;
		glueOuterRadius = aGlueOuterRadius;
		glueInnerRadiusMeter = aGlueInnerRadiusMeter;
		glueOuterRadiusMeter = aGlueOuterRadiusMeter;
		_graphics2d = aGraphics2d;
		OsmStrokeDataGeom osmStrokeDataGeom = new OsmStrokeDataGeom();		// glue外側境界より内側にあるストローク.
		OsmStrokeDataGeom osmInnerStrokeDataGeom = new OsmStrokeDataGeom();	// glue内側境界にあるストローク.
		OsmStrokeDataGeom osmOuterStrokeDataGeom = new OsmStrokeDataGeom();	// glue外側境界にあるストローク.
		osmStrokeDataGeom.startConnection();
		osmInnerStrokeDataGeom.startConnection();
		osmOuterStrokeDataGeom.startConnection();
		osmStrokeDataGeom.insertStrokeDataInCircle(centerLngLat, glueOuterRadiusMeter);
		osmInnerStrokeDataGeom.calcStrokeOverlapedCircle(centerLngLat, glueInnerRadiusMeter, true);
		osmOuterStrokeDataGeom.calcStrokeOverlapedCircle(centerLngLat, glueOuterRadiusMeter, true);
		osmStrokeDataGeom.endConnection();
		osmInnerStrokeDataGeom.endConnection();
		osmOuterStrokeDataGeom.endConnection();
		// 描画数決定.
		double glueArea = Math.PI*glueOuterRadius*glueOuterRadius-Math.PI*glueInnerRadius*glueInnerRadius;	// glue領域の面積.
		double oneStrokeArea = (glueOuterRadius)*8;				// ストローク1本あたりの推定面積.
		int drawStrokeNum = (int)((glueArea/oneStrokeArea)*0.3);					// glue領域におけるストロークの割合をα(=0.3)割くらいになるようにストローク数を設定.
		int innerEdgeDrawNum = drawStrokeNum/2;//40;//drawStrokeNum/2;//
		int outerEdgeDrawNum = drawStrokeNum/2;//150;//drawStrokeNum/2;//
		innerEdgeDrawNum = innerEdgeDrawNum > osmInnerStrokeDataGeom._strokeId.size() ? osmInnerStrokeDataGeom._strokeId.size() : innerEdgeDrawNum;
		outerEdgeDrawNum = outerEdgeDrawNum > osmOuterStrokeDataGeom._strokeId.size() ? osmOuterStrokeDataGeom._strokeId.size() : outerEdgeDrawNum;
//		System.out.println("outer  "+glueOuterRadius+""+"  inner  "+glueInnerRadius);
//		System.out.println("glueArea"+glueArea);
//		System.out.println("oneStrokeArea"+oneStrokeArea);
//		System.out.println("drawStrokeNum"+drawStrokeNum);
		// innner側の選択.
		ArrayList<Integer> innerOuterStrokeId = new ArrayList<>();
		for(int i=0; i<innerEdgeDrawNum; i++){	// 長いストロークn本.
			innerOuterStrokeId.add(osmInnerStrokeDataGeom._strokeId.get(i));
		}
		
		// outer側の選択.
		for(int i=0; i<outerEdgeDrawNum; i++){	// 長いストロークn本.
			innerOuterStrokeId.add(osmOuterStrokeDataGeom._strokeId.get(i));
		}
		// 重複削除.
		HashSet<Integer> hashSet = new HashSet<>();
		hashSet.addAll(innerOuterStrokeId);
		innerOuterStrokeId.clear();
		innerOuterStrokeId.addAll(hashSet);
		// 選択したストロークの端点がglueの中でそこで切れていたら，接するストロークを選択.
		ArrayList<Integer> newAddStrokeId = new ArrayList<>();	// 新規に追加するストロークID.
		for(int i=0; i<innerOuterStrokeId.size(); i++){
			if(	// 選択したストロークの始点側の端点がglueの中にある.
				LngLatMercatorUtility.calcDistanceFromLngLat(
				centerLngLat,osmStrokeDataGeom._strokeArcPoint.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).get(0))> glueInnerRadiusMeter
				&&
				LngLatMercatorUtility.calcDistanceFromLngLat(
				centerLngLat,osmStrokeDataGeom._strokeArcPoint.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).get(0)) < glueOuterRadiusMeter
			){
//				System.out.println("ストロークの始点がglueの中 : "+ osmStrokeDataGeom._strokeArcPoint.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).get(0));
				// 端点で行き止まりになるストロークに接するストロークを探す.
				for(int j=0; j<osmStrokeDataGeom._strokeArcPoint.size(); j++){
					HashSet<Point2D> tmp = new HashSet<>(osmStrokeDataGeom._strokeArcPoint.get(j));
					int oldnum = tmp.size();
					tmp.add(osmStrokeDataGeom._strokeArcPoint.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).get(0));
					if(tmp.size() == oldnum){
//						System.out.println("ストロークの追加");
						newAddStrokeId.add(osmStrokeDataGeom._strokeId.get(j));
					}
				}
			}
			if(	// 選択したストロークの終点側の端点がglueの中にある.
				LngLatMercatorUtility.calcDistanceFromLngLat(
				centerLngLat,
				osmStrokeDataGeom._strokeArcPoint
					.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i)))
					.get(osmStrokeDataGeom._strokeArcPoint
							.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).size()-1))> glueInnerRadiusMeter
				&&
				LngLatMercatorUtility.calcDistanceFromLngLat(
				centerLngLat,
				osmStrokeDataGeom._strokeArcPoint
					.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i)))
					.get(osmStrokeDataGeom._strokeArcPoint
							.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).size()-1)) < glueOuterRadiusMeter
			){
//				System.out.println("ストロークの終点がglueの中 : "+ osmStrokeDataGeom._strokeArcPoint
//						.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i)))
//						.get(osmStrokeDataGeom._strokeArcPoint
//								.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).size()-1));
				// 端点で行き止まりになるストロークに接するストロークを探す.
				for(int j=0; j<osmStrokeDataGeom._strokeArcPoint.size(); j++){
					HashSet<Point2D> tmp = new HashSet<>(osmStrokeDataGeom._strokeArcPoint.get(j));
					int oldnum = tmp.size();
					tmp.add(osmStrokeDataGeom._strokeArcPoint
							.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i)))
							.get(osmStrokeDataGeom._strokeArcPoint.get(osmStrokeDataGeom._strokeIdToIndexHash.get(innerOuterStrokeId.get(i))).size()-1));
					if(tmp.size() == oldnum){
//						System.out.println("ストロークの追加");
						newAddStrokeId.add(osmStrokeDataGeom._strokeId.get(j));
					}
				}
			}
		}
//		System.out.println("変化前: "+innerOuterStrokeId.size());
		// 重複削除, 新規に追加.
		HashSet<Integer> hashSet2 = new HashSet<>();
		hashSet2.addAll(newAddStrokeId);
		hashSet2.addAll(innerOuterStrokeId);
		innerOuterStrokeId.clear();
		innerOuterStrokeId.addAll(hashSet2);
//		System.out.println("変化ご: "+innerOuterStrokeId.size());

		strokeId = new ArrayList<>();
		roadPath = new ArrayList<>();
		clazzList = new ArrayList<>();
		for(Integer num: innerOuterStrokeId){
			strokeId.add(num);
			roadPath.add(osmStrokeDataGeom._strokeArcPoint.get(osmStrokeDataGeom._strokeIdToIndexHash.get(num)));
			clazzList.add(osmStrokeDataGeom._strokeClazz.get(osmStrokeDataGeom._strokeIdToIndexHash.get(num)));
			
		}
	}
	
	
}
