package parking.database;

import parking.util.Logger;
import parking.util.LoggingTag;

import java.io.IOException;
import java.net.UnknownHostException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.BasicDBList;

public class MongoInterface {
	private static Map<String, MongoInterface> m_instance;
	private MongoClient m_client;
    private DB m_db;
    private DBCollection m_counter = null;

    private UserDB m_userDB;
    private SignDB m_signDB;
    private PictureDB m_pictureDB;
    private TrajectoryDB m_trajectoryDB;
    private MapDB m_mapDB;
    private MapEditDB m_mapEditDB;

    private Logger logger;

    private static final String defaultDB = "parking";
    private static final String demoDB = "demo";
    private static final String testDB = "test";

    private MongoInterface(String db, Logger parentLogger) { // private to share connections via getIntance()
    	if (parentLogger == null) {
    		logger = new Logger(LoggingTag.Database, "MongoInterface", "ctor");
    	}
    	else {
    		logger = new Logger(parentLogger, this, LoggingTag.Database);
    	}
    	logger.log("opening mongo db " + db);
		try {
	    	m_client = new MongoClient("localhost", 27017);
	    	m_db = m_client.getDB(db);
	    	m_counter = m_db.getCollection("counters");	    	
	    	m_userDB = new UserDB(this);
	    	m_signDB = new SignDB(this);
	    	m_pictureDB = new PictureDB(this);
	    	m_trajectoryDB = new TrajectoryDB(this);
            m_mapEditDB = new MapEditDB(this);
            m_mapDB = new MapDB(this);
	    }
		catch (MongoException | UnknownHostException e) {
	     	e.printStackTrace();
        }   
    }

    public static MongoInterface getInstance(Logger parentLogger) {
		return  getInstance(null, parentLogger);
    }
    
    public static MongoInterface getInstance(int port, Logger parentLogger) {
		String db;
		if (port == 8080) {
	    	db = defaultDB;
		}
		else if (port == 8081) {
	    	db = demoDB;
		}
		else {
			db = testDB;
		}
		return  getInstance(db, parentLogger);
    }
    
    public static MongoInterface getInstance(String db, int port, Logger parentLogger) {
		if (db == null) {
	    	if (port == 8080) {
				db = defaultDB;  // default database
	    	}
	    	else if (port == 8081) {
	    		db = demoDB;
			}
	    	else {
				db = testDB; 
	    	}
		}
		return getInstance(db, parentLogger);
    }
    
    public static MongoInterface getInstance(String db,  Logger parentLogger) {
		if (db == null) {
	    	db = defaultDB;  // default database
		}	
		if (m_instance == null) {
	    	m_instance = new HashMap<String, MongoInterface>();
		}	
		if (m_instance.get(db) == null) {
	    	parentLogger.log("create Mongo DB connection to db = " + db);
	    	MongoInterface db_if = new MongoInterface(db, parentLogger);
	    	m_instance.put(db, db_if);
		}
		return m_instance.get(db);
    }

    public Object getNextID() {
		if (m_counter.count() == 0) {
	    	BasicDBObject document = new BasicDBObject();
	    	document.append("_id", "entries");
            document.append("seq", 0);
            m_counter.insert(document);
		}
        BasicDBObject searchQuery = new BasicDBObject("_id", "entries");
        BasicDBObject increase = new BasicDBObject("seq", 1);
        BasicDBObject updateQuery = new BasicDBObject("$inc", increase);
        DBObject result = m_counter.findAndModify(searchQuery, null, null,
            false, updateQuery, true, false);

        return result.get("seq");
    }

    public DB getDB() {
    	return m_db;
    }
    
    public UserDB getUserDB() {
    	return m_userDB;
    }

    public SignDB getSignDB() {
    	return m_signDB;
    }

    public PictureDB getPictureDB() {
    	return m_pictureDB;
    }

    public TrajectoryDB getTrajectoryDB() {
    	return m_trajectoryDB;
    }

    public MapDB getMapDB() {
        return m_mapDB;
    }

    public MapEditDB getMapEditDB() {
        return m_mapEditDB;
    }

    public Logger getLogger() {
    	return logger;
    }
}