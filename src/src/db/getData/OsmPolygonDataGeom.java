package src.db.getData;

import java.awt.geom.Point2D;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.postgis.PGgeometry;


import src.db.GeometryParsePostgres;
import src.db.HandleDbTemplateSuper;
import src.DbConfig;

public class OsmPolygonDataGeom extends HandleDbTemplateSuper{
//	protected static final String DBNAME = "osm_all_db";	// Database Name
//	protected static final String SCHEMA = "public";
////	protected static final String TBNAME_POINT = "planet_osm_point";
//	protected static final String TBNAME_LINE = "planet_osm_line";
//	protected static final String TBNAME_POLYGON = "planet_osm_polygon";
////	protected static final String TBNAME_ROAD = "planet_osm_road";
//	protected static final String USER = "postgres";			// user name for DB.
//	protected static final String PASS = "usadasql";		// password for DB.
//	protected static final String URL = "rain2.elcom.nitech.ac.jp";
//	protected static final int PORT = 5432;
//	protected static final String DBURL = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME;


//	protected static final String DBNAME = "osm_morikoro_20151201";	// Database Name
//	protected static final String SCHEMA = "public";
////	protected static final String TBNAME_POINT = "planet_osm_point";
//	protected static final String TBNAME_LINE = "planet_osm_line";
//	protected static final String TBNAME_POLYGON = "planet_osm_polygon";
//	protected static final String TBNAME_ROAD = "planet_osm_road";
//	protected static final String USER = "postgres";			// user name for DB.
//	protected static final String PASS = "usadasql";		// password for DB.
//	protected static final String URL = "rain2.elcom.nitech.ac.jp";
//	protected static final int PORT = 5432;
//	protected static final String DBURL = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME;

	
	
	public ArrayList<Long> _facilityId = new ArrayList<>();
	public ArrayList<String> _facilityName = new ArrayList<>();
	public ArrayList<String> _facilityType = new ArrayList<>();
	public ArrayList<ArrayList<Point2D>> _facilityLocation = new ArrayList<>();
	
	public OsmPolygonDataGeom(){
		super(DbConfig.DBNAME_osm_morikoro_20151201, DbConfig.USER, DbConfig.PASS, DbConfig.DBURL_osm_morikoro_20151201, HandleDbTemplateSuper.POSTGRESJDBCDRIVER_STRING);
		_facilityId = new ArrayList<>();
		_facilityName = new ArrayList<>();
		_facilityType = new ArrayList<>();
		_facilityLocation = new ArrayList<>();
	}
	/**
	 * ポリゴンの施設データを取得
	 */
	public void addFacilityPolygon(String aColumnName, String aValue, Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat){
		try{
			String stmt = "" +
					"select " +
						"osm_id, name, st_transform(way, 4326) as way, \""+aColumnName+"\" " +
					"from " +
						""+DbConfig.SCHEMA_public+"."+DbConfig.TBNAME_planet_osm_polygon+" " +
					"where " +
						" st_intersects(" +
							"st_transform("+
								"st_geomFromText(" +
									"'polygon(("+
										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
										aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
										aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
										aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+
									"))',"+HandleDbTemplateSuper.WGS84_EPSG_CODE+
								"), 900913"+
							"),"+
						"way) ";
			stmt += aValue.equals("all") ? (" and \""+aColumnName+"\"<>''") : (" and \""+aColumnName+"\"='"+ aValue+"'");
//			System.out.println(stmt);
			ResultSet rs = execute(stmt);
			while(rs.next()){
				_facilityId.add(rs.getLong("osm_id"));
				_facilityName.add(rs.getString("name"));
				_facilityLocation.add(GeometryParsePostgres.pgGeometryPolygon((PGgeometry)rs.getObject("way")));
				_facilityType.add(aValue);
			}
//			System.out.println(_facilityName);
//			System.out.println(_facilityLocation);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void addFacilityLine(String aColumnName, String aValue, Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat){
		try{
			String stmt = "" +
					"select " +
						"osm_id, name, st_transform(way, 4326) as way, \""+aColumnName+"\" " +
					"from " +
						""+DbConfig.SCHEMA_public+"."+DbConfig.TBNAME_planet_osm_line+" " +
					"where " +
						" st_intersects(" +
							"st_transform("+
								"st_geomFromText(" +
									"'polygon(("+
										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
										aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
										aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
										aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
										aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+
									"))',"+HandleDbTemplateSuper.WGS84_EPSG_CODE+
								"), 900913"+
							"),"+
						"way) ";
			stmt += aValue.equals("all") ? (" and \""+aColumnName+"\"<>''") : (" and \""+aColumnName+"\"='"+ aValue+"'");
//			System.out.println(stmt);
			ResultSet rs = execute(stmt);
			while(rs.next()){
				_facilityId.add(rs.getLong("osm_id"));
				_facilityName.add(rs.getString("name"));
				_facilityLocation.add(GeometryParsePostgres.getLineStringMultiLine2((PGgeometry)rs.getObject("way")));
				_facilityType.add(aValue);
			}
//			System.out.println(_facilityName);
//			System.out.println(_facilityLocation);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
