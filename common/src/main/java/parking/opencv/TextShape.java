package parking.opencv;

import parking.util.Logger;
import parking.util.LoggingTag;

import org.opencv.core.Point;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class Vec {
	public int dx;
	public int dy;

	public Vec( int dx, int dy) {
		this.dx = dx;
		this.dy = dy;
	}

	public String toString() {
		return dx+","+dy;
	}

	public static Pos add(Pos p, Vec v) {
		return new Pos( p.x + v.dx, p.y + v.dy);
	}
}

class ScanState {
	public Pos next;
	public boolean closure;

	ScanState(Pos next) {
		this.next = next;
		closure = false;
	}

	ScanState(boolean closure) {
		next = null;
		this.closure = closure;
	}
}

class ProtoSegment extends TextSegment {
	
	public SegmentPoint endpoint1;
	public SegmentPoint endpoint2;
	private double area;

	private static double minMergeDist = 2.0;

	public ProtoSegment(SegmentPoint ep1, SegmentPoint ep2) {
		super(SegType.PROTO);
		endpoint1 = ep1;
		endpoint2 = ep2;
		computeArea();
		System.out.println("Created ProtoSegment "+this);
	}

	private void computeArea() {
		Line l = new Line( endpoint1.pos, endpoint2.pos);
		area = l.length * (endpoint1.thickness + endpoint2.thickness)/2;
	}

	public double getArea() {
		return area;
	}

	public void doMerge(List<TextSegment> removed) {
		for (TextSegment s : removed) {
			ProtoSegment seg = (ProtoSegment)s;
			double[] d = new double[4];
			d[0] = getDist( endpoint1.pos, seg.endpoint1.pos);
			d[1] = getDist( endpoint1.pos, seg.endpoint2.pos);
			d[2] = getDist( endpoint2.pos, seg.endpoint1.pos);
			d[3] = getDist( endpoint2.pos, seg.endpoint2.pos);
			double minDist = d[0];
			int mini = 0;
			SegmentPoint nearest = seg.endpoint1;
			for ( int i=1 ; i<4 ; i++ ) {
				if (minDist > d[i]) {
					minDist = d[i];
					mini = i;
					nearest = i%2 == 1 ? seg.endpoint2 : seg.endpoint1;
				}
			}
			if (minDist < minMergeDist) {
				if ( mini < 2) {
					endpoint1 = nearest;
				}
				else {
					endpoint2 = nearest;
				}
			}
		}
	}

	private static double getDist(Point p1, Point p2) {
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		return Math.sqrt(dx*dx + dy*dy);
	}

	public String toString() {
		return endpoint1+" "+endpoint2+" "+area;
	}
}

class AnulusSegment extends TextSegment {
	private Point center;
	private double outerRadius;
	private double innerRadius;

	public AnulusSegment(Point center, double outerRadius, double innerRadius) {
		super(SegType.ANULUS);
		this.center = center;
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
	}

	public AnulusSegment(Point center, double outerRadius, double innerRadius, SegType type) {
		super(type);
		this.center = center;
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
	}
}

class TransitionData {
	public List<List<Double>> transList;
	public List<Double> vals;

	public TransitionData(List<List<Double>> transList, List<Double> vals) {
		this.transList = transList;
		this.vals = vals;
	}
}

class TopologyChanges {
	public List<Integer> allChanges;
	public List<Integer> filteredChanges;

	public TopologyChanges(List<Integer> allChanges, List<Integer> filteredChanges) {
		this.allChanges = allChanges;
		this.filteredChanges = filteredChanges;
	}
}

public class TextShape {

	private int[][] data;
	private BufferedImage display;
	private int id;
	private Color color;
	private List<Pos> points;
	private Point centroid;
	private Rectangle bound;
	private Line baseline;
	private Line left;
	private double aspectRatio;
	private List<TextSegment> textSegments;
	private String xTopology;
	private String yTopology;
	private List<TextChoice> textMatches;
	private double major;  // major axis
	private double minor;  // minor axis
	private double radius;
	private boolean closed;
	private int minX;
	private TextShapeClassifier classifier;
	private Logger m_logger;

	private static final int WHITE = 0xffffffff;
	private static final int BLACK = 0xff000000;
	private static final int UNDEFINED = -888888888;
	private static final int maxShapeGap = 2;
	private static final int maxRegionSize = 10000;
	private static final int maxTrans = 10;
	private static final double areaThresh = 0.01;
	private static final int minTopChangeLen = 3;
	private static final int minSegmentLength = 3;
	private static final double segmentMergeMinDist = 1.0;
	private static final double minBranchSplitThicknessChangeNorm = 0.1;
	private static final double minBranchSplitThicknessChangeAbs = 3;
 	private static final double minBranchSplitAngleChange = 0.3;
 	private static final double maxStrokeDistance = 0.3;
 	private static final double minNormArea = 0.001;
 	private static final double maxNormArea = 0.25;

	private static final Vec[] delta = { new Vec(-1,-1), new Vec(0,-1), new Vec(1,-1), new Vec(1,0), new Vec(1,1), 
										 new Vec(0,1), new Vec(-1,1), new Vec(-1,0)};
	private static final Color[] colors = new Color[] { Color.red, Color.green, Color.orange, Color.cyan, Color.pink, 
		Color.yellow, Color.magenta,  Color.blue};

	public TextShape(int[][] data, BufferedImage display, int id, Logger logger) {
		m_logger = new Logger(logger, this, LoggingTag.Shape);
		this.data = data;
		this.display = display;
		this.id = id;
		color = colors[id % 8];
		classifier = TextShapeClassifier.getInstance(m_logger);
	}

	public void add(Pos p, Graphics dg) {
		if (points == null) {
			points = new ArrayList<Pos>();
		}
		points.add(p);
//		System.out.println("Set value "+id+" at "+p);
		data[p.x][p.y] = id;
		dg.setColor(color);
		dg.fillRect(p.x, p.y,1,1);
	}

	public int size() {
		return points.size();
	}

	public double getAspectRatio() {
		return aspectRatio;
	}

	public List<TextSegment> getTextSegments() {
		return textSegments;
	}

	public List<TextChoice> getTextMatches() {
		return textMatches;
	}

	public String getTextMatchesAsString() {
		if (textMatches == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(10);
		for ( int i=0 ; i<textMatches.size() ; i++ ) {
			sb.append(textMatches.get(i).getText());
		}
		return sb.toString();
	}

	public int getMinX() {
		return minX;
	}

	public String getTopologyX() {
		return xTopology;
	}

	public String getTopologyY() {
		return yTopology;
	}

	public Rectangle getBound() {
		return bound;
	}

	public Line getBaseline() {
		return baseline;
	}

	public void setClosure(boolean value) {
		closed = value;
	}

	public void computeProperties() {
		if (points.size() == 0) {
			return;
		}
		double sumX = 0;
		double sumY = 0;
		int Xmax = 0;
		int Ymax = 0;
		int Ymin = display.getHeight();
		int Xmin = display.getWidth();
		for ( Pos p : points) {
			sumX += p.x;
			sumY += p.y;
			Xmax = Xmax > p.x ? Xmax : p.x;
			Xmin = Xmin < p.x ? Xmin : p.x;
			Ymax = Ymax > p.y ? Ymax : p.y;
			Ymin = Ymin < p.y ? Ymin : p.y;
		}
		minX = Xmin;
		Point tl = new Point(Xmin,Ymin);
		Point bl = new Point(Xmin,Ymax);
		Point br = new Point(Xmax,Ymax);
		Point tr = new Point(Xmax,Ymin);
		Point[] corners = { tl , bl, br , tr};
		bound = new Rectangle( corners, null);
		baseline = new Line( bl, br );
		left = new Line( tl, bl );
		aspectRatio = left.length / baseline.length;
		centroid = new Point( sumX/points.size(), sumY/points.size());
		double area = baseline.length * left.length;
		double displayArea = display.getWidth() * display.getHeight();
		double normArea = area/displayArea;
		if (normArea > minNormArea && normArea < maxNormArea) { 
//			System.out.println("Segment shape with size="+points.size()+" center = "+centroid+" bound = "+bound);
			textSegments = doSegment();
			textMatches = classifier.getMatchingText(this);
		}
		else {
//			System.out.println("Skip shape with size="+points.size()+" center = "+centroid+" bound = "+bound+" norm area = "+normArea+" aspect = "+aspectRatio);
		}
	}

	public BufferedImage getImage() {
		return display;
	}

	public String toString() {
		return "size="+points.size()+" center = "+centroid+" bound = "+bound+" aspect = "+aspectRatio+" topology = "+xTopology+" "+yTopology+" "+getTextMatchesAsString();
	}

	private List<TextSegment> doSegment() {
		
		TransitionData xTransData = getTransitions(true);
		TopologyChanges xTopChanges = getTopologyChanges(xTransData);
		xTopology = computeTopology(xTransData, xTopChanges);
//		System.out.println("Got x topology: "+xTopology);
		List<TextSegment> xSegments = segmentScan(xTransData, xTopChanges, true);
//		System.out.println("Got "+xSegments.size()+" segments in x scan");
		for (TextSegment xseg : xSegments) {
//			System.out.println(xseg);
		}
		TransitionData yTransData = getTransitions(false);
		TopologyChanges yTopChanges = getTopologyChanges(yTransData);
		yTopology = computeTopology(yTransData, yTopChanges);
//		System.out.println("Got y topology: "+yTopology);
		List<TextSegment> ySegments = segmentScan(yTransData, yTopChanges, false);
//		System.out.println("Got "+ySegments.size()+" segments in y scan");
		for (TextSegment yseg : ySegments) {
//			System.out.println(yseg);
		}
		if (xSegments.size() > 0 && ySegments.size() > 0)  {
			if ( xSegments.size() < ySegments.size()) {
//				System.out.println("Using x segments");
				return xSegments;
			}
			else {
//				System.out.println("Using y segments");
				return ySegments;
			}
		}
		else if (xSegments.size() > 0) {
//			System.out.println("Using x segments");
			return xSegments;
		}
		else if (ySegments.size() > 0) {
//			System.out.println("Using y segments");
			return ySegments;
		}
		else {
//			System.out.println("No segments found");
			return null;
		}
	}

	private List<TextSegment> segmentScan(TransitionData transData, TopologyChanges topologyChange, boolean xdir) {
		List<TextSegment> segments = new ArrayList<TextSegment>();
		if (xdir) {
//			System.out.println("Performing segmentScan in x dir");
		}
		else {
//			System.out.println("Performing segmentScan in y dir");
		}
//		System.out.println("There are "+topologyChange.allChanges.size()+" topological regsions");
		for ( int t=0 ; t < topologyChange.allChanges.size() ; t++ ) {
			int begin = topologyChange.allChanges.get(t);			
			int end = t<topologyChange.allChanges.size()-1 ? topologyChange.allChanges.get(t+1) : transData.transList.size()-1;

			if (begin < end) {
				int numTrans = transData.transList.get(begin).size();
				
				int numBranches = numTrans/2;			
				List<SegmentPoint[]> points = new ArrayList<SegmentPoint[]>();	
//				System.out.println("Topological region "+t+" contains "+numBranches+" branches");		
				for ( int branch = 0 ; branch <numBranches ; branch++) {
					points.add(getPoints(transData, begin, end, branch, numBranches, xdir));	
//					System.out.println("Found "+ points.get(branch).length + " points for branch "+branch+" from "+begin+" to "+end);			
				}
/*			
			int numGap = numBranches - 1;
			List<double[]> gap = new ArrayList<double[]>();
			for ( int g = 0 ; g <numGap ; g++) {
				double[] gp = getGap(transData, begin, end, g);				
				gap.add(gp);
			}
			
			List<SegmentPoint[]> mergedPoints = findMerges(points);	
			for ( int b=0 ; b<mergedPoints.size() ; b++) {
				List<TextSegment> segs = splitBranch(mergedPoints.get(b), xdir);
				System.out.println("Branch "+b+" was split into "+segs.size()+" segments");
				segments.addAll(segs);
			}
*/
				for ( int b=0 ; b<points.size() ; b++) {
					List<TextSegment> segs = splitBranch(points.get(b), xdir);
//					System.out.println("Branch "+b+" was split into "+segs.size()+" segments");
					segments.addAll(segs);
				}
			}
		}
	
		return segments;
	}

	TextSegment createSegment(SegmentPoint[] points, int begin, int end, boolean xdir) {
/*		
		ArcSegment arc = new ArcSegment(points, begin, end, xdir);
		StraightSegment straight = new StraightSegment(points, begin, end, xdir);		
		if (arc.getCost() < straight.getCost()) {
			return arc;
		}
		else {
			return straight;
		}
*/
		return new StraightSegment(points, begin, end, xdir);	
	}

	//
	//  find pairs of branches which should be merged because they represent a
	//  single continuous curve
	//
	private List<SegmentPoint[]> findMerges(List<SegmentPoint[]> points) {
		Map<Integer, Integer> merges = new HashMap<Integer, Integer>();
		List<SegmentPoint[] > mergedPoints = new ArrayList<SegmentPoint[]>();
		for ( int b1 = 0 ; b1 < points.size() ; b1++ ) {
			SegmentPoint[] points1 = points.get(b1);
			SegmentPoint s1 = points1[0];
			SegmentPoint e1 = points1[points1.length-1];
			for ( int b2 = b1+1 ; b2 < points.size() ; b2++) {
				SegmentPoint[] points2 = points.get(b2);
				SegmentPoint s2 = points2[0];
				SegmentPoint e2 = points2[points2.length-1];
				if (segmentMergeMinDist > computeMinDist(s1,e1,s2,e2)) {
					if ( merges.get(b1) == null && merges.get(b2) == null) {
//						System.out.println("Merging branches "+b1+" and "+b2);
						merges.put(b1, b2);
						SegmentPoint[] mPts = new SegmentPoint[points1.length + points2.length];
						for ( int i=0 ; i<points1.length ; i++) {
							mPts[i] = points1[i];
						}
						for ( int i=0 ; i<points2.length ; i++) {
							mPts[i+points1.length] = points2[i];
						}
						mergedPoints.add( mPts );
					}
				}
			}
		}
		for (int b=0 ; b<points.size() ; b++) {
			Integer merged = merges.get(b);
			if (merged == null) {
				mergedPoints.add(points.get(b));  // add remaining branches that were not merged
			}
		}
		return mergedPoints; 
	}

	private double computeMinDist(SegmentPoint s1 , SegmentPoint e1, SegmentPoint s2, SegmentPoint e2) {
		Line l = new Line(s1.pos, e1.pos);
		double[] dist = new double[4];
		dist[0] = SegmentPoint.distance(s1,s2,l.length);
		dist[1] = SegmentPoint.distance(s1,e2,l.length);
		dist[2] = SegmentPoint.distance(e1,s2,l.length);
		dist[3] = SegmentPoint.distance(e1,e2,l.length);
		double minDist = dist[0];
		for ( int i=1 ; i<4 ; i++ ) {
			minDist = minDist > dist[i] ? dist[i] : minDist;
		}
		return minDist;
	}

	//
	//  find step changes in angle and/or thickness which mark where a branch  
	//  should be split into seperate segments
	//
	private List<TextSegment> splitBranch(SegmentPoint[] points, boolean xdir) {
		int i=0;
		int size = points.length;
		int prevSplit = 0;
		double dim = xdir ? left.length : baseline.length;
		List<Integer> splits = new ArrayList<Integer>();
		List<SegmentPoint> before = new ArrayList<SegmentPoint>();
		List<SegmentPoint> after = new ArrayList<SegmentPoint>();
		for ( i=1 ; i<size ; i++ ) {
			int prev = i-1;
			// FIXME: is it better to use a ratio rather than difference for thickness changes?
			double absDth = Math.abs(points[i].thickness - points[prev].thickness);
			double normDth = absDth/dim;
			double da = Math.abs(points[i].angle - points[prev].angle);
/*			
			if ((absDth > minBranchSplitThicknessChangeAbs && normDth > minBranchSplitThicknessChangeNorm) 
					|| da > minBranchSplitAngleChange) {
*/
			if (absDth > minBranchSplitThicknessChangeAbs && normDth > minBranchSplitThicknessChangeNorm) { // temp disable angle split						
				int len1 = prev - prevSplit + 1;
				int len2 = size - prev;
				if (len1 >= minSegmentLength && len2 >= minSegmentLength) {
//					System.out.println("Adding split at "+i+" because abs dth = "+absDth+" norm dth = "+normDth+" da = "+da);
					splits.add(prev);
					prevSplit = prev;
					before.add(points[prev]);
					after.add(points[i]);
				}
				else {
//					System.out.println("Ignoring split at "+i+ " because len1 = "+len1+"len2 = "+len2+" abs dth = "+absDth+" norm dth = "+normDth+" da = "+da);
				}
			}
		}
		prevSplit = 0;
		int currSplit = 0;
		List<TextSegment> segments = new ArrayList<TextSegment>();
		for ( i=0 ; i<splits.size() ; i++) {
			currSplit = splits.get(i);
			if (currSplit > prevSplit) {				
				TextSegment seg = createSegment(points, prevSplit, currSplit, xdir);
//				System.out.println("before = "+before.get(i)+" after = "+after.get(i)+" seg = "+seg);
				segments.add(seg);
				prevSplit = currSplit+1;
			}
		}
		if (prevSplit < points.length-1) {
			TextSegment seg = createSegment(points, prevSplit, points.length-1, xdir);
//			System.out.println("before = "+points[prevSplit]+" after = "+points[points.length-1]+" seg = "+seg);
			segments.add(seg);
		}
		
		
		return segments;
	}

	private Point getEndpoint(TransitionData transData, int index, int branch, boolean xdir) {
		double x,y;
		List<Double> trans = transData.transList.get(index);
		int trans1 = trans.size()-1 <= 2*branch ? trans.size()-2 : 2*branch;
		
		if (xdir) {
			x = transData.vals.get(index);
			y = transData.transList.get(index).get(trans1);			
		}
		else {
			y = transData.vals.get(index);
			x = transData.transList.get(index).get(trans1);
		}
		return new Point(x,y);
	}
	//
	// compute thickness along the segment
	//
	private SegmentPoint[] getPoints(TransitionData transData, int begin, int end, int branch, int numBranches, boolean xdir) {
		int size = end - begin;
//		System.out.println("get points begin = "+begin+" end = "+end+" size = "+size+" branch "+branch);
		SegmentPoint[] segPoints = new SegmentPoint[size];
		for ( int i=begin ; i<end ; i++ ) {
			int prev = i==begin ? i : i-1;
			int next = i==end-1 ? end-1 : i+1;

			double run = transData.vals.get(next) - transData.vals.get(prev);

			List<Double> trans = transData.transList.get(i);

			List<Double> prevTrans = transData.transList.get(prev);
			List<Double> nextTrans = transData.transList.get(next);			

			int trans1 = trans.size()-1 <= 2*branch ? trans.size()-2 : 2*branch;
			int next1 = nextTrans.size()-1 <= 2*branch ? nextTrans.size()-2 : 2*branch;
			int prev1 = prevTrans.size()-1 <= 2*branch ? prevTrans.size()-2 : 2*branch; 
			
			double thickness =  trans.get(trans1+1) - trans.get(trans1);
											
			double rise1 = nextTrans.get(next1) - prevTrans.get(prev1);
			double rise2 = nextTrans.get(next1+1) - prevTrans.get(prev1+1);
			double rise =  (rise1 + rise2)/2;
			double angle = xdir ? Math.atan2(rise, run) : Math.atan2(run, rise);			

			SegmentPoint p = new SegmentPoint( getEndpoint(transData, i, branch, xdir), angle, thickness );
//			System.out.println("Got point "+p);
			segPoints[i-begin] = p;
		}
		return segPoints;
	}
	//
	// compute gap between segments
	//
	private double[] getGap(TransitionData transData, int begin, int end, int g) {
		int size = end - begin;
		double[] gap = new double[size];
		for ( int i=begin ; i<end ; i++ ) {
			List<Double> trans = transData.transList.get(i);
			int g1 = trans.size()-1 <= 2*g+1 ?  trans.size()-2 : 2*g+1;
			gap[i] =  trans.get(g1+1) - trans.get(g1);
		}
		return gap;
	}

	//
	//  topology changes are defined as points where the number of segments as viewed from 
	//  a particular direction (either along the x or y axis) changes. For example when 
	//  looking at the letter M along the y axis (x is the indep var or xdir = true) the 
	//  number of segments is always 1. When looking along the x axis, there are 4. Because
	//  the thickness > 0, the yTopology for M is really 2:4:3 or 2:4:3:2 i.e. we start with 2 
	//  segments, split into 4 which then merge into 3.
	//
	private TopologyChanges getTopologyChanges(TransitionData transData) {
		List<Integer> filteredChanges = new ArrayList<Integer>();
		List<Integer> allChanges = new ArrayList<Integer>();
		int prevNumTrans = -1;
		int prevValidNumTrans = -1;
		int i=0;
		int size = transData.transList.size();
		int candidateTopologyChange = 0;
		int currNumTrans = -1;
		while ( i<size) {			
			currNumTrans = transData.transList.get(i).size();
			if (currNumTrans != prevNumTrans) {
				candidateTopologyChange = i;
				allChanges.add(i);
				int numTrans = currNumTrans;
//				System.out.println("Found candidate change "+prevNumTrans+" -> "+currNumTrans+ " at "+candidateTopologyChange);
				while (numTrans == currNumTrans && i<size-1) {
					i++;
					numTrans = transData.transList.get(i).size();
				}
				int candidateTopologyLen = i-candidateTopologyChange;
//				System.out.println("candidate len = "+candidateTopologyLen);
				//
				//  only record topology changes longer than the minimum size
				//
				if (candidateTopologyLen > minTopChangeLen) {
					if (prevValidNumTrans != currNumTrans) {
//						System.out.println("Adding candidate "+candidateTopologyChange);
						filteredChanges.add(candidateTopologyChange);
						prevValidNumTrans = currNumTrans;
					}
					else {
//						System.out.println("Reject candidate "+candidateTopologyChange+" because prev valid is "+prevValidNumTrans);
					}
				}
				else {
//					System.out.println("Reject candidate "+candidateTopologyChange+" because it is too short");
				}
			}
			else {
				i++;
			}
			prevNumTrans = currNumTrans;
		}
/*
		if (prevValidNumTrans != currNumTrans) {
			System.out.println("Adding candidate "+candidateTopologyChange);
			topologyChange.add(candidateTopologyChange);
		}
*/		
//		System.out.println("Found "+allChanges.size()+" total changes "+filteredChanges.size()+" filtered");
		return new TopologyChanges(allChanges, filteredChanges);
	}

	private String computeTopology(TransitionData transData, TopologyChanges topologyChange) {
		if (topologyChange.filteredChanges.size() > 0) {
			StringBuilder sb = new StringBuilder(10);
			int index = topologyChange.filteredChanges.get(0);
			int numTrans = transData.transList.get(index).size();
			int numSeg = numTrans/2;
			sb.append(numSeg);
			for ( int t=1 ; t < topologyChange.filteredChanges.size() ; t++ ) {
				index = topologyChange.filteredChanges.get(t);
				numTrans = transData.transList.get(index).size();
				numSeg = numTrans/2;
				sb.append(":"+numSeg);
			}
			return sb.toString();
		}
		return "NONE";
	}

	private TransitionData getTransitions(boolean xdir) {
		List<List<Double>> transList = new ArrayList<List<Double>>();
		List<Double> indepVals = new ArrayList<Double>();
		int i;
		double indepVar;
		double indepStart = xdir ? baseline.p1.x : left.p1.y;
		double indepEnd = xdir ? baseline.p2.x : left.p2.y;
		double depStart = xdir ? left.p1.y : baseline.p1.x;
		double depEnd = xdir ? left.p2.y : baseline.p2.x;
		for ( i=0, indepVar=indepStart ; indepVar<=indepEnd ; indepVar++, i++) {
			int prevValue = WHITE;			
			List<Double> trans = new ArrayList<Double>();
			int currValue = 0;
			double depVar;
			for ( depVar=depStart ; depVar<=depEnd ; depVar++) {
				currValue = xdir ? data[round(indepVar)][round(depVar)] : data[round(depVar)][round(indepVar)];
				if (currValue == id && prevValue == WHITE) {
					trans.add(depVar);
				}
				else if (currValue == WHITE && prevValue == id) {
					trans.add(depVar);
				}
				prevValue = currValue;
			}
			if (currValue == id) {
				trans.add(depVar);
			}
//			System.out.println("Adding trans of size "+trans.size()+" at i="+i);
			transList.add(trans);
			indepVals.add(indepVar);
		}
		return new TransitionData(transList, indepVals);
	}

	//
	//  matches the segments derived from a shape to the properties of a letter/digit
	//
	public static double matchSegments( TextProperties props, Rectangle bounds, List<TextSegment> segments) {
		List<List<Line>> strokeLists = props.getStrokes(bounds);
		double totalStrokeCoverage = 0.0;
		double totalStrokeLen = 0.0;
		double bestScore = 0.0;
		List<Double> matches = new ArrayList<Double>();
		Map< TextSegment, List<Line>> segmentCoverage = new HashMap<TextSegment, List<Line>>();
		for (List<Line> strokes :  strokeLists) {
//			System.out.println("  match stroke set...");
			for (Line stroke : strokes) {
				totalStrokeCoverage += matchBar(stroke, segments, bounds, segmentCoverage);
				totalStrokeLen += stroke.length;
			}
			double strokeScore = totalStrokeCoverage/totalStrokeLen;
			double segmentCoverageScore = getSegmentCoverageScore(segments, segmentCoverage);
			double score = (strokeScore + segmentCoverageScore)/2;
//			System.out.println("  stroke score = "+strokeScore+" seg cov score = "+segmentCoverageScore+" total ="+score);
			bestScore = score > bestScore ? score : bestScore;
		}
		
//		System.out.println("  best score is "+bestScore);
		return bestScore;
	}

	private static double matchBar( Line stroke, List<TextSegment> segments, Rectangle bounds, Map< TextSegment, List<Line>> segmentCoverage) {
		List<Line> coveredList = new ArrayList<Line>();
//		System.out.println("    Match stroke "+stroke);
		for ( TextSegment seg : segments) {
			if (seg instanceof StraightSegment) {
				StraightSegment straight = (StraightSegment)seg;
				LinePair covered = getCoverage(stroke, straight, bounds);
				if (covered != null) {
					if (covered.line1 != null) {
						coveredList.add(covered.line1);
					}
					if (covered.line2 != null) {
						List<Line> segCoveredList = segmentCoverage.get(seg);
						if (segCoveredList == null) {
							segCoveredList = new ArrayList<Line>();
						}
						segCoveredList.add(covered.line2);
						segmentCoverage.put( seg, segCoveredList);
					}
				}
			}
		}
		double combCov = getCombinedCoverage(coveredList);
//		System.out.println("    stroke cov combined = "+combCov);
		return combCov;
	}

	private static double getSegmentCoverageScore(List<TextSegment> segments, Map<TextSegment, List<Line>> segmentCoverage) {
		double sumSegmentLen = 0.0;
		double sumCoverage = 0.0;
		for ( TextSegment segment : segments ) {
			if ( segment instanceof StraightSegment) {
				StraightSegment straight = (StraightSegment)segment;
				Line l = new Line( straight.endpoint1, straight.endpoint2);
				sumSegmentLen += l.length;
			}
			sumCoverage += getCombinedCoverage(segmentCoverage.get(segment));		
		}
		return sumCoverage / sumSegmentLen;
	}

	private static double getCombinedCoverage(List<Line> coveredList) {
		if (coveredList == null) {
			return 0.0;
		}
		List<Line> combined = new ArrayList<Line>();
		for ( Line l1 : coveredList) {
			if (l1 == null) {
				System.out.println("ERROR: got null value in covered list");
			}
			if (l1.p1 == null || l1.p2 == null) {
				System.out.println("ERROR: got line with null endpoint in covered list");
			}
			boolean merged = false;
			for ( int i=0 ; i< combined.size() ; i++) {
				Line comboLine = Line.mergeLines( l1, combined.get(i));
				if (comboLine != null) {
					combined.set( i, comboLine);
					merged = true;
				}
			}				
			if ( merged == false) {
				combined.add(l1);
			}
		}
		double combinedCoverage = 0.0;
		for ( Line l : combined) {
			combinedCoverage += l.length;
		}
		return combinedCoverage;
	}
 
 	// FIXME: segments should be aligned with the stoke, also seg thickness is not accounted for
	private static LinePair getCoverage(Line stroke, StraightSegment seg, Rectangle bounds) {
		double maxDist = maxStrokeDistance*( bounds.getWidth() * Math.abs(Math.sin(stroke.angle)) + bounds.getHeight() * Math.abs(Math.cos( stroke.angle)) );
		double strokeDist1 = Line.getDistance(stroke, seg.endpoint1);
		double strokeDist2 = Line.getDistance(stroke, seg.endpoint2);
		double minStrokeDist = strokeDist1 > strokeDist2 ? strokeDist2 : strokeDist1;
		Line segLine = new Line( seg.endpoint1, seg.endpoint2);

		Line strokeCoverageLine = null;
		if ( minStrokeDist < maxDist) {			
			double q1 = stroke.projectScalar(seg.endpoint1);
			double q2 = stroke.projectScalar(seg.endpoint2);
			strokeCoverageLine = stroke.getCoverageLine(q1, q2);
//			System.out.println("    q1 ="+q1+" q2 = "+q2+" stroke cov line = "+strokeCoverageLine);
		}
		double segDist1 = Line.getDistance(segLine, stroke.p1);
		double segDist2 = Line.getDistance(segLine, stroke.p2);
		double minSegDist = segDist1 > segDist2 ? segDist2 : segDist1;
		Line segCoverageLine = null;
		if (minSegDist < maxDist) {
			double p1 = segLine.projectScalar(stroke.p1);
			double p2 = segLine.projectScalar(stroke.p2);
			segCoverageLine = segLine.getCoverageLine(p1,p2);
		}
//		System.out.println("       against segment "+seg.major+" thck= "+seg.thickness);
//		System.out.println("       strk d: "+minStrokeDist+" seg d: "+minSegDist+" max dist = "+maxDist);
		if (strokeCoverageLine != null) {
//			System.out.println("        strk cov = "+strokeCoverageLine.length);
		}
		else {
//			System.out.println("        strk cov = null");
		}
		if (segCoverageLine != null) {
//			System.out.println("        seg cov = "+segCoverageLine.length);
		}
		else {
//			System.out.println("        seg cov = null");
		}	
		return new LinePair( strokeCoverageLine, segCoverageLine);
	}

	public static List<TextShape> getShapes(BufferedImage input, ShapeGenMode mode, Logger logger) {
		int[][] working = imageToMatrix(input);
		BufferedImage display = copyInput(input);
		Graphics dg = display.createGraphics();	
		List<TextShape> shapes = new ArrayList<TextShape>();
		int id = 1;		
//		System.out.println("get shapes from image "+display.getWidth()+" x "+display.getHeight());
		for ( int x=0 ; x<display.getWidth() ; x++ ) {
			int prevValue = WHITE;
			for ( int y=0 ; y<display.getHeight() ; y++ ) {
				int value = working[x][y];
				switch (mode) {
				
				case BINBORDER:
					if (value == BLACK && prevValue == WHITE && y>0) {
//						System.out.println("scan new shape at "+x+" "+y+" black is "+BLACK+" white is "+WHITE);
						TextShape shape = getShape(working, display, x, y, id++, dg, mode, logger);
//						System.out.println("Add new shape "+shape);
						shapes.add(shape);
					}
					break;				
				case EDGESCAN:
					if (value == WHITE) {
						TextShape shape = getShape(working, display, x, y, id++, dg, mode, logger);
						shapes.add(shape);
					}
					break;
				case BINREGION:
				case BINREGIONRECURSIVE:
					if (value == BLACK) {
						TextShape shape = getShape(working, display, x, y, id++, dg, mode, logger);
						shapes.add(shape);
					}
					break;
				default:
					System.out.println("ERROR: unsupported mode"+mode);
				}
				prevValue = value;
			}
		}
		dg.dispose();
		return shapes;
	}

	private static TextShape getShape(int[][] working, BufferedImage display, int startx, int starty, 
					int id, Graphics dg, ShapeGenMode mode, Logger logger) {
		switch(mode) {
			case BINREGION:
				return getShapeRegion(working, display, startx, starty, id, dg, logger);
			case BINREGIONRECURSIVE:
				return getShapeRegionRecursive(working, display, startx, starty, id, dg, logger);
			case BINBORDER:
			case EDGESCAN:
				return getShapeBorder(working, display, startx, starty, id, dg, mode, logger);
			default:
				System.out.println("ERROR: unsupported mode"+mode);
		}
		return null;
	}

	private static TextShape getShapeRegion(int[][] working, BufferedImage display, int startx, int starty, 
					int id, Graphics dg, Logger logger) {
		List<Pos> open = new ArrayList<Pos>();
		TextShape shape = new TextShape(working, display, id, logger);
		Pos start = new Pos(startx, starty);
		open.add(start);
//		int itr = 0;
		while ( open.size() > 0) {			
			Pos p = open.remove(0);
			shape.add(p, dg);
//			StringBuilder sb = new StringBuilder(100);
//			sb.append(itr+" added:");
			for ( int z=0 ; z<delta.length ; z++ ) {
				Pos neighbor = addz(p,z);				
				if ( getValue(working, neighbor) == BLACK) {				
//					sb.append(neighbor+" ");
					open.add(neighbor);
					working[neighbor.x][neighbor.y] = id;					
				}
			}
//			System.out.println(sb);
//			System.out.println("end iter "+itr+" open size = "+open.size());
//			itr++;
		}
		shape.computeProperties();
		return shape;
	}

	private static TextShape getShapeRegionRecursive(int[][] working, BufferedImage display, int startx, int starty, 
					int id, Graphics dg, Logger logger) {

		TextShape shape = new TextShape(working, display, id, logger);
		Pos start = new Pos(startx, starty);
		GrowShapeRegion(working, shape, start, dg);
		shape.computeProperties();
		return shape;
	}

	private static void GrowShapeRegion(int[][] working, TextShape shape, Pos p, Graphics dg) {

		shape.add(p, dg);
		for ( int z=0 ; z<delta.length ; z++ ) {
			Pos neighbor = addz(p,z);
			if ( getValue(working, neighbor) == BLACK) {				
				GrowShapeRegion(working, shape, neighbor, dg);
			}
		}
	}

	private static TextShape getShapeBorder(int[][] working, BufferedImage display, int startx, int starty, 
					int id, Graphics dg, ShapeGenMode mode, Logger logger) {
		TextShape shape = new TextShape(working, display, id, logger);
		Pos orig = new Pos( startx, starty);
		Pos curr = orig;
		Pos prev = mode == ShapeGenMode.BINBORDER ? new Pos(curr.x, curr.y-1) : null;
		ScanState state = null;
		while (curr != null) {
			shape.add(curr, dg);
			state = mode == ShapeGenMode.BINBORDER ? getScanStateBinary(working, curr, prev, orig, id) : 
							 	getScanStateCanny(working, curr, prev, orig, id);
			prev = curr;
			curr = state.next;		
		}
		if (state != null) {
			shape.setClosure(state.closure);
		}
		shape.computeProperties();
		return shape;
	}
	//
	// just shoot me...
	//
	private static ScanState getScanStateBinary(int[][] working, Pos curr, Pos prev, Pos orig, int id) {
		int start=0;
//		System.out.println("curr = "+curr+"("+getValue(working,curr)+") prev = "+prev+"("+getValue(working,prev)+") id = "+id);		
		while ( !prev.equals(addz(curr,start))) {
//			System.out.println("prev = "+prev+" curr = "+curr+" curr+start = "+addz(curr,start));
			start++;
		}		
		int z = start;
		int value;
		if (getValue(working, prev)==id) {
			
			z = incz(z);
			while ( (value  = getValueZ(working, z,curr)) == BLACK || value == UNDEFINED) { // find first BLACK -> WHITE transition
//				System.out.println("value at z="+z+"("+addz(curr, z)+") is "+value);
				z = incz(z);
			}
//			System.out.println("value at prev = id as expected, z advanced to "+z+" value = "+value+" curr+z="+addz(curr, z));
		}
		else {
//			System.out.println("value at prev "+prev+" is unexpectedly "+getValue(working, prev)+" expected id ="+id);
		}
		value = getValueZ(working, z, curr);
		if ( value == id) {
			if (orig.equals(addz(curr, z))) {
				System.out.println("Got id at orig, returning closure");
				return new ScanState(true); // closure
			}
			else {
				System.out.println("Encountered border before returning to origin at curr = "+curr+" z="+z+" ("+addz(curr,z)+")");
			}
		}
		else if (value != WHITE) {
			System.out.println("Got unexpected value at curr = "+curr+" +z="+z+" ("+addz(curr,z)+")");
			return new ScanState(false);
		}
		while ( (value = getValueZ(working, z,curr)) == WHITE || value == UNDEFINED) { // find first WHITE -> BLACK transition
			z = incz(z);
		}
//		System.out.println("Skipped white values, z advanced to "+z+" value = "+value+" curr+z="+addz(curr, z));		
		if (value == BLACK) {
			int first = z;
			z = incz(z);
			value = getValueZ(working, z,curr);
			if (value == WHITE) {
//				System.out.println("hit black at z="+first+" curr+z="+addz(curr, first)+ " and hit white at z="+z+" ("+addz(curr,z)+")");
				while ( (value = getValueZ(working, z,curr)) == WHITE || value == UNDEFINED) { // find second WHITE -> BLACK transition
					z =incz(z);
				}

				if (value == BLACK) {
					System.out.println("Return second transition z="+z+" ("+addz(curr,z)+")");
					return new ScanState( addz(curr, z));
				}
			}
			else {
				return new ScanState( addz(curr, first));
			}
		}
		else if (value == id) {
			System.out.println("Hit dead end at curr = "+curr+" +z="+z+" ("+addz(curr,z)+")");
			return new ScanState(false);
		}
		System.out.println("Returning unexpectedly with curr = "+curr+" +z="+z+" ("+addz(curr,z)+")"+" value = "+value);
		return new ScanState(false);
	}

	private static int getValueZ(int[][] working, int z, Pos curr) {
		Pos p = addz(curr, z);
		return getValue( working, p);
	}

	private static int getValue(int[][] working, Pos p) {
		if ( p.x>=0 && p.y>=0 && p.x<working.length && p.y<working[0].length) {
			return working[p.x][p.y];
		}
		else {
			return UNDEFINED;
		}
	}

	private static Pos addz(Pos p, int z) {
		return Vec.add(p, delta[z]);
	}

	private static int incz(int z) {
		return z==delta.length-1 ? 0 : z+1;
	}

	private static ScanState getScanStateCanny(int[][] working, Pos curr, Pos prev, Pos orig, int id) {
//		System.out.println("curr = "+curr);
		if (prev != null) {
//			System.out.println("prev = "+prev);
		}
		int window = maxShapeGap+1;
		List<Pos> choices = new ArrayList<Pos>();
		for ( int x=curr.x-window ; x<=curr.x+window ; x++) {
			for ( int y=curr.y-window ; y<=curr.y+window ; y++) {
				if ( x>=0 && y>=0 && x<working.length && y<working[0].length) {
					if ( (x != curr.x || y != curr.y) && (prev == null || x != prev.x || y != prev.y)) {
						int value = working[x][y];
//						System.out.println("Got value "+value+" from "+x+" "+y);
						if (value == WHITE) {
//							System.out.println("Found new point at "+x+" "+y);
							choices.add(new Pos(x,y));
						}
						else if (value == id) { // closure
//							System.out.println("Found closure at "+x+" "+y);
							return new ScanState(true); 
						}
					}
				}
			}
		}
		if (choices.size() == 0) {
//			System.out.println("return with no next choice");
			return new ScanState(false);
		}
		else if (choices.size() == 1) {
//			System.out.println("return with unique next choice");
			return new ScanState(choices.get(0));
		}
		else { // make a choice
//			System.out.println("selecting next choice");
			return new ScanState(selectNext(choices, orig));
		}
	}

	private static Pos selectNext(List<Pos> choices, Pos orig) {
		double minDist = Pos.getDist(orig, choices.get(0));
		int mini = 0;
		for ( int i=1 ; i<choices.size() ; i++) {
			double dist = Pos.getDist(orig, choices.get(i));
			if (dist < minDist) {
				minDist = dist;
				mini = i;
			}
		}
		return choices.get(mini);
	}

	private static BufferedImage copyInput(BufferedImage input) {
		BufferedImage copy = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = copy.createGraphics();
		g.drawImage(input, 0, 0, null);
		g.dispose();
		return copy;
	}

	private static int[][] imageToMatrix(BufferedImage input) {
		int[][] matrix = new int[input.getWidth()][input.getHeight()];
		for ( int i=0 ; i<input.getWidth() ; i++ ) {
			for ( int j=0 ; j<input.getHeight() ; j++ ) {
				matrix[i][j] = input.getRGB(i,j);
			}
		}
		return matrix;
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}