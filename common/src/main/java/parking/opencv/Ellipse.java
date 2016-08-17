package parking.opencv;

import org.opencv.core.Point;

import java.awt.image.BufferedImage;

import parking.display.MyGraph;

import java.util.Collections;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

enum ConvergeStatus {
	PROGRESSING,
	SUCCESS,
	FAIL
}

class PolarComparator implements Comparator<Polar> {
	@Override
	public int compare(Polar p1, Polar p2) {
		if (p1.theta > p2.theta) {
			return -1;
		}
		else if (p1.theta < p2.theta) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class Extrema {
	public Polar max;
	public Polar min;
	public Polar opp;
	public Polar minor1;
	public Polar minor2;

	public Extrema(Polar max, Polar min, Polar opp, Polar minor1, Polar minor2) {
		this.max = max;
		this.min = min;
		this.opp = opp;
		this.minor1 = minor1;
		this.minor2 = minor2;
	}

	public String toString() {
		return "max = "+max+" min = "+min+" opp = "+opp+" minor1 = "+minor1+" minor2 = "+minor2;
	}
}

class Curve {
	private List<PolarDeriv> points;
	private double sumDist;

	public Curve() {
		points = new ArrayList<PolarDeriv>();
		sumDist = 0.0;
	}

	public void add(PolarDeriv p) {
		points.add(p);
	}

	public int size() {
		return points.size();
	}

	public PolarDeriv get(int i) {
		return points.get(i);
	}

	public List<PolarDeriv> getPoints() {
		return points;
	}

	public void addDist(double dist) {
		sumDist += dist;
	}

	public double getSumDist() {
		return sumDist;
	}

	public double getAverageDist() {
		if (points.size() > 0) {	
			return sumDist/points.size();
		}
		return -1;
	}
	public Point getCentroid() {
		double sumX = 0.0;
		double sumY = 0.0;
		for ( PolarDeriv pd : points) {
			sumX += pd.val.r * Math.cos(pd.val.theta);
			sumY += pd.val.r * Math.sin(pd.val.theta);
		}
		return new Point( sumX / size(), sumY / size());
	}

	public String toString() {
		if (size() > 0) {
			return "size="+size()+" start="+points.get(0)+" end="+points.get(size()-1)+" avg dist="+getAverageDist()+" centroid = "+getCentroid();
		}
		else {
			return "empty";
		}
	}
}

class CurvePair {
	public Curve curve1;
	public Curve curve2;

	public CurvePair(Curve curve1, Curve curve2) {
		this.curve1 = curve1;
		this.curve2 = curve2;
	}

	public Curve merge(double curveDistThresh) {
		Curve mergedCurve = null;
		double d1 = PolarDeriv.distance(curve1.get(0), curve2.get(curve2.size()-1));
		if (d1 < curveDistThresh) {
//			System.out.println("Merging curves "+curve1+" and "+curve2+" at "+curve1.get(0)+" : "+curve2.get(curve2.size()-1)+" dist = "+d1);
			mergedCurve = new Curve();
			for ( PolarDeriv val : curve2.getPoints()) {
				mergedCurve.add(val);
			}
			for ( PolarDeriv val : curve1.getPoints()) {
				mergedCurve.add(val);
			}
		}
		double d2 = PolarDeriv.distance(curve2.get(0), curve1.get(curve1.size()-1));
		if (d2 < curveDistThresh) {
//			System.out.println("Merging curves "+curve2+" and "+curve1+" at "+curve2.get(0)+" : "+curve1.get(curve1.size()-1)+" dist = "+d2);
			mergedCurve = new Curve();
			for ( PolarDeriv val : curve1.getPoints()) {
				mergedCurve.add(val);
			}
			for ( PolarDeriv val : curve2.getPoints()) {
				mergedCurve.add(val);
			}
		}
		if (mergedCurve != null) {
			mergedCurve.addDist(curve1.getSumDist());
			mergedCurve.addDist(curve2.getSumDist());
		}
		return mergedCurve;
	}
}

class PolarDeriv {
	public Polar val;
	public double deriv;

	public PolarDeriv(Polar value, double deriv) {
		this.val = value;
		this.deriv = deriv;
	}

	public String toString() {
		return val+" "+deriv;
	} 

	public static double distance(PolarDeriv v1, PolarDeriv v2) {
		double dr = v1.val.r - v2.val.r;		
		double dth = angleDist(v1, v2);
		return Math.sqrt(dr*dr + dth*dth);
	}

	public static double distance2(PolarDeriv v1, PolarDeriv v2) {
		double dr = v1.val.r - v2.val.r;
		double rmax = v1.val.r > v2.val.r ? v1.val.r : v2.val.r;
		double dth = angleDist(v1, v2);
		double dd = v1.deriv - v2.deriv;
		return Math.sqrt(dr*dr + dth*dth + dd*dd);
	}

	private static double angleDist(PolarDeriv v1, PolarDeriv v2) {
		double rmax = v1.val.r > v2.val.r ? v1.val.r : v2.val.r;
		double dth = Math.abs(v1.val.theta - v2.val.theta);
		double dth2 = Math.abs(dth - 2*Math.PI); // 2 pi is really 0
		dth = dth > dth2 ? dth2 : dth;
		return dth*rmax; // convert angle to arc distance 
	}
}

public class Ellipse {
	
	private double major;
	private double minor;
	private double angle;
	private Point center;
	private Transformation transformation;
	
	private static BufferedImage graph;

	private static final double convergeThresh = 2.0;
	private static final int minPointKludge = 2; //  FIXME: this number should depend on the sign's aspect ratio
	private static final double curveDistThresh = 5.0;

	public Ellipse(double major, double minor) {
		this.major = major;
		this.minor = minor;
		center = new Point(0.0, 0.0);
	}

	public Ellipse(double major, double minor, Point center, double angle) {
		this.major = major;
		this.minor = minor;
		this.center = center;
		this.angle = angle;
	}

	public Ellipse( Point center, Extrema extrema ) {
		this.center = center;
		major = extrema.max.r;
		minor = extrema.minor1.r;
		angle = extrema.max.theta;
	}

	public Point getCenter() {
		return center;
	}

	public double getMajor() {
		return major;
	}

	public double getMinor() {
		return minor;
	}

	public double getAngle() {
		return angle;
	}

	public BufferedImage getGraph() {
		return graph;
	}

	public Transformation getTransform() {
		return transformation;
	}

	public void setTransform(Transformation transformation) {
		this.transformation = transformation;
	}

	public String toString() {
		return major+" "+minor+" "+center.x+","+center.y+" "+angle;
	}

	public static Ellipse rotate(Ellipse ellipse, double theta) {
		return new Ellipse( ellipse.getMajor(), ellipse.getMinor(), ellipse.getCenter(), ellipse.getAngle() + theta);
	}

	public static Ellipse translate(Ellipse ellipse, Vector trans) {
		Point newPos = Vector.add( ellipse.getCenter(), trans);
		return new Ellipse( ellipse.getMajor(), ellipse.getMinor(), newPos, ellipse.getAngle());
	}

	public static Ellipse getEllipse(BufferedImage image, Rectangle border, Line vertAxis) {
		Point pos = border.getCirclePos(vertAxis);
		double height = border.getCircleHeight();
		int maxRadius = border.getCircleRadiusMax();
		int minRadius = border.getCircleRadiusMin();
		final int maxIter = 10;
//		System.out.println("Search for ellipse at "+pos.x+" "+pos.y+" max radius = "+maxRadius+" min radius = "+minRadius);
		int minPoints = minPointKludge*(maxRadius+minRadius);
		Curve bestCurve = getBestCurve(image, pos, height, maxRadius, minRadius);		
		Extrema extrema = getExtrema(bestCurve);
		Vector prevDelta = new Vector(pos, new Point( pos.x+maxRadius, pos.y));
		Vector delta = getDelta(extrema, pos, vertAxis);
//		System.out.println("begin with pos = "+pos.x+" "+pos.y+" extr = "+extrema+" delta = "+delta);
		ConvergeStatus status = ConvergeStatus.PROGRESSING;
		int itr = 0;
		while ((status = getConvergeStatus(delta, prevDelta, bestCurve, minPoints)) == ConvergeStatus.PROGRESSING && itr < maxIter) {
			pos = Vector.add(pos, delta);
//			System.out.println("New center is "+pos.x+" "+pos.y);
			bestCurve = getBestCurve(image, pos, height, maxRadius, minRadius);
			extrema = getExtrema(bestCurve);
			prevDelta = delta;
			delta = getDelta(extrema, pos, vertAxis);
			itr++;	
		}
		
		if (status == ConvergeStatus.SUCCESS) {
//			System.out.println("Exit after "+itr+" iterations at "+pos+" extr = "+extrema);
			return new Ellipse( pos, extrema );
		}
		else {
//			System.out.println("Exit with no ellipse found");
			return null;
		}
	}

	private static ConvergeStatus getConvergeStatus(Vector delta, Vector prevDelta, Curve curve, int minPoints) {
		if (curve == null || curve.size() < minPoints) {
//			System.out.println("Failed to find ellipse because only "+curve.size()+" points were found min points = "+minPoints);
			return ConvergeStatus.FAIL;
		}
		if (delta.getLength() < convergeThresh) {
//			System.out.println("Converged with delta "+delta.getLength()+" < thresh "+convergeThresh);
			return ConvergeStatus.SUCCESS;
		}
		if (delta.getLength() > 2*prevDelta.getLength()) {
//			System.out.println("Stop because delta "+delta.getLength()+" > prev delta "+prevDelta.getLength());
			return ConvergeStatus.SUCCESS;
		}
		return ConvergeStatus.PROGRESSING;
	}

	// Note: if vertAxis is specified, keep the center of the ellipse on the axis
	private static Vector getDelta( Extrema extrema, Point pos, Line vertAxis) {
		if (extrema == null || extrema.max == null) {
			return null;
		}		
		Vector delta = new Vector( Math.cos(extrema.max.theta), Math.sin(extrema.max.theta));		
		delta = Vector.scale(delta, (extrema.max.r-extrema.opp.r)/2);
//		System.out.println("extrema = "+extrema+" delta = "+ delta);
		if (vertAxis != null) { 
			Point newp = Vector.add(pos, delta);
			Point proj = vertAxis.project(newp);
			return new Vector( pos, proj);
		}		
		return delta;
	}

	private static Extrema getExtrema(Curve curve) {
		if (curve == null || curve.size() == 0) {
			return null;
		}	
		Polar max = curve.get(0).val;
		Polar min = max;
		for ( PolarDeriv pd : curve.getPoints() ) {
			Polar val = pd.val;
			max = val.r > max.r ? val : max;
			min = val.r < min.r ? val : min;
		}
		double oppAng;
		double minor1;
		if (max.theta > 0) {
			oppAng = max.theta - Math.PI;
			minor1 = max.theta - Math.PI/2;
		}
		else {
			oppAng = max.theta + Math.PI;
			minor1 = max.theta + Math.PI/2;
		}
		double minor2 = minor1 > 0 ? minor1 - Math.PI : minor1 + Math.PI;
		double oppVal = -1;
		double minorVal1 = -1;
		double minorVal2 = -1;;
		double oppDist = Math.PI;
		double dist1 = Math.PI;
		double dist2 = Math.PI;
		for ( PolarDeriv pd : curve.getPoints() ) {
			if (Math.abs(pd.val.theta - minor1) < dist1) {
				dist1 = Math.abs(pd.val.theta - minor1);
				minorVal1 = pd.val.r;
			}
			if (Math.abs(pd.val.theta - minor2) < dist2) {
				dist2 = Math.abs(pd.val.theta - minor2);
				minorVal2 = pd.val.r;
			}
			if (Math.abs(pd.val.theta - oppAng) < oppDist) {
				oppDist = Math.abs(pd.val.theta - oppAng);
				oppVal = pd.val.r;
			}
		}	
		return new Extrema( max, min, new Polar( oppVal, oppAng), new Polar(minorVal1, minor1 ),  new Polar(minorVal2, minor2 ));
	}

	private static Curve getBestCurve(BufferedImage image, Point origin, double height, double maxRadius, double minRadius) {
		List<Polar> allValues = convertAnulusToPolar(image, origin, height, maxRadius, minRadius);
		PolarComparator comparator = new PolarComparator();
		Collections.sort(allValues, comparator);
		int size = allValues.size();
		double[] deriv = new double[size];
		int i;
		Curve allValueDerivs = new Curve();
		
		for ( i=0 ; i<size ; i++) {
//			int prev = i>0 ? i-1 : size-1;
			int prev = i>0 ? i-1 : i;
			int next = i==size-1 ? 0 : i+1;
			deriv[i] = allValues.get(next).theta - allValues.get(prev).theta;
			PolarDeriv pd = new PolarDeriv(allValues.get(i), deriv[i]);
			allValueDerivs.add(pd); 
		}
		double[] angle = new double[size];
		double[] rad = new double[size];
		double[] d = new double[size];
		double maxD = 0;
		
		List<Curve> curves = new ArrayList<Curve>();
		for ( i=0 ; i<size ; i++) {		
			int p = i==0 ? size-1 : i-1;
			PolarDeriv prev = allValueDerivs.get(p);	
			Curve choice = curves.size() > 0 ? choice = curves.get(0) : null;
			PolarDeriv curr = allValueDerivs.get(i);

			angle[i] = curr.val.theta;
			rad[i] = curr.val.r;
			d[i] = PolarDeriv.distance2(curr, prev);
//			d[i] = PolarDeriv.distance(curr, prev);
//			System.out.println("Got dist = "+d[i]+" for angle = "+angle[i]+" r = "+rad[i]+" deriv = "+deriv[i]);
			maxD = maxD > d[i] ? maxD : d[i];

			double minDist = -1;
			if (choice != null) {
				minDist = PolarDeriv.distance2(curr, choice.get(choice.size()-1));
//				minDist = PolarDeriv.distance(curr, choice.get(choice.size()-1));;				
//				System.out.println("dist to first curve is "+minDist);
				for (Curve c : curves) {
					double dist = PolarDeriv.distance2(curr, c.get(c.size()-1));
//					double dist = PolarDeriv.distance(curr, c.get(c.size()-1));					
//					System.out.println("dist to curve " +c+" is"+dist);
					if (dist < minDist) {
						minDist = dist;
						choice = c;						
						PolarDeriv cend = c.get(c.size()-1);
/*						
						double dth = curr.val.theta - cend.val.theta;
						System.out.println("choice is now "+choice+" curr = "+curr+" end = "+cend);
						System.out.println(" dist = "+dist+" dist is "+PolarDeriv.distance2(curr, cend));
						System.out.println(" curr angle = "+curr.val.theta+" end angle = "+cend.val.theta+" dth = "+dth+" dth2 = "+dth*dth);
*/						
					}
				}

			}
			if (minDist >=0 && minDist < curveDistThresh) {
//				System.out.println("Got min dist = "+minDist+" adding to curve "+choice);
				choice.add(curr);
				choice.addDist(minDist);
			}
			else {
//				System.out.println("Got min dist = "+minDist+" not adding to curve "+choice);
				Curve curve = new Curve();
				curves.add(curve);
				curve.add(curr);
			}

		}
//		System.out.println("Got max dist =" + maxD);
//		MyGraph grph = new MyGraph( angle, d, null, null);
//		graph = grph.getGraph();
//		System.out.println("Before merging num curves = "+curves.size());
		for (Curve c : curves) {
			Extrema ex = getExtrema(c);
//			System.out.println("Found curve "+c+" ext = "+ex);
		}
		//
		//  check if any curves should be merged due to wrap around
		//
		List<Curve> merges = new ArrayList<Curve>();
		Map<Curve, Boolean> mergeMap = new HashMap<Curve, Boolean>();
		for (Curve c1 : curves) {
			mergeMap.put(c1, false);
			for (Curve c2 : curves) {
				if (c1 != c2 && mergeMap.get(c1) == false && (mergeMap.get(c2) == null || mergeMap.get(c2) == false)) {
					CurvePair pair = new CurvePair(c1, c2);
					Curve mergedCurve = pair.merge(curveDistThresh);
					if (mergedCurve != null) {
						merges.add(mergedCurve);
						mergeMap.put(c1, true);
						mergeMap.put(c2, true);
					}
				}
			}
		}
		for ( Curve c : curves ) {
			if (mergeMap.get(c) == false) {
				merges.add(c);
			}
		}
//		System.out.println("After merging num curves = "+merges.size());
		for (Curve c : merges) {
			Extrema ex = getExtrema(c);
//			System.out.println("Found curve "+c+" ext = "+ex);
		}
		//
		//  Select the best (largest) after merging
		//
		if (merges.size() > 0) {
			Curve best = merges.get(0);
			for ( Curve c : merges) {
				if (c.size() > best.size()) {
					best = c;
				}
			}
			return best;
		}
		return null;
	}

	private static List<Polar> convertAnulusToPolar(BufferedImage image, Point origin, double height, double maxRadius, double minRadius) {
		final int WHITE = 0xffffffff;
		final int BLACK = 0xff000000;
		List<Polar> pValues = new ArrayList<Polar>();
		for ( double x=origin.x-maxRadius ; x<=origin.x+maxRadius ; x++) {
			int ix = round(x);
			for ( double y=origin.y-maxRadius-height/2 ; y<=origin.y+maxRadius+height/2 ; y++) {
				int iy = round(y);
				if ( ix>=0 && iy>=0 && ix<image.getWidth() && iy<image.getHeight()) {				
					
					int value = image.getRGB(ix,iy);
					if (value == WHITE) {
						Vector v = new Vector(origin, new Point(x,y));
						if (v.y <= height/2 && v.y>= -height/2) {
							if (v.x <= maxRadius && v.x >= minRadius) {
								pValues.add(new Polar(v));
							}
						}
						else {
							Vector t = new Vector(0, height/2);
							Vector u = v.y > height/2 ? Vector.sub(v, t) : Vector.add(v, t);
							if (u.length <= maxRadius && u.length >= minRadius) {
								pValues.add(new Polar(v));
							}
						}
					}
					else if (value != BLACK) {
						System.out.println("Unexpected value "+value);
					}
				}
			}
		}
		return pValues;
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}
