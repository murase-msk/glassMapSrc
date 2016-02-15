package src.db.getData;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.sql.ResultSet;
import java.util.ArrayList;

import javax.smartcardio.Card;

import org.apache.naming.java.javaURLContextFactory;
import org.postgis.PGgeometry;

import servlet.DrawElasticRoad;
import src.DbConfig;
import src.db.GeometryParsePostgres;
import src.db.HandleDbTemplateSuper;
import src.coordinate.*;

/**
 * OSM道路データを扱う
 * @author murase
 *
 */
public class OsmRoadDataGeom extends HandleDbTemplateSuper {
	
	/** リンクID */
	public ArrayList<Integer> _linkId;
	/** (sourcePoint, targetPoint)の組 */
	public ArrayList<Line2D> _link;
	public ArrayList<Integer> _sourceId;
	public ArrayList<Integer> _targetId;
	/** km */
	public ArrayList<Double> _length;
	/** cost */
	public ArrayList<Double> _length2;
	/** 道路のクラス */
	public ArrayList<Integer> _clazz;
	/** 道路の形状を表す */
	public ArrayList<ArrayList<Line2D>> _arc;
	public ArrayList<ArrayList<Point2D>> _arc2;
	
	//////////////////////////
	/** リンクID */
	public ArrayList<Integer> __linkId;
	/** (sourcePoint, targetPoint)の組 */
	public ArrayList<Line2D> __link;
	public ArrayList<Integer> __sourceId;
	public ArrayList<Integer> __targetId;
	/** km */
	public ArrayList<Double> __length;
	/** cost */
	public ArrayList<Double> __length2;
	/** 道路のクラス */
	public ArrayList<Integer> __clazz;
	/** 道路の形状を表す */
	public ArrayList<ArrayList<Line2D>> __arc;
	public ArrayList<ArrayList<Point2D>> __arc2;
	
	public OsmRoadDataGeom(){
		super(DbConfig.DBNAME_osm_road_db, DbConfig.USER, DbConfig.PASS, DbConfig.DBURL_osm_road_db, HandleDbTemplateSuper.POSTGRESJDBCDRIVER_STRING);
	}
	
	/**
	 * 矩形範囲のデータを取り出す
	 */
	public void insertOsmRoadData(Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat, String aRoadType, String aConstraint){
		_linkId = new ArrayList<>();
		_link = new ArrayList<>();
		_sourceId = new ArrayList<>();
		_targetId = new ArrayList<>();
		_length = new ArrayList<>();
		_length2 = new ArrayList<>();
		_clazz = new ArrayList<>();
		_arc = new ArrayList<>();
		_arc2 = new ArrayList<>();
		String table = (aRoadType.equals("all") ? DbConfig.TBNAME_osm_japan_car_bike_foot_2po_4pgr : DbConfig.TBNAME_osm_japan_car_2po_4pgr);
		table = aRoadType.equals("rail") ? DbConfig.TBNAME_osm_japan_rail_2po_4pgr: table;
		String constraint = aConstraint.equals("")? "": " and " + aConstraint;
		try{
			String statement;
			// SRID=4326.
			statement = "select " +
					" id, osm_name,osm_source_id, osm_target_id, clazz, source, target, km, cost, x1, y1, x2, y2, geom_way" +
					" from " + table + " " +
					" where" +
					" st_intersects(" +
						"st_geomFromText(" +
							"'polygon(("+
								aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+
							"))',"+WGS84_EPSG_CODE+
						"), "+
					"geom_way) " +constraint+
					"";
//			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				_linkId.add(rs.getInt("id"));
				_sourceId.add(rs.getInt("source"));
				_targetId.add(rs.getInt("target"));
				_link.add((Line2D)new Line2D.Double(rs.getDouble("x1"), rs.getDouble("y1"), rs.getDouble("x2"), rs.getDouble("y2")));
				_length.add(rs.getDouble("km"));
				_length2.add(rs.getDouble("cost"));
				_clazz.add(rs.getInt("clazz"));
				_arc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("geom_way")));
				_arc2.add(GeometryParsePostgres.getLineStringMultiLine2((PGgeometry)rs.getObject("geom_way")));
			}
			rs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * 指定したpolygon(wkt形式のString型)内の道路データを取得する
	 */
	public void getOsmRoadFromPolygon(Point2D aCenterLngLat, double aRadius, boolean innerCheck){
		__linkId = new ArrayList<>();
		__link = new ArrayList<>();
		__sourceId = new ArrayList<>();
		__targetId = new ArrayList<>();
		__length = new ArrayList<>();
		__length2 = new ArrayList<>();
		__clazz = new ArrayList<>();
		__arc = new ArrayList<>();
		
		try{
			String statement;
			// SRID=4326.
			statement = "select " +
					" id, osm_name,osm_source_id, osm_target_id, clazz, source, target, km, cost, x1, y1, x2, y2, geom_way" +
					" from "+DbConfig.TBNAME_osm_japan_car_2po_4pgr+" " +
					" where" +
					" st_intersects(" +
						"st_transform(" +
							"ST_Buffer(" +
								"st_transform(" +
									"ST_SetSRID(ST_MakePoint("+aCenterLngLat.getX()+", "+aCenterLngLat.getY()+"),"+WGS84_EPSG_CODE+"), "+
									WGS84_UTM_EPGS_CODE+"" +
								"), "+aRadius+"" +
							"), "+WGS84_EPSG_CODE+"" +
						"), "+
						"geom_way) " +
					" and " +
					" clazz > 12" +
					"";
//			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				__linkId.add(rs.getInt("id"));
				// innercheckがtrueだったらaRadiusの外側のノードIDを-1にする.
				if(!innerCheck || LngLatMercatorUtility.calcDistanceFromLngLat(aCenterLngLat, new Point2D.Double(rs.getDouble("x1"), rs.getDouble("y1"))) < aRadius){
					__sourceId.add(rs.getInt("source"));
				}else{
					__sourceId.add(-1);
				}
				if(!innerCheck || LngLatMercatorUtility.calcDistanceFromLngLat(aCenterLngLat, new Point2D.Double(rs.getDouble("x2"), rs.getDouble("y2"))) < aRadius){
					__targetId.add(rs.getInt("target"));
				}else{
					__targetId.add(-1);
				}
				__link.add((Line2D)new Line2D.Double(rs.getDouble("x1"), rs.getDouble("y1"), rs.getDouble("x2"), rs.getDouble("y2")));
				__length.add(rs.getDouble("km"));
				__length2.add(rs.getDouble("cost"));
				__clazz.add(rs.getInt("clazz"));
				__arc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("geom_way")));
			}
			rs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
