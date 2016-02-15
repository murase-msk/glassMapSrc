package src.drawMitinariSenbetuAlgorithm;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.naming.InitialContext;

import com.sun.org.apache.bcel.internal.generic.NEW;

import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.ConvertMercatorXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.coordinate.LngLatMercatorUtility;
import src.db.getData.OsmRoadDataGeom;

/**
 * 道なり道路選別手法
 * 
 * 道なりルール
 * Rule1
 * 1つのリンクと接続していたらそれが道なりのリンク
 * Rule2
 * 2つ以上のリンクと接続していたらなす角が指定した角度α以下が道なりのリンク
 * Rule3
 * 省略
 * Rule4
 * T字路(2つのリンクと接続し，2つのリンクのなす角がα以上)ならその2つのリンクは道なりのリンク
 * 
 * 選択ルール
 * focus-glueをまたぐリンク(focus側を始点，glue側を終点)から道なりルールを再帰的に適用
 * 
 * 
 * @author murase
 *
 */
public class MitinariDouroSenbetuAlgorithm {
	/** 道なりとする最大のなす角 pi/6(30°) */
	private static final double ALPHA = Math.PI/6;
	
	/** 初期の緯度経度Point2D形式 */
	private  Point2D centerLngLat = new Point2D.Double(136.9309671669116, 35.15478942665804);// 鶴舞公園.
	/** 中心点からglue内側の長さ(メートル) */
	private double glueInnerRadiusMeter;
	/** 中心点からglue外側の長さ(メートル)  */
	private double glueOuterRadiusMeter;
	
	// 求める値.
	/** 道なり選別手法によって選択されたリンクの集合 */
	public ArrayList<DirectLink> _selectedLinkSet = new ArrayList<>();
	
	/**
	 * コンストラクタ
	 * @param aCenterLngLat
	 * @param aGlueInnerRadiusMeter
	 * @param aGlueOuterRadiusMeter
	 */
	public MitinariDouroSenbetuAlgorithm(Point2D aCenterLngLat, double aGlueInnerRadiusMeter, double aGlueOuterRadiusMeter){
		centerLngLat = aCenterLngLat;
		glueInnerRadiusMeter = aGlueInnerRadiusMeter;
		glueOuterRadiusMeter = aGlueOuterRadiusMeter;
		initExec();
	}
	
	/**
	 * 初期設定をして実行
	 */
	public void  initExec(){
		// focus-glue境界の円上のリンクを取り出す.
		OsmRoadDataGeom osmInnerRoadDataGeom = new OsmRoadDataGeom();
		OsmRoadDataGeom osmOuterRoadDataGeom = new OsmRoadDataGeom();
		osmInnerRoadDataGeom.startConnection();
		osmOuterRoadDataGeom.startConnection();
		osmInnerRoadDataGeom.getOsmRoadFromPolygon(centerLngLat, glueInnerRadiusMeter, false);
		osmOuterRoadDataGeom.getOsmRoadFromPolygon(centerLngLat, glueOuterRadiusMeter, true);
		osmInnerRoadDataGeom.endConnection();
		osmOuterRoadDataGeom.endConnection();
		
		// focus,glue内の道路のDirectLink.
		ArrayList<DirectLink> osmOuterRoadLink = new ArrayList<>();
		for(int i=0; i<osmOuterRoadDataGeom.__link.size(); i++){
			osmOuterRoadLink.add(new DirectLink(osmOuterRoadDataGeom.__linkId.get(i), osmOuterRoadDataGeom.__link.get(i), 
					osmOuterRoadDataGeom.__sourceId.get(i), osmOuterRoadDataGeom.__targetId.get(i), osmOuterRoadDataGeom.__length.get(i), 
					osmOuterRoadDataGeom.__clazz.get(i), osmOuterRoadDataGeom.__arc.get(i)));
		}
		
		// 道なりを選択していく最初のリンクのスタック
		ArrayList<DirectLink> linkStack = new ArrayList<>();
		for(int i=0; i<osmInnerRoadDataGeom.__link.size(); i++){
			// P1がfocusの中,P2がfocusの外にあるならP1が始点P2が終点.
			if(LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, osmInnerRoadDataGeom.__link.get(i).getP1()) < glueInnerRadiusMeter &&
					LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, osmInnerRoadDataGeom.__link.get(i).getP2()) > glueInnerRadiusMeter
			){
				linkStack.add(new DirectLink(osmInnerRoadDataGeom.__linkId.get(i), osmInnerRoadDataGeom.__link.get(i), 
						osmInnerRoadDataGeom.__sourceId.get(i), osmInnerRoadDataGeom.__targetId.get(i), osmInnerRoadDataGeom.__length.get(i), 
						osmInnerRoadDataGeom.__clazz.get(i), osmInnerRoadDataGeom.__arc.get(i)));
				linkStack.get(linkStack.size()-1).setSelectedFlg(true);
			// P2がfocusの中,P1がfocusの外にあるならP2が始点P1が終点.
			}else if(LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, osmInnerRoadDataGeom.__link.get(i).getP2()) < glueInnerRadiusMeter &&
					LngLatMercatorUtility.calcDistanceFromLngLat(centerLngLat, osmInnerRoadDataGeom.__link.get(i).getP1()) > glueInnerRadiusMeter
					){
				linkStack.add(new DirectLink(osmInnerRoadDataGeom.__linkId.get(i), osmInnerRoadDataGeom.__link.get(i), 
						osmInnerRoadDataGeom.__sourceId.get(i), osmInnerRoadDataGeom.__targetId.get(i), osmInnerRoadDataGeom.__length.get(i), 
						osmInnerRoadDataGeom.__clazz.get(i), osmInnerRoadDataGeom.__arc.get(i)));
				linkStack.get(linkStack.size()-1).reverseAngle();
				linkStack.get(linkStack.size()-1).setSelectedFlg(true);
			// そうでないならP1始点P2終点，P2始点P1終点の両方を行う.
			}else{
//				linkPool.add(new DirectLink(osmInnerRoadDataGeom.__linkId.get(i), osmInnerRoadDataGeom.__link.get(i), 
//						osmInnerRoadDataGeom.__sourceId.get(i), osmInnerRoadDataGeom.__targetId.get(i), osmInnerRoadDataGeom.__length.get(i), 
//						osmInnerRoadDataGeom.__clazz.get(i), osmInnerRoadDataGeom.__arc.get(i)));
//				linkPool.get(linkPool.size()-1).setSelectedFlg(true);
//				linkPool.add(new DirectLink(osmInnerRoadDataGeom.__linkId.get(i), osmInnerRoadDataGeom.__link.get(i), 
//						osmInnerRoadDataGeom.__sourceId.get(i), osmInnerRoadDataGeom.__targetId.get(i), osmInnerRoadDataGeom.__length.get(i), 
//						osmInnerRoadDataGeom.__clazz.get(i), osmInnerRoadDataGeom.__arc.get(i)));
//				linkPool.get(linkPool.size()-1).reverseAngle();
//				linkPool.get(linkPool.size()-1).setSelectedFlg(true);
			}
		}
		_selectedLinkSet = getMitinari(linkStack, osmOuterRoadLink);
		
	}
	
	/***
	 * リンクスタックの末尾から1つ取り出し，道なりに辿っていく
	 * @param aLinkStack
	 */
	private ArrayList<DirectLink> getMitinari(ArrayList<DirectLink> aLinkStack, ArrayList<DirectLink> aOsmOuterRoadLink){
		ArrayList<DirectLink> selectedLinkSet = new ArrayList<>();
		while(aLinkStack.size() != 0){
			DirectLink oneLink = aLinkStack.get(aLinkStack.size()-1);	// 末尾のリンクを取り出す.
			selectedLinkSet.add(aLinkStack.get(aLinkStack.size()-1));	// このリンクは選択された.
			aLinkStack.remove(aLinkStack.get(aLinkStack.size()-1));	// リンクプールから削除.
			ArrayList<DirectLink> next = nextMitinari(oneLink, aOsmOuterRoadLink);
//			System.out.println("next"+next);
			aLinkStack.addAll(next);	// 次の道なりを求めてリンクプールへ入れる.
		}
		return selectedLinkSet;
	}
	
	
	/**
	 * 指定した有向辺リンクから次に道なりになるリンクを取り出す
	 * @param aDirectLink 有向辺のリンク
	 * @param aOsmOuterRoadLink focus,glue内部にあるリンク
	 */
	public ArrayList<DirectLink> nextMitinari(DirectLink aDirectLink, ArrayList<DirectLink> aOsmOuterRoadLink){
//		System.out.println("directlink string :  "+aDirectLink.toString());
		ArrayList<Integer> touchedLinkIndex = new ArrayList<>();	// aDirectLinkと接触するリンクのaOsmOuterRoadLinkに対するインデックス.
		// 引数で指定したリンクと接触するリンクを探す.
		for(int i=0; i<aOsmOuterRoadLink.size(); i++){
			if( aDirectLink.sourceId == aOsmOuterRoadLink.get(i).sourceId &&
				aDirectLink.targetId == aOsmOuterRoadLink.get(i).targetId ||
				aDirectLink.sourceId == aOsmOuterRoadLink.get(i).targetId &&
				aDirectLink.targetId == aOsmOuterRoadLink.get(i).sourceId){	// 自分自身と同じであれば無視する.
				continue;
			}
			if(aDirectLink.targetId == aOsmOuterRoadLink.get(i).sourceId && aDirectLink.targetId > 0){
				touchedLinkIndex.add(i);
			}else if(aDirectLink.targetId == aOsmOuterRoadLink.get(i).targetId && aDirectLink.targetId > 0){
				touchedLinkIndex.add(i);
				// 順番の入れ替え.
				aOsmOuterRoadLink.get(i).reverseAngle();
			}
		}
		ArrayList<DirectLink> mitinariLink = new ArrayList<>();
		// ここで行き止まり.
		if(touchedLinkIndex.size() == 0){
//			System.out.println("行き止まり");
			return mitinariLink;
		// 1つしかつながっていないならそれが道なり.
		}else if(touchedLinkIndex.size() == 1){
			if(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).selectedFlg == false){	// すでに選択されていなければ追加する.
				aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).setTFlg(aDirectLink.tFlg);
				aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).setSelectedFlg(true);
				mitinariLink.add(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)));
			}
//			System.out.println("1つだけつながっている");
			return mitinariLink;
		// 2つならT字路の確認をして，T字路なら2つとも道なり，そうでないならなす角がα以下のリンクを選択.
		}else if(touchedLinkIndex.size() == 2){
			if(!aDirectLink.tFlg){// まだT字路を通っていないならT字路判定をする.
				if(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).link)>ALPHA && 
						getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).link)>ALPHA){	// T字路であったならそれは2つとも道なりとする.
					if(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).selectedFlg == false){	// すでに選択されていなければ追加する.
						aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).setTFlg(true);	// T字路フラグをtrueにする.
						aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).setSelectedFlg(true);
						mitinariLink.add(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)));
					}
					if(aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).selectedFlg == false){	// すでに選択されていなければ追加する.
						aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).setTFlg(true);	// T字路フラグをtrueにする.
						aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).setSelectedFlg(true);
						mitinariLink.add(aOsmOuterRoadLink.get(touchedLinkIndex.get(1)));
					}
//					System.out.println("T字路判定2つのリンク選択");
					return mitinariLink;
				}
			}
			// T字路判定をしなくていいときはなす角がα以下のリンクを選択する.
			if(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).link)<ALPHA){
				if(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).selectedFlg == false){	// すでに選択されていなければ追加する.
					aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).setTFlg(aDirectLink.tFlg);
					aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).setSelectedFlg(true);
					mitinariLink.add(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)));
				}
			}
			if(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).link)<ALPHA){
				if(aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).selectedFlg == false){	// すでに選択されていなければ追加する.
					aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).setTFlg(aDirectLink.tFlg);
					aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).setSelectedFlg(true);
					mitinariLink.add(aOsmOuterRoadLink.get(touchedLinkIndex.get(1)));
				}
			}
//			System.out.println(aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).toString());
//			System.out.println("angle "+ Math.toDegrees(ALPHA) +"  "+ Math.toDegrees(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(0)).link)));
//			System.out.println(aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).toString());
//			System.out.println("angle "+ Math.toDegrees(ALPHA) +"  "+ Math.toDegrees(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(1)).link)));

//			System.out.println("2つ以上のリンクが接触");
			return mitinariLink;
		// 3つ以上ならなす角がα以下のリンクを選択する.
		}else if(touchedLinkIndex.size() > 2){
			for(int i=0; i<touchedLinkIndex.size(); i++){
//				System.out.println(aOsmOuterRoadLink.get(touchedLinkIndex.get(i)).toString());
//				System.out.println("angle "+ Math.toDegrees(ALPHA) +"  "+ Math.toDegrees(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(i)).link)));
				if(getAngle(aDirectLink.link, aOsmOuterRoadLink.get(touchedLinkIndex.get(i)).link)<ALPHA){
					if(aOsmOuterRoadLink.get(touchedLinkIndex.get(i)).selectedFlg == false){	// すでに選択されていなければ追加する.
						aOsmOuterRoadLink.get(touchedLinkIndex.get(i)).setTFlg(aDirectLink.tFlg);
						aOsmOuterRoadLink.get(touchedLinkIndex.get(i)).setSelectedFlg(true);
						mitinariLink.add(aOsmOuterRoadLink.get(touchedLinkIndex.get(i)));
					}
				}
			}
//			System.exit(0);
//			System.out.println("3つ以上のリンクが接触");
			return mitinariLink;
		}
//		System.out.println("おかしい");
		return null;	
		
		
	}
	
	/**
	 * 2つのベクトルのなす角を求める
	 * @return
	 */
	private double getAngle(Line2D l1, Line2D l2){
		Point2D va = new Point2D.Double(l1.getX2()-l1.getX1(), l1.getY2()-l1.getY1());
		Point2D vb = new Point2D.Double(l2.getX2()-l2.getX1(), l2.getY2()-l2.getY1());
		return Math.acos((va.getX()*vb.getX()+va.getY()*vb.getY())/(Math.hypot(va.getX(), va.getY())*Math.hypot(vb.getX(), vb.getY())));
	}
	
}
