package parking.map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import java.util.List;
import java.util.ArrayList;

public class MapBorder {
	
	public List<Position> points;
	public Position center;
	public double radiusKm;
	public String region;
	public String id;

	public MapBorder() {

	}

	public MapBorder(String id, String region) {
		this.region = region;
		this.id = id;
	}

	public MapBorder(List<Position> points) {
		this.points = points;
		computeProperties();
	}

	public void setBorder(List<Position> points) {
		this.points = points;
		computeProperties();
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public void add(Position p) {
		if (points == null) {
			points = new ArrayList<Position>();
		}
		points.add(p);
		computeProperties();
	}

	public void addPoints(List<Position> pts) {
		if (pts != null && pts.size() > 0) {
			if (points == null) {
				points = new ArrayList<Position>();
			}
			points.addAll(pts);
			computeProperties();
		}
	}

	private void computeProperties() {
		double sumLat = 0.0;
		double sumLng = 0.0;
		int n = points.size();
		if ( n == 0 ) {
			return;
		}
		for (Position p : points) {
			sumLat += p.getLatitude();
			sumLng += p.getLongitude();
		}
		center = new Position( sumLat/n , sumLng/n );
		radiusKm = 0.0;
		for (Position p : points) {
			double dist = Position.getDistanceKm(p, center);
			if (dist > radiusKm) {
				radiusKm = dist;
			}
		}
	}

	public List<Position> getBorder() {
		return points;
	}

	public Position getCenter() {
		return center;
	}

	public String getRegion() {
		return region;
	}

	public String getID() {
		return id;
	}

	public boolean contains(Position p) {
		double dist = Position.getDistanceKm(p, center);
		return dist <= radiusKm;
	}

	public BasicDBList toDBList() {
		if (points == null) {
			return null;
		}
		BasicDBList borderList = new BasicDBList();
		for (int i=0 ; i<points.size() ; i++) {
			BasicDBObject obj = new BasicDBObject();
			Position p = points.get(i);
			obj.append("lat", p.getLatitude());
			obj.append("lng", p.getLongitude());
			borderList.add(obj);
		}
		return borderList;
	}

	public boolean isVisible(MapBounds bounds) {
		if (points != null) {
			for (Position p : points) {
				if (bounds.inside(p)) {
					return true;
				}
			}
		}
		return false;
	}
	public double getDistanceKm(Position p) {
		if (center != null) {
			return Position.getDistanceKm(p, center);
		}
		return -1; // should not happen
	}

	public static boolean intersects(MapBorder b1, MapBorder b2) {
		for (Position p : b1.getBorder()) {
			if (b2.contains(p)) {
				return true;
			}
		}
		for (Position p : b2.getBorder()) {
			if (b1.contains(p)) {
				return true;
			}
		}
		return false;
	}

	public static boolean intersects(MapBounds bounds, MapBorder border) {
		for (Position p : border.getBorder()) {
			if (bounds.inside(p)) {
				return true;
			}
		}		
		return false;
	}

	public static MapBorder getBorder(BasicDBList borderList) {
		MapBorder border = new MapBorder();
		for (Object obj : borderList) {
	    	BasicDBObject tObj = (BasicDBObject)obj;
	    	Position p = new Position( (Double)tObj.get("lat"), (Double)tObj.get("lng"));
	    	border.add(p);
	    }
	    return border;
	}
}