package parking.map;

public class MapBounds {

	private Position northEast;
	private Position southWest;

	public MapBounds(Position northEast, Position southWest) {
		this.northEast = northEast;
		this.southWest = southWest;
	}

	public boolean inside(Position p) {
		if ( p.getLatitude() < northEast.getLatitude() && p.getLatitude() > southWest.getLatitude() &&
				p.getLongitude() < northEast.getLongitude() && p.getLongitude() > southWest.getLongitude() ) {
			return true;
		}
		return false;
	}
}