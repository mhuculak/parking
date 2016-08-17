package parking.opencv;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.opencv.core.Point;
//
//  A wrapper on the line classs that can be used to form clusters
//  of colinear lines
//
public class PolarLine implements Clusterable {

	private double[] theta;
	private Line line;
	private double width;
	private double height;
	Point origin;
	double radius;

	public PolarLine(Line line, double width, double height) {
		this.line = line;
		this.width = width;
		this.height = height;
		theta = computeTheta();
	}
	//
	// creates a polar
	//
	public PolarLine(double[] theta, Line line, double width, double height) {
		this.theta = theta;
		this.width = width;
		this.height = height;
		this.line = project(line);
	}

	public double[] getPoint() { // needed to implement Clusterable
		return theta; 
	}

	public Line getLine() {
		return line;
	}

	public double[] getTheta() {
		return theta;
	}

	private void setup() {
		origin = new Point(width/2,height/2);
		radius = new Vector(width/2,height/2).length;
	}

	private double[] computeTheta() {
		setup();
		Point pmin = line.project(origin);
		Vector pm = new Vector( origin, pmin);
		double z = Math.sqrt( radius*radius - pm.length * pm.length);
		Point p1 =  extend( pmin, line.p1, line.p2, z);
		Point p2 =  extend( pmin, line.p2, line.p1, z);
		Vector v1 = new Vector( origin, p1);
		Vector v2 = new Vector( origin, p2);
		double[] theta = new double[2];
		theta[0] = v1.angle;
		theta[1] = v2.angle;
		return theta;
	}

	private Point extend( Point from, Point p1,  Point p2, double by) {
		Vector u = new Vector( p1, p2);
		if ( u.length > 0) {
			Vector v = Vector.scale( u, by/u.length );
			return Vector.add( from, v);
		}
		return null;
	}

	private Line project(Line l) {
		setup();
		Vector v1 = new Vector( radius, theta[0], true);
		Vector v2 = new Vector( radius, theta[1], true);
		Line main = new Line( Vector.add( origin, v1), Vector.add( origin, v2) );
		return main.project(l);
	}

	public String toString() {
		return theta[0]+" "+theta[1]+" "+line;
	}
}