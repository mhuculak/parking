package parking.map;

public class MapBounds {

	private Position northEast;
	private Position southWest;

	public MapBounds(Position northEast, Position southWest) {
		this.northEast = northEast;
		this.southWest = southWest;
	}

	public boolean inside(Position p) {
		if ( p== null || northEast == null || southWest == null) {
			return false;
		}
		if ( p.getLatitude() < northEast.getLatitude() && p.getLatitude() > southWest.getLatitude() &&
				p.getLongitude() < northEast.getLongitude() && p.getLongitude() > southWest.getLongitude() ) {
			return true;
		}
		return false;
	}

	public Position getCenter() {
		return new Position( (northEast.getLatitude()+southWest.getLatitude())/2, (northEast.getLongitude()+southWest.getLongitude())/2);
	}

	public Position getNE() {
		return northEast;
	}

	public Position getSW() {
		return southWest;
	}

	public String toString() {
		return "ne:"+northEast+" sw:"+southWest;
	}
}