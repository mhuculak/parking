package parking.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class MapOverlaySegment implements MapOverlay {

	private boolean working;
	private Color color;
	private List<MapOverlayPoint> points = new ArrayList<MapOverlayPoint>();
	private final Color streetOverlayColor = Color.BLUE;
	private final Color streetOverlayWorkingColor = Color.RED;

	public MapOverlaySegment(boolean working) {
		this.working = working;
		if (working) {
			color = streetOverlayWorkingColor;
		}
		else {
			color = streetOverlayColor;
		}
	}

	public void setWorking(boolean working) {
		this.working = working;
	}

	public void addPoint(DisplayPosition p) {
		MapOverlayPoint op = new MapOverlayPoint(p, color);
		if (points.size() > 1) {
			double headDist = DisplayPosition.distance(op, points.get(0));
			double tailDist = DisplayPosition.distance(op, points.get(points.size()-1));
			if (headDist > tailDist) {
				points.add(op);
			}
			else {
				points.add(0, op );
			}			
		}
		else {
			points.add(op);
		}
	}
	//
	//  Insert the new point at the location than minimizes the total length of the resulting segment
	//
	public void insertPoint( DisplayPosition p ) {
		MapOverlayPoint newPoint = new MapOverlayPoint(p, color);
		List<MapOverlayPoint> best = null;
		Double minCost = null;
		for ( int i=1 ; i<points.size() ; i++ ) {
			List<MapOverlayPoint> candidate = getCandidate(newPoint, i);
			double cost = getCost(candidate);
			if (minCost == null || minCost > cost) {
				minCost = cost;
				best = candidate;
			}
		}
		points = best;
	}

	private List<MapOverlayPoint> getCandidate(MapOverlayPoint newPoint, int index) {
		List<MapOverlayPoint> candidate = new ArrayList<MapOverlayPoint>();
		for (MapOverlayPoint pt : points) {
			candidate.add(pt);
		}
		candidate.add(index, newPoint);
		return candidate;
	}

	private double getCost(List<MapOverlayPoint> candidate) {	
		double cost = 0.0;
		for (int i=1 ; i<points.size() ; i++) {
			cost += DisplayPosition.distance( candidate.get(i), candidate.get(i-1));
		}
		return cost;
	}

	public boolean deletePoint( MapOverlayPoint dp ) {
		Iterator<MapOverlayPoint> iter = points.iterator();
		while(iter.hasNext()){
    		if(iter.next().equals(dp)) {
        		iter.remove();
        		return true;
        	}
		}
		return false;
	}

	public List<MapOverlayPoint> getPoints() {
		return points;
	}

	public boolean isCloseTo(DisplayPosition dp, double r) {
		return false;
	}

	public void pan(DisplayPosition delta) {
		List<MapOverlayPoint> updated = new ArrayList<MapOverlayPoint>();
		for ( int i=0 ; i<points.size() ; i++) {
//			points.set(i, DisplayPosition.delta( points.get(i), delta));
//			System.out.println("Old point: "+points.get(i)+" new point: "+DisplayPosition.delta( points.get(i), delta));
			updated.add(new MapOverlayPoint(DisplayPosition.delta( points.get(i), delta), color));
		}
		points = updated;
	}

	public void movePoint(DisplayPosition target) {

	}

	public void zoom(Dimension size, double scale) {		
		List<MapOverlayPoint> updated = new ArrayList<MapOverlayPoint>();
		for ( int i=0 ; i<points.size() ; i++) {
			double x = size.width/2.0 + scale * ( points.get(i).x - size.width/2.0);
			double y = size.height/2.0 + scale * ( points.get(i).y - size.height/2.0);
//			System.out.println("Old point: "+points.get(i)+" new point: "+x+" "+y);
			updated.add(new MapOverlayPoint( x, y, color));
		}
		points = updated;
	}

	public void select(boolean selected) {

	}

	public void draw(Graphics2D g2) {
		g2.setColor(color);
//		System.out.println("Draw overlay segment with "+points.size()+" points color = "+color);
		for ( int i=0 ; i<points.size() ; i++) {
			MapOverlayPoint ov = points.get(i);			
			if (i<points.size()-1) {
				DisplayPosition curr = ov;
				DisplayPosition next = points.get(i+1);
				g2.drawLine((int)(curr.x+0.5), (int)(curr.y+0.5), (int)(next.x+0.5), (int)(next.y+0.5));
			};
			ov.draw(g2);
		}
	}
}