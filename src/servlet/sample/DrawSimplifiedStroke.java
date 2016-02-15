package servlet.sample;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import servlet.ErrorMsg;
import src.QuickSort2;
import src.coordinate.ConvertLngLatXyCoordinate;
import src.coordinate.GetLngLatOsm;
import src.db.GeneralPurposeGeometry;
import src.db.getData.OsmRoadDataGeom;
import src.db.getData.OsmStrokeDataGeom;

/**
 * simplificationしたストロークの描画
 * @author murase
 *
 */
public class DrawSimplifiedStroke {
	/** 地図の大きさ */
	public Point windowSize = new Point(700, 700);
	/** 初期の緯度経度Point2D形式 */
	private  Point2D centerLngLat;// = new Point2D.Double(136.9309671669116, 35.15478942665804);// 鶴舞公園.
	/** スケール. */
	private int scale;// = 15;
	/** 閾値 */
	private  double threshold = 50;
	
	
	Graphics2D _graphics2d;
	public GetLngLatOsm _getLngLatOsm;
	public ConvertLngLatXyCoordinate _convert;
	public Point2D _upperLeftLngLat;
	public Point2D _lowerRightLngLat;
	
	/**
	 * http://localhost:8080/EmmaGlueMuraseOriginal/MainServlet?type=DrawSimplifiedStroke&centerLngLat=136.9309671669116,35.15478942665804&scale=15&threshold=50
	 * @param request
	 * @param response
	 */
	public DrawSimplifiedStroke(HttpServletRequest request, HttpServletResponse response){
		// 必須パラメータがあるか.
		if(
				request.getParameter("scale")==null||
				request.getParameter("centerLngLat")==null
			){
			ErrorMsg.errorResponse(request, response, "必要なパラメータがありません");
			return;
		}
		// パラメータの受け取り.
		centerLngLat = new Point2D.Double(
				Double.parseDouble(request.getParameter("centerLngLat").split(",")[0]), 
				Double.parseDouble(request.getParameter("centerLngLat").split(",")[1]));
		scale = Integer.parseInt(request.getParameter("scale"));
		threshold = Integer.parseInt(request.getParameter("threshold"));
		try{
			OutputStream out=response.getOutputStream();
			ImageIO.write( drawImage(), "png", out);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 道路データの取得しbufferedimageの作成
	 * @return
	 */
	private BufferedImage drawImage(){
		BufferedImage bfImage = new BufferedImage( windowSize.x, windowSize.y, BufferedImage.TYPE_INT_ARGB);
		_graphics2d = (Graphics2D) bfImage.getGraphics();
		// アンチエイリアス設定：遅いときは次の行をコメントアウトする.
		_graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// 中心座標とスケールから 左上と右下の座標取得.
		_getLngLatOsm = new GetLngLatOsm(centerLngLat, scale, windowSize);
		_upperLeftLngLat = _getLngLatOsm._upperLeftLngLat;
		_lowerRightLngLat = _getLngLatOsm._lowerRightLngLat;
		// 緯度経度とXy座標の変換用インスタンス.
		_convert = new ConvertLngLatXyCoordinate((Point2D.Double)_upperLeftLngLat,
				(Point2D.Double)_lowerRightLngLat,windowSize);
		
		// ストローク取得.
		OsmStrokeDataGeom osmStrokeDataGeom = new OsmStrokeDataGeom();
		osmStrokeDataGeom.startConnection();
		osmStrokeDataGeom.insertStrokeData(_upperLeftLngLat, _lowerRightLngLat);
		osmStrokeDataGeom.endConnection();
		paintStroke(osmStrokeDataGeom._strokeArcPoint);
		
		return bfImage;
	}
	
	/**
	 * ストロークの描画
	 * @param aPathList
	 */
	public void paintStroke(ArrayList<ArrayList<Point2D>> aPathList){
		for(int i=0; i<aPathList.size(); i++){
			ArrayList<Point> simplifiedStroke = simplifyStroke(_convert.convertLngLatToXyCoordinate(aPathList.get(i)));
			drawPath(simplifiedStroke, 1, Color.pink);
			drawPoint(simplifiedStroke, 6, new Color(240,128,128,128));
		}
		
	}
	/**
	 * simplificationをする
	 * @param aPathList
	 * @return
	 */
	public ArrayList<Point> simplifyStroke(ArrayList<Point> aPath){
		ArrayList<Point> pathList = new ArrayList<>(aPath);	// ディープコピー必要？.
		ArrayList<Integer> pathIndex = new ArrayList<>();	// pathのインデックス.
		ArrayList<Double> eachPathValue = new ArrayList<>();	// 各頂点における評価値.
//		int inWindowPointNum = 0; // 画面内にある点の数.
		int removeNum = 0;	// 消した数.
		int renzokuRemoveNum = 0;	// 連続で消した数.
//		GeneralPurposeGeometry generalPurposeGeometry = new GeneralPurposeGeometry();
//		generalPurposeGeometry.startConnection();
		
		for(int i=0; i<aPath.size(); i++){
			pathIndex.add(i);
		}
		
		
		for(int i=0; i<pathList.size(); i++){
//			if(		pathList.get(i).getX() < 0 || windowSize.getX() < pathList.get(i).getX()||
//					pathList.get(i).getY() < 0 || windowSize.getY() < pathList.get(i).getY()
//				){// 画面外にあれば何もしない.
//				eachPathValue.add(Double.MAX_VALUE);
//				continue;
//			}
			if(i==0 || i==pathList.size()-1){	// 最初と最後も何もしない.
				eachPathValue.add(Double.MAX_VALUE);
//				inWindowPointNum++;
				continue;
			}
			/////////////////////////////////////
			//前回の結果を使う(パターン1)
			/////////////////////////////////////
			// pathList.get(i-1), pathList.get(i), pathList.get(i+1).
			// v_a, v_b, theta.
//				Point2D va = new Point2D.Double(
//						pathList.get(i-1).getX() - pathList.get(i).getX(), 
//						pathList.get(i-1).getY() - pathList.get(i).getY());
//				Point2D vb = new Point2D.Double(
//						pathList.get(i+1).getX() - pathList.get(i).getX(), 
//						pathList.get(i+1).getY() - pathList.get(i).getY());
//				Point2D vc = new Point2D.Double(
//						pathList.get(i+1).getX() - pathList.get(i-1).getX(), 
//						pathList.get(i+1).getY() - pathList.get(i-1).getY());
//				double thetaRadian = Math.acos((va.getX()*vb.getX()+va.getY()*vb.getY())/(Math.hypot(va.getX(), va.getY())*Math.hypot(vb.getX(), vb.getY())));
//				double L2ErrorNorm = 1.0/2.0*Math.hypot(va.getX(), va.getY())*Math.hypot(vb.getX(), vb.getY())*Math.sin(thetaRadian);
				/////////////////////////////////////
				
			///////////////////////////////////////////////////////////
			// もともとあるline(original curve)と間引いたときのline(removedcurve)で囲まれた面積を求める.(パターン2)
			// original curve : aPath.get(i+removeNum-renzokuRemoveNum-1)~aPath.get(i+removeNum+1).
			// removed curve  : pathList.get(i-1)~pathList.get(i+1)
			/////////////////////////////////////////////////////////////
//				ArrayList<Point2D> originalCurve = new ArrayList<>();	// もともとあるline.
//				ArrayList<Point2D> removedCurve = new ArrayList<>();	// 間引いたときのline.
//				for(int k=i+removeNum-renzokuRemoveNum-1; k<=i+removeNum+1; k++){
//					originalCurve.add(aPath.get(k));
//				}
//				removedCurve.add(pathList.get(i-1));
//				removedCurve.add(pathList.get(i+1));
//				// originalCurvをremovedCurveで切った時の結果
//				ArrayList<ArrayList<Point2D>> splittedLine  = generalPurposeGeometry.splitLine(originalCurve, removedCurve);
//				double L2ErrorNorm=0;
//				if(splittedLine==null){	// 2つのlineが重なっていた.
//					L2ErrorNorm = 0;
//				}else{
//					for(int k=0;k<splittedLine.size(); k++){
//						splittedLine.get(k).add(new Point2D.Double(splittedLine.get(k).get(0).getX(), splittedLine.get(k).get(0).getY())); // 多角形になるようにする.
//						L2ErrorNorm += calcPolygonArea(splittedLine.get(k)); //polygonの面積を計算.
//					}
//				}
				//////////////////////////////////////////////
				
//				System.out.println("l2errorNorm : " + L2ErrorNorm);
//				if(L2ErrorNorm < threshold || Double.isNaN(L2ErrorNorm)){ // 閾値より小さかったら中間点を削除する.
//					System.out.println("remove");
//					pathList.remove(i);
//					i--;
//					removeNum++;
//					renzokuRemoveNum++;
//				}else{
//					renzokuRemoveNum = 0;
//				}
			
			/////////////////////////////////////////////////////////
			// p_n-1, p_n, p_n+1の面積を求める.
			/////////////////////////////////////////////////////////
			double area = calcPolygonArea(new ArrayList<>(Arrays.asList(
					(Point2D)new Point2D.Double(aPath.get(i-1).getX(), aPath.get(i-1).getY()),
					(Point2D)new Point2D.Double(aPath.get(i).getX(), aPath.get(i).getY()), 
					(Point2D)new Point2D.Double(aPath.get(i+1).getX(), aPath.get(i+1).getY()),
					(Point2D)new Point2D.Double(aPath.get(i-1).getX(), aPath.get(i-1).getY()))));
			eachPathValue.add(area);
//			inWindowPointNum++;
		}
		QuickSort2<Double, Integer> quickSort2 = new QuickSort2<>(eachPathValue, pathIndex, false);
		pathIndex = quickSort2.getArrayList2();
//		// 評価値の低いものからn個選択.
		for(int i=0; i<pathList.size()*((100-threshold)/100.0); i++){	// 画面上のnバーセントのノードを表価値の低いノードから削除.
			if(eachPathValue.get(pathIndex.get(i)) == Double.MAX_VALUE){	// 端点は絶対残す.
				break;
			}
			pathList.set(pathIndex.get(i), null);	// nullをセット.
//			eachPathValue.set(pathIndex.get(i), null);
		}
		
		removeNum = 0;
		for(int i=0; i<pathList.size(); i++){	// nullをセットしたノードを削除.
			if(pathList.get(i) == null){
				pathList.remove(i);
				i--;
				removeNum++;
			}
		}
		////////////////////////////////////////////////////////
//		generalPurposeGeometry.endConnection();
		return pathList;
	}
	
	/**
	 * 多角形の面積を求める
	 * @param (p.get(0) とp.get(p.size())はおなじでないといけない)
	 */
	private double calcPolygonArea(ArrayList<Point2D> p){
		double menseki = 1.0/2.0;
//		System.out.println("ppp "+p);
		for(int i=0;i<p.size()-1; i++){
//			System.out.println("pp "+p.get(i));
//			System.out.println("pp "+p.get(i+1));
//			System.out.println("aa  "+(p.get(i).getX()-p.get(i+1).getX()));
//			System.out.println("bb  "+(p.get(i).getY()+p.get(i+1).getY()));
			menseki += (p.get(i).getX()-p.get(i+1).getX())*(p.get(i).getY()+p.get(i+1).getY());
		}
		return Math.abs(menseki);
	}
	
	/**
	 * パスの描画
	 * @param path
	 * @param aPathWidth
	 * @param aColor
	 */
	private void drawPath(ArrayList<Point> path,int aPathWidth, Color aColor){
		_graphics2d.setPaint(aColor);
		_graphics2d.setStroke(new BasicStroke(aPathWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		GeneralPath generalPath = new GeneralPath();
		for(int i=0; i<path.size(); i++){
			if(i==0){
				generalPath.moveTo(path.get(i).getX(), path.get(i).getY());
			}
			generalPath.lineTo(path.get(i).getX(), path.get(i).getY());
		}
		_graphics2d.draw(generalPath);
	}
	/**
	 * 点の描画
	 * @param path
	 * @param aRadius
	 * @param aColor
	 */
	private void drawPoint(ArrayList<Point> path,int aRadius, Color aColor){
		for(int i=0; i<path.size(); i++){
			Ellipse2D.Double ellipse = new Ellipse2D.Double(path.get(i).getX()-aRadius/2, path.get(i).getY()-aRadius/2, aRadius, aRadius);
			_graphics2d.setStroke(new BasicStroke(1.0f));
			_graphics2d.setPaint(Color.black);
			_graphics2d.draw(ellipse);	// 輪郭の描画.
			_graphics2d.setPaint(aColor);
			_graphics2d.fill(ellipse);	// 内部塗りつぶし.
		}
	}
	
}
