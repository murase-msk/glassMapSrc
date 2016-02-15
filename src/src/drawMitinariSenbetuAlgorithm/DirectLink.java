package src.drawMitinariSenbetuAlgorithm;

import java.awt.geom.Line2D;
import java.util.ArrayList;

/**
 * 有向辺のデータ
 * @author murase
 *
 */
public class DirectLink {
	
	public int linkId;
	public Line2D link;
	int sourceId;
	int targetId;
	double length;
	public int clazz;
	/** 詳細な道路の形状を表す 向きはない */
	public ArrayList<Line2D> arc;
	
	/** 1回T字路を通ったか */
	boolean tFlg = false;
	/** 選択されたか */
	boolean selectedFlg = false;
	
	public DirectLink(int alinkId, Line2D alink, int asourceId, int atargetId, double alength, int aclazz, ArrayList<Line2D> aarc){
		linkId = alinkId;
		link = alink;
		sourceId = asourceId;
		targetId = atargetId;
		length = alength;
		clazz = aclazz;
		arc = aarc;
	}
	
	/**
	 * 方向を逆にする
	 */
	public void reverseAngle(){
		this.link = new Line2D.Double(this.link.getP2(), this.link.getP1());
		int tmp = this.sourceId;
		this.sourceId = this.targetId;
		this.targetId = tmp;
	}
	
	/**
	 * T字路フラグを設定
	 */
	public void setTFlg(boolean flg){
		this.tFlg = flg;
	}
	public void setSelectedFlg(boolean aFlg){
		this.selectedFlg = aFlg;
	}
	
	public String toString(){
		return "LinkId:"+linkId+", link:linestring("+link.getX1()+" "+link.getY1()+","+link.getX2()+" "+link.getY2()+"), sourceId:"+sourceId+" targetId:"+targetId+", clazz"+clazz;
	}
}
