package parking.database;

import parking.map.StreetSegment;
import parking.map.Position;
import parking.map.SimpleSign;
import parking.map.MapBounds;
import parking.map.MapBorder;
import parking.schedule.ParkingSchedule;

import parking.util.Logger;
import parking.util.LoggingTag;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;
import com.mongodb.WriteResult;

import java.util.List;
import java.util.ArrayList;

public class MapEditDB {

	private MongoInterface m_mongo;
	private DBCollection m_mapEdit;
	private Logger m_logger;

	public MapEditDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_mapEdit = m_mongo.getDB().getCollection("mapEdit");
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.MapEditDB);
	}

	public void addSegmentsAsVisible(List<StreetSegment> segments, String user) {
		for (StreetSegment seg : segments) {
			addSegmentAsVisible(seg, user);
		}
	}

	public void addSegmentAsVisible(StreetSegment segment, String user) {
		Object id = m_mongo.getNextID();
		BasicDBObject document = MapEntity.createSegmentEntry(id, segment, null);
		if (user != null) {
			document.append("user", user);
		}
		document.append("visible", "true");
		m_mapEdit.insert(document);
	}

	public StreetSegment getSegment(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_mapEdit.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			StreetSegment seg = MapEntity.getSegment(document);
			return seg;
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for segment id " + id);
		}
		return null;
	}

	public List<StreetSegment> getStreetSegments(MapBounds bounds, String user) {
		m_logger.log("looking for segements");
		BasicDBObject searchQuery = new BasicDBObject("user", user);
		DBCursor cursor = m_mapEdit.find(searchQuery);
		List<StreetSegment> segments = new ArrayList<StreetSegment>();
		while (cursor.hasNext()) {
			DBObject document = cursor.next();
			if (document.get("region") == null) {
				StreetSegment segment = MapEntity.getSegment(document);
				BasicDBObject newFields = new BasicDBObject();
				if (segment.isVisible(bounds)) {
					segments.add(segment);
					newFields.append("visible", "true");							
				}
				else {
					newFields.append("visible", "false");
				}
				BasicDBObject thisQ = new BasicDBObject("_id", Integer.parseInt(segment.getID()));
				BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);
	    		m_mapEdit.update(thisQ , updateQuery);
	    	}	
		}
		if (segments.size() > 0) {
			m_logger.log("returning "+segments.size()+" segments");
			return segments;
		}
		m_logger.error("No segments found");
		return null;
	}

	public Position findWork(String user) {
		BasicDBObject searchQuery = new BasicDBObject("user", user);
		DBCursor cursor = m_mapEdit.find(searchQuery);
		while (cursor.hasNext()) {
			DBObject document = cursor.next();
			if (document.get("region") == null) {
				StreetSegment segment = MapEntity.getSegment(document);			
				if (segment.getPoints() != null && segment.getPoints().size() > 0) {
					return segment.getPoints().get(0);
				}
				if (segment.getCorners() != null && segment.getCorners().size() > 0) {
					return segment.getCorners().get(0);
				}
				if (segment.getSigns() != null && segment.getSigns().size() > 0) {
					return segment.getSigns().get(0).position;
				}
			}
		}
		return null;
	}

	public StreetSegment select(Position p, String user) {
		BasicDBObject searchQuery = new BasicDBObject("user", user);
		searchQuery.append("visible", "true");
		DBCursor cursor = m_mapEdit.find(searchQuery);
		double minDist = -1;
		StreetSegment selected = null;
		while (cursor.hasNext()) {
			DBObject document = cursor.next();
			if (document.get("region") == null) {
				StreetSegment segment = MapEntity.getSegment(document);
				Double dist = segment.getDistanceKm(p);
				if (minDist < 0 || minDist > dist) {
					minDist = dist;
					selected = segment;
				}
			}
		}
		return selected;
	}

	public DBObject getDoc(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_mapEdit.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			return document;
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for segment id " + id);
		}
		return null;
	}

	public boolean addPoint(String id, Position p, String user) {
		m_logger.log("add point "+p.toString());
		DBObject document = getDoc(id);
		if (document != null) {
			List<Position> points = null;
			if (document.get("points") != null) {
				points = MapEntity.toPosList((BasicDBList)document.get("points"));
				m_logger.log("found "+points.size()+" points in DB");
			}
			else {
				points = new ArrayList<Position>();
				m_logger.log("created new list of points");
			}
			points.add(p);
			BasicDBObject newFields = new BasicDBObject("points", MapEntity.posToDBList(points));
			BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
			BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);
	
	    	WriteResult result = m_mapEdit.update(searchQuery, updateQuery);
	    	return result.getN() == 1;
//			return true;
		}
		return false;
	}

	public boolean addCorner(String id, Position p, String user) {
		return false;
	}

	public boolean movePoint(String id,  Position orig, Position dest) {
		m_logger.log("move point "+orig.toString()+" to "+dest.toString());
		DBObject document = getDoc(id);
		if (document != null) {
			List<Position> points = null;
			if (document.get("points") != null) {
				points = MapEntity.toPosList((BasicDBList)document.get("points"));
				m_logger.log("found "+points.size()+" points in DB");
				int index = -1;
				double min = 0.0;
				for (int i=0 ; i<points.size() ; i++) {
					double dist = 1000*Position.getDistanceKm(points.get(i), orig);
					m_logger.log("distance to point "+points.get(i)+" is "+dist+" m");
					if (dist < 0.001) { // 1 mm
						if (index < 0 || dist < min) {							
							index = i;
							min = dist;
							m_logger.log("index = "+index+" min = "+min);
						}
					}
				}
				if (index > -1) {
					m_logger.log("set index = "+index+" to position "+dest);
					points.set(index, dest);
					BasicDBObject newFields = new BasicDBObject("points", MapEntity.posToDBList(points));
					BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
					BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);
	
	    			WriteResult result = m_mapEdit.update(searchQuery, updateQuery);
	    			return result.getN() == 1;
				}
				m_logger.error("failed to move point");
			}
		}
		return false;
	}

	public boolean moveCorner(String id, Position orig, Position p, String user) {
		return false;
	}

	public boolean clearSegment(String id) {
		DBObject document = getDoc(id);
		if (document != null) {
			boolean removed = false;
			BasicDBObject removeFields = new BasicDBObject();
			if (document.get("points") != null) {
				removeFields.append("points","");			
				removed = true;
			}
			if (document.get("corners") != null) {
				removeFields.append("corners","");
				removed = true;
			}
			if (removed) {
				BasicDBObject newFields = new BasicDBObject("saved", "true");
				BasicDBObject updateQuery = new BasicDBObject();
				updateQuery.append("$set", newFields);
				updateQuery.append("$unset", removeFields);
				BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));						
    			WriteResult result = m_mapEdit.update(searchQuery, updateQuery);
	    		return result.getN() == 1;
	    	}
		}
		return false;
	}

	public boolean saveSegment(String id, List<Position> points, List<Position> corners) {
		DBObject document = getDoc(id);
		if (document != null) {
			BasicDBObject newFields = new BasicDBObject();
			boolean content = false;
			if (points != null) {
				newFields.append("points", MapEntity.posToDBList(points));
				content = true;
			}
			if (corners != null) {
				newFields.append("corners", MapEntity.posToDBList(corners));
				content = true;
			}
			if (content) {
				newFields.append("saved", "true");	
				BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
				BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);	
	    		WriteResult result = m_mapEdit.update(searchQuery, updateQuery);
	    		return result.getN() == 1;
	    	}
		}
		return false;
	}

	public void removeSegment(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_mapEdit.find(searchQuery);
		if (cursor.count()==1) {
			m_mapEdit.remove(searchQuery);
		}
		else {
			m_logger.error("while trying to remove found " + cursor.count() + " entries for segment id " + id);
		}
	}

	public boolean publishSegment(String id) {
		StreetSegment segment = MapEntity.getSegment(getDoc(id));
		if (m_mongo.getMapDB().addSegment(segment)) {
			m_mongo.getSignDB().publishSigns(segment.getSigns());			
			removeSegment(id);
			return true;
		}
		m_logger.error("could not add segment to mapDB");		
		return false;
	}

	public boolean publishSavedSegments() {
		return false;
	}

	public String saveBorder(String id, String region, List<Position> points) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_mapEdit.find(searchQuery);
		if (cursor.count() == 1) {
			BasicDBObject newFields = new BasicDBObject();
			if (points != null) {
				newFields.append("points", MapEntity.posToDBList(points));
			}
			if (region != null) {
				newFields.append("region", region);
			}
			BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);	
	    	WriteResult result = m_mapEdit.update(searchQuery, updateQuery);
	    	return id;
	    }
	    else {
	    	m_logger.error("could not save border "+id+" in mapDB");
	    }
	    return null;
	}

	public String saveBorder(String region, List<Position> points, String user) {
		BasicDBObject searchQuery = new BasicDBObject("region", region);
		searchQuery.append("user", user);
		DBCursor cursor = m_mapEdit.find(searchQuery);
		if (cursor.count() == 0) {
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("points", MapEntity.posToDBList(points));
			document.append("user", user);
			document.append("region", region);
			WriteResult result = m_mapEdit.insert(document);
			return id.toString();
		}
		else if (cursor.count() == 1) {
			DBObject document = cursor.next();
			BasicDBObject newFields = new BasicDBObject("points", MapEntity.posToDBList(points));
			BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);	
	    	WriteResult result = m_mapEdit.update(searchQuery, updateQuery);
	    	return document.get("_id").toString();
		}
		else {
			m_logger.error("could not add border mapDB");
		}
		return null;
	}

	public MapBorder getBorder(String id) {
		return getBorder(getDoc(id));
	}

	private MapBorder getBorder(DBObject document) {
		String id = document.get("_id") == null ? null : document.get("_id").toString();
		String region = document.get("region") == null ? null : document.get("region").toString();		
		MapBorder border = new MapBorder(id, region);
		if (document.get("points") != null) {
			border.setBorder(MapEntity.toPosList((BasicDBList)document.get("points")));
		}		
		return border;
	}

	public List<MapBorder> getBorders(MapBounds bounds, String user) {
		m_logger.log("looking for borders");
		BasicDBObject searchQuery = new BasicDBObject("user", user);
		DBCursor cursor = m_mapEdit.find(searchQuery);
		List<MapBorder> borders = new ArrayList<MapBorder>();
		while (cursor.hasNext()) {
			DBObject document = cursor.next();
			if (document.get("region") != null) {
				MapBorder border = getBorder(document);
				BasicDBObject newFields = new BasicDBObject();
				if (border.isVisible(bounds)) {
					borders.add(border);
					newFields.append("visible", "true");							
				}
				else {
					newFields.append("visible", "false");
				}
				BasicDBObject thisQ = new BasicDBObject("_id", Integer.parseInt(border.getID()));
				BasicDBObject updateQuery = new BasicDBObject( "$set", newFields);
	    		m_mapEdit.update(thisQ , updateQuery);
	    	}	
		}	
		if (borders.size() > 0) {
			m_logger.log("returning "+borders.size()+" borders");
			return borders;
		}
		m_logger.error("No borders found");
		return null;
	}

	public MapBorder selectBorder(Position p, String user) {
		BasicDBObject searchQuery = new BasicDBObject("user", user);
		searchQuery.append("visible", "true");
		DBCursor cursor = m_mapEdit.find(searchQuery);
		double minDist = -1;
		MapBorder selected = null;
		while (cursor.hasNext()) {
			DBObject document = cursor.next();
			if (document.get("region") != null) {
				MapBorder border = getBorder(document);
				Double dist = border.getDistanceKm(p);
				if (minDist < 0 || minDist > dist) {
					minDist = dist;
					selected = border;
				}
			}
		}
		return selected;
	}

	public boolean publishBorder(String id, String region) {
		return false;
	}
}
