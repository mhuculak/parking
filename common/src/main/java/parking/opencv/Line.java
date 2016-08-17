package parking.opencv;

import parking.display.SignImage;

import org.opencv.core.Point;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

//
// Clusterable lets us find groups of parallel lines
//
public class Line implements Clusterable {
	
	public Point p1;
	public Point p2;
	public double length;
	public double angle;
	public int cluster;
	public double score;          // from SignEdge
	public double threshold;      // from SignEdge

	public Line(Point p1, Point p2) {
		this.p1 = p1;
		this.p2 = p2;
		computeProperties();
	}

	public Line(Point p, Vector v) {
		p1 = p;
		p2 = Vector.add(p, v);
		computeProperties();
	}

//	public Line(double[][] points, double[] scores, double[] thresholds) {
	public Line(double[][] points, boolean xdir, double[] scores) { 
		SimpleRegression simpleRegression = new SimpleRegression(true);
		simpleRegression.addData(points);
		double min = points[0][0];
		double max = points[0][0];
		for ( int i = 1 ; i<points.length ; i++) {
			min = Math.min(points[i][0], min); 
			max = Math.max(points[i][0], max);
		}
		if (xdir) {	
			p1 = new Point(min, simpleRegression.predict(min));
			p2 = new Point(max, simpleRegression.predict(max));
		}
		else {
			p1 = new Point(simpleRegression.predict(min), min);
			p2 = new Point(simpleRegression.predict(max), max);
		}
		computeProperties();
		cluster = 7;
		if (scores != null && scores.length > 0) {
			double scoreSum = 0.0;
			for ( int k=0 ; k<scores.length ; k++ ) {
				scoreSum += scores[k];
			}
			score = scoreSum;
//			score = scoreSum / scores.length;
		}
/*		
		if (thresholds != null && thresholds.length > 0) {
			double thresholdSum = 0.0;
			for ( int k=0 ; k<thresholds.length ; k++ ) {
				thresholdSum += thresholds[k];
			}
			threshold = thresholdSum / thresholds.length;
		}
*/		
	}

	private void computeProperties() {
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		length = Math.sqrt(dx*dx + dy*dy);
		angle = Math.atan2(dy, dx);
	}

	
	public double[] getPoint() { // needed to implement Clusterable
		double[] arr = new double[1];
		arr[0]= angle;
		return arr; 
	}

	public double diagonal() {
		return Math.abs(Math.abs(angle) - Math.PI/4);
	}
	//
	// find a point on the line
	//
	public Point findPoint(double ratio) {
		Vector v = new Vector(p1, p2);
		return new Point(p1.x + ratio*v.x, p1.y + ratio*v.y);

	}

	public double getY(double x) {
		double dx = p2.x - p1.x;
		return p1.x + dx*Math.tan(angle);
	}

	public double getX(double y) {
		double dy = p2.y - p1.y;
		return p1.y + dy/Math.tan(angle);
	}

	// project a point onto a line
	public Point project(Point p) {
		Vector u = new Vector( this );
		Vector orth = new Vector( -u.y, u.x);
		Line orthLine = new Line( p, Vector.add(p, orth));		
		Point proj = getIntersection( this, orthLine );
		proj = proj == null ? p : proj; //  handle case where p is already on the line
		return proj;
	}

	public Line project(Line l) {
		return new Line( project(l.p1), project(l.p2) );
	}
	//
	// project a point onto a line, return scalar position of point
	// i.e 0 < ret < 1 is on the line
	//
	public double projectScalar(Point p) {
		Point proj = project(p);
		Vector z = new Vector( p1, proj );
		Vector u = new Vector(p1, p2);
		double dot = Vector.dot( z, u);
		if ( dot > 0) {
			return z.length / length;
		}
		else {
			return -z.length / length;
		}  
	}

	public Line getCoverageLine(double q1, double q2) {
		if ( q1 < 0 ) {
			if ( q2 < 0 ) {
				return null;
			}
			else if ( q2 > 1.0) {
				return this;
			}
			else {
				return new Line( p1, findPoint(q2) );
			}
		}
		else if (  q1 > 1.0 ) {
			if ( q2 > 1.0) {
				return null;
			}
			else if ( q2 < 0 ) {
				return this;
			}
			else {
				return new Line( findPoint(q2), p2);
			}
		}
		else {
			if ( q2 < 0) {
				return new Line( p1, findPoint(q1));
			}
			else if ( q2 > 1.0) {
				return new Line( findPoint(q1), p2);
			}
			else {
				return new Line( findPoint(q1), findPoint(q2) );
			}
		}
	}

	public static Line add(Line l, Vector v) {
		Point p1 = new Point(l.p1.x + v.x, l.p1.y + v.y);
		Point p2 = new Point(l.p2.x + v.x, l.p2.y + v.y);
		return new Line(p1, p2);
	}

	public static List<Line> verifyLines(List<Line> lines, SignImage image) {
		double lineVerificationThreshold = 0.5;
		double minLineLength = 50.0;
		List<Line> verifiedLines = new ArrayList<Line>();
		for (Line line: lines) {
			boolean verified = image.verifyLine(line, lineVerificationThreshold, minLineLength);
			if (verified) {
				verifiedLines.add(line);
			}
		}
		return verifiedLines;
	}
/*
	public static List<Line> mergeLines(List<Line> lines) {
		final double errorThresh = 10.0;
		List<Line> mergedLines = new ArrayList<Line>();
		Map<Line, Line> merged = new HashMap<Line, Line>();		
		for (Line line : lines) {
			List<Line> merges = new ArrayList<Line>();
			if (merged.get(line) == null) {
//				System.out.println("Finding merges for line "+line); 
				for (Line l : lines) {
					if (line != l) {
						double e = sumError2(line, l);
//						System.out.println("got error "+e+" with "+l);
						if (e < errorThresh) {
//							System.out.println("merging lines with error " + e);
							merged.put(l, line);
							merges.add(l);
						}						
					}
				}
				if (merges.size() > 0) {
//					System.out.println("Merging with "+merges.size()+" other lines");
					Line mergedLine = doMerge(line,merges);
					mergedLines.add(mergedLine);
				}
				else {
					mergedLines.add(line);
				}
			}
			else {
//				System.out.println("Already merged "+line); 
			}			
		}
		
		return mergedLines;	
	}

	public static List<Line> mergeLinesFromClusters(List<CentroidCluster<Line>> clusters) {
		List<Line> mergedLines = new ArrayList<Line>();		
		doMergeLines(clusters, mergedLines, null);
		return mergedLines;		
	}

	public static List<Map<Line, List<Line>>> mergeLinesDebug(List<CentroidCluster<Line>> clusters) {				
		List<Map<Line, List<Line>>> mergesList = new ArrayList<Map<Line, List<Line>>>();
		doMergeLines(clusters, null, mergesList);
		return mergesList;
	}

	private static void doMergeLines(List<CentroidCluster<Line>> clusters, List<Line> mergedLines, 
										List<Map<Line, List<Line>>> mergesList) {
		final double errorThresh = 10.0;
		int i = 0;		
		for (CentroidCluster<Line> cl : clusters) {
			
			Map<Line, List<Line>> merges = new HashMap<Line, List<Line>>();
			Map<Line, Line> merged = new HashMap<Line, Line>();
			for (Line line: cl.getPoints()) {
//				System.out.println("merged map size is " + merged.size());
				line.cluster = i;				
				if (merged.get(line) == null) {
					System.out.println("Check merge for line "+line);
					List<Line> mergeList = new ArrayList<Line>();
					for (Line l: cl.getPoints()) {
						if (line != l) {							
							double e = sumError2(line, l);
//							System.out.println("got error "+e+" with "+l);
							if (e < errorThresh) {
//								System.out.println("merging lines with error " + e);
								merged.put(l, line);
								mergeList.add(l);
							}
						}
					}
					merges.put(line, mergeList);
				}
				else {
//					System.out.println("already merged line "+line);
				}
				
			}
			
			if (mergesList != null) {
				mergesList.add(merges);
			}
			else if (mergedLines != null) {
				for (Map.Entry<Line, List<Line>> entry : merges.entrySet()) {
					Line mergedLine = doMerge(entry.getKey(), entry.getValue());
					mergedLines.add(mergedLine);
				}
			}
			else {
				System.out.println("ERROR: nothing to do!");
			}
			i++;
		}

	}
*/
	private static double sumError2(Line line, Line l) {		
		double d1 = getDistance(line, l.p1);
		double d2 = getDistance(line, l.p2);
		return d1*d1 + d2*d2;
	}

/*
	private static Line doMerge(Line line, List<Line> mergeList) {
		
		if (mergeList != null && mergeList.size() > 0) {
//			System.out.println( mergeList.size() + " merges for line " + line.toString());
			int numPoints = 2*(mergeList.size()+1);
			double[][] points = new double[numPoints][2];		
			double[] scores = new double[numPoints];
			double[] thresholds = new double[numPoints];
			addPoints(line, points, 0);
			scores[0] = line.score;
			thresholds[0] = line.threshold;
			int i = 2;
			int k=0;
			for (Line l : mergeList) {
//				System.out.println("   Merging " + l.toString() + " distance = " + getDistance(line, l.p1) + " " + getDistance(line, l.p2));
				addPoints(l, points, i);				
				scores[++k] = l.score; 
				thresholds[k] = l.threshold;
				i = i + 2;
			}				
			return new Line(points, scores, thresholds);
		}
//		System.out.println("no merges for line " + line.toString());
		return line; // no merges so line does not change
	}
*/
	private static void addPoints(Line line, double[][] points, int index) {
		points[index][0] = line.p1.x;
		points[index][1] = line.p1.y;
		points[index+1][0] = line.p2.x;
		points[index+1][1] = line.p2.y;
	}

	// perp distance from point to a line
	public static double getDistance(Line line, Point p) {
		Vector v = new Vector(line.p1, line.p2);
		Vector u = new Vector(line.p1, p);
		Vector w = new Vector(line.p2, p);
		if (u.getLength() == 0.0 || w.getLength() == 0.0) {
			return 0.0;
		}
		double cosTheta = Vector.dot(u, v) / ( u.getLength()*v.getLength());
		double theta = Math.acos(cosTheta);
		return Math.abs(u.getLength()*Math.sin(theta));
	}

	public static double getArea(Line line, Point p) {
		double d = getDistance(line, p);
		return line.length * d / 2;
	}

	//
	// find intersection of lines of infinite length i.e. extend beyond their endpoints
	//
	public static Point getIntersection(Line line1, Line line2) {
		if (line1.angle == line2.angle) {			
			return null;
		}
		double x1 = line1.p1.x;
		double y1 = line1.p1.y;
		double x2 = line1.p2.x;
		double y2 = line1.p2.y;
		double x3 = line2.p1.x;
		double y3 = line2.p1.y;
		double x4 = line2.p2.x;
		double y4 = line2.p2.y;
		double x = ((x1*y2-y1*x2)*(x3-x4) - (x1-x2)*(x3*y4 - y3*x4)) / ((x1-x2)*(y3-y4) - (y1-y2)*(x3-x4));
		double y = ((x1*y2-y1*x2)*(y3-y4) - (y1-y2)*(x3*y4 - y3*x4)) / ((x1-x2)*(y3-y4) - (y1-y2)*(x3-x4));
		return new Point(x,y);
	}

	public static Point getIntersection2(Line line1, Line line2) {
		Point inter = getIntersection(line1, line2);
		if (inter == null) {
			return null;
		}
		double z1 = line1.projectScalar(inter);
		double z2 = line2.projectScalar(inter);
		if (0 <= z1 && z1 <= 1.0 && 0 <= z2 && z2 <= 1.0) {
			return inter;
		}
		return null;
	}
	//
	// find intersection of line segments of finite length FIXME: does not work
	//
	public static Point getIntersection3(Line line1, Line line2) {
		Vector t = new Vector(line1);
//		System.out.println(" t = " + t);
		Vector e = new Vector(line2);
		double txe = Vector.cross(t,e);
		Vector sv = new Vector(line1.p1, line2.p1);
		double svxt = Vector.cross(sv,t);		
		if (txe == 0.0 && svxt == 0.0) {
			System.out.println("lines are parallel...");
			double tt = Vector.dot(t,t);
			double z0 = Vector.dot(sv, t) / tt;
			Vector w = Vector.add(sv,t);
			double z1 = Vector.dot(w,t) / tt;
			if (z0 > 1.0 || z1 < 0.0) {
				return null;
			}
			else {
				return new Point(line1.p1.x + z0*t.x, line1.p1.y + z0*t.x);								
			}
		}
		else if (txe == 0.0) {
			return null;
		}	
		else {
//			System.out.println("compute normal intersection...");
			double svxe = Vector.cross(sv,e);
			double z = svxe / txe;
			double u = svxt / txe;
//			System.out.println("got params z = " + z + " u = " + u);
			if (z>=0.0 && z<=1.0 && u>=0.0 && u<=1.0) { 
//				return new Position(s.x + z*t.x, s.y + z*t.x);	
				return new Point(line1.p1.x + z*t.x, line1.p1.y + z*t.y);				
			}
			else { // intersection is not on at least one of the lines
				return new Point(line1.p1.x + z*t.x, line1.p1.y + z*t.y);
			}
		}
//		return null;
	}

	public static Line mergeLines( Line l1, Line l2) {
		double q1 = l2.projectScalar(l1.p1);
		double q2 = l2.projectScalar(l1.p2);
		if (q1 < 0) {
			if ( q2 < 0) {
				return null;
			}
			else if ( q2 > 1.0 ) {
				return l1;
			}
			else {
				return new Line( l1.p1, l2.p2);
			}
			
		}
		else if ( q1 > 1.0) {
			if ( q2 < 0) {
				return l1;
			}
			else if ( q2 > 1.0 ) {
				return null;
			}
			else {
				return new Line( l1.p1, l2.p1);
			}
		}
		else {
			if ( q2 < 0) {
				return new Line( l1.p2, l2.p2);
			}
			else if ( q2 > 1.0 ) {
				return new Line( l1.p2, l2.p1);
			}
			else {
				return l2;
			}
		}
	}

	public static Line rotate(Line l, Point center, double angle) {
		Vector v1 = new Vector( center, l.p1);
		Vector v2 = new Vector( center, l.p2);
		Point p1 = Vector.add( center, Vector.rotate(v1, angle));
		Point p2 = Vector.add( center, Vector.rotate(v2, angle));
		return new Line(p1, p2);
	}

	public static Line translate(Line l, Vector t) {
		Point p1 = Vector.add(l.p1, t);
		Point p2 = Vector.add(l.p2, t);
		return new Line(p1, p2);
	}

	public static Line scale(Line l, double factor) {
		return new Line ( new Point(factor*l.p1.x, factor*l.p1.y), new Point(factor*l.p2.x, factor*l.p2.y) );
	}

	public String toString() {
		return p1.toString() + ":" + p2.toString() + " len = " + length + " dir = " + angle;
	}
}