package parking.database;

import parking.map.Position;
import parking.map.MapBounds;
import parking.map.Sign;
import parking.map.SimpleSign;
import parking.map.SignMarker;
import parking.map.Address;
import parking.map.StreetSegment;
import parking.schedule.ParkingSchedule;
import parking.security.User;

import parking.util.Logger;
import parking.util.LoggingTag;
import parking.util.Utils;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import java.util.Collections;
import java.util.Comparator;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

class SignMarkerComparator implements Comparator<SignMarker> {
	@Override
	public int compare(SignMarker m1, SignMarker m2) {
		if (m1.getIDasInt() > m2.getIDasInt()) {
			return -1;
		}
		else if (m1.getIDasInt() < m2.getIDasInt()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

public class SignDB {

	private MongoInterface m_mongo;
	private DBCollection m_signs;
	private DBCollection m_publishedSigns;
	private Logger m_logger;

	public SignDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_signs = m_mongo.getDB().getCollection("signs");                     // raw data
		m_publishedSigns = m_mongo.getDB().getCollection("publishedSigns");   // moved here when schedules are added to MapDB
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.SignDB);
	}
	//
	// create new sign from pricture file
	// intended for use by image upload servlet
	//
	public Sign addSign(Sign sign, File file, String user) {

		Date tstamp = new Date();
		BasicDBObject document = new BasicDBObject();
		Object id = m_mongo.getNextID();
		document.append("_id", id);
		String imageName = m_mongo.getPictureDB().addPicture(id.toString(), file);
		document.append("im", imageName);
		document.append("t", tstamp.getTime());
		document.append("u", user);
		document.append("c", 0);
		doAddSign(sign, document);
		m_signs.insert(document);
		
		return getSign(document);

	}

	//
	//  create new Sign using picture already in the database, picture renamed to SignID.jpg
	//  intended for use by DB cleanup tool
	//
	public Sign addSign(Sign sign) {
		Date tstamp = new Date();
		BasicDBObject document = new BasicDBObject();
		Object id = m_mongo.getNextID();
		document.append("_id", id);
		String imageName = PictureDB.createPictureName(id.toString(), sign.getImageName());
		if (m_mongo.getPictureDB().renamePicture(sign.getImageName(), imageName)) {
			document.append("im", imageName);
			document.append("t", tstamp.getTime());
			document.append("c", 0);
			doAddSign(sign, document);
			m_signs.insert(document);		
			return getSign(document);
		}
		return null;
	}

	public void updateSign(Sign sign, String user) {
		String id = sign.getID();
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count() == 1) {
			BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	int updates = Math.max(sign.getUpdates(), getUpdates(cursor.next()));
	    	newFields.append("c", ++updates);
	    	doAddSign(sign, newFields);
	    	if (user != null) {
	    		newFields.append("u", user);
	    	}
	    	updateQuery.append( "$set", newFields);
	    	m_signs.update(searchQuery, updateQuery);
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for sign id " + id);
		}
	}

	private void doAddSign(Sign sign, BasicDBObject document) {
		addPosition(sign.getPosition(), document);
		if (sign.getParkingSchedule() != null) {
			document.append("s", sign.getParkingSchedule().toString());
		}
		if (sign.getAutoSchedule() != null) {
			document.append("auto", sign.getAutoSchedule().toString());
		}
	}

	private void addPosition(Position p, BasicDBObject document) {
		if (p != null) {
			document.append("lat", p.getLatitude());
			document.append("lng", p.getLongitude());
		}
	}

	public int getSize() {
		DBCursor cursor = m_signs.find();
		return cursor.size();
	}

	public List<SignMarker> getSignMarkers(int max) {
		List<SignMarker> signMarkers = getAllSignMarkers();
		SignMarkerComparator comparator = new SignMarkerComparator();
		Collections.sort(signMarkers, comparator);

		List<SignMarker> latest = new ArrayList<SignMarker>();
		for ( int i=0 ; i<max && i<signMarkers.size() ; i++) {
			latest.add(signMarkers.get(i));
		}
		return latest;
	}

	public List<SignMarker> getSignMarkers(MapBounds bounds) {
		List<SignMarker> signMarkers = getAllSignMarkers();
		List<SignMarker> inBounds = new ArrayList<SignMarker>();
		for ( SignMarker marker : signMarkers) {
			if (bounds.inside(marker.getPosition())) {
				inBounds.add(marker);
			}
			else {
			}
		}
		return inBounds;
	}

	private List<SignMarker> getAllSignMarkers() {
		DBCursor cursor = m_signs.find();
		List<SignMarker> signMarkers = new ArrayList<SignMarker>();
		int n = 0;
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	Position p = getPosition(document);
	    	if ( p != null) {
	    		Object idobj = document.get("_id");
	    		SignMarker signMarker = new SignMarker(idobj.toString(), p);
	    		signMarkers.add(signMarker);
	    	}
		}
		return signMarkers;
	}

	private Position getPosition(DBObject document) {
		Object latObj = document.get("lat");
		Object lngObj = document.get("lng");
		if (latObj != null && lngObj != null) {
			return new Position( (Double)latObj, (Double)lngObj );
		}
		return null;
	}

	private Position getPositionOLD(DBObject document) {
		Object pos = document.get("position");
		
		if (pos != null) {
			return new Position( pos.toString() );
		}
		return null;
	}			

	public SignMarker getSignMarker(String id) {
		Sign sign = getSign(id);
		return new SignMarker(id, sign.getPosition());
	}

	public Sign getSign(DBObject document) {
		Position p = getPosition(document);
		
		Object idobj = document.get("_id");
		Object imageObj = document.get("im");
		String imageName = imageObj == null ? null : imageObj.toString();
		Object tobj = document.get("t");
		Date tstamp = tobj == null ? null : new Date((Long)tobj);
	
//		Sign sign = new Sign(idobj.toString(), p, a, m_logger);
		Sign sign = new Sign(idobj.toString(), p, imageName, m_logger);
		sign.setTimeStamp(tstamp);

		if (document.get("s") != null) {
//			ParkingSchedule sched = new ParkingSchedule(document.get("s").toString(), m_logger);
			ParkingSchedule sched = new ParkingSchedule(document.get("s").toString());
			sign.setParkingSchedule(sched);
		}
		if (document.get("auto") != null) {
//			ParkingSchedule autoSched = new ParkingSchedule(document.get("auto").toString(), m_logger);
			ParkingSchedule autoSched = new ParkingSchedule(document.get("auto").toString());
			sign.setAutoSchedule(autoSched);
		}
		
		sign.setUpdates(getUpdates(document));
		return sign;
	}

	public Sign getSignOLD(DBObject document) {
		Position p = getPositionOLD(document);
		
		Object idobj = document.get("_id");
		Object imageObj = document.get("name");
		String imageName = imageObj == null ? null : imageObj.toString();

		Object tobj = document.get("tstamp");
		Date tstamp = tobj == null ? null : new Date((Long)tobj);
	
//		Sign sign = new Sign(idobj.toString(), p, a, m_logger);
		Sign sign = new Sign(idobj.toString(), p, imageName, m_logger);
		sign.setTimeStamp(tstamp);

		if (document.get("schedule") != null) {
//			ParkingSchedule sched = new ParkingSchedule(document.get("schedule").toString(), m_logger);
			ParkingSchedule sched = new ParkingSchedule(document.get("schedule").toString());
			sign.setParkingSchedule(sched);
		}
		if (document.get("auto") != null) {
//			ParkingSchedule autoSched = new ParkingSchedule(document.get("autoSchedule").toString(), m_logger);
			ParkingSchedule autoSched = new ParkingSchedule(document.get("autoSchedule").toString());
			sign.setAutoSchedule(autoSched);
		}
		
		sign.setUpdates(getUpdates(document));
		return sign;
	}

	private int getUpdates(DBObject document) {
		if (document.get("c") != null) {
			Object updateObj = document.get("c");
			return (Integer)updateObj;			
		}
		return 0;
	}

	public Sign getSign(String id) {
		return getSign(id, m_signs);
	}

	private Sign getSign(String id, DBCollection collection) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = collection.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Sign sign = getSign(document);
			return sign;
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for sign id " + id);
		}
		return null;
	}

	public Sign getSignOLD(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Sign sign = getSignOLD(document);
			return sign;
		}
		else {
			m_logger.error("found " + cursor.count() + " entries for sign id " + id);
		}
		return null;	
	}

	public String getUser(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Object obj = document.get("u");
			String user = null;
			if (obj != null) {
				user = obj.toString();
			}
			if (user == null) {
				obj = document.get("user");
				if (obj != null) {
					user = obj.toString();
				}
			}
			return user;
		}
		return null;
	}

	public List<String> getSignsIDs() {
		DBCursor cursor = m_signs.find();
		List<String> signIDs = new ArrayList<String>();
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	Object idobj = document.get("_id");	    		
	    	signIDs.add(idobj.toString());	    	
		}
		return signIDs;
	}

	public boolean moveSign(String id, Position p, String user) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			BasicDBObject updateQuery = new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	addPosition(p, newFields);
	    	newFields.append("u", user);
	    	updateQuery.append( "$set", newFields);
	    	m_signs.update(searchQuery, updateQuery);
	    	return true;
	    }
		else {
			m_logger.error("moveSign found " + cursor.count() + " signs for id" + id);
			return false;
		}
	}

	public boolean removeSign(Sign sign) {
		String id = sign.getID();
		if (id == null){
			m_logger.error("Cannot remove sign with null id");
			return false;
		}
		int intID = Utils.parseInt(id);
		if (intID > 0) {
			BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
			DBCursor cursor = m_signs.find(searchQuery);
			if (cursor.count() == 1) {
				if (sign.getImageName() != null) {
					m_mongo.getPictureDB().removePicture(sign.getImageName());
				}
				m_signs.remove(searchQuery);
				m_logger.log("removed sign with id "+ id);
				return true;
			}		
			m_logger.error("Cannot remove sign "+id+" because number found = "+cursor.count());
		}
		else {
			m_logger.error("Cannot remove sign with invalid id "+id);
		}
		return false;
	}

	public List<StreetSegment> getStreetSegments(MapBounds bounds) {
		DBCursor cursor = m_signs.find();
		List<Sign> signs = new ArrayList<Sign>();
		int n = 0;
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	Position p = getPosition(document);
	    	if (p!= null && bounds.inside(p)) {
	    		Sign sign = getSign(document);
	    		signs.add(sign);
	    	}
	    }
	    if (signs.size() > 0) {
	    	return Sign.findStreetSegments(signs);
	    }
		return null;
	}

	public Position findWork() {
		DBCursor cursor = m_signs.find();
		List<Sign> signs = new ArrayList<Sign>();
		while (cursor.hasNext()) {
			DBObject document = cursor.next();
	    	Position p = getPosition(document);
	    	if (p != null) {
	    		return p;
	    	}
		}
		return null;
	}

	private void publishSign(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			m_signs.remove(searchQuery);
			m_publishedSigns.insert(document);
		}
	}

	public void publishSigns(List<SimpleSign> signs) {
		for (SimpleSign sign : signs) {
			publishSign(sign.id);
		}
	}

	public Sign getPublishedSign(String id) {
		return getSign(id, m_publishedSigns);
	}

}