package parking.opencv;

import org.opencv.core.Point;

public class Vector {
	
	public double x;
	public double y;
	public double length;
	public double angle;

	public Vector(Point p1, Point p2) {
		x = p2.x - p1.x;
		y = p2.y - p1.y;
		computeProps();
	}

	public Vector(Line line) {
		x = line.p2.x - line.p1.x;
		y = line.p2.y - line.p1.y;
		computeProps();
	}

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
		computeProps();
	}

	private void computeProps() {
		length = Math.sqrt(x*x + y *y);
		angle = Math.atan2(y, x);
	}

	public double getLength() {
		return length;
	}

	public static double dot(Vector v1, Vector v2) {
		return v1.x*v2.x + v1.y*v2.y;
	}

	// compute 2-D cross product of two vectors
	public static double cross(Vector v1, Vector v2) {
		return v1.x * v2.y - v1.y * v2.x; 
	}

	public static Vector add(Vector v1, Vector v2) {
		return new Vector(v1.x + v2.x, v1.y + v2.y);
	}

	public static Point add(Point p, Vector v) {
		return new Point(v.x + p.x, v.y + p.y);
	}

	public static Point sub(Point p, Vector v) {
		return new Point( p.x - v.x, p.y - v.y);
	}

	public static Vector sub(Vector u, Vector v) {
		return new Vector( u.x - v.x, u.y - v.y);
	}

	public static Vector scale(Vector v, double s) {
		return new Vector( s*v.x, s*v.y);
	}

	public static Vector rotate(Vector v, double angle) {
		return new Vector(v.x*Math.cos(angle)-v.y*Math.sin(angle), v.x*Math.sin(angle)+v.y*Math.cos(angle));
	}

	public String toString() {
		return x + ":" + y + ">" + length;
	}
}