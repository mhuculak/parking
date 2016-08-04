package parking.opencv;

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
	
	private Line[] side;
	private Line[] origLines;
	private Line top;
	private Line bottom;
	private Line left;
	private Line right;
	private Line vertAxis;
	private Line horAxis;
	private Point[] corner;
	private double[] angle;
	private double perimeter;
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
	private final static double[] minPerimeterCoverage = { 0.5, 0.4, 0.3 };  // % of a rectangle perimeter that must be visible
	private final static double[] minSideCoverage = { 0.2, 0.1, 0.05 };       // % of each side that must be visible		
	private final static double[] maxAspectRatioError = { 0.1, 0.2, 0.3 };
	private final static double[] maxDeviation = { 0.2, 0.3, 0.4 };

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
		for ( i=0 ; i<4 ; i++) {
			int next = i==3 ? 0 : i+1;
			side[i] = new Line(corner[i], corner[next]);			
			if (origLines != null) {
				side[i].score = origLines[i].score;
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
			}
			else {
				top = side[2];
				bottom = side[0];
			}
			if (side[1].p1.x < side[3].p1.x) {
				left = side[1];
				right = side[3];
			}
			else {
				left = side[3];
				right = side[1];
			}
		}
		else {
			width =  (side[1].length + side[3].length)/2.0;
			height = (side[0].length + side[2].length)/2.0;
			if (side[0].p1.x < side[2].p1.x) {
				left = side[0];
				right = side[2];
			}
			else {
				left = side[2];
				right = side[0];
			}
			if (side[1].p1.y < side[3].p1.y) {
				top = side[1];
				bottom = side[3];
			}
			else {
				top = side[3];
				bottom = side[1];
			}		
		}
		aspectRatio = width/height;
		score = computeScore();
		threshold = computeThreshold();
		getBounds();
		computeCentroid();
		computeAxes();
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

	//
	// FIXME1: the clustering may be blocking key line pairs resulting in a weak top choice
	//        should re-write this without clusters
	//
	// FIXME2: when the image is weak we may be missing key edges...to fix this we can also
	//         consider rect = line pair + single for higher levels (more relaxed constraints)
	//
	public static List<Rectangle> getRectangles(List<CentroidCluster<Line>> clusters, int minDim) {
		List<Rectangle> fullList = null;
		List<Rectangle> rectangles = null;
		int level = -1;
		final double dupDistThresh = 0.1;
		do {
			fullList = getRectanglesLevel(clusters, minDim, ++level);
			rectangles = eliminateDuplicates(fullList, dupDistThresh);
		} while (rectangles.size() < minRectangles[level] && level < numLevels-1);
		RectangleComparator comparator = new RectangleComparator();
		Collections.sort(rectangles, comparator);

		System.out.println("Found " + rectangles.size() + " rectangles at level "+level);
		for ( int i=0 ; i<100 && i<rectangles.size() ; i++) {
//			System.out.println(rectangles.get(i).getScoreDetails());
		}
		return rectangles;
	}

	//
	//  FIXME: angle deviation calc is wrong e.g. angle1 = pi, angle2 = 0  !!!, use SignImage.getAngleDeviation()
	//
	
	private static List<Rectangle> getRectanglesLevel(List<CentroidCluster<Line>> clusters, int minDim, int level) {
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
		System.out.println("Found " + numHor + " horizontal lines and " + numVert + " vertical lines");
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
		System.out.println("Found " + verticalLinePairs.size() + " vertical pairs, made " + verStats.count + 
				" checks max " + verStats.max + " min " + verStats.min + " avg " + verStats.sum/verStats.count);
		System.out.println("Found " + horizontalLinePairs.size() + " horizontal pairs, made "+ horStats.count +
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
			System.out.println(sb.toString());
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
	
	private static int round(double val) {
		return (int)(val +0.5);
	}
}