package parking.database;

//import parking.display.DisplayMap;
import parking.database.MongoInterface;
import parking.database.SignDB;
import parking.map.SignMarker;
import parking.map.MapBounds;
import parking.util.Logger;

import java.util.List;
import java.util.ArrayList;

public class MongoDBinterface implements DatabaseInterface {
	
	private Logger logger;
	private String db;
	//
	//  FIXME: ctor should be passed a db string. MongoInterface uses a hard coded port (27017 and localhost)
	//
	public MongoDBinterface(Integer port, Logger logger) {
		db = "test";
		this.logger = logger;
	}

	public List<SignMarker> getSignMarkers(MapBounds bounds, DBcallback callback) {
		MongoInterface mif = MongoInterface.getInstance(db, logger);
		return mif.getSignDB().getSignMarkers(bounds);
	}
}