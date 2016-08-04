package parking.opencv;

import org.opencv.core.Point;

public class Circle {
	private Point center;
	private double radius;

	public Circle(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	public Point getCenter() {
		return center;
	}

	public double getRadius() {
		return radius;
	}

	public String toString() {
		return center.x+","+center.y+" "+radius;
	}
}