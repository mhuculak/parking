package parking.opencv;

import org.opencv.core.Point;

public class LinePair {

	public Line line1;
	public Line line2;
	public double angle;
	public double distance;  // perp dist between lines
	public double coverage1; // what fraction of line1 is covered by line2
	public double coverage2; // what fraction of line2 is covered by line1
	
	public LinePair(Line line1, Line line2) {
		this.line1 = line1;
		this.line2 = line2;
		if (line1 != null && line2 != null) {
			angle = getAngle(line1, line2);
			distance = getDistance();
			coverage1 = getCoverage(line1, line2);	
			coverage2 = getCoverage(line2, line1);
		}
	}
	//
	//  convert angle from range (-pi, pi) into range (0, pi/2)
	//
	public double getAngleDeviation() {		
		double abs = Math.abs(angle);
		if (abs > Math.PI) {
			System.out.println("ERROR: expecting angle from "+(-Math.PI)+" to "+Math.PI);
			abs -= Math.PI;
		}
		if (abs > Math.PI/2) {
			abs = Math.PI - abs;
		}
		return Math.abs(abs);
	}

	public double getMaxDeviationFrom(LinePair pair, double refAngle) {
		double[] theta = new double[4];
		theta[0] = (new LinePair( line1, pair.line1)).getAngleDeviation();
		theta[1] = (new LinePair( line1, pair.line2)).getAngleDeviation();
		theta[2] = (new LinePair( line2, pair.line1)).getAngleDeviation();
		theta[3] = (new LinePair( line2, pair.line2)).getAngleDeviation();
		double maxDev = 0;
		for ( int i=0 ; i<4 ; i++) {
			double dev = Math.abs( refAngle - theta[i] );
			maxDev = maxDev < dev ? dev : maxDev;
		}
		return maxDev;
	}

	public static double getAngle(Line l1, Line l2) {
		Vector v1 = new Vector(l1);
		Vector v2 = new Vector(l2);
		double z = Vector.dot(v1,v2)/(v1.getLength()*v2.getLength());
		return Math.acos(z);
	}

	// get min perp distance  between the lines
	private double getDistance() {
		double[] d = new double[4];
		d[0] = Line.getDistance(line1, line2.p1);
		d[1] = Line.getDistance(line1, line2.p2);
		d[2] = Line.getDistance(line2, line1.p1);
		d[3] = Line.getDistance(line2, line1.p2);
		double min = d[0];
		for ( int i=1 ; i<4 ; i++ ) {
			if (d[i] < min) {
				min = d[i];
			}
		}
		return min;
		
	}

	private double getCoverage(Line a, Line b) {
		double beta1 = getNearest(a, b.p1);	
		double beta2 = getNearest(a, b.p2);
		boolean outside1 = false;
		boolean outside2 = false;
		if (beta1 < 0.0 || beta1 > 1.0) {
			outside1 = true;
		}
		if (beta2 < 0.0 || beta2 > 1.0) {
			outside2 = true;
		}

		if (outside1 && outside2) {
			return 0.0; // no coverage
		}
		else if (outside1) { // partial case1
			return beta1 > 0.0 ? 1.0 - beta2 : beta2;			
		}
		else if (outside2) { // partial case2
			return beta2 > 0.0 ? 1.0 - beta1 : beta1;			
		}
		else {
			return 1.0; // fully covered
		}

	}

	private double getNearest(Line a, Point p) {
		double dist = Line.getDistance(a, p);
		Vector v = new Vector(a);
		double k = dist/v.getLength();
		Vector orth = new Vector(-k*v.y, k*v.x);
		double test = Vector.dot(orth, new Vector(p, a.p2));
		Point near;
		if (test > 0) {
			near = Vector.add(p, orth);
		}
		else {
			near = Vector.sub(p, orth);
		}
		Vector pv = new Vector( a.p1, near);
		return Vector.dot(v, pv) / (v.getLength() * pv.getLength());
	}

}