package src.db.getData;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import org.postgis.Geometry;
import org.postgis.PGgeometry;

import src.db.GeometryParsePostgres;
import src.db.HandleDbTemplateSuper;
import src.DbConfig;

/**
 * ストロークテーブルを使ったデータ処理
 * @author murase
 *
 */
public class OsmStrokeDataGeom extends HandleDbTemplateSuper{
	
	
	/** ストロークID */
	public ArrayList<Integer> _strokeId = new ArrayList<>();
	
	public OsmStrokeDataGeom() {
		super(DbConfig.DBNAME_osm_road_db, DbConfig.USER, DbConfig.PASS, DbConfig.DBURL_osm_road_db, HandleDbTemplateSuper.POSTGRESJDBCDRIVER_STRING);
	}
	
	// 切り出さずにそのままのストローク.
	/** データベースからそのまま取り出したストローク(arc形式) */
	public ArrayList<ArrayList<Line2D>> _strokeArc = new ArrayList<>();
	public ArrayList<ArrayList<Point2D>> _strokeArcPoint = new ArrayList<>();
	/** ストロークのWKT形式 */
	public ArrayList<String> _strokeArcString = new ArrayList<>();
	/** ストロークの長さ */
	public ArrayList<Double> _strokeLength = new ArrayList<>();
	/** ストロークIDからインデックスを求めるハッシュ */
	public HashMap<Integer, Integer> _strokeIdToIndexHash = new HashMap<>();
	/** ストロークの道路クラス */
	public ArrayList<Integer> _strokeClazz = new ArrayList<>();
	/** ストロークのMBR(小さい方) */
	public ArrayList<Point2D> _mbrMinXy = new ArrayList<>();
	/** ストロークのMBR(大きい方) */
	public ArrayList<Point2D> _mbrMaxXy = new ArrayList<>();
	
	
	/**
	 * 範囲内のストロークを取り出す(矩形範囲)
	 * @param aUpperLeftLngLat
	 * @param aLowerRightLngLat
	 */
	public void insertStrokeData(Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat){
		_strokeId = new ArrayList<>();
		_strokeLength = new ArrayList<>();
		_strokeArc = new ArrayList<>();
		_strokeArcPoint = new ArrayList<>();
		_strokeArcString = new ArrayList<>();
		_strokeIdToIndexHash = new HashMap<>();
		_strokeClazz = new ArrayList<>();
		try{
			String statement;
			statement = "select "+
					" id, length, stroke_clazz, "+
					" flatted_arc_series, " +
					" st_asText(flatted_arc_series) as strokeString" +
					" from "+DbConfig.SCHEMA_stroke+"."+DbConfig.TBNAME_flatted_stroke_table+" " +
					" where" +
					" st_intersects(" +
						"flatted_arc_series, "+
						"st_polygonFromText(" +
							"'Polygon(("+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+
								"))'," +
							""+HandleDbTemplateSuper.WGS84_EPSG_CODE+")" +
						") order by length desc;";
//			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				_strokeId.add(rs.getInt("id"));
				_strokeLength.add(rs.getDouble("length"));
				_strokeArc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcPoint.add(GeometryParsePostgres.getLineStringMultiLine2((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcString.add(rs.getString("strokeString"));
				_strokeIdToIndexHash.put(rs.getInt("id"), _strokeId.size()-1);
				_strokeClazz.add(rs.getInt("stroke_clazz"));
			}
			rs.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 範囲内のストロークを取り出す(円範囲)
	 * @param aCenterLngLat
	 * @param aRadiusMeter
	 */
	public void insertStrokeDataInCircle(Point2D aCenterLngLat, double aRadiusMeter){
		_strokeId = new ArrayList<>();
		_strokeLength = new ArrayList<>();
		_strokeArc = new ArrayList<>();
		_strokeArcPoint = new ArrayList<>();
		_strokeArcString = new ArrayList<>();
		_strokeIdToIndexHash = new HashMap<>();
		_strokeClazz = new ArrayList<>();
		_mbrMinXy = new ArrayList<>();
		_mbrMaxXy = new ArrayList<>();
		ArrayList<Point2D> oneMbr = new ArrayList<>();
		try{
			String statement;
			statement = "select "+
					" id, length, stroke_clazz, "+
					" flatted_arc_series, " +
					" st_asText(flatted_arc_series) as strokeString, " +
					" st_envelope(flatted_arc_series) as MBR "+
					" from "+DbConfig.SCHEMA_stroke+"."+DbConfig.TBNAME_flatted_stroke_table+" " +
					" where" +
					" st_intersects(" +
						"flatted_arc_series, "+
						"st_transform(" +
							"st_buffer(" +
								"st_transform(" +
									"st_geomFromText(" +
										"'point("+aCenterLngLat.getX()+" "+aCenterLngLat.getY()+")', "+HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
									")," +
									"" +HandleDbTemplateSuper.WGS84_UTM_EPGS_CODE+
								")," +
								""+aRadiusMeter+"" +
							"),"+HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
						")"+
					") order by length desc;";
//			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				_strokeId.add(rs.getInt("id"));
				_strokeLength.add(rs.getDouble("length"));
				_strokeArc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcPoint.add(GeometryParsePostgres.getLineStringMultiLine2((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcString.add(rs.getString("strokeString"));
				_strokeIdToIndexHash.put(rs.getInt("id"), _strokeId.size()-1);
				_strokeClazz.add(rs.getInt("stroke_clazz"));
				
				oneMbr = GeometryParsePostgres.pgGeometryPolygon((PGgeometry)rs.getObject("MBR"));
				setMbr(oneMbr);
			}
			rs.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * MBRの設定
	 * @param oneMbr
	 */
	public void setMbr(ArrayList<Point2D> oneMbr){
		if(oneMbr.size() == 0){	// うまくはいらないときは手動で.
			Point2D maxXy = new Point2D.Double(), minXy = new Point2D.Double();
			for(int i=0; i<_strokeArcPoint.get(_strokeArcPoint.size()-1).size(); i++){
				if(i==0){
					maxXy = _strokeArcPoint.get(_strokeArcPoint.size()-1).get(0);
					minXy = _strokeArcPoint.get(_strokeArcPoint.size()-1).get(0);
					continue;
				}
				if(maxXy.getX() < _strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getX()){// maxXの更新.
					maxXy = new Point2D.Double(_strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getX(), maxXy.getY());
				}if(maxXy.getY() < _strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getY()){// maxYの更新.
					maxXy = new Point2D.Double(maxXy.getX(), _strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getY());
				}if(minXy.getX() > _strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getX()){// minXの更新.
					minXy = new Point2D.Double(_strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getX(), minXy.getY());
				}if(minXy.getY() > _strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getY()){// minYの更新.
					minXy = new Point2D.Double(minXy.getX(), _strokeArcPoint.get(_strokeArcPoint.size()-1).get(i).getY());
				}
			}
			_mbrMaxXy.add(maxXy);
			_mbrMinXy.add(minXy);
		}else{	// うまくいくときはoneMbrを使う.
//			System.out.println(_strokeArcString.get(_strokeArcString.size()-1));
//			System.out.println(" mbr  "+oneMbr);
			_mbrMinXy.add(oneMbr.get(0));
			_mbrMaxXy.add(oneMbr.get(2));
		}
	}
	
	// 切り出さずにそのままのストローク.
//	/** データベースからそのまま取り出したストローク(arc形式) */
//	public ArrayList<ArrayList<Line2D>> _strokeArc = new ArrayList<>();
//	/** ストロークのWKT形式 */
//	public ArrayList<String> _strokeArcString = new ArrayList<>();
//	/** ストロークの長さ */
//	public ArrayList<Double> _strokeLength = new ArrayList<>();
//	/** ストロークIDからインデックスを求めるハッシュ */
//	public HashMap<Integer, Integer> _strokeIdToIndexHash = new HashMap<>();
	// 切り出したストローク.
	/** データベースから切り出したストローク　arc形式 */
	public ArrayList<ArrayList<Line2D>> _subStrokeArc = new ArrayList<>();
	/** データベースから切り出したストローク(WKT型) */
	public ArrayList<String> _subStrokeString = new ArrayList<>();
	/** データベースから切り出したストロークの長さ */
	public ArrayList<Double> _subStrokeLength = new ArrayList<>();
	/** 切り出しgeojson形式 */
	public ArrayList<String> _cutoutGeojson = new ArrayList<>();
	/**
	 * 指定の範囲のストロークを切り出す.
	 * 切り出したストロークと切り出さないストロークの両方を求める.
	 */
	public void cutOutStroke(Point2D aUpperLeftLngLat, Point2D aLowerRightLngLat){
		_subStrokeArc = new ArrayList<>();
		_subStrokeString = new ArrayList<>();
		_subStrokeLength = new ArrayList<>();
		
		_strokeId = new ArrayList<>();
		_strokeLength = new ArrayList<>();
		_strokeArc = new ArrayList<>();
		_strokeArcString = new ArrayList<>();
		_strokeIdToIndexHash = new HashMap<>();
		try{
			String statement;
			statement = "select "+
					" id, length,"+
					" flatted_arc_series, " +
					" st_asText(flatted_arc_series) as strokeString, " +
					"st_intersection(" +
						"st_polyFromText(" +
							"'Polygon(("+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+
							"))'," + HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
						"), flatted_arc_series" +
					") as cutoutStroke, " +
					"st_asText(" +
						"st_intersection(" +
							"st_polyFromText(" +
								"'Polygon(("+
									aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
									aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
									aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
									aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
									aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+
								"))'," + HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
							"), flatted_arc_series" +
						")" +
					") as cutoutStrokeString, " +
					" st_asGeoJson(" +
						"st_intersection(" +
							"st_polyFromText(" +
								"'Polygon(("+
									aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
									aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
									aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
									aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
									aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+
								"))'," + HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
							"), flatted_arc_series" +
						")" +
					") as geojson"+
//					" st_asGeoJson(" +
//					"	st_intersection(" +
//							"st_polyFromText(" +
//								"'Polygon(("+
//									aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
//									aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
//									aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
//									aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
//									aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+
//								"))'," + HandleDbTemplateSuper.WGS84_EPSG_CODE+""+ "),  flatted_arc_series)) as geojson" +
					" from "+DbConfig.SCHEMA_stroke+"."+DbConfig.TBNAME_flatted_stroke_table+" " +
					" where" +
					" st_intersects(" +
						"st_polygonFromText(" +
							"'Polygon(("+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aUpperLeftLngLat.getY()+","+
								aLowerRightLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aLowerRightLngLat.getY()+","+
								aUpperLeftLngLat.getX()+" "+aUpperLeftLngLat.getY()+
								"))'," +
							""+HandleDbTemplateSuper.WGS84_EPSG_CODE+")," +
						"flatted_arc_series) order by length desc;";
			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				_strokeId.add(rs.getInt("id"));
				_strokeLength.add(rs.getDouble("length"));
				_strokeArc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcString.add(rs.getString("strokeString"));
				_strokeIdToIndexHash.put(rs.getInt("id"), _strokeId.size()-1);
				
				_subStrokeArc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("cutoutStroke")));
				_subStrokeString.add(rs.getString("cutOutStrokeString"));
				_cutoutGeojson.add(rs.getString("geojson"));
//				System.out.println(_strokeId.get(_strokeId.size()-1));
//				System.out.println(_subStrokeArc.get(_subStrokeArc.size()-1));
			}
			rs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		_subStrokeLength = getCutOutStrokeLength(_subStrokeString);	// 長さを求める.
	}
	
	/**
	 * 指定した半径の円にかかるストロークを取り出す.
	 * @param aUpperLeftLngLat
	 * @param aLowerRightLngLat
	 * @param radiusMeter 半径(メートル)
	 */
	public void calcStrokeOverlapedCircle(Point2D centerLngLat, double radiusMeter, boolean orderFlg){
		_strokeId = new ArrayList<>();
		_strokeLength = new ArrayList<>();
		_strokeArc = new ArrayList<>();
		_strokeArcPoint = new ArrayList<>();
		_strokeArcString = new ArrayList<>();
		_strokeIdToIndexHash = new HashMap<>();
		_strokeClazz = new ArrayList<>();
		try{
			String statement;
			statement = 
					"select "+
						" id, length, stroke_clazz, "+
						" flatted_arc_series, " +
						" st_asText(flatted_arc_series) as strokeString" +
					" from "+DbConfig.SCHEMA_stroke+"."+DbConfig.TBNAME_flatted_stroke_table+" " +
					" where " +
						" st_crosses("+
							"st_transform("+
								" st_buffer(" +
									"st_transform("+
										"st_geomFromText(" +
											"'Point("+(centerLngLat.getX())+" "+
												(centerLngLat.getY())+")'" +
											", "+HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
										")," +
										WGS84_UTM_EPGS_CODE +
									")," +
									radiusMeter+
								"), " +
							""+HandleDbTemplateSuper.WGS84_EPSG_CODE+")" +
						",flatted_arc_series)" +
						"";
			statement += orderFlg ? " order by length desc ":"";
//			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				_strokeId.add(rs.getInt("id"));
				_strokeLength.add(rs.getDouble("length"));
				_strokeArc.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcPoint.add(GeometryParsePostgres.getLineStringMultiLine2((PGgeometry)rs.getObject("flatted_arc_series")));
				_strokeArcString.add(rs.getString("strokeString"));
				_strokeIdToIndexHash.put(rs.getInt("id"), _strokeId.size()-1);
				_strokeClazz.add(rs.getInt("stroke_clazz"));
			}
			rs.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 指定したストロークが指定した半径の円と交差するか
	 */
	public boolean checkIntersectStrokeAndCircle(Point2D centerLngLat, String aStrokeString, double radiusMeter){
		try{
			String statement;
			statement = 
					"select "+
						" st_crosses("+
							"st_transform("+
								" st_buffer(" +
									"st_transform("+
										"st_geomFromText(" +
											"'Point("+(centerLngLat.getX())+" "+
												(centerLngLat.getY())+")'" +
											", "+HandleDbTemplateSuper.WGS84_EPSG_CODE+"" +
										")," +
										WGS84_UTM_EPGS_CODE +
									")," +
									radiusMeter+
								"), " +
							""+HandleDbTemplateSuper.WGS84_EPSG_CODE+")" +
							",st_geomFromText('"+aStrokeString+"', "+HandleDbTemplateSuper.WGS84_EPSG_CODE+")" +
						")";
			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				return rs.getBoolean(1);
			}
			rs.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * 2つのストロークが接するか
	 */
	public boolean checkIntersectTwoStroke(String aStrokeString1, String aStrokeString2){
		try{
			String statement;
			statement = 
					"select "+
						" st_crosses("+
							 "st_geomFromText('"+aStrokeString1+"', "+HandleDbTemplateSuper.WGS84_EPSG_CODE+")" +
							",st_geomFromText('"+aStrokeString2+"', "+HandleDbTemplateSuper.WGS84_EPSG_CODE+")" +
						")";
			System.out.println(statement);
			ResultSet rs = execute(statement);
			while(rs.next()){
				return rs.getBoolean(1);
			}
			rs.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	//////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////
	
	/**
	 * 上位ｎこのストロークを求める
	 * @param topN
	 * @return
	 */
	public ArrayList<ArrayList<Line2D>> getTopN(int topN){
		ArrayList<ArrayList<Line2D>> top10 = new ArrayList<>();
		try{
			String stmt = "select flatted_arc_series from "+DbConfig.SCHEMA_stroke+"."+DbConfig.TBNAME_flatted_stroke_table+" order by length desc limit "+topN+"";
			ResultSet rSet = execute(stmt);
			while(rSet.next()){
				top10.add(GeometryParsePostgres.getLineStringMultiLine((PGgeometry)rSet.getObject(1)));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return top10;
	}
	
	/**
	 * 切り出したストロークの長さを求める.
	 * wkt形式のジオメトリ
	 */
	public ArrayList<Double> getCutOutStrokeLength(ArrayList<String> aStrokeString){
		ArrayList<Double> strokeLength = new ArrayList<>();
		try{
			ResultSet rs = null;
			for(int i=0; i<_strokeId.size(); i++){
				String statement;
				statement = "select " +
						" st_length(st_transform(st_geomFromText('"+aStrokeString.get(i)+"',"+HandleDbTemplateSuper.WGS84_EPSG_CODE+"), "+WGS84_UTM_EPGS_CODE+")) as strokeLength" +
						";";
				rs = execute(statement);
				while(rs.next()){
					strokeLength.add(rs.getDouble("strokeLength"));
				}
			}
			System.out.println("lenght"+strokeLength);
			rs.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		return strokeLength;
	}
	
	
	/**
	 * 指定したストロークIDのデータを取得する
	 */
	public String testStrokeString(int aStrokeId){
		String wktString = "";
		try{
			String stmt = "select st_asText(flatted_arc_series) from "+DbConfig.SCHEMA_stroke+"."+DbConfig.TBNAME_flatted_stroke_table+" where id= "+aStrokeId+""+"";
			System.out.println(stmt);
			ResultSet rs = execute(stmt);
			while(rs.next()){
				wktString = rs.getString(1);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return wktString;
	}
	
}
