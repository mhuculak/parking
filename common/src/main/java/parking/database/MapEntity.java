package parking.database;

import parking.map.Country;
import parking.map.MapBorder;
import parking.map.MapBounds;
import parking.map.Position;
import parking.map.StreetSegment;
import parking.map.SimpleSign;
import parking.schedule.ParkingSchedule;

import parking.util.Logger;
import parking.util.LoggingTag;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;

import java.util.List;
import java.util.ArrayList;

public class MapEntity {

	private MongoInterface m_mongo;
	private DBCollection m_collection;
	private String m_name;
	private String m_id;
	private MapEntityType m_type;
	private MapBorder m_border;
	private Logger m_logger;

	public MapEntity(MongoInterface mongo, String id, String name, MapEntityType type) {
		m_mongo = mongo;
		m_id = id;
		m_name = name;
		m_type = type;
		m_collection = m_mongo.getDB().getCollection(m_name);
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.MapDB);
	}

	public String toString() {
		return m_name + " " + m_type.toString();
	}

	public MapBorder getBorder() {
		return m_border;
	}

	public String getName() {
		return m_name;
	}

	public MapEntityType getType() {
		return m_type;
	}

	public MapEntity addEntity(String name) {
		BasicDBObject searchQuery = new BasicDBObject("entity", name);
		DBCursor cursor = m_collection.find(searchQuery);
		MapEntityType type = MapEntityType.subType.get(m_type);
		if (cursor.count()==0) {
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("entity", name);
			document.append("type", type);
			MapEntity newEntity = new MapEntity(m_mongo, id.toString(), m_name+"_"+name, type);
			m_collection.insert(document);
			return newEntity;
		}
		else {
			m_logger.log("Entity "+name+" already exists");
		}
		return null;
	}

	public MapEntity addEntity(String name, MapBorder entityBorder) {
		if (m_border == null) {
			m_logger.error("Cannot add "+name+" to "+m_name+" because it has no border");
			return null;
		}
		if (!MapBorder.intersects(m_border, entityBorder)) {
			m_logger.error("Cannot add "+name+" to "+m_name+" because it is outside the border");
			return null;
		}
		return addEntity(name);
	}

	public void addBorder(String name, MapBorder border) {
		BasicDBObject searchQuery = new BasicDBObject("entity", name);
		DBCursor cursor = m_collection.find(searchQuery);
		if (cursor.count() == 1) {
			BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	newFields.append("border", border.toDBList());
	    	updateQuery.append( "$set", newFields);
	    	m_collection.update(searchQuery, updateQuery);
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for entity " + m_name+"_"+name);
		}
	}

	public MapEntity getEntity(String name) {
		BasicDBObject searchQuery = new BasicDBObject("entity", m_name+"_"+name);
		DBCursor cursor = m_collection.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			MapEntity entity = getEntity(m_mongo, document);
			return entity;
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for entity " + name);
		}
		return null;
	}

	//
	//  the border allows us to have ambiguous entries e.g. two towns in the same state with same name
	//
	public MapEntity getEntity(String name, MapBorder entityBorder) {
		BasicDBObject searchQuery = new BasicDBObject("entity", m_name+"_"+name);
		DBCursor cursor = m_collection.find(searchQuery);
		if (cursor.hasNext()) {
			DBObject document = cursor.next();
			MapEntity entity = getEntity(m_mongo, document);
			if (MapBorder.intersects(entity.getBorder(), entityBorder)) {
				return entity;
			}
		}
		return null;
	}

	public static MapEntity getEntity(MongoInterface mongo, DBObject document) {
		Object idobj = document.get("_id");
		Object nameObj = document.get("entity");
		Object typeObj = document.get("type");
		BasicDBList borderObj = (BasicDBList)document.get("border");
		String id = idobj == null ? null : idobj.toString();
		String name = nameObj == null ? null : nameObj.toString();
		MapEntityType type = typeObj == null ? null : MapEntityType.valueOf(typeObj.toString());
		MapEntity entity = new MapEntity(mongo, idobj.toString(), nameObj.toString(), type);
		if (borderObj != null) {
			MapBorder border = MapBorder.getBorder(borderObj);
			entity.setBorder(border);
		}
		return entity;
	}

	public List<MapEntity> getChildren() {
		DBCursor cursor = m_collection.find();
		List<MapEntity> children = new ArrayList<MapEntity>();
		int n = 0;
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	children.add(getEntity(m_mongo, document));
	    }
	    return children;
	}

	public List<MapEntity> getMatches(Position p, MapBounds bounds) {
		DBCursor cursor = m_collection.find();
		return getMatches(p, bounds, cursor);
	}

	private List<MapEntity> getMatches(Position p, MapBounds bounds, DBCursor cursor) {
		List<MapEntity> matches = new ArrayList<MapEntity>();
		int n = 0;
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	BasicDBList borderObj = (BasicDBList)document.get("border");
	    	if (borderObj != null) {
	    		MapBorder childBorder = MapBorder.getBorder(borderObj);
	    		if (MapBorder.intersects(bounds, childBorder)) {
	    			matches.add(getEntity(m_mongo, document));
	    		}
	    		else if (childBorder.contains(p)) {
	    			matches.add(getEntity(m_mongo, document));
	    		}
	    	}
	    }
	    return matches;
	}

	public List<MapEntity> getMatches(Position p, MapBounds bounds, MapEntityType type) {
		BasicDBObject searchQuery = new BasicDBObject("type", type.toString());
		DBCursor cursor = m_collection.find(searchQuery);
		return getMatches(p, bounds, cursor);
	}

	public List<StreetSegment> getSegments(Position p, MapBounds bounds) {
		List<StreetSegment> segments = new ArrayList<StreetSegment>();
		DBCursor cursor = m_collection.find();
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	BasicDBList borderObj = (BasicDBList)document.get("border");
	    	if (borderObj != null) {
	    		MapBorder segmentBorder = MapBorder.getBorder(borderObj);
	    		if (MapBorder.intersects(bounds, segmentBorder)) {
	    			segments.add(getSegment(document));
	    		}
	    		else if (segmentBorder.contains(p)) {
	    			segments.add(getSegment(document));
	    		}
	    	}
	    }
	    return segments;
	}

	public static StreetSegment getSegment(DBObject document) {
		String id = document.get("_id") == null ? null : document.get("_id").toString();
		String streetName = document.get("streetName") == null ? null : document.get("streetName").toString();
		ParkingSchedule oddSched = document.get("scheduleOdd") == null ? null : new ParkingSchedule(document.get("scheduleOdd").toString());
		ParkingSchedule evenSched = document.get("scheduleEven") == null ? null : new ParkingSchedule(document.get("scheduleEven").toString());
		StreetSegment segment = new StreetSegment(id, streetName, evenSched, oddSched);
		if (document.get("points") != null) {
			segment.setPoints(toPosList((BasicDBList)document.get("points")));
		}
		if (document.get("corners") != null) {
			segment.setCorners(toPosList((BasicDBList)document.get("corners")));
		}
		if (document.get("signs") != null) {
			segment.setSigns(toSignList((BasicDBList)document.get("signs")));
		}
		if (document.get("saved") != null && document.get("saved").toString().equals("true")) {
			segment.saved = true;
		}
		return segment;
	}

	public boolean addSegment(StreetSegment segment, MapBorder segBorder) {	
		Object id = m_mongo.getNextID();	
		BasicDBObject document = createSegmentEntry(id, segment, segBorder);
		m_collection.insert(document);
		return true;
	}

	public static BasicDBObject createSegmentEntry(Object id, StreetSegment segment, MapBorder segBorder) {
		BasicDBObject document = new BasicDBObject();
		document.append("_id", id);						
		if (segment.getStreetName() != null) {
			document.append("streetName", segment.getStreetName());
		}		
		if (segment.getScheduleOdd() != null) {
			document.append("scheduleOdd", segment.getScheduleOdd().toString());
		}
		if (segment.getScheduleEven() != null) {
			document.append("scheduleEven", segment.getScheduleEven().toString());
		}
		if (segment.getPoints() != null) {
			document.append("points", posToDBList(segment.getPoints()));
		}
		if (segment.getCorners() != null) {
			document.append("corners", posToDBList(segment.getCorners()));
		}
		if (segment.getSigns() != null) {
			document.append("signs", signsToDBList(segment.getSigns()));
		}
		if (segBorder != null) {
			document.append("border", segBorder.toDBList());
		}
		return document;
	}

	public static BasicDBList posToDBList(List<Position> posList) {
		if (posList == null) {
			return null;
		}
		BasicDBList dbList = new BasicDBList();
		for (int i=0 ; i<posList.size() ; i++) {
			BasicDBObject obj = new BasicDBObject();
			Position p = posList.get(i);
			obj.append("lat", p.getLatitude());
			obj.append("lng", p.getLongitude());
			dbList.add(obj);
		}
		return dbList;
	}

	private static BasicDBList signsToDBList(List<SimpleSign> signs) {
		if (signs == null) {
			return null;
		}
		BasicDBList dbList = new BasicDBList();
		for (int i=0 ; i<signs.size() ; i++) {
			BasicDBObject obj = new BasicDBObject();
			SimpleSign sign = signs.get(i);
			if (sign.position != null) {
				obj.append("lat", sign.position.getLatitude());
				obj.append("lng", sign.position.getLongitude());
			}
			if (sign.schedule != null) {
				obj.append("schedule", sign.schedule);
			}
			if (sign.id != null) {
				obj.append("id", sign.id);
			}
			dbList.add(obj);
		}
		return dbList;
	}

	public boolean constains(Position p) {
		return m_border.contains(p);
	}

	public void setBorder(MapBorder border) {
		m_border = border;
	}

	public static List<Position> toPosList(BasicDBList dbList) {
		List<Position> pList = new ArrayList<Position>();
		for (Object obj : dbList) {
	    	BasicDBObject tObj = (BasicDBObject)obj;
	    	Position p = new Position( (Double)tObj.get("lat"), (Double)tObj.get("lng"));
	    	pList.add(p);
	    }
	    return pList;
	}

	public static List<SimpleSign> toSignList(BasicDBList dbList) {
		List<SimpleSign> signs = new ArrayList<SimpleSign>();
		for (Object obj : dbList) {
	    	BasicDBObject tObj = (BasicDBObject)obj;
	    	Position p = null;
	    	if (tObj.get("lat") != null && tObj.get("lng") != null) {
	    		p = new Position( (Double)tObj.get("lat"), (Double)tObj.get("lng"));
	    	}
	    	SimpleSign sign = new SimpleSign( tObj.get("id").toString(), p, tObj.get("schedule").toString());
	    	signs.add(sign);
	    }
	    return signs;
	}
}