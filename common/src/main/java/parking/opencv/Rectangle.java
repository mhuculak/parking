package parking.opencv;

import parking.util.Logger;
import parking.util.LoggingTag;

import org.opencv.core.Point;
import org.apache.commons.math3.ml.clustering.CentroidCluster;

import java.util.Collections;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class PairStats {
		public double max;
		public double min;
		public double sum;
		public int count;
		public int added;
		public int numClusters;
		public int[][] clusterPairs;
		public int[][] addedPairs;

		public PairStats() {
			max = 0.0;
			min = -1.0;
			sum = 0.0;
			count = 0;
			added = 0;
		}

		public PairStats(int numClusters) {
			max = 0.0;
			min = -1.0;
			sum = 0.0;
			count = 0;
			added = 0;
			this.numClusters = numClusters;
			clusterPairs = new int[numClusters][numClusters];
			addedPairs = new int[numClusters][numClusters];
		}

		public void add(PairStats ps, int cluster1, int cluster2) {
			max = max < ps.max ? ps.max : max;
			if (min < 0.0 || min > ps.min) {
				min = ps.min;
			}
			sum += ps.sum;
			count += ps.count;
			added += ps.added;
			if (clusterPairs != null) {
				clusterPairs[cluster1][cluster2] += ps.count;
				addedPairs[cluster1][cluster2] += ps.added;
			}
		}
}

class RectangleComparator implements Comparator<Rectangle> {
	@Override
	public int compare(Rectangle r1, Rectangle r2) {
		if (r1.getScore() > r2.getScore()) {
			return -1;
		}
		else if (r1.getScore() < r2.getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

public class Rectangle {
	
	
	private Line[] side;           // array of sides 
	private Line[] origLines;      // lines used to create 
	private Line top;              // top side
	private Line bottom;           // bottom side
	private Line left;             // left side
	private Line right;            // right side
	private Line vertAxis;
	private Line horAxis;
	private Point[] corner;        // vertices
	private double[] angle;        // angles at each corner
	private double topRight;       // angle at top right corner
	private double topLeft;        // angle at top left corner
	private double bottomRight;    // angle at bottom right corner
	private double bottomLeft;     // angle at bottom left corner
	private double perimeter;
	private double area;
	private Point centroid;
	private double coverage;
	private double minCoverage;
	private double maxAngle;
	private double aspectRatio; // horizontal/vertical
	private double score;
	private double[] scoreVector; // components used to compute score
	private double threshold;
	private Point boundDim;
	private Point boundPos;
	private double width;
	private double height;
	private String scoreDetails;
	private Transformation transformation;
	private double order;

	//
	//     TUNE ME
	//

	//
	// FIXME: are all signs the same aspect ratio? 
	// Also, should use ellipse major/minor ratio to determine sign aspect ratio
	// 
	private static final double idealAspectRatio = 0.67; 

	private static final double circleSearchHeight = 0.2;
	private static final double circleMaxRadiusWidthRatio = 2.5;
	private static final double circleMinRadiusWidthRatio = 5.0;

	private static final int numLevels = 3;
	private final static int[] minRectangles = {10, 1, 1};
	private final static double[] minimumSeparationFactor = { 0.25, 0.2, 0.1};
//	private final static double[] minimumSeparationFactor = { 0.1, 0.1, 0.1};
//	private final static double[] minPerimeterCoverage = { 0.5, 0.4, 0.3 };  // % of a rectangle perimeter that must be visible
	private final static double[] minPerimeterCoverage = { 0.2, 0.2, 0.2 };  // % of a rectangle perimeter that must be visible
//	private final static double[] minSideCoverage = { 0.2, 0.1, 0.05 };       // % of each side that must be visible		
	private final static double[] minSideCoverage = { 0.05, 0.05, 0.05 };       // % of each side that must be visible		
//	private final static double[] maxAspectRatioError = { 0.2, 0.3, 0.4 };
	private final static double[] maxAspectRatioError = { 0.4, 0.4, 0.4 };
//	private final static double[] maxDeviation = { 0.4, 0.4, 0.4 };
	private final static double[] maxDeviation = { 0.2, 0.3, 0.4 };	
	private final static double[] miniumDistance = { 50, 20 , 10 }; // used to prune rectangle list of similar elements

	public Rectangle(Point[] corner, Rectangle old) {
		this.corner = corner;
		getProperties();
		if (old != null) { // used when inheriting properties from a decendant
			this.scoreVector = old.getScoreVector();
			this.score = old.getScore();
			this.threshold = old.getThreshold();
			this.scoreDetails = old.getScoreDetails();
			this.coverage = old.getCoverage();
			this.origLines = old.getOrigLines();
		}
	}
	//
	// FIXME: when creating a new rectangle from the intersection of four lines, we are simply
	//        assigning the line scores to the side scores of the newly constructed sides with
	//        out considering the new geometry. For example a line with a high score may not
	//        actually share any of the new border and thus should not pass the score on.
	//
	public Rectangle(Line l1, Line l2, Line l3, Line l4) { // FIXME: assumes lines are in order
		createRectangle(l1, l2, l3, l4);
	}

	public Rectangle( LinePair pair1, LinePair pair2 ) {
		createRectangle(pair1.line1, pair2.line1, pair1.line2, pair2.line2);
	}

	public Rectangle( List<Point> cornerList) { // not in order
		corner = new Point[4];
		for ( int i=0 ; i<4 ; i++ ) {
			corner[i] = cornerList.get(i);
		}
		reorderCorners();
		getProperties();
	}

	private void createRectangle(Line l1, Line l2, Line l3, Line l4) {
		corner = new Point[4];
		corner[0] = Line.getIntersection(l1,l2);
		corner[1] = Line.getIntersection(l2,l3);
		corner[2] = Line.getIntersection(l3,l4);
		corner[3] = Line.getIntersection(l4,l1);
		origLines = new Line[4];
		origLines[0] = l2;
		origLines[1] = l3;
		origLines[2] = l4;
		origLines[3] = l1;	

		getProperties();
	}

	private void getProperties() {
		int i;
		side = new Line[4];		
		angle = new double[4];
		minCoverage = -1.0;
		double coveredPerimeter = 0.0;
		order = getOrder(corner);
		for ( i=0 ; i<4 ; i++) {
			int next = i==3 ? 0 : i+1;
			side[i] = new Line(corner[i], corner[next]);			
			if (origLines != null) {

				side[i].score = origLines[i].score;

				// FIXME: this assumes orig line projects completely within the side
//				side[i].score = side[i].project(origLines[i]).length / side[i].length;

				side[i].threshold = origLines[i].threshold;
				double cov = origLines[i].length / side[i].length;
				if (minCoverage == -1.0 || cov < minCoverage) {
					minCoverage = cov;
				}
				coveredPerimeter += origLines[i].length;
			}
		}
		perimeter = 0.0;
		maxAngle = 0.0;
		for ( i=0 ; i<4 ; i++) {
			int next = i==3 ? 0 : i+1;
			angle[i] = LinePair.getAngle(side[i], side[next]);
			maxAngle = maxAngle < angle[i] ? angle[i] : maxAngle;
			perimeter += side[i].length;
		}
		if (coveredPerimeter > 0.0) {
			coverage = coveredPerimeter / perimeter;
		}
		else {
			coverage = 0.0;
		}
		
		if (Math.abs(Math.sin(side[0].angle)) < 1.0 / Math.sqrt(2.0) ) {
			width = (side[0].length + side[2].length)/2.0;
			height =  (side[1].length + side[3].length)/2.0;
			if (side[0].p1.y < side[2].p1.y) {
				top = side[0];
				bottom = side[2];
				if (order < 0) {
					left = side[1];
					right = side[3];
					topRight = angle[0];
					topLeft = angle[1];
					bottomLeft = angle[2];
					bottomRight = angle[3];
				}
				else {
					left = side[3];
					right = side[1];
					topLeft = angle[0];
					topRight = angle[1];
					bottomRight = angle[2];
					bottomLeft = angle[3];					
				}
			}
			else {
				top = side[2];
				bottom = side[0];
				if (order < 0) {
					right = side[1];
					left = side[3];
					bottomLeft = angle[0];
					bottomRight = angle[1];
					topRight = angle[2];
					topLeft = angle[3];					
				}
				else {
					right = side[3];
					left = side[1];
					bottomRight = angle[0];
					bottomLeft = angle[1];
					topLeft = angle[2];
					topRight = angle[3];					
				}
			}			
			if ( right.p1.x < left.p1.x) {
				System.out.println("ERROR: incorrect labeling for "+this.toString()+" order = "+order+" left = "+left+" right = "+right);
			}
		}
		else {
			width =  (side[1].length + side[3].length)/2.0;
			height = (side[0].length + side[2].length)/2.0;
			if (side[0].p1.x < side[2].p1.x) {
				left = side[0];
				right = side[2];
				if (order < 0) {
					bottom = side[1];
					top = side[3];
					topRight = angle[3];
					topLeft = angle[0];
					bottomLeft = angle[1];
					bottomRight = angle[2];
				}
				else {
					bottom = side[3];
					top = side[1];
					topRight = angle[2];
					topLeft = angle[1];
					bottomLeft = angle[0];
					bottomRight = angle[3];
				}
			}
			else {
				left = side[2];
				right = side[0];
				if (order < 0) {
					top = side[1];
					bottom = side[3];
					topRight = angle[1];
					topLeft = angle[2];
					bottomLeft = angle[3];
					bottomRight = angle[0];
				}
				else {
					top = side[3];
					bottom = side[1];
					topRight = angle[0];
					topLeft = angle[3];
					bottomLeft = angle[2];
					bottomRight = angle[1];
				}
			}
			if ( top.p1.y > bottom.p1.y) {
				System.out.println("ERROR: incorrect labeling for "+this.toString()+" order = "+order+" top = "+top+" bottom = "+bottom);
			}		
		}
		aspectRatio = width/height;
		score = computeScore();
		threshold = computeThreshold();
		getBounds();
		computeCentroid();
		computeAxes();
		area = computeArea();
	}

	public double getMaxAngle() {
		return maxAngle;
	}

	public Point[] getCorners() {
		return corner;
	}

	public double getCoverage() {
		return coverage;
	}

	public double getMinCoverage() {
		return minCoverage;
	}

	public Line[] getSides() {
		return side;
	}

	public double getAspectRatio() {
		return aspectRatio;
	}

	public double getScore() {
		return score;
	}

	public double[] getScoreVector() {
		return scoreVector;
	}

	public double getThreshold() {
		return threshold;
	}

	public double getPerimeter() {
		return perimeter;
	}

	public Point getCentroid() {
		return centroid;
	}

	public Line getVertAxis() {
		return vertAxis;
	}

	public Line getHorAxis() {
		return horAxis;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	public Line[] getOrigLines() {
		return origLines;
	}

	public Point getCirclePos(Line vertAxis) {
		if (vertAxis == null) {
			Point topMid = top.findPoint( 0.5);		
			vertAxis = new Line(topMid, centroid);
		}
		double ratio = getCircleRadiusMax()/vertAxis.length + circleSearchHeight/2;
		return vertAxis.findPoint( ratio ); 
	}

	public Line extendVertAxis(Line vertAxis) {
		Point topMid = Line.getIntersection(top, vertAxis);
		Point projCent = vertAxis.project(centroid);
		return new Line(topMid, projCent);
	}

	public double getCircleHeight() {
		Point topMid = top.findPoint( 0.5);
		Line vertAxis = new Line(topMid, centroid);
		return vertAxis.length*circleSearchHeight;
	}

	public int getCircleRadiusMax() {
		return round(width/circleMaxRadiusWidthRatio);
	}

	public int getCircleRadiusMin() {
		return round(width/circleMinRadiusWidthRatio);
	}

	public double getDisplacmentFromVerticalAxis(Point pos) {
		Point topMid = top.findPoint( 0.5);
		Line vertAxis = new Line(topMid, centroid);
		return Line.getDistance(vertAxis, pos);
	}

	private void computeCentroid() {
		centroid = new Point(0,0);
		for ( int i=0 ; i<4 ; i++) {
			centroid.x += corner[i].x;
			centroid.y += corner[i].y;
		}
		centroid.x = centroid.x/4;
		centroid.y = centroid.y/4;
	}

	public void computeAxes() {
		Point midTop = top.findPoint(0.5);
		Point midBottom = bottom.findPoint(0.5);
		vertAxis = new Line(midTop, midBottom);
		Point midLeft = left.findPoint(0.5);
		Point midRight = right.findPoint(0.5);
		horAxis = new Line(midLeft, midRight);
	}
/*
	public double computeScore() {
		scoreVector = new double[6]; // 0-3 : side score, 4: aspect ratio, 5: coverage
		double aspectScore = aspectRatio > idealAspectRatio ? idealAspectRatio/aspectRatio : aspectRatio/idealAspectRatio;
		double sumScore2 = aspectScore*aspectScore + coverage*coverage;
		double sum = aspectScore + coverage;
		for (int i=0 ; i<4 ; i++) {
			scoreVector[i] = side[i].score;
			sumScore2 += side[i].score * side[i].score;
			sum += side[i].score;
		}		
		scoreVector[4] = aspectScore;
		scoreVector[5] = coverage;		
//		score[6] =  computeAngleScore();
		score = perimeter*Math.sqrt((sumScore2)/6.0);
		StringBuilder sb = new StringBuilder(100);
		sb.append(score+" ");
		for ( int i=0 ; i<4 ; i++) {
			sb.append(i+":"+scoreVector[i]+" ");
		}

		sb.append("Asp:"+scoreVector[4]+" ");
		sb.append("Cov:"+scoreVector[5]+" ");
		sb.append("Tot:"+sum+" ");
		sb.append("Per:"+perimeter+" ");
		scoreDetails = sb.toString();
		return score;
	}
*/
	//
	//  Shape based score
	//
	public double computeScore() {
		double aspectScore = aspectRatio > idealAspectRatio ? idealAspectRatio/aspectRatio : aspectRatio/idealAspectRatio;		
		double angleScore = 0.0;
		double horizontalSkew = top.length / bottom.length;
		double verticalSkew = left.length / right.length;
		//
		// because pictures are always taken from below, we expect top < bottom and right = left
		//
		double vertSkewError = Math.abs( verticalSkew - 1.0);
		double horSkewError = Math.abs( horizontalSkew - 1.0);
		if (horizontalSkew < 1.0 && vertSkewError < 0.1) {
			angleScore = 1.0 - Math.abs( bottomLeft - bottomRight);
			horSkewError = 0.0;
		}
		else {
			LinePair pair1 = new LinePair(top, bottom);
			LinePair pair2 = new LinePair(left, right);
			double deviation = pair1.getMaxDeviationFrom(pair2, Math.PI/2);
			angleScore = 1.0 - deviation;
		}
		double skewScore = 1.0 - vertSkewError - horSkewError;
		if (skewScore < 0) {
			skewScore = 0.0;
		}
		double score = Math.sqrt( aspectScore * aspectScore + angleScore *angleScore + skewScore * skewScore  ) / Math.sqrt(3);
		scoreVector = new double[3];
		scoreVector[0] = aspectScore;
		scoreVector[1] = angleScore;
		scoreVector[2] = skewScore;
		StringBuilder sb = new StringBuilder(100);
		sb.append(score+" ");
		sb.append("Asp:"+scoreVector[0]+" ");
		sb.append("Ang:"+scoreVector[1]+" ");
		sb.append("Skw:"+scoreVector[2]+" ");
		sb.append("Per:"+perimeter+" ");
		scoreDetails = sb.toString();
		return score;
	}

	public double computeAngleScore() {
		double[] topAngle = new double[2];
		double[] bottomAngle = new double[2];
		topAngle[0] = LinePair.getAngle(left, top);
		topAngle[1] = LinePair.getAngle(right, top);
		double topDiff = topAngle[0] - topAngle[1];
		double topScore = topDiff/Math.PI;
		topScore = 1 - topScore*topScore;
		System.out.println("top "+topAngle[0]+" "+topAngle[1]+" score = "+topScore+" diff = "+topDiff);
		bottomAngle[0] = LinePair.getAngle(left, bottom);
		bottomAngle[1] = LinePair.getAngle(right, bottom);
		double bottomDiff = bottomAngle[0] - bottomAngle[1];
		double bottomScore = bottomDiff/Math.PI;
		bottomScore = 1 - bottomScore*bottomScore;
		System.out.println("bottom "+bottomAngle[0]+" "+bottomAngle[1]+" score = "+bottomScore+" diff = "+bottomDiff);
		return Math.sqrt((topScore+bottomScore)/2);
	}

	public double computeThreshold() {
		double threshold = side[0].threshold;
		for (int i=1 ; i<3 ; i++) {
			threshold = side[i].threshold > threshold ? side[i].threshold : threshold;
		}
		return threshold;
	}
/*
	public double computeThreshold() {
		
		double tSum = 0.0;
		for ( int i=0 ; i<4 ; i++ ) {
			tSum += side[i].threshold;
		}
		return tSum / 4;
	}
*/
	public double getCorrectAngle() {
		double correction = 0.0;
		for ( int i=0 ; i<4 ; i++ ) {			
			double corr =  getCorrection(side[i].angle);
//			System.out.println("Got correction " + corr + " for side " + side[i]);
			correction += corr;
		}
		return correction/4;
	}

	private double getCorrection(double a) {
		double corr = 0.0;
		double s = Math.abs(Math.sin(a));
		if (s < 1.0/Math.sqrt(2.0)) {
			if (a > Math.PI/2.0) {
				corr = Math.PI - a;
			}
			else if (a < -Math.PI/2.0) {
				corr = -Math.PI - a;
			}
			else {
				corr = -1.0*a;
			}
		}
		else {
			corr = Math.PI/2.0-a;
			if (corr > Math.PI/2.0) {
				corr = corr - Math.PI;
			}
		}
		
		return corr;
	}

	public Point getBoundPos() {
		return boundPos;
	}

	public Point getBoundDim() {
		return boundDim;
	}

	private void reorderCorners() {
		Line[] s = new Line[4];
		for ( int i=0 ; i<4 ; i++ ) {
			int next = (i+1) % 4;
			s[i] = new Line( corner[i], corner[next]);
		}
		boolean reorder = false;
		for ( int i=0 ; i<4 ; i++ ) {
			int opp = (i+2)%4;
			if (Line.getIntersection2(s[i], s[opp]) != null) {
				reorder = true;
			}
		}
		if (reorder) {
			Point temp = corner[2];
			corner[2] = corner[1];
			corner[1] = temp;
		}
	}

	public double getArea() {
		return area;
	}

	private double computeArea() {
		Line base = new Line (corner[0], corner[2]);
		double d1 = Line.getDistance(base, corner[1]);
		double d2 = Line.getDistance(base, corner[3]);
		return base.length * (d1 + d2) / 2;
	}

	private void getBounds() {
		double minX = corner[0].x;
		double maxX = minX;
		double minY = corner[0].y;
		double maxY = minY;
		for ( int i=1; i<4 ; i++ ) {
			minX = corner[i].x < minX ? corner[i].x : minX;
			minY = corner[i].y < minY ? corner[i].y : minY;
			maxX = corner[i].x > maxX ? corner[i].x : maxX;
			maxY = corner[i].y > maxY ? corner[i].y : maxY;
		}
		boundDim =  new Point(maxX-minX, maxY-minY);
		boundPos = new Point(minX, minY);
	}

	public Rectangle getBounded() {
		Point[] bCorner = new Point[4];
		for ( int i=0 ; i<4 ; i++) {
			bCorner[i] = new Point(corner[i].x-boundPos.x, corner[i].y-boundPos.y);
		}
		return new Rectangle(bCorner, null);
	}

	public Transformation getTransform() {
		return transformation;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public void setTransform(Transformation trans) {
		this.transformation = trans;
	}

	public String getScoreDetails() {
		return scoreDetails;
	}

	public double getDistance(Rectangle rect) {
		Point[] rcorner = rect.getCorners();
		double order = getOrder(corner);
		double rorder = getOrder(rcorner);
		Point[] rcorner2 = null;
		if (order * rorder < 0) {
			rcorner2 = new Point[4];
			rcorner2[0] = rcorner[3];
			rcorner2[1] = rcorner[2];
			rcorner2[2] = rcorner[1];
			rcorner2[3] = rcorner[0];
		}
		else {
			rcorner2 = rcorner;
		}
		double min = getDistance(corner, rcorner2, 0);;
		for ( int i=1 ; i<4 ; i++) {
			double d = getDistance(corner, rcorner2, i);
			min = Math.min(min, d);
		}
		return min;
	}

	private double getDistance(Point[] c1, Point[] c2, int index) {
		double max = 0;
		for ( int i=0 ; i<4 ; i++ ) {
			int j = (i+index) % 4;
			Vector v = new Vector( c1[i], c2[j]);
			max = Math.max(max, v.length);
		}
		return max;
	}

	private double getOrder(Point[] vertex) {
		double sumCross = 0;
		Vector[] edge = new Vector[4];
		for ( int i=0 ; i<4 ; i++) {
			int next = (i+1) % 4;
			edge[i] = new Vector( vertex[i], vertex[next]);
		}
		for ( int i=0 ; i<4 ; i++) {
			int next = (i+1) % 4;
			sumCross += Vector.cross(edge[i], edge[next]);
		}
		return sumCross;
	}

	public double getIntersection(Rectangle rect) {
		
		Line[] rside = rect.getSides();
		Point[] rcorner = rect.getCorners();
		boolean[] inside = new boolean[4];
		boolean[] rinside = new boolean[4];
		int ic = 0;
		int ric = 0;
		for ( int i=0 ; i<4 ; i++) {
			rinside[i] = contains(rcorner[i]);
			ric = rinside[i] ? ric+1 : ric;
			inside[i] = rect.contains(corner[i]);
			ic = inside[i] ? ic+1 : ic;
		}
		if (ic == 4) {
			return this.getArea();
		}
		else if (ric == 4) {
			return rect.getArea();
		}
		else if (ic == 0 && ric == 0) {
			return 0.0;
		}
		List<Point> intersection = new ArrayList<Point>();
		List<Point> contained = new ArrayList<Point>();
 		for ( int i=0 ; i<4 ; i++ ) {						
			for ( int j=0 ; j<4 ; j++ ) {
				Point inter = Line.getIntersection2( side[i], rside[j]);
				if ( inter != null) {
					intersection.add(inter);
				}
			}
			if (rinside[i]) {
				contained.add(rcorner[i]);
			}
			if (inside[i]) {
				contained.add(corner[i]);
			}
		}
		
		if ( intersection.size() == 2 && contained.size() == 2) {
			intersection.addAll(contained);
			return new Rectangle(intersection).getArea();
		}
		else if (intersection.size() == 2 &&  contained.size() == 1 ) {
			intersection.addAll(contained);
			return Line.getArea(new Line( intersection.get(0), intersection.get(1) ), intersection.get(2));
		}
		else if (intersection.size() == 2 && contained.size() == 3) {
			if ( ic == 3) {
				return getArea51( intersection, corner, inside);
			}
			else if ( ric == 3) {
				return getArea51( intersection, rcorner, rinside);
			}
			else {
				return 0; // TBD: home plate
			}
		}
		else if (intersection.size() == 4 && contained.size() == 2) {
			// TBD: intersection area = 3 positive triangle and 1 negative
			return 0;
		}
		else if (intersection.size() == 4 && contained.size() == 1) {
			// TBD: intersection area = 3 tringles
			return 0;
		}
		else if (intersection.size() == 0) {
			return 0; // 'contained' points are just touching the border so not a problem
		}
		System.out.println("ERROR: unexpected inter size = "+intersection.size()+" contained size = "+contained.size()+" ic = "+ic+" ric = "+ric);
		System.out.println("win = "+this);
		System.out.println("sign border = "+rect);
		for (Point p : intersection) {
				System.out.println(" inter   "+p);
		}
		for (Point p : contained) {
				System.out.println(" contained   "+p);
		}
		return 0.0;
	}

	private double getArea51(List<Point> inter, Point[] corner, boolean[] inside) {
		Point outside = null;
		for ( int i=0 ; i<4 ; i++ ) {
			if (!inside[i]) {
				outside = corner[i];
			}
		}
		return this.getArea() - Line.getArea(new Line( inter.get(0), inter.get(1) ), outside); 
	}

	public boolean contains(Point p) {
		double tarea = 0.0;
		for ( int i=0 ; i<4 ; i++ ) {			
			tarea += Line.getArea(side[i], p);
		}
		double delta = Math.abs(tarea - area);
		if (delta > 0.0001 * area) {
//				System.out.println("point "+p+" is not in side because area = "+area+" tarea = "+tarea+" delta = "+delta);
			return false;
		}
		return true; // if tarea == area the point is on the border
	}

	public String toString() {
		return corner[0].x+","+corner[0].y+" "+corner[1].x+","+corner[1].y+" "+corner[2].x+","+corner[2].y+" "+corner[3].x+","+corner[3].y;
	}

	public String ancestor() {
		return origLines[0]+" "+origLines[1]+" "+origLines[2]+" "+origLines[3];
	}

	public String showLen() {
		return side[0].length+" "+side[1].length+" "+side[2].length+" "+side[3].length;
	}

	public static Rectangle rotate(Rectangle rect, double angle) {
		Point[] rotatedCorner = new Point[4];
		Point[] corner = rect.getCorners();
		for ( int i=0 ; i<4 ; i++ ) {
			Vector v = new Vector(rect.getCentroid(), corner[i]);
			Vector u = Vector.rotate(v, angle);
			rotatedCorner[i] = Vector.add(rect.getCentroid(), u);
		}
		return new Rectangle(rotatedCorner, rect);
	}

	public static Rectangle translate(Rectangle rect, Vector t) {
		Point[] translatedCorner = new Point[4];
		Point[] corner = rect.getCorners();
		for ( int i=0 ; i<4 ; i++ ) {			
			translatedCorner[i] = Vector.add(corner[i], t);
		}
		return new Rectangle(translatedCorner, rect);
	}

	public Rectangle scale(double factor) {
		Point[] scaledCorner = new Point[4];
		for ( int i=0 ; i<4 ; i++ ) {
			scaledCorner[i] = new Point( factor*corner[i].x, factor*corner[i].y);
		}
		Rectangle rec = new Rectangle( scaledCorner, this);
		if (transformation != null) {
			Transformation trans = new Transformation( transformation );
			trans.setScale( factor );
			rec.setTransform(trans);
		}
		return rec;
	}

	public static List<Rectangle> getRectanglesFromLines(List<Line> lines, int minDim) {
		Logger logger = new Logger(LoggingTag.Border, "Rectangle", "getRectanglesFromLines");
		List<Rectangle> fullList = null;
		List<Rectangle> rectangles = null;
		int level = -1;
		final double dupDistThresh = 0.1;
		do {
			fullList = getRectanglesFromLinesLevel(lines, minDim, ++level);
			rectangles = eliminateDuplicates(fullList, dupDistThresh);
		} while (rectangles.size() < minRectangles[level] && level < numLevels-1);
		
		logger.log("Found " + rectangles.size() + " rectangles at level "+level);
//		for ( int i=0 ; i<100 && i<rectangles.size() ; i++) {
//			logger.log(rectangles.get(i).getScoreDetails());
//		}
		return rectangles;
	}

	private static List<Rectangle> getRectanglesFromLinesLevel(List<Line> lines, int minDim, int level) {
		Logger logger = new Logger(LoggingTag.Border, "Rectangle", "getRectanglesFromLines");
		final double minSeparation = minimumSeparationFactor[level] * minDim;
		final double maxAngleDev = maxDeviation[level];
		final double minDist = miniumDistance[level];
		System.out.println("min separation factor = "+minimumSeparationFactor[level]+" min dim = "+minDim+" min separation = "+minSeparation);
		List<Rectangle> rectangles = new ArrayList<Rectangle>();
		List<LinePair> parallelLines = getParallelLines(lines, minSeparation, maxAngleDev);
		Map<LinePair, List<LinePair>> orthPairSets = getOrthogonalSets(parallelLines, maxAngleDev);
		for ( LinePair lp1 : orthPairSets.keySet()) {
			List<LinePair> pairList = orthPairSets.get(lp1);
			for ( LinePair lp2 : pairList) {
				Rectangle rect = new Rectangle(lp1, lp2);
				
				if (isDistinguished(rectangles, rect, minDist)) {
					rectangles.add( rect );
				}
				
			}
		}
		RectangleComparator comparator = new RectangleComparator();
		Collections.sort(rectangles, comparator);
		return rectangles;
	}

	//
	//  used to prune rectangles that are indistinguishable FIXME: very slow
	//
	private static boolean isDistinguished(List<Rectangle> rList, Rectangle rect, double minDist) {
		for ( Rectangle r : rList) {
			if ( rect.getDistance(r) < minDist ) {
				return false;
			}
		}
		return true;
	}

	private static List<LinePair> getParallelLines(List<Line> lines, double minSeparation, double maxAngleDev) {
		System.out.println("min separation = "+minSeparation);
		List<LinePair> parallelLines = new ArrayList<LinePair>();
		for ( int i=0 ; i<lines.size() ; i++ ) {
			Line l1 = lines.get(i);
			for ( int j=i+1 ; j<lines.size() ; j++ ) {
				Line l2 = lines.get(j);
				LinePair pair = new LinePair(l1, l2);
				if (pair.distance > minSeparation && pair.getAngleDeviation() < maxAngleDev) {
//					System.out.println("Adding pair "+pair.line1+" "+pair.line2);
					parallelLines.add(pair);
				}
				else if (pair.distance > minSeparation) {
//					System.out.println("Reject pair angle dev = "+pair.getAngleDeviation()+" "+pair.line1+" "+pair.line2);
				}
				else if (pair.getAngleDeviation() < maxAngleDev) {
//					System.out.println("Reject pair dist = "+pair.distance+" "+pair.line1+" "+pair.line2);
				}
			}
		}
		return parallelLines;
	}

	private static Map<LinePair, List<LinePair>> getOrthogonalSets(List<LinePair> parallelLines, double maxAngleDev) {
		Map<LinePair, List<LinePair>> orthSet = new HashMap<LinePair, List<LinePair>>();
		for ( int i=0 ; i<parallelLines.size() ; i++ ) {
			LinePair pair1 = parallelLines.get(i);
			List<LinePair> pairList = new ArrayList<LinePair>();
			for ( int j=i+1 ; j<parallelLines.size() ; j++ ) {
				LinePair pair2 = parallelLines.get(j);
				if (pair1.getMaxDeviationFrom(pair2, Math.PI/2) < maxAngleDev) {
					pairList.add(pair2);
				}
			}
			orthSet.put(pair1, pairList);
		}
		return orthSet;
	}
/*
	//
	// FIXME1: the clustering may be blocking key line pairs resulting in a weak top choice
	//        should re-write this without clusters
	//
	// FIXME2: when the image is weak we may be missing key edges...to fix this we can also
	//         consider rect = line pair + single for higher levels (more relaxed constraints)
	//
	public static List<Rectangle> getRectanglesFromClusters(List<CentroidCluster<Line>> clusters, int minDim) {
		Logger logger = new Logger(LoggingTag.Border, "Rectangle", "getRectanglesFromClusters");
		List<Rectangle> fullList = null;
		List<Rectangle> rectangles = null;
		int level = -1;
		final double dupDistThresh = 0.1;
		do {
			fullList = getRectanglesFromClustersLevel(clusters, minDim, ++level);
			rectangles = eliminateDuplicates(fullList, dupDistThresh);
		} while (rectangles.size() < minRectangles[level] && level < numLevels-1);
		
		logger.log("Found " + rectangles.size() + " rectangles at level "+level);
//		for ( int i=0 ; i<100 && i<rectangles.size() ; i++) {
//			logger.log(rectangles.get(i).getScoreDetails());
//		}
		return rectangles;
	}

	//
	//  FIXME: angle deviation calc is wrong e.g. angle1 = pi, angle2 = 0  !!!, use SignImage.getAngleDeviation()
	//
	
	private static List<Rectangle> getRectanglesFromClustersLevel(List<CentroidCluster<Line>> clusters, int minDim, int level) {
		Logger logger = new Logger(LoggingTag.Border, "Rectangle", "getRectanglesFromClustersLevel");
		final double minimumSeparation = minimumSeparationFactor[level] * minDim;
		List<LinePair> verticalLinePairs = new ArrayList<LinePair>();
		List<LinePair> horizontalLinePairs = new ArrayList<LinePair>();
		Map<CentroidCluster<Line>, String> clusterDirection = new HashMap<CentroidCluster<Line>, String>();		
		int numVert = 0;
		int numHor = 0;
		int j,i=0;
		for (CentroidCluster<Line> cl : clusters) {
			double[] ca =  cl.getCenter().getPoint();
			double absa =  Math.abs(ca[0]);
			double verticalDeviation = Math.abs(absa - Math.PI/2.0);
			double horizontalDeviation = absa;
			boolean isVertical = verticalDeviation < maxDeviation[level] ? true : false;
			boolean isHorizontal = horizontalDeviation < maxDeviation[level] ? true : false;
			if (isVertical) {
				clusterDirection.put(cl, "vertical");
				numVert += cl.getPoints().size();
			}
			else if (isHorizontal) {
				clusterDirection.put(cl, "horizontal");
				numHor += cl.getPoints().size();
			}
			else {
				clusterDirection.put(cl, "none");
			}
//			System.out.println("cluster " + i + " angle = " + ca[0] + " direction is " + clusterDirection.get(cl) + " num points = " + cl.getPoints().size());
			i++;
		}
		int numClusters = i;		
		PairStats verStats = new PairStats(numClusters);
		PairStats horStats = new PairStats(numClusters);
		logger.log("Found " + numHor + " horizontal lines and " + numVert + " vertical lines");
		i=0;
		for (CentroidCluster<Line> cluster : clusters) {
//			System.out.println("cluster " + i);
			if (clusterDirection.get(cluster).equals("vertical")) {
				j=0;			
				for (CentroidCluster<Line> cl : clusters) {
					if (clusterDirection.get(cl).equals("vertical")) {
						for (Line line: cluster.getPoints()) {
//							System.out.println("enter with " + verticalLinePairs.size() + " vertical line pairs");
							PairStats ps = doAddLinePairs(line, cl, verticalLinePairs, minimumSeparation, clusterDirection.get(cl));
							verStats.add(ps, i, j);
						}
					}
					j++;
				}
			}
			else if (clusterDirection.get(cluster).equals("horizontal")) {
				j=0;
				for (CentroidCluster<Line> cl : clusters) {
					if (clusterDirection.get(cl).equals("horizontal")) {
//						System.out.println("paired with cluster " + j);
						for (Line line: cluster.getPoints()) {
//							System.out.println("enter with " + horizontalLinePairs.size() + " horizontal line pairs");
							PairStats ps = doAddLinePairs(line, cl, horizontalLinePairs, minimumSeparation, clusterDirection.get(cl));
							horStats.add(ps, i, j);
						}
					}
					j++;
				}
			}
			i++;			
		}
		logger.log("Found " + verticalLinePairs.size() + " vertical pairs, made " + verStats.count + 
				" checks max " + verStats.max + " min " + verStats.min + " avg " + verStats.sum/verStats.count);
		logger.log("Found " + horizontalLinePairs.size() + " horizontal pairs, made "+ horStats.count +
				"checks max " + horStats.max + " min " + horStats.min + " avg " + horStats.sum/horStats.count);
		i=0;
		for ( CentroidCluster<Line> cl : clusters) {
			StringBuilder sb = new StringBuilder(100);
			sb.append("cluster " + i + " : ");
			for ( j=0 ; j<numClusters ; j++) {
				if (clusterDirection.get(cl).equals("horizontal")) {
					sb.append(horStats.addedPairs[i][j]+"/"+horStats.clusterPairs[i][j]+", ");
				}
				else if (clusterDirection.get(cl).equals("vertical")) {
					sb.append(verStats.addedPairs[i][j]+"/"+verStats.clusterPairs[i][j]+", ");
				}
			}
			logger.log(sb.toString());
			i++;
		}
		List<Rectangle> rectangles = new ArrayList<Rectangle>();
		for (LinePair vlp : verticalLinePairs) {
			for (LinePair hlp : horizontalLinePairs) {

				Rectangle rect = new Rectangle( vlp.line1, hlp.line1, vlp.line2, hlp.line2);
				double dev = Math.abs(Math.abs(rect.getMaxAngle() - Math.PI/2.0));
				if (dev < maxDeviation[level]) {
					if (minPerimeterCoverage[level] < rect.getCoverage() && minSideCoverage[level] < rect.getMinCoverage()) {
//						System.out.println(rect);
//						System.out.println(vlp.line1+","+hlp.line1+","+vlp.line2+","+hlp.line2);
//						System.out.println(vlp.line1.angle+" "+hlp.line1.angle+" "+vlp.line2.angle+" "+hlp.line2.angle);
						double aspectRatioError = Math.abs(rect.getAspectRatio() - idealAspectRatio);
						if (aspectRatioError < maxAspectRatioError[level]) {
							rectangles.add(rect);
						}
						else {
//							System.out.println("ignore rectangle " + rect.showLen() + " with aspect ratio " + rect.getAspectRatio());
						}
					}
					else {
//						System.out.println("ignore rectangle "+rect+" with coverage of " + rect.getCoverage()+" min coverage of "+rect.getMinCoverage());
					}
				}
				else {
//					System.out.println("ignore rectangle with max angle " + rect.getMaxAngle());
				}
			}
		}
		RectangleComparator comparator = new RectangleComparator();
		Collections.sort(rectangles, comparator);
		return rectangles;
	}

	private static PairStats doAddLinePairs(Line line, CentroidCluster<Line> cl, List<LinePair> linePairs, final double minimumSeparation, String type) {
		double max = 0.0;		
		PairStats ps = new PairStats();;
		if (type.equals("horizontal")) {
//			System.out.println("Adding pairs for " + line.toString());
		}
		int count = linePairs.size();
		for (Line l: cl.getPoints()) {
			if (line != l) {
				LinePair lp = new LinePair(line, l);
				ps.count++;
				ps.sum += lp.distance;
				if (ps.min <0 || ps.min > lp.distance) {
					ps.min = lp.distance;
				}
				ps.max = lp.distance > ps.max ? lp.distance : ps.max;
				if (lp.distance > minimumSeparation) {					
					addUniqueLinePair(lp, linePairs);
					ps.added++;
					count++;
				}
				if (lp.distance > max) {
					max = lp.distance;
				}
				if (type.equals("horizontal")) {
					
//					System.out.println("   " + l.toString() + " dist = " + lp.distance+" count = "+count);
//					my(line, l);
				}
			}
		}
		return ps;
//		System.out.println("max separation is " + max);
	}

	private static void addUniqueLinePair(LinePair linePair, List<LinePair> linePairs) {
		LinePair mirror = new LinePair(linePair.line2, linePair.line1);
		for (LinePair lp : linePairs) {
			if (lp.equals(linePair) || lp.equals(mirror)) {
				return; // duplicate
			}
		}
		linePairs.add(linePair);
	}
*/
	public static boolean areDuplicate(Rectangle r1, Rectangle r2, double dupDistThresh) {
//		System.out.println("Check if "+r1+" and "+r2+" are duplicates");
		Point[] c1 = r1.getCorners();
		Point[] c2 = r2.getCorners();
		for ( int i=0 ; i<4 ; i++ ) {
			boolean dup = false;
			for ( int j=0 ; j<4 ; j++) {
				if ( dist(c1[i], c2[j]) < dupDistThresh ) {
					dup = true;
				}
			}
			if (dup == false) {
				return false;
			}
		}
//		System.out.println("duplicate");
		return true;
	}

	private static double dist(Point p1, Point p2) {
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		return Math.sqrt( dx+dx + dy*dy );
	}

	private static List<Rectangle> eliminateDuplicates(List<Rectangle> full, double dupDistThresh) {
		double scoreThresh = 10;
		List<Rectangle> reduced = new ArrayList<Rectangle>();
		Map<Rectangle, Boolean> eliminated = new HashMap<Rectangle, Boolean>();
		for ( int i=0 ; i<full.size() ; i++) {
			Rectangle rect = full.get(i);
			Boolean isEliminated = eliminated.get(rect);
			if (isEliminated == null || isEliminated == false) {
				// no need to go thru the entire list assuming it is sorted by score
				for ( int j=i+1 ; j<full.size() && full.get(j).getScore() > rect.getScore() - scoreThresh ; j++) {
					if (areDuplicate(rect, full.get(j), dupDistThresh)) {
						eliminated.put(full.get(j), true);
					}
				}
				reduced.add(rect);
			}			
		}
		System.out.println("full size is "+full.size()+" reduced is "+reduced.size());
		return reduced;
	}

/*
	private static void my(Line line1, Line line2) {
		double[] d = new double[4];
		d[0] = Line.getDistance(line1, line2.p1);
		my2(line1, line2.p1);
		d[1] = Line.getDistance(line1, line2.p2);
		my2(line1, line2.p2);
		d[2] = Line.getDistance(line2, line1.p1);
		my2(line2, line1.p1);
		d[3] = Line.getDistance(line2, line1.p2);
		my2(line2, line1.p2);
		System.out.println("     " + d[0] + "  " + d[1] + "  " + d[2] + "  " + d[3]);		
	}

	private static void my2(Line line, Point p) {
		Vector v = new Vector(line.p1, line.p2);
		Vector u = new Vector(line.p1, p);
		double cosTheta = Vector.dot(u, v) / ( u.getLength()*v.getLength());
		double theta = Math.acos(cosTheta);
		double res = u.getLength()*Math.sin(theta);
		System.out.println("       v=" + v.toString() + " u="+u.toString()+" costh="+cosTheta+" theta="+theta+" sinTheta="+Math.sin(theta)+" d="+res);
	}
*/	
	private static int round(double val) {
		return (int)(val +0.5);
	}
}