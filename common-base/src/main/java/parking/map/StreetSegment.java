package parking.map;

import parking.schedule.ParkingSchedule;

import java.util.List;
import java.util.ArrayList;

public class StreetSegment {
	
	public String id;
	private List<Position> points;       // endpoint of segment or point where direction changes
	private List<SimpleSign> signs;        // position of signs in the segment
	private List<Position> corners;      // position of corners in the segment
	public String name;
	private ParkingSchedule scheduleOdd;
	private ParkingSchedule scheduleEven;
	public boolean saved;
	public boolean published;

	public StreetSegment(String streetName) {
		this.name = streetName;
		saved = false;
		published = false;
	}

	public StreetSegment(StreetSegment seg) {
		this.id = seg.id;
		this.name = seg.name;
		this.saved = seg.saved;
		this.published = seg.published;
		if (seg.points != null) {
			for (Position p : seg.points) {
				addPoint(p);
			}
		}
		if (seg.signs != null) {
			for (SimpleSign s : seg.signs) {
				addSign(s);
			}
		}
		if (seg.corners != null) {
			for (Position p : seg.corners) {
				addCorner(p);
			}
		}
//
//      TODO:
//		
//		if (scheduleOdd != null) {
//			this.scheduleOdd = new ParkingSchedule(scheduleOdd);
//		}
//		if (scheduleEven != null) {
//			this.scheduleEven = new ParkingSchedule(scheduleEven);
//		}
		
		this.scheduleOdd = seg.scheduleOdd;
		this.scheduleEven = seg.scheduleEven;

	}

	public StreetSegment(String id, String streetName, ParkingSchedule even, ParkingSchedule odd, boolean published) {
		this.id = id;
		this.name = streetName;
		saved = false;
		this.published = published;
	}

	public void addPoint(Position p) {
		if (points == null) {
			points = new ArrayList<Position>();
		}
		points.add(p);
	}

	public void addSign(String id, Position p, ParkingSchedule sched) {
		if (signs == null) {
			signs = new ArrayList<SimpleSign>();
		}
		SimpleSign simple = new SimpleSign(id, p, sched.toString());
		signs.add(simple);
	}

	private void addSign(SimpleSign sign) {
		if (signs == null) {
			signs = new ArrayList<SimpleSign>();
		}
		signs.add(sign);
	}

	public void addCorner(Position p) {
		if (corners == null) {
			corners = new ArrayList<Position>();
		}
		corners.add(p);
	}

	public void setEven(ParkingSchedule even) {
		scheduleEven = even;
	}

	public void setOdd(ParkingSchedule odd) {
		scheduleOdd = odd;
	}

	public List<Position> getPoints() {
		return points;
	}

	public List<SimpleSign> getSigns() {
		return signs;
	}

	public List<Position> getCorners() {
		return corners;
	}

	public String getID() {
		return id;
	}

	public String getStreetName() {
		return name;
	}

	public ParkingSchedule getScheduleEven() {
		return scheduleEven;
	}

	public ParkingSchedule getScheduleOdd() {
		return scheduleOdd;
	}

	public boolean isPublished() {
		return published;
	}

	//
	//  FIXME: need to find distance to line formed by
	//         pairs of points in points
	//
	public double getDistanceKm(Position pos) {
		double minDist = -1;
		if (points != null) {
			for ( Position p : points) {
				minDist = getMin(pos, p, minDist);
			}
		}
		if (corners != null) {
			for ( Position p : corners) {
				minDist = getMin(pos, p, minDist);
			}
		}
		if (signs != null ) {
			for ( SimpleSign s : signs) {
				minDist = getMin(pos, s.position, minDist);
			}
		}
		return minDist;
	}

	private double getMin(Position pos, Position p, double minDist) {
		double dist = Position.getDistanceKm(pos, p);
		if (minDist < 0 || minDist > dist) {
			return dist;
		}
		return minDist;
	}

	//
	// Could use bounding box to optimize
	// 
	public boolean isVisible(MapBounds bounds) {
		if (points != null) {
			for ( Position p : points) {
				if (bounds.inside(p)) {
					return true;
				}
			}
		}
		if (corners != null) {
			for ( Position p : corners) {
				if (bounds.inside(p)) {
					return true;
				}
			}
		}
		if (signs != null ) {
			for ( SimpleSign s : signs) {
				if (bounds.inside(s.position)) {
					return true;
				}
			}
		}
		return false;
	}

	public void setPoints(List<Position> points) {
		this.points = points;
	}

	public void setCorners(List<Position> corners) {
		this.corners = corners;
	}

	public void setSigns(List<SimpleSign> signs) {
		this.signs = signs;
	}

}