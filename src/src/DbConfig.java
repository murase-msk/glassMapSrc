package src;

/**
 * データベースの設定
 * @author murase
 *
 */
public class DbConfig {
	
	


	
	
	public static final String USER = "postgres";			// user name for DB.
	public static final String PASS = "usadasql";		// password for DB.
	public static final String URL = "osm3.cgq49v9481vk.ap-northeast-1.rds.amazonaws.com";//"rain2.elcom.nitech.ac.jp";//osm3.cgq49v9481vk.ap-northeast-1.rds.amazonaws.com
//	public static final String URL = "osm3.cgq49v9481vk.ap-northeast-1.rds.amazonaws.com";
	public static final int PORT = 5432;
	
	
	public static final String DBNAME_osm_road_db = "osm_road_db";
	public static final String DBURL_osm_road_db = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME_osm_road_db;

	public static final String SCHEMA_stroke = "stroke_v2";
	public static final String TBNAME_flatted_stroke_table = "flatted_stroke_table";
	
	public static final String SCHEMA_public  = "public";
	public static final String TBNAME_osm_japan_car_bike_foot_2po_4pgr = "osm_japan_car_bike_foot_2po_4pgr";
	public static final String TBNAME_osm_japan_car_2po_4pgr = "osm_japan_car_2po_4pgr";
	public static final String TBNAME_osm_japan_rail_2po_4pgr = "osm_japan_rail_2po_4pgr";
	
	public static final String DBNAME_osm_all_db = "osm_all_db";
	public static final String DBURL_osm_all_db = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME_osm_all_db;
	public static final String TBNAME_planet_osm_line = "planet_osm_line";
	public static final String TBNAME_planet_osm_polygon = "planet_osm_polygon";
	
	public static final String DBNAME_osm_morikoro_20151201 = "osm_morikoro_20151201";
	public static final String DBURL_osm_morikoro_20151201 = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME_osm_morikoro_20151201;


}
