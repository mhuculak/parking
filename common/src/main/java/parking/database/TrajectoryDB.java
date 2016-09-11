package parking.database;

import parking.map.Trajectory;
import parking.map.Position;
import parking.util.Logger;
import parking.util.LoggingTag;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;

public class TrajectoryDB {

	private MongoInterface m_mongo;
	private DBCollection m_trajectory;
	private Logger m_logger;

	public TrajectoryDB(MongoInterface mongo) {
		m_mongo = mongo;
		m_trajectory = m_mongo.getDB().getCollection("trajectory");
		m_logger = new Logger(m_mongo.getLogger(), this, LoggingTag.TrajectoryDB);
	}

	public void addTrajectory(String signID, Trajectory trajectory, double startTime, double endTime) {
		BasicDBObject document = new BasicDBObject();
		Object id = m_mongo.getNextID();
		document.append("_id", id);
		document.append("signID", signID);
		BasicDBList trajList = new BasicDBList();
		for (int i=0 ; i<trajectory.getPositions().size() ; i++) {
			BasicDBObject obj = new BasicDBObject();
			Position p = trajectory.getPositions().get(i);
			obj.append("lat", p.getLatitude());
			obj.append("lng", p.getLongitude());
			obj.append("acc", p.getAccuracy());
			m_logger.log("adding time entry "+i+" = "+trajectory.getTimeList().get(i));
			obj.append("t", trajectory.getTimeList().get(i));
			trajList.add(obj);
		}
		document.append("trajectory", trajList);
		document.append("start", startTime);
		document.append("end", endTime);
		m_trajectory.insert(document);
		m_logger.log("Added trajectory for sign "+signID+" with "+trajectory.getPositions().size()+" items start = "+startTime+" end = "+endTime);
	}

	public Trajectory getTrajectory(String signID) {
		BasicDBObject searchQuery = new BasicDBObject("signID", signID);
		DBCursor cursor = m_trajectory.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Trajectory trajectory = new Trajectory();
			BasicDBList  trajList = (BasicDBList)document.get("trajectory");
	    	for (Object obj : trajList) {
	    		BasicDBObject tObj = (BasicDBObject)obj;
	    		Position p = new Position( (Double)tObj.get("lat"), (Double)tObj.get("lng"), (Double)tObj.get("acc"));
	    		double t = (Double)tObj.get("t");
	    		m_logger.log("found time entry "+t);;
	    		trajectory.add(p, t);
	    	}
	    	m_logger.log("found trajectory for sign "+signID+" with "+trajectory.getPositions().size()+" items");
	    	return trajectory;
		}
		m_logger.log("no trajectory found  for sign "+signID);
		return null;
	}

	public double getStart(String signID) {
		BasicDBObject searchQuery = new BasicDBObject("signID", signID);
		DBCursor cursor = m_trajectory.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Object obj = document.get("start");
			if (obj != null) {
				Double start =  (Double)obj;
				m_logger.log("Found start = "+start+" for "+signID);
				return start;
			}
		}
		return 0.0;
	}

	public double getEnd(String signID) {
		BasicDBObject searchQuery = new BasicDBObject("signID", signID);
		DBCursor cursor = m_trajectory.find(searchQuery);
		if (cursor.count()==1) {
			DBObject document = cursor.next();
			Object obj = document.get("end");
			if (obj != null) {
				Double end =  (Double)obj;
				m_logger.log("Found end = "+end+" for "+signID);
				return end;
			}
		}
		return 0.0;
	}
}