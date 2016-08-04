package parking.opencv;

import org.opencv.core.Point;

public class SegmentPoint {
	public double angle;
	public double thickness;
	public Point pos;

	public SegmentPoint(Point p, double angle, double thickness) {
		pos = p;
		this.angle = angle;
		this.thickness = thickness;
	}

	public static double distance(SegmentPoint p1, SegmentPoint p2, double len) {
		double dx = p1.pos.x - p2.pos.x;
		double dy = p1.pos.y - p2.pos.y;
		double dt = p1.thickness - p2.thickness;
		double da = len*(p1.angle - p2.angle);   // length needed to make angle differences comparable to linear differences
		return Math.sqrt( dx*dx + dy*dy + dt*dt + da*da);
	}

	public String toString() {
		return pos+" "+angle+" "+thickness;
	}
}
