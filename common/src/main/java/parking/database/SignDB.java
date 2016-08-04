package parking.database;

import parking.map.Position;
import parking.map.Sign;
import parking.map.SignMarker;
import parking.map.Address;
import parking.schedule.ParkingSchedule;

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

	public SignDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_signs = m_mongo.getDB().getCollection("signs");
	}
	//
	// FIXME: what if the sign is updated !!!
	//
	public SignMarker addSign(Sign sign, File file) {

		m_mongo.getPictureDB().addPicture(sign.getPictureTag(), file);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", sign.getPictureTag());		
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count() == 0) {
			Date tstamp = new Date();
			BasicDBObject document = new BasicDBObject();
			Object id = m_mongo.getNextID();
			document.append("_id", id);
			document.append("tstamp", tstamp.getTime());
			document.append("name", sign.getPictureTag());
			doAddSign(sign, document);
			m_signs.insert(document);
			return new SignMarker(id.toString(), sign.getPosition());
		}
		else {
			System.out.println("ERROR: record " + sign.getPictureTag() + " already exists");
		}
		return null;
	}

	public void updateSign(Sign sign) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", sign.getPictureTag());		
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count() == 1) {
			BasicDBObject updateQuery =  new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	doAddSign(sign, newFields);
	    	updateQuery.append( "$set", newFields);
	    	m_signs.update(searchQuery, updateQuery);
		}
	}

	private void doAddSign(Sign sign, BasicDBObject document) {			
			document.append("position", sign.getPosition().toString());
			Address address = sign.getAddress();
			if (address != null) {
				document.append("address", address.toString());				
			}
//			if (sign.getDirection() != null) {
//				document.append("d1", sign.getDirection().getP1().toString());
//				document.append("d2", sign.getDirection().getP2().toString());
//			}
			if (sign.getParkingSchedule() != null) {
				document.append("schedule", sign.getParkingSchedule().toString());
			}
			if (sign.getAutoSchedule() != null) {
				document.append("autoSchedule", sign.getAutoSchedule().toString());
			}
	}

	public List<SignMarker> getSignMarkers(int max) {
		DBCursor cursor = m_signs.find();
		List<SignMarker> signMarkers = new ArrayList<SignMarker>();
		int n = 0;
		while (cursor.hasNext()) {
	    	DBObject document = cursor.next();
	    	Position p = new Position(document.get("position").toString());
	    	Object idobj = document.get("_id");
	    	SignMarker signMarker = new SignMarker(idobj.toString(), p);
	    	signMarkers.add(signMarker);
		}
		SignMarkerComparator comparator = new SignMarkerComparator();
		Collections.sort(signMarkers, comparator);

		List<SignMarker> latest = new ArrayList<SignMarker>();
		for ( int i=0 ; i<max && i<signMarkers.size() ; i++) {
			latest.add(signMarkers.get(i));
		}
		return latest;
	}

	public Sign getSign(DBObject document) {
		Position p = new Position(document.get("position").toString());
		Address a = new Address(document.get("address").toString());
		String tag = document.get("name").toString(); 
		Sign sign = new Sign(p, tag, a);
		if (document.get("schedule") != null) {
			ParkingSchedule sched = new ParkingSchedule(document.get("schedule").toString());
			sign.setParkingSchedule(sched);
		}
		if (document.get("autoSchedule") != null) {
			ParkingSchedule autoSched = new ParkingSchedule(document.get("autoSchedule").toString());
			sign.setAutoSchedule(autoSched);
		}
		return sign;
	}

	public Sign getSign(String id) {
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Sign sign = getSign(document);
			return sign;
		}
		else {
			System.out.println("ERROR: found " + cursor.count() + " entries for sign id " + id);
		}
		return null;
	}

	public Sign getSignFromTag(String tag) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("name", tag);		
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Sign sign = getSign(document);
			return sign;
		}
		else {
			System.out.println("ERROR: found " + cursor.count() + " entries for sign tag " + tag);
		}
		return null;
	}

	public boolean moveSign(String id, Position p) {
		Address a = Sign.reverseGeocode(p);
		BasicDBObject searchQuery = new BasicDBObject("_id", Integer.parseInt(id));
		DBCursor cursor = m_signs.find(searchQuery);
		if (cursor.count()==1) {
			BasicDBObject updateQuery = new BasicDBObject();
	    	BasicDBObject newFields = new BasicDBObject();
	    	newFields.append("position", p.toString());
	    	newFields.append("address", a.toString());
	    	updateQuery.append( "$set", newFields);
	    	m_signs.update(searchQuery, updateQuery);
	    	return true;
	    }
		else {
			System.out.println("ERROR: moveSign found " + cursor.count() + " signs for id" + id);
			return false;
		}
	}

}