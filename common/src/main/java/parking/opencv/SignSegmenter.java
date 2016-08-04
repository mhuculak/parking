package parking.opencv;

import parking.display.MyGraph;

import org.opencv.core.Point;

import java.awt.image.BufferedImage;

import java.util.List;
import java.util.ArrayList;

enum TransSegClass {
	BUSY,
	OPEN
}

class TransSegment {
	public int start;
	public int end;
	public int len;
	public double avgValue;
	public TransSegClass segClass;

	public TransSegment(int start, int end, int[] numTrans, double thresh) {
		this.start = start;
		this.end = end;
		len = end - start;
		avgValue = 0.0;
		for ( int i=start ; i<end ; i++ ) {
			avgValue += numTrans[i];
		}
		avgValue = avgValue / len;
		if (avgValue < thresh) {
			segClass = TransSegClass.OPEN;
		}
		else {
			segClass = TransSegClass.BUSY;
		}
	}

	public double getNormalizedLen(int dim) {
		double dlen = len;
		return dlen/dim;
	}

	public String toString() {
		return start+" "+end+" "+len+" "+avgValue+" "+segClass;
	}
}

public class SignSegmenter {
	private Rectangle border;
	private BufferedImage image;
	private List<LinearSample> hSamples;
	private List<LinearSample> vSamples;
	private BufferedImage graph;
	private List<Line> sampleLines;
	private Line horAxis;
	private Line vertAxis;
	private List<TransSegment> vSegments;
	private int vertSide;
	private int horSide;
	private int vertAxisSize;
	private int horAxisSize;
	private List<Rectangle> textBorders;

	private final int numTransBusyThresh = 3;
	private final double numTransBorderThresh = 0.1;
	private final double minSegLen = 0.02;


	public SignSegmenter(BufferedImage image, Rectangle border) {
		this.image = image;
		this.border = border;
		sampleLines = new ArrayList<Line>();
		graph = null;
	}

	public void getSegments() {
		getAxes();
		getSamples();
		findTextBorders();
	}
		
	private void findTextBorders() {
		textBorders = new ArrayList<Rectangle>();
		int[] numTransH = new int[vertAxisSize];
		double[] deriv = new double[vertAxisSize];
		int i=0;
		for ( LinearSample sample : hSamples ) {
			numTransH[i++] = sample.getNumTransitions(numTransBorderThresh);
		}
		double maxDeriv = 0.0;
		double avgDeriv = 0.0;
		for ( i=0 ; i<numTransH.length ; i++) {
			deriv[i] = computeSlope(i, numTransH, 3);
			maxDeriv = maxDeriv < deriv[i] ? deriv[i] : maxDeriv;
			avgDeriv += deriv[i];
		}
		avgDeriv = avgDeriv / numTransH.length;
//		System.out.println("max deriv is "+maxDeriv+" avg = "+avgDeriv);
		for ( i=0 ; i<numTransH.length ; i++) {
			deriv[i] = deriv[i] / maxDeriv;
		}

		vSegments = segmentWithThresh( numTransH, deriv, numTransBusyThresh, 0.5);
//		MyGraph grph = new MyGraph( numTransH, deriv);
//		graph = grph.getGraph();
		for (TransSegment seg : vSegments) {
			if (seg.segClass == TransSegClass.BUSY) {
				if (seg.getNormalizedLen(image.getHeight()) > minSegLen) {
					double leftBound = findBoundH(0.01, 0.5, seg.start, seg.end, 0.1);
					double rightBound = findBoundH(0.5, 0.99, seg.start, seg.end, 0.9);
					Line top = hSamples.get(seg.start).getEndpoints();
					Line bottom = hSamples.get(seg.end).getEndpoints();
					Point[] bounds = new Point[4];
					bounds[0] = top.findPoint(leftBound);
					bounds[1] = top.findPoint(rightBound);
					bounds[2] = bottom.findPoint(rightBound);
					bounds[3] = bottom.findPoint(leftBound);
					textBorders.add(new Rectangle(bounds, null));
				}
				else {
//					System.out.println("Skip BUSY seg of len " + seg.len);
				}
			}
		}
	}

	private List<TransSegment> segmentWithThresh( int[] values, double[] deriv, int threshold, double derivThresh) {
		List<TransSegment> segments = new ArrayList<TransSegment>();
		int currSegVal = values[0];
		int currSegStart = 0;
		TransSegment currentSeg = null;
		int i=0;
		do {
			do {
				i++;				
			} while ( sameSeg(i, values, deriv, currSegVal, threshold, derivThresh) && i < values.length-3);
			currentSeg= new TransSegment(currSegStart, i, values, threshold);
//			System.out.println("Found segment "+currentSeg);
			currSegStart = i;
			currSegVal = values[i];
			segments.add(currentSeg);
		} while( i < values.length-2);
		return segments;
	}

	private double findBoundH(double hStart, double hEnd, int vStart, int vEnd, double defaultBoundary) {
		
		int count = 0;
		int first = -1;
		int i=0;
		for ( LinearSample samp : vSamples ) {
			if (samp.getDist() > hStart && samp.getDist() < hEnd) {				
				count++;
				first = first < 0 ? i : first;
			}
			i++;
		}
		int[] numTrans = new int[count];		
		for ( i=0 ; i<count ; i++ ) {
			numTrans[i] = vSamples.get(i+first).getNumTrans(vStart, vEnd);
		}
		double[] deriv = new double[count];
		double maxDeriv = 0;
		for ( i=0 ; i<count ; i++) {
			deriv[i] = computeSlope(i, numTrans, 3);
			maxDeriv = maxDeriv > Math.abs(deriv[i]) ? maxDeriv : Math.abs(deriv[i]);
		}
		for ( i=0 ; i<count ; i++) {
			deriv[i] = deriv[i] / maxDeriv;
		}
		List<TransSegment> segs = segmentWithThresh(numTrans, deriv, numTransBusyThresh, 0.5);
		TransSegment maxOpen = null;
		for (TransSegment seg : segs) {
			if (seg.segClass == TransSegClass.OPEN && (maxOpen == null || seg.len > maxOpen.len)) {
				maxOpen = seg;
			}
		}
		if (maxOpen != null) {
			double dbound = maxOpen.end;
			dbound = dbound / numTrans.length;
			return hStart + dbound*(hEnd-hStart);
		}
		return defaultBoundary;	// failure	
	}

	private boolean sameSeg(int pos, int[] values, double[] deriv, int baseValue, int threshold, double derivThresh) {
		if (Math.abs(deriv[pos]) > derivThresh) {
			int next = pos+1;
			if (next < deriv.length-1 && Math.abs(deriv[next]) <= derivThresh) {
//				System.out.println("deriv "+deriv[pos]+" is above threshold");
				return false;
			}
		}
		if ( baseValue < threshold && values[pos] < threshold ) {
			return true;
		}
		else if ( baseValue >= threshold && values[pos] >= threshold ) {
			return true;
		}
		return false;
	}

	private void getSamples() {	
		int i;
		Line[] side = border.getSides();
		hSamples = new ArrayList<LinearSample>();
		int numClear = 0;
		int numBack = 0;
		vertAxisSize = round(vertAxis.p2.y-vertAxis.p1.y+1);
		double x,y;
		for ( i=0, y=vertAxis.p1.y ; y<=vertAxis.p2.y ; y++, i++) {
			double dist = i;
			dist = dist/vertAxisSize;
			double x1 = side[vertSide].getX(y);
			double x2 = side[vertSide+2].getX(y); 
			Line sampleLine = new Line(new Point(x1,y), new Point(x2,y));
			LinearSample linSample = new LinearSample(sampleLine, 5, 0.01, dist);
			linSample.getSamples(image);			
			linSample.findTransitions();
			linSample.findSegments();
//			graph = linSample.getGraph();
//			sampleLines.add(sampleLine);
			hSamples.add(linSample);
			
			if (linSample.getNumTransitions(0.1)==0) {
				numClear++;
			}
			if(linSample.allBackGround(0.1)) {
				numBack++;
			}
		}
		System.out.println("Got " + numClear + " clear and " + numBack + " background horizontal samples");
		vSamples = new ArrayList<LinearSample>();
		numClear = 0;
		numBack = 0;
		horAxisSize = round(horAxis.p2.x-horAxis.p1.x+1);
		int[] numTransV = new int[horAxisSize];		
		for ( i=0, x=horAxis.p1.x ; x<=horAxis.p2.x ; x++, i++) {
			double dist = i;
			dist = dist/horAxisSize;
			double y1 = side[horSide].getX(x);
			double y2 = side[horSide+2].getX(x); 
			Line sampleLine = new Line(new Point(x,y1), new Point(x,y2));
			LinearSample linSample = new LinearSample(sampleLine, 5, 0.01, dist);
			linSample.getSamples(image);			
			linSample.findTransitions();
			linSample.findSegments();
			vSamples.add(linSample);
			numTransV[i] = linSample.getNumTransitions(0.1);
			
			if (linSample.getNumTransitions(0.2)==0) {
				numClear++;
			}
			if(linSample.allBackGround(0.2)) {
				numBack++;
			}
		}
		
		System.out.println("Got " + numClear + " clear and " + numBack + " background vertcal samples");		
	}

	private void getAxes() {
		int i;
		Line[] side = border.getSides();
		
		for ( i=0 ; i<2 ; i++) {
			Point p1 = side[i].findPoint(0.5);
			Line l;
			Point p2;
			if (Math.abs(Math.sin(side[i].angle)) < 1.0/Math.sqrt(2)) {
				horSide = i;
			 	l = new Line(p1, new Point(p1.x, p1.y+10));
			 	p2 = Line.getIntersection(l, side[i+2]);
			 	if (p1.y < p2.y) {
			 		vertAxis = new Line(p1, p2);
			 	}
			 	else {
			 		vertAxis = new Line(p2, p1);
			 	}
			}
			else {
				vertSide = i;
			 	l = new Line(p1, new Point(p1.x+10, p1.y));
			 	p2 = Line.getIntersection(l, side[i+2]);
			 	if (p1.x < p2.x) {
			 		horAxis = new Line(p1, p2);
			 	}
			 	else {
			 		horAxis = new Line(p2, p1);
			 	}
			}
		}
	}

	public double computeSlope(int pos, int[] data, int window) {
		int len = 2*window+1;
		double[] l = new double[len];
		double[] value = new double[len];
		int i=0;
		int z=0;
//		StringBuilder sb = new StringBuilder(100);

		for ( i=0, z=pos-window ; z<=pos+window; z++, i++) {
			if (z<0) {
				value[i] = data[0];
			}
			else if (z>=data.length) {
				value[i] = data[data.length-1];
			}
			else {
				value[i] = data[z];
			}
			l[i] = z;
//			sb.append(l[i]+" "+value[i]+",");
		}
			
		TrendLine trendLine = new PolyTrendLine(2);
		trendLine.setValues(value, l);
//		System.out.println("adding points "+sb);
		int s = pos-window;
		int e = pos+window;
		double sval = trendLine.predict(pos-window);
		double eval = trendLine.predict(pos+window);
//		System.out.println("start "+s+" predict "+sval+" end "+e+" predict "+eval);
		return (eval-sval)/len;
	}

	public List<Rectangle> getTextBorders() {
		return textBorders;
	}

	public BufferedImage getGraph() {
		return graph;
	}

	public List<Line> getSampleLines() {
		return sampleLines;
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}