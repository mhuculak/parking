package parking.database;

import parking.map.SignMarker;
import parking.map.MapBounds;
//import parking.display.DisplayMap;

import java.util.List;

public interface DatabaseInterface {
	
	public List<SignMarker> getSignMarkers(MapBounds bounds, DBcallback callback);
}