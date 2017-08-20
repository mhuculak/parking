package parking.database;

import parking.map.SignMarker;
//import parking.display.DisplayMap;

import java.util.List;

public interface DBcallback {
	public void setSignMarkers(List<SignMarker> signMarkers);
}