package src.db.getData;

import java.awt.geom.Point2D;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.postgis.PGgeometry;

import src.DbConfig;
import src.db.GeometryParsePostgres;
import src.db.HandleDbTemplateSuper;

/**
 * OSMのLineデータを取得する
 * @author murase
 *
 */
public class OsmLineDataGeom extends HandleDbTemplateSuper{
//	protected static final String DBNAME = "osm_all_db";	// Database Name
////	protected static final String SCHEMA = "stroke";
////	protected static final String TBNAME_POINT = "planet_osm_point";
//	protected static final String TBNAME_LINE = "planet_osm_line";
////	protected static final String TBNAME_POLYGON = "planet_osm_polygon";
////	protected static final String TBNAME_ROAD = "planet_osm_road";
//	protected static final String USER = "postgres";			// user name for DB.
//	protected static final String PASS = "usadasql";		// password for DB.
//	protected static final String URL = "rain2.elcom.nitech.ac.jp";
//	protected static final int PORT = 5432;
//	protected static final String DBURL = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME;
	
	
	// lineのデータ.
	public ArrayList<ArrayList<Point2D>> _arc;
	public ArrayList<Integer> _clazz;
	
	public OsmLineDataGeom() {
		super(DbConfig.DBNAME_osm_all_db, DbConfig.USER, DbConfig.PASS, DbConfig.DBURL_osm_all_db, HandleDbTemplateSuper.POSTGRESJDBCDRIVER_STRING);
	}
	
	/**
	 * 指定したカラム名とその値で検索する
	 * @param aColumnName
	 * @param aValue
	 */
	public void insertLineDataSpecificColumn(String aColumnName, String aValue, Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat){
		_arc = new ArrayList<>();
		_clazz = new ArrayList<>();
		try{
			String stmt = "" +
					"select " +
						"osm_id, name, st_transform(way, 4326) as way ,"+aColumnName+" " +
					"from " +
						""+DbConfig.TBNAME_planet_osm_line+" " +
					"where " +
						" st_intersects(" +
							"way, "+
							"st_transform("+
								"st_polygonFromText(" +
									"'polygon(("+
										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
										aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
										aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
										aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+
									"))',"+HandleDbTemplateSuper.WGS84_EPSG_CODE+
								"), 900913"+
							")"+
						") ";
			stmt += aValue.equals("all") ? (" and "+aColumnName+"<>''") : (" and "+aColumnName+"='"+ aValue+"'");
//			System.out.println(stmt);
			ResultSet rs = execute(stmt);
			while(rs.next()){
				_clazz.add(2);	// 地下鉄はすべて２.
				_arc.add(GeometryParsePostgres.getLineStringMultiLine2((PGgeometry)rs.getObject("way")));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * ポリゴンの施設データを取得
	 */
//	private void facilityPolygon(String aColumnName, String aValue, Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat){
//		try{
//			String stmt = "" +
//					"select " +
//						"osm_id, name, st_centroid(st_transform(way, 4326)) as way, "+aColumnName+" " +
//					"from " +
//						""+TBNAME_POLYGON+" " +
//					"where " +
//						" st_contains(" +
//							"st_transform("+
//								"st_geomFromText(" +
//									"'polygon(("+
//										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
//										aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
//										aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
//										aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
//										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+
//									"))',"+HandleDbTemplateSuper.WGS84_EPSG_CODE+
//								"), 900913"+
//							"),"+
//						"way) ";
//			stmt += aValue.equals("all") ? (" and "+aColumnName+"<>''") : (" and "+aColumnName+"='"+ aValue+"'");
//			System.out.println(stmt);
//			ResultSet rs = execute(stmt);
//			while(rs.next()){
//				_facilityId.add(rs.getLong("osm_id"));
//				_facilityName.add(rs.getString("name"));
//				_facilityType.add(rs.getString(aColumnName));
//				_facilityLocation.add(GeometryParsePostgres.pgGeometryToPoint2D((PGgeometry)rs.getObject("way")));
//			}
//			System.out.println(_facilityName);
//			System.out.println(_facilityLocation);
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//	}
	
}
