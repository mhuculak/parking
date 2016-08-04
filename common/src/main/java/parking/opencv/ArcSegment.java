package parking.opencv;

import org.opencv.core.Point;

public class ArcSegment extends TextSegment {	
	private double startAngle;
	private double endAngle;
	private Point center;
	private double outerRadius;
	private double innerRadius;
	private double cost;

	public ArcSegment(Point center, double outerRadius, double innerRadius, double startAngle, double endAngle) {
		super(SegType.CURVE);
		this.startAngle = startAngle;
		this.endAngle = endAngle;
	}

	public ArcSegment(SegmentPoint[] points, int start, int end, boolean xdir) {
		super(SegType.CURVE);
		double x = 0;
		double y = 0;
		int size = end - start + 1;
		for ( int i=start ; i<=end ; i++) {
			x += points[i].pos.x;
			y += points[i].pos.y;
		}
		center = new Point(x/size, y/size);
//		System.out.println("Centroid is "+center);
		Polar[] inner = new Polar[size];
		Polar[] outer = new Polar[size];
		innerRadius = 0;
		outerRadius = 0;		
		startAngle = -1;
		endAngle = 0;
		for ( int i=0 ; i<size ; i++) {
			Vector v = new Vector(points[i+start].pos, center);
			Vector t = xdir ? new Vector(0,points[i+start].thickness) : new Vector(points[i+start].thickness, 0);
			inner[i] = new Polar(v);
			outer[i] = new Polar(Vector.add(v,t));
//			System.out.println("at "+i+" "+points[i+start]+" v ="+v+" t = "+t+" inner = "+inner[i]+" outer = "+outer[i]);
			innerRadius += inner[i].r;
			outerRadius += outer[i].r;
			if (startAngle == -1 || startAngle > inner[i].theta) {
				startAngle = inner[i].theta;
			} 
			endAngle = endAngle < inner[i].theta ? inner[i].theta : endAngle;
		}
		innerRadius = innerRadius/size;
		outerRadius = outerRadius/size;	
		double sum2 = 0.0;
		for ( int i=0 ; i<size ; i++) {
			double dr = inner[i].r - innerRadius;
			sum2 += dr*dr;
		}
		cost = Math.sqrt(sum2);
	}

	public double getCost() {
		return cost;
	}

	public String toString() {
		return center+" inner = "+innerRadius+" outer = "+outerRadius+" th0 = "+startAngle+" th1 = "+endAngle+" cost = "+cost;
	}
}