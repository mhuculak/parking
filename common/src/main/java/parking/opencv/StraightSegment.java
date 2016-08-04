package parking.opencv;

import org.opencv.core.Point;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class StraightSegment extends TextSegment {
	public Line startEdge;
	public Line endEdge;
	public Point endpoint1;
	public Point endpoint2;
	public Line major;
	public double angle;
	public double thickness;
	private double cost;

	public StraightSegment(Line start, Line end) {
		super(SegType.STRAIGHT);
		startEdge = start;
		endEdge = end;
	}

	public StraightSegment(Line start, Line end, double thickness, double angle) {
		super(SegType.STRAIGHT);
		startEdge = start;
		endEdge = end;
		this.angle = angle;
		this.thickness = thickness;
	}

	public StraightSegment(SegmentPoint[] points, int start, int end, boolean xdir) {
		super(SegType.STRAIGHT);
//		System.out.println("new straight seg from "+start+" "+points[start]+" "+end+" "+ points[end]);
		Line l1 = linearRegression(points, start, end, xdir, false);
		Line l2 = linearRegression(points, start, end, xdir, true);
		endpoint1 = new Point( (l1.p1.x+l2.p1.x)/2, (l1.p1.y+l2.p1.y)/2);
		endpoint2 = new Point( (l1.p2.x+l2.p2.x)/2, (l1.p2.y+l2.p2.y)/2);
		startEdge = new Line( l1.p1, l2.p1);
		endEdge = new Line( l1.p2, l2.p2 );		
		angle = (l1.angle + l2.angle)/2;
		thickness = (startEdge.length + endEdge.length)/2;
		cost = computeCost(points, start, end);
		major = new Line(endpoint1, endpoint2);
		if (thickness > major.length) { // flip the segment so that endpoints lie on major axis
			angle = (startEdge.angle + endEdge.angle)/2;
			endpoint1 = l1.findPoint(0.5);
			endpoint2 = l2.findPoint(0.5);
			major = new Line(endpoint1, endpoint2);
			startEdge = l1;
			endEdge = l2;
			thickness = (startEdge.length + endEdge.length)/2;
//			System.out.println("Flipped: start = "+startEdge+" end = "+endEdge+" thickness = "+thickness+" major = "+major);
		}
		else {
//			System.out.println("Normal: start = "+startEdge+" end = "+endEdge+" thickness = "+thickness+" major = "+major);
		}
	}

	public double getCost() {
		return cost;
	}

	public String toString() {
		return "start = "+startEdge+" end = "+endEdge+" thickness = "+thickness+" angle = "+angle+" cost = "+cost;
	}

	private double computeCost(SegmentPoint[] points, int start, int end) {
		double sum2 = 0.0;
/*		
		Point s = new Point((startEdge.p1.x + startEdge.p2.x)/2, (startEdge.p1.y + startEdge.p2.y)/2);
		Point e = new Point((endEdge.p1.x + endEdge.p2.x)/2, (endEdge.p1.y + endEdge.p2.y)/2);		
		Line l = new Line(s,e);
*/
		Line l = new Line(startEdge.p1, endEdge.p1);		
//		System.out.println("compute straight cost using line "+l);
		for ( int i=start ; i<=end ; i++) {
			double d = Line.getDistance(l, points[i].pos);
//			System.out.println("dist to l from "+ points[i].pos+ " is "+d);
			sum2 += d*d;
		}
		return Math.sqrt(sum2);
	}

	private Line linearRegression(SegmentPoint[] points, int start, int end, boolean xdir, boolean addThick) {
		int size = end-start+1;
		double[][] pts = new double[size][2];
		double min = xdir ? points[start].pos.x : points[start].pos.y;
		double max = min;
		SimpleRegression sr = new SimpleRegression(true);
		for ( int i=0 ; i<size ; i++) {
			if (xdir) {
				Point p;			
				if (addThick) {
					Vector thick = new Vector(0, points[i+start].thickness);
					p = Vector.add( points[i+start].pos, thick); 
				}
				else {
					p = points[i+start].pos;
				}
				pts[i][0] = p.x;
				pts[i][1] = p.y;
			}
			else {
				Point p;			
				if (addThick) {
					Vector thick = new Vector(points[i+start].thickness, 0);
					p = Vector.add( points[i+start].pos, thick); 
				}
				else {
					p = points[i+start].pos;
				}
				pts[i][0] = p.y;
				pts[i][1] = p.x;
			}
			min = pts[i][0] < min ? pts[i][0] : min;
			max = pts[i][0] > max ? pts[i][0] : max;
		}
		sr.addData(pts);
		double minVal = sr.predict(min);
		double maxVal = sr.predict(max);
		Point s,e;
		if (xdir) {
			s = new Point(min, minVal);
			e = new Point(max, maxVal);
		}
		else {
			s = new Point(minVal, min);
			e = new Point(maxVal, max);
		}
		return new Line(s,e);
	}

}