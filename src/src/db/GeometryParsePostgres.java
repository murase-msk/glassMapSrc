package src.db;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgis.Geometry;
import org.postgis.LineString;
import org.postgis.LinearRing;
import org.postgis.MultiLineString;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;


/**
 * ジオメトリのパースをする
 * @author murase
 *
 */
public class GeometryParsePostgres {
	
//	public static final int SRID = 4301;
	
	public GeometryParsePostgres(){
		
	}
	
	/**
	 * point型のデータをWKT形式のPointに変換
	 * @param aPoint
	 * @return
	 */
	public static String point2dString(Point2D aPoint, int SRID){
		String polygonString="POINT(";
		polygonString += aPoint.getX() + " " + aPoint.getY();
		polygonString +=")";
		
		
		polygonString = "ST_PointFromText(\'"+polygonString+"\', "+SRID+")";
		
		return polygonString;
	}
	/**
	 * ArrayList<Point2d>からst_lineFromTextに変換する
	 * @param aMultiLine
	 * @return
	 */
	static public String linePointString(ArrayList<Point2D> aMultiLine, int SRID){
		// MultiLineString((),())の作成.
		String multiLineStiring = "'LineString(";
		for(int i=0; i<aMultiLine.size(); i++){
			multiLineStiring +=""+aMultiLine.get(i).getX()+" "+aMultiLine.get(i).getY()+",";
		}
		multiLineStiring = multiLineStiring.substring(0, multiLineStiring.length()-1);
		multiLineStiring +=")'";
		
		String lineFromText = "st_geomFromText("+multiLineStiring+", "+SRID+")";
		return lineFromText;
	}
	/**
	 * ArrayListからWKT形式のPolygonを返す
	 * @param anode
	 * @return
	 */
	public static String polygonString(ArrayList<Point2D> aMultiNode, int SRID){
		String polygonWKT = "'Polygon((";
		
		for(int i=0; i<aMultiNode.size(); i++){
			polygonWKT += ""+aMultiNode.get(i).getX() + " " + aMultiNode.get(i).getY() + ",";
		}
		polygonWKT = polygonWKT.substring(0, polygonWKT.length()-1);
		polygonWKT +="))'";
		polygonWKT = "st_polygonFromText("+polygonWKT+", "+SRID+")";
		return polygonWKT;
	}
	/**
	 * PGgeometryからpoint2Dに変換
	 * @param geom
	 * @return
	 */
	public static Point2D pgGeometryToPoint2D(PGgeometry geom){
		Point2D point = new Point2D.Double();
		
		if(geom.getGeoType() == Geometry.POINT){
			Point geometryPoint = (Point)geom.getGeometry();
			point = new Point2D.Double(geometryPoint.getX(), geometryPoint.getY());
		}
		return point;
	}
	/**
	 * PGeometry(LineStringの複数ポイント)オブジェクトをLine2Dに変換
	 * @deprecated getLineStringMultiLine2()を使うこと
	 * @see GeometryParsePostgres#getLineStringMultiLine2()
	 */
	public static ArrayList<Line2D> getLineStringMultiLine(PGgeometry aGeom){
		ArrayList<Line2D> lineStringMultiLine = new ArrayList<>();
		if(aGeom.getGeoType() == Geometry.LINESTRING){
			LineString lineString = (LineString)aGeom.getGeometry(); 
			for(int i=0; i<lineString.numPoints()-1; i++){
				lineStringMultiLine.add(new Line2D.Double(
						lineString.getPoint(i).getX(), lineString.getPoint(i).getY(), 
						lineString.getPoint(i+1).getX(), lineString.getPoint(i+1).getY()));
			}
		}
		return lineStringMultiLine;
	}
	/**
	 * PGeometry(LineStringの複数ポイント)オブジェクトをArrayList<Point2D>に変換
	 * @param aGeom
	 * @return
	 */
	public static ArrayList<Point2D> getLineStringMultiLine2(PGgeometry aGeom){
		ArrayList<Point2D> lineStringMultiPoint = new ArrayList<>();
		if(aGeom.getGeoType() == Geometry.LINESTRING){
			LineString lineString = (LineString)aGeom.getGeometry(); 
			for(int i=0; i<lineString.numPoints(); i++){
				lineStringMultiPoint.add(new Point2D.Double(lineString.getPoint(i).getX(), lineString.getPoint(i).getY()));
			}
		}
		return lineStringMultiPoint;
	}
	/**
	 * PGeometry(MultiLineString)オブジェクトをPoint2dに変換(arcで取得)
	 */
	public static ArrayList<ArrayList<Point2D>> getMulitLimeStringDatas2(PGgeometry aGeom){
		ArrayList<ArrayList<Point2D>> multiLineString = new ArrayList<>();
		if(aGeom.getGeoType() == Geometry.MULTILINESTRING){
			MultiLineString mLineString = (MultiLineString)aGeom.getGeometry(); 
			for( int r = 0; r < mLineString.numLines(); r++) {
				LineString lineString = mLineString.getLine(r);
				ArrayList<Point2D> tmpLineString = new ArrayList<>();
				for(int j=0; j<lineString.numPoints(); j++){
					tmpLineString.add(
							(Point2D)new Point2D.Double(
									lineString.getPoint(j).x, 
									lineString.getPoint(j).y)
							);
				}
				multiLineString.add(tmpLineString);
			} 
		}else if(aGeom.getGeoType() == Geometry.LINESTRING){
			LineString lineString = (LineString)aGeom.getGeometry(); 
			ArrayList<Point2D> tmpLineString = new ArrayList<>();
			for(int i=0; i<lineString.numPoints(); i++){
				tmpLineString.add(new Point2D.Double(
						lineString.getPoint(i).getX(), lineString.getPoint(i).getY()));
			}
			multiLineString.add(tmpLineString);
		}else{
			System.out.println("MultilineString か lineStringでありません");
			return null;
		}
		return multiLineString;
	}
	/**
	 * PGeometry(MultiLineString)オブジェクトをLine2Dに変換(端点のみ)(linkで取得)
	 * @param aPointString
	 * @return
	 */
	public static ArrayList<Line2D> getMultiLineStringData(PGgeometry aGeom){
		ArrayList<Line2D> multiLineString = new ArrayList<>();
		if(aGeom.getGeoType() == Geometry.MULTILINESTRING){
			MultiLineString mLineString = (MultiLineString)aGeom.getGeometry(); 
			for( int r = 0; r < mLineString.numLines(); r++) { 
				LineString lineString = mLineString.getLine(r);
				multiLineString.add(
						(Line2D)new Line2D.Double(
								lineString.getPoint(0).x,
								lineString.getPoint(0).y,
								lineString.getPoint(lineString.numPoints()-1).x,
								lineString.getPoint(lineString.numPoints()-1).y));
			} 
		}
		return multiLineString;
	}
	/**
	 * PGeometry(MultiLineString)オブジェクトをLine2Dに変換(arcで取得)
	 */
	public static ArrayList<ArrayList<Line2D>> getMulitLimeStringDatas(PGgeometry aGeom){
		ArrayList<ArrayList<Line2D>> multiLineString = new ArrayList<>();
		if(aGeom.getGeoType() == Geometry.MULTILINESTRING){
			MultiLineString mLineString = (MultiLineString)aGeom.getGeometry(); 
			for( int r = 0; r < mLineString.numLines(); r++) {
				LineString lineString = mLineString.getLine(r);
				ArrayList<Line2D> tmpLineString = new ArrayList<>();
				for(int j=0; j<lineString.numPoints()-1; j++){
					tmpLineString.add(
							(Line2D)new Line2D.Double(
									lineString.getPoint(j).x, 
									lineString.getPoint(j).y, 
									lineString.getPoint(j+1).x, 
									lineString.getPoint(j+1).y)
							);
				}
				multiLineString.add(tmpLineString);
			} 
		}else if(aGeom.getGeoType() == Geometry.LINESTRING){
			LineString lineString = (LineString)aGeom.getGeometry(); 
			ArrayList<Line2D> tmpLineString = new ArrayList<>();
			for(int i=0; i<lineString.numPoints()-1; i++){
				tmpLineString.add(new Line2D.Double(
						lineString.getPoint(i).getX(), lineString.getPoint(i).getY(), 
						lineString.getPoint(i+1).getX(), lineString.getPoint(i+1).getY()));
			}
			multiLineString.add(tmpLineString);
		}else{
			System.out.println("MultilineString か lineStringでありません");
		}
		return multiLineString;
	}
	
	/**
	 * PGgeometryからPolygonに変換
	 */
	public static ArrayList<Point2D> pgGeometryPolygon(PGgeometry geom){
		ArrayList<Point2D> polygonGeom = new ArrayList<>();
		
		if(geom.getGeoType() == Geometry.POLYGON){
			Polygon pl = (Polygon)geom.getGeometry(); 
			for( int r = 0; r < pl.numRings(); r++) { 
				LinearRing rng = pl.getRing(r); 
				for( int p = 0; p < rng.numPoints(); p++ ) { 
					Point pt = rng.getPoint(p); 
					polygonGeom.add(new Point2D.Double(pt.x, pt.y));
				}
			}
		}
		return polygonGeom;
	}
	
	
	
	
	
	////////////////////////////////////////////
	////////////////////////////////////////////
	//////////////非推奨関数//////////////////////////////
	////////////////////////////////////////////
	/**
	 * PGeometry(LineStringの複数ポイント)オブジェクトをPoint2D.Doubleに変換
	 * @deprecated
	 * @param aGeom
	 * @return
	 */
	public static ArrayList<Point2D> getLineStringMultiPoint(PGgeometry aGeom){
		ArrayList<Point2D> lineStringMultiPoint = new ArrayList<>();
		if(aGeom.getGeoType() == Geometry.LINESTRING){
			LineString lineString = (LineString)aGeom.getGeometry(); 
			for(int i=0; i<lineString.numPoints(); i++){
				lineStringMultiPoint.add(new Point2D.Double(lineString.getPoint(i).x, lineString.getPoint(i).y));
			}
		}
		return lineStringMultiPoint;
	}
	/**
	 * WKT形式のpointからPoint2D.DOubleに変換する
	 * @deprecated PGeometryを使ったものを使用すること
	 * @param aPointString
	 * @return
	 */
	public static Point2D.Double parsingPoint2d(String aPointString){
		Point2D.Double point2d = new Point2D.Double();
		String regex = "" +
				"\\(" +
				"(([1-9]\\d*|0)(\\.\\d+)?)" +
				"\\s" +
				"(([1-9]\\d*|0)(\\.\\d+)?)" +
				"\\)$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(aPointString);
		
		while(matcher.find()){
			point2d = new Point2D.Double(
					java.lang.Double.parseDouble(matcher.group(1)), 
					java.lang.Double.parseDouble(matcher.group(4)));
		}
		return point2d;
	}
	/**
	 * MultiLineString((xx yy,xx yy),(xx yy,xx yy))からｘｘ、yyを取り出す
	 * WKT形式のPolygonからArrayList<Line2d>に変換する
	 * @deprecated PGeometryを使ったものを使用すること
	 * @param aStringLine
	 * @return
	 */
	public static ArrayList<Line2D> parsingMultiLine(String aStringLine){
		ArrayList<Line2D> line2ds = new ArrayList<>();
		String regex ="\\((\\d+\\.?\\d*)\\s*(\\d+\\.?\\d*)\\s*,\\s*(\\d+\\.?\\d*)\\s*(\\d+\\.?\\d*)\\)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(aStringLine);
		
		while(matcher.find()){
			line2ds.add(new Line2D.Double(
					java.lang.Double.parseDouble(matcher.group(1)),
					java.lang.Double.parseDouble(matcher.group(2)),
					java.lang.Double.parseDouble(matcher.group(3)),
					java.lang.Double.parseDouble(matcher.group(4))));
		}
		return line2ds;
	}
	/**
	 * ArrayList<Line2D>からst_MlineFromTextに変換する
	 * @deprecated PGeometryを使ったものを使用すること
	 * @param aMultiLine
	 * @return
	 */
	static public String multiLineString(ArrayList<Line2D> aMultiLine, int SRID){
		// MultiLineString((),())の作成.
		String multiLineStiring = "'MultiLineString(";
		for(int i=0; i<aMultiLine.size(); i++){
			multiLineStiring +="("+aMultiLine.get(i).getX1()+" "+aMultiLine.get(i).getY1()+"";
			multiLineStiring +=",";
			multiLineStiring +=""+aMultiLine.get(i).getX2()+" "+aMultiLine.get(i).getY2()+"),";
		}
		multiLineStiring = multiLineStiring.substring(0, multiLineStiring.length()-1);
		multiLineStiring +=")'";
		
		String lineFromText = "st_MlineFromText("+multiLineStiring+", "+SRID+")";
		return lineFromText;
	}
	/**
	 * WKT形式のPolygonから緯度経度を取り出す
	 * @deprecated PGeometryを使ったものを使用すること
	 * @see GeometryParsePostgres#pgGeometryPolygon()
	 * @param aPolygonString
	 * @return
	 */
	public static ArrayList<Point2D.Double> parsingPolygon(String aPolygonString){
		String regex ="\\d[^,)]+";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(aPolygonString);
		
		ArrayList<Point2D.Double> polygonArrayList = new ArrayList<>();
		
		while(matcher.find()){
			String[] lnglatStrings = matcher.group().split(" ");
			polygonArrayList.add(new Point2D.Double(
					java.lang.Double.parseDouble(lnglatStrings[0]),
					java.lang.Double.parseDouble(lnglatStrings[1])));
		}
		return polygonArrayList;
	}
	
}
