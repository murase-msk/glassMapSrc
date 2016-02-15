package src.db;

import java.awt.geom.Point2D;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.postgis.Geometry;
import org.postgis.PGgeometry;

/**
 * 汎用的な使い方をする
 * @author murase
 *
 */
public class GeneralPurposeGeometry extends HandleDbTemplateSuper{
	
	protected static final String DBNAME = "osm_all_db";	// Database Name
	protected static final String USER = "postgres";			// user name for DB.
	protected static final String PASS = "usadasql";		// password for DB.
	protected static final String URL = "rain2.elcom.nitech.ac.jp";
	protected static final int PORT = 5432;
	protected static final String DBURL = "jdbc:postgresql://"+URL+":"+PORT+"/" + DBNAME;
	
	
	public GeneralPurposeGeometry(){
		super(DBNAME, USER, PASS, DBURL, HandleDbTemplateSuper.POSTGRESJDBCDRIVER_STRING);
	}
	
	/**
	 * lineを切る
	 * st_difference()   st_split()を使ってもいい?
	 * @param aSplitedLine
	 * @param aBlade
	 */
	public ArrayList<ArrayList<Point2D>> splitLine(ArrayList<Point2D> aSplitedLine, ArrayList<Point2D> aBlade){
		GeometryParsePostgres.linePointString(aSplitedLine, -1);
		GeometryParsePostgres.linePointString(aBlade, -1);
		ArrayList<ArrayList<Point2D>> splitLine = new ArrayList<>();
		try{
			String stmt = "select " +
					"st_difference(" +
						GeometryParsePostgres.linePointString(aSplitedLine, -1)+
						","+
						GeometryParsePostgres.linePointString(aBlade, -1)+
					") as diff";
			System.out.println(stmt);
			ResultSet rs = execute(stmt);
			while(rs.next()){
				splitLine = GeometryParsePostgres.getMulitLimeStringDatas2((PGgeometry)rs.getObject("diff"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return splitLine;
	}
	
	
	
}
