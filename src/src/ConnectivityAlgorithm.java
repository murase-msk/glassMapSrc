package src;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

import  src.db.getData.OsmStrokeDataGeom;
import sun.launcher.resources.launcher;
import  servlet.DrawElasticStrokeConnectivity_v2;

/**
 * コネクティビティーを保つためのクラス
 * postgisを使ったヴァージョン
 * @author murase
 *
 */
public class ConnectivityAlgorithm {
	
	/** 最大何本ストロークを描画するか */
	public static final int MAX_DRAW_NUM = 50;
	
	
	// 最初に必要な変数.
	/** データベースからそのまま取り出したストローク(arc形式) */
	public ArrayList<ArrayList<Line2D>> _strokeArc = new ArrayList<>();
	public ArrayList<ArrayList<Point2D>> _strokeArcPoint = new ArrayList<>();
	/** ストロークのWKT形式 */
	public ArrayList<String> _strokeArcString = new ArrayList<>();
	/** ストロークの長さ */
	public ArrayList<Double> _strokeLength = new ArrayList<>();
	/** ストロークIDからインデックスを求めるハッシュ */
	public HashMap<Integer, Integer> _strokeIdToIndexHash = new HashMap<>();
	/** ストロークのクラス */
	public ArrayList<Integer> _strokeClazz = new ArrayList<>();
	
	/** 中心点からglue内側の長さ(メートル) */
	public double _glueInnerRadiusMeter;
	/** 中心点からglue外側の長さ(メートル)  */
	public double _glueOuterRadiusMeter;
//	private Point2D _upperLeftLngLat;
//	private Point2D _lowerRightLngLat;
	private  Point2D _centerLngLat;
	
	
	// 中間処理で使う.
	/** 描画が確定していないストロークのインデックス */
	public ArrayList<Integer> _reserveQueue = new ArrayList<>();
	// 最終結果.
	/** 描画が確定したストロークのインデックス */
	public ArrayList<Integer> _selectedStrokeIndex = new ArrayList<Integer>();

	
	
	public ConnectivityAlgorithm(OsmStrokeDataGeom aOsmStrokeDataGeom, DrawElasticStrokeConnectivity_v2 DrawElasticStrokeConnectivity_v2){
		initSetting(aOsmStrokeDataGeom, DrawElasticStrokeConnectivity_v2);
		selectDrawingStroke();
	}
	
	public void initSetting(OsmStrokeDataGeom aOsmStrokeDataGeom, DrawElasticStrokeConnectivity_v2 DrawElasticStrokeConnectivity_v2){
		_strokeArc = aOsmStrokeDataGeom._strokeArc;
		_strokeArcPoint = aOsmStrokeDataGeom._strokeArcPoint;
		_strokeArcString = aOsmStrokeDataGeom._strokeArcString;
		_strokeLength = aOsmStrokeDataGeom._strokeLength;
		_strokeIdToIndexHash = aOsmStrokeDataGeom._strokeIdToIndexHash;
		_strokeClazz = aOsmStrokeDataGeom._strokeClazz;
		_glueInnerRadiusMeter = DrawElasticStrokeConnectivity_v2.glueInnerRadiusMeter;
		_glueOuterRadiusMeter = DrawElasticStrokeConnectivity_v2.glueOuterRadiusMeter;
		_centerLngLat = DrawElasticStrokeConnectivity_v2.centerLngLat;
	}
	
	/**
	 * 描画するストロークを選択する
	 */
	public void selectDrawingStroke(){
		
		_selectedStrokeIndex = new ArrayList<>();
		_reserveQueue = new ArrayList<>();
		
		OsmStrokeDataGeom osmStrokeDataGeom = new OsmStrokeDataGeom();
		osmStrokeDataGeom.startConnection();
		// ストロークの重要度が高い順に順に調べる.
		for(int i=0; i<_strokeArcString.size(); i++){
			if(
				osmStrokeDataGeom.checkIntersectStrokeAndCircle(_centerLngLat, _strokeArcString.get(i), _glueInnerRadiusMeter)
				&&
				osmStrokeDataGeom.checkIntersectStrokeAndCircle(_centerLngLat, _strokeArcString.get(i), _glueOuterRadiusMeter)
			){// focus-contextを直接またぐストロークである.
				_selectedStrokeIndex.add(i);
				if(_selectedStrokeIndex.size() > MAX_DRAW_NUM) break;	// 指定した数だけ描画したら終了.
				// 新しく描画か確定したストロークと保留キューにあるストロークが交差するか確かめる.
				func(osmStrokeDataGeom, _strokeArcString.get(i));
			}else{
				_reserveQueue.add(i);
			}
		}
		
		osmStrokeDataGeom.endConnection();
	}
	
	/**
	 *  新しく描画か確定したストロークと保留キューにあるストロークが交差するか確かめる.
	 * @param osmStrokeDataGeom
	 * @param strokeString 新しく描画が確定したストローク
	 */
	public void func(OsmStrokeDataGeom osmStrokeDataGeom, String strokeString){
		// 新しく描画か確定したストロークと保留キューにあるストロークが交差するか確かめる.
		for(int j=0; j<_reserveQueue.size(); j++){
			if(osmStrokeDataGeom.checkIntersectTwoStroke(strokeString, _strokeArcString.get(_reserveQueue.get(j)))){
				_selectedStrokeIndex.add(_reserveQueue.get(j));
				if(_selectedStrokeIndex.size() > MAX_DRAW_NUM) break;// 指定した数だけ描画したら終了.
				_reserveQueue.remove(j);
				j--;
				func(osmStrokeDataGeom, _strokeArcString.get(_selectedStrokeIndex.get(_selectedStrokeIndex.size()-1)));	// 再帰で呼び出す.
			}
		}
	}
	
}
