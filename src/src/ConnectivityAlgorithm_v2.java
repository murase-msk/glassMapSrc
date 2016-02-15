package src;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;

import  src.db.getData.OsmStrokeDataGeom;
import sun.launcher.resources.launcher;
import  servlet.DrawElasticStrokeConnectivity_v2;
import src.coordinate.LngLatMercatorUtility;

/**
 * コネクティビティーを保つためのクラス
 * v2 PostGISを使わないようにする
 * @author murase
 *
 */
public class ConnectivityAlgorithm_v2 {
	
	/** 最大何本ストロークを描画するか */
	public static final int MAX_DRAW_NUM = 20;
	
	
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
	/** ストロークのMBR(小さい方) */
	public ArrayList<Point2D> _mbrMinXy = new ArrayList<>();
	/** ストロークのMBR(大きい方) */
	public ArrayList<Point2D> _mbrMaxXy = new ArrayList<>();

	
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

	
	
	public ConnectivityAlgorithm_v2(OsmStrokeDataGeom aOsmStrokeDataGeom, DrawElasticStrokeConnectivity_v2 DrawElasticStrokeConnectivity_v2){
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
		_mbrMinXy = aOsmStrokeDataGeom._mbrMinXy;
		_mbrMaxXy = aOsmStrokeDataGeom._mbrMaxXy;
	}
	
	/**
	 * 描画するストロークを選択する
	 */
	public void selectDrawingStroke(){
		
		_selectedStrokeIndex = new ArrayList<>();
		_reserveQueue = new ArrayList<>();
		
		// ストロークの重要度が高い順に順に調べる.
		for(int i=0; i<_strokeArcString.size(); i++){
//			System.out.println("oneStroke");
			if(
				isStepOverFromFocusToContext(_centerLngLat, _glueInnerRadiusMeter, _glueOuterRadiusMeter, _strokeArcPoint.get(i)) || // focus-contextを直接またぐストロークである.
				isConnectedIndirect(_strokeArcPoint.get(i), i)// focus-contextを間接的にまたぐストロークである.
			){
				_selectedStrokeIndex.add(i);
//				System.out.println("add");
				if(_selectedStrokeIndex.size() > MAX_DRAW_NUM) break;	// 指定した数だけ描画したら終了.
				// 新しく描画か確定したストロークと保留キューにあるストロークが交差するか確かめる.
				func(_strokeArcPoint.get(i));
			}else{
				_reserveQueue.add(i);
			}
		}
	}
	
	/**
	 *  新しく描画か確定したストロークと保留キューにあるストロークが交差するか確かめる.
	 * @param osmStrokeDataGeom
	 * @param strokeString 新しく描画が確定したストローク
	 */
	public void func(ArrayList<Point2D> aStrokeArcPoint){
		// 新しく描画か確定したストロークと保留キューにあるストロークが交差するか確かめる.
		for(int j=0; j<_reserveQueue.size(); j++){
			if(isIntersectTwoStrokePoint2D(aStrokeArcPoint, _strokeArcPoint.get(_reserveQueue.get(j)))){
				_selectedStrokeIndex.add(_reserveQueue.get(j));
				//System.out.println("func add");
				if(_selectedStrokeIndex.size() > MAX_DRAW_NUM) break;// 指定した数だけ描画したら終了.
				_reserveQueue.remove(j);
				j--;
				func(_strokeArcPoint.get(_selectedStrokeIndex.get(_selectedStrokeIndex.size()-1)));	// 再帰で呼び出す.
			}
		}
	}
	
	
	/**
	 * 指定したストロークがfocus-contextをまたぐか
	 * @param aCenterLngLat　中心の緯度経度
	 * @param aGlueInnerRadiusMeter glue内側の半径(メートル)
	 * @param aGlueOuterRadiusMeter glue外側の半径(メートル)
	 * @param aStrokeArcPoint ある1つのストローク
	 * @return
	 */
	public boolean isStepOverFromFocusToContext(Point2D aCenterLngLat, double aGlueInnerRadiusMeter, double aGlueOuterRadiusMeter,
			ArrayList<Point2D> aStrokeArcPoint){
		
		boolean innerFlg = false, outerFlg = false;
		for(Point2D item: aStrokeArcPoint){
			if(isInCircle(aCenterLngLat, item, aGlueInnerRadiusMeter)){	// focusの内側にあるか.
				innerFlg = true;
			}
			if(!isInCircle(aCenterLngLat, item, aGlueOuterRadiusMeter)){// contextの外側にあるか.
				outerFlg = true;
			}
			if((innerFlg == true) && (outerFlg == true)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 指定した円の中に点があるか
	 * @param p1 1つの点
	 * @param p2 もう1つの点
	 * @param border 円の中にあるかどうかを判定する長さ
	 * @return
	 */
	public boolean isInCircle(Point2D p1, Point2D p2, double border){
		if (LngLatMercatorUtility.calcDistanceFromLngLat(p1, p2) < border){
			return true;
		}
		return false;
	}
	
	/**
	 * 指定したストロークが「間接的にfocus-contextをつなぐストローク」であるか確かめる
	 * そのために，指定したストロークが他のすべてのストロークと交差するか調べる
	 * 
	 * 「間接的にfocus-contextをつなぐストローク」とは，描画が確定したストロークと交差し，かつ，focusかcontextのどちらかと交差すること
	 * 
	 * @param aStrokeArcPoint 交差するか調べたいストローク
	 * @return
	 */
	public boolean isConnectedIndirect(ArrayList<Point2D> aStrokeArcPoint, int aStrokeIndex){
		for(int i=0; i<_selectedStrokeIndex.size(); i++){
			// MBRを使って簡易判定.
			if(
					_mbrMaxXy.get(_selectedStrokeIndex.get(i)).getX() < _mbrMinXy.get(aStrokeIndex).getX() ||
					_mbrMaxXy.get(_selectedStrokeIndex.get(i)).getY() < _mbrMinXy.get(aStrokeIndex).getY() ||
					_mbrMinXy.get(_selectedStrokeIndex.get(i)).getX() > _mbrMaxXy.get(aStrokeIndex).getX() ||
					_mbrMinXy.get(_selectedStrokeIndex.get(i)).getY() > _mbrMaxXy.get(aStrokeIndex).getY() 
					){
				return false;
			}
			
			// 普通に交差判定.
			if(isIntersectTwoStrokePoint2D(aStrokeArcPoint, _strokeArcPoint.get(_selectedStrokeIndex.get(i)))){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 2つのストロークの交差判定遅い版? こっちの方が早い? あまり変わらない?
	 * @param s1
	 * @param s2
	 * @return
	 */
	public boolean isIntersectTwoStrokePoint2D(ArrayList<Point2D> s1, ArrayList<Point2D> s2){
		for(int i=0; i<s1.size()-1; i++){
			for(int j=0; j<s2.size()-1; j++){
				if(new Line2D.Double(s1.get(i), s1.get(i+1)).intersectsLine(s2.get(j).getX(), s2.get(j).getY(), s2.get(j+1).getX(), s2.get(j+1).getY())){
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * 2つのストロークが交差するか
	 * @param s1 _strokeArcPointの形
	 * @param s2 _strokeArcPointの形
	 * @return
	 */
	public boolean isIntersectTwoStroke(ArrayList<Point2D> s1, ArrayList<Point2D> s2){
		// 端点が同じだったら消す
		if(s1.get(0).getX() == s1.get(s1.size()-1).getX() && s1.get(0).getY() == s1.get(s1.size()-1).getY()){
			s1.remove(s1.size()-1);
		}
		if(s2.get(0).getX() == s2.get(s2.size()-1).getX() && s2.get(0).getY() == s2.get(s2.size()-1).getY()){
			s2.remove(s2.size()-1);
		}
		
		// 端点で交差するか確かめる.
		if(
			(s1.get(0).getX() == s2.get(0).getX() && s1.get(0).getY() == s2.get(0).getY())||
			(s1.get(0).getX() == s2.get(s2.size()-1).getX() && s1.get(0).getY() == s2.get(s2.size()-1).getY())||
			(s1.get(s1.size()-1).getX() == s2.get(0).getX() && s1.get(s1.size()-1).getY() == s2.get(0).getY())||
			(s1.get(s1.size()-1).getX() == s2.get(s2.size()-1).getX() && s1.get(s1.size()-1).getY() == s2.get(s2.size()-1).getY())
		){
			return true;
		}
		
		// 中で交差するか確かめる.
		// 2つのストロークをそれぞれ偶数番目だけのセグメントを取り出して1つの集合とする
		ArrayList<Line2D> sList = new ArrayList<>();
		for(int i=0; i<s1.size()-1; i=i+2){
			sList.add(new Line2D.Double(s1.get(i), s1.get(i+1)));
		}
		for(int i=0; i<s2.size()-1; i=i+2){
			sList.add(new Line2D.Double(s2.get(i), s2.get(i+1)));
		}
		return isIntersectLines(sList);
	}
	
	/**
	 * 複数の線分が交差しているか(端点でしか交差しないことを想定)
	 * @return
	 */
	public boolean isIntersectLines(ArrayList<Line2D> segmentList){
		
		HashSet<String> hashString = new HashSet<>();
		for(int i=0; i<segmentList.size(); i++){
			hashString.add(""+segmentList.get(i).getX1()+"_"+segmentList.get(i).getY1());
			hashString.add(""+segmentList.get(i).getX2()+"_"+segmentList.get(i).getY2());
		}
		if(hashString.size() == segmentList.size()*2){
			return false;	// 交差しない.
		}
		return true;// 交差する.
	}
	
}
