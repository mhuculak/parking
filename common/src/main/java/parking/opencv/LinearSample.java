package parking.opencv;

import parking.display.MyGraph;

import java.awt.image.BufferedImage;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;


enum DerivClass {
	FLAT,
	RISE,
	FALL
}

enum SegClass {
	FORE,
	BACK
}

class SampleSegment {
	public int start;
	public int end;
	public int len;
	public double avgValue;
	public SegClass segClass;

	public SampleSegment(int start, int end, double[] dsamp) {
		this.start = start;
		this.end = end;
		len = end - start;
		avgValue = 0.0;
		for ( int i=start ; i<end ; i++ ) {
			avgValue += dsamp[i];
		}
		avgValue = avgValue / len;
	}
	public String toString() {
		return start+" "+end+" "+len+" "+avgValue;
	}
}

class Transition {
	public DerivClass derivClass;
	public int pos;

	public Transition(int pos, DerivClass derivClass) {
		this.pos = pos;
		this.derivClass = derivClass;
	}
}

public class LinearSample {

	private Line endpoints;
	private int size;
	private Intensity[] samples;
	private double[] dsamp;
	private List<Transition> transitions;
	private DerivClass[] derivClass;
	private List<SampleSegment> sampleSegments; 
	private boolean isHorizontal;
	private int window;
	private double derivThresh;
	private BufferedImage graph;
	private int start;
	private int end;
	private double dist;
	
	public LinearSample(Line endpoints, int window, double derivThresh, double dist) {
		this.endpoints = endpoints;
		this.window = window;
		this.derivThresh = derivThresh;
		this.dist = dist;
		double eps = 0.01;
		if (Math.abs(endpoints.p1.y - endpoints.p2.y) < eps) {
			size = Math.abs(round(endpoints.p1.x - endpoints.p2.x+1));
			start = endpoints.p1.x < endpoints.p2.x ? round(endpoints.p1.x) : round(endpoints.p2.x);
			end = endpoints.p1.x > endpoints.p2.x ? round(endpoints.p1.x) : round(endpoints.p2.x);
			isHorizontal = true;
		}
		else if (Math.abs(endpoints.p1.x - endpoints.p2.x) < eps) {
			isHorizontal = false;
			size = Math.abs(round(endpoints.p1.y - endpoints.p2.y+1));
			start = endpoints.p1.y < endpoints.p2.y ? round(endpoints.p1.y) : round(endpoints.p2.y);
			end = endpoints.p1.y > endpoints.p2.y ? round(endpoints.p1.y) : round(endpoints.p2.y);
		}
		else {
			System.out.println("ERROR: linear sample must be either horizontal or vertical "+endpoints);
		}
//		System.out.println("size = " + size + " for " + endpoints);
		int delta = end - start;
		size = size > delta ? size : delta;
		samples = new Intensity[size];
	}

	public void findSegments() {
		sampleSegments = new ArrayList<SampleSegment>();
		Transition prevTrans = new Transition(0,DerivClass.FLAT);
		for (Transition trans : transitions) {
			SampleSegment seg = new SampleSegment(prevTrans.pos, trans.pos, dsamp);
			sampleSegments.add(seg);
//			System.out.println("Got seg "+seg);
			prevTrans = trans;
		}
		if (sampleSegments.size()==0) {
			return;
		}
		double max = 0.0;
		int maxPos = 0;
		int i=0;
		for (SampleSegment seg : sampleSegments) {
			if (seg.avgValue > max) {
				max = seg.avgValue;
				maxPos = i;
			}  	
			i++;
		}
		SampleSegment maxSeg = sampleSegments.get(maxPos);
		maxSeg.segClass = SegClass.BACK;
		for ( i=maxPos ; i<sampleSegments.size()-1 ; i++) {
			SampleSegment seg = sampleSegments.get(i);
			SampleSegment next = sampleSegments.get(i+1);
			if (seg.segClass == SegClass.BACK) {
				next.segClass = SegClass.FORE;
			}
			else {
				next.segClass = SegClass.BACK;
			}
		}
		for (i=maxPos ; i>0 ; i--) {
			SampleSegment seg = sampleSegments.get(i);
			SampleSegment next = sampleSegments.get(i-1);
			if (seg.segClass == SegClass.BACK) {
				next.segClass = SegClass.FORE;
			}
			else {
				next.segClass = SegClass.BACK;
			}
		} 

	}

	public void findTransitions() {
		double[] derivative = new double[size];
		derivClass = new DerivClass[size];
		int i;
		double maxSlope = 0.0;
		double sumSlope = 0.0;
		for ( i=0 ; i<size ; i++) {
			derivative[i] = computeSlope(i);
			double aslope = Math.abs(derivative[i]);
			sumSlope += aslope;
			maxSlope = maxSlope < aslope ? aslope : maxSlope;
//			System.out.println("Got slope " + derivative[i] + " at " + i);
			if (Math.abs(derivative[i]) < derivThresh) {
				derivClass[i] = DerivClass.FLAT;
			}
			else if (derivative[i] < 0.0) {
				derivClass[i] = DerivClass.FALL;
			}
			else if (derivative[i] > 0.0) {
				derivClass[i] = DerivClass.RISE;
			}
			else {
				System.out.println("ERROR: what happened?");
			}
		}
		double avg = sumSlope/size;
//		System.out.println("max slope is " + maxSlope + " avg = " + avg);
//		MyGraph my = new MyGraph( dsamp, derivative );
//		graph = my.getGraph();	
		DerivClass currentTrans = DerivClass.FLAT;
		DerivClass prevTrans = DerivClass.FLAT;
		transitions = new ArrayList<Transition>();
		int numTrans = 0;
		double peak = 0.0;
		int peakPos = 0;
		for (i=0 ; i<size-1 ; i++) {
			int next = i+1;
			if (Math.abs(derivative[i]) > peak) {
				peak = Math.abs(derivative[i]);
				peakPos = i;
			}
			if ((derivClass[i]==DerivClass.RISE && derivClass[next]==DerivClass.FALL) || 
				(derivClass[next]==DerivClass.RISE && derivClass[i]==DerivClass.FALL)) {
//				System.out.println("WARNING: rapid changes encountered for threshold " + derivThresh);
			}
			if (derivClass[i] != derivClass[next] && derivClass[next] == DerivClass.FLAT) {	
				if (currentTrans != derivClass[i]) {
					prevTrans = currentTrans;			
					currentTrans = derivClass[i];
					transitions.add(new Transition(peakPos,currentTrans));
					peak = 0;
					numTrans++;
//					System.out.println("Found " + currentTrans + " transition at " + peakPos + " current = " + prevTrans);
				}
				else {
//					System.out.println("Skipping transition "+ derivClass[i] + " at " + peakPos + " because curr = "+ currentTrans);
				}
			}
		}
		
	}

	private double computeSlope(int pos) {
		int len = 2*window+1;
		double[] l = new double[len];
		double[] value = new double[len];
		int i=0;
		int z=0;
		StringBuilder sb = new StringBuilder(100);

		for ( i=0, z=pos-window ; z<=pos+window; z++, i++) {
			if (z<0) {
				value[i] = samples[0].val();
			}
			else if (z>=size) {
				value[i] = samples[size-1].val();
			}
			else {
				value[i] = samples[z].val();
			}
			l[i] = z;
			sb.append(l[i]+" "+value[i]+",");
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

	public void getSamples(BufferedImage image) {
		dsamp = new double[size];
		int i=0;
//		try {
		for ( i=0 ; i<size ; i++) {
			samples[i] = new Intensity(0.0);
			dsamp[i] = samples[i].val();
		}	
		if (isHorizontal) {			
			int x;
			int iy = round(endpoints.p1.y);			
			for ( i=0, x=start ; x<end ; x++, i++) {
				if (x>=0 && iy>=0 && x<image.getWidth() && iy<image.getHeight()) {
					samples[i] = ImageBorderParams.getIntensity(new Color(image.getRGB(x, iy)));
					dsamp[i] = samples[i].val();
				}						 
			}			
		}
		else {			
			int y;
			int ix = round(endpoints.p1.x);
			for ( i=0, y=start ; y<end ; y++, i++) {
				if (ix>=0 && y>=0 && ix<image.getWidth() && y<image.getHeight()) {
					samples[i] = ImageBorderParams.getIntensity(new Color(image.getRGB(ix, y)));
					dsamp[i] = samples[i].val();
				}								
			}
		}
/*		
		}
		catch (Exception ex) {
			System.out.println("i="+i+" start="+start+" end="+end+" size="+size+"samp len="+samples.length+" "+dsamp.length);
			ex.printStackTrace();
		}
*/

//		MyGraph my = new MyGraph( dsamp );
//		graph = my.getGraph();
	}

	public int getNumTransitions(double distRatio) {
		int dist = round(size*distRatio);
		int num = 0;
		for (Transition trans : transitions) {
			if (trans.pos > dist && trans.pos < size-dist) {
				num++;
			}
		}
		return num;
	}

	public int getNumTrans(int start, int end) {
		int num = 0;
		for (Transition trans : transitions) {
			if (trans.pos > start && trans.pos < end) {
				num++;
			}
		}
		return num;
	}

	public boolean allBackGround(double distRatio) {
		int start = round(size*distRatio);
		int end = size-start;
		int count = 0;
		for ( int i=0 ; i<sampleSegments.size() ; i++) {
			SampleSegment seg = sampleSegments.get(i);
			if (seg.start >= start && seg.end <= end && seg.segClass == SegClass.BACK) {
				count++;
			}
		}
		return count==1;
	}

	public double getDist() {
		return dist;
	}

	public Line getEndpoints() {
		return endpoints;
	}

	public BufferedImage getGraph() {
		return graph;
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}