package parking.opencv;

import parking.display.SignImage;
import parking.util.Logger;
import parking.util.LoggingTag;

import org.opencv.core.Point;

import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Line2D;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class ScoreWindow {
	public int lower;
	public int upper;

	public ScoreWindow(int lower, int upper) {
		this.lower = lower;
		this.upper = upper;
	}

	public boolean within(int limit) {
		if (Math.abs(lower) < limit && upper < limit) {
			return true;
		}
		return false;
	}
}
class ScoreStats {
	public double max;
	public int maxi;
	public Point maxPos;
	public ScoreWindow window;
	
	public ScoreStats(double max, int maxi, Point maxPos, ScoreWindow window) {
		this.max = max;
		this.maxi = maxi;
		this.window = window;
		this.maxPos = maxPos;
	}
	public String toString() {
		if (maxPos == null) {
			return max+" "+maxi+" null "+window.lower+" "+window.upper;
		}
		else {
			return max+" "+maxi+" "+maxPos.x+","+maxPos.y+" "+window.lower+" "+window.upper;
		}
	}
	public ScoreWindow next() {
		if (maxi==0 && window.lower<0) {
			return new ScoreWindow(2*window.lower, window.lower );
		}
		else if (maxi == window.upper - window.lower-1 && window.upper>0) {
			return new ScoreWindow(window.upper, 2*window.upper);
		}
		else {
			return null; // convered to a maximum
		}
	}


}

class EdgeComparator implements Comparator<EdgeBorder> {
	@Override
	public int compare(EdgeBorder e1, EdgeBorder e2) {
		if (e1.getScore() > e2.getScore()) {
			return -1;
		}
		else if (e1.getScore() < e2.getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

public class EdgeBorder {

	private BufferedImage image;
	private Point position;
	private Point origPos;
	private double thickness;
	private double threshold;
	private Line edge;
	private Line origEdge;
	private double score;
	private Vector disp;

	public EdgeBorder(BufferedImage image, Point position, Line edge, double thickness, double threshold) {
		this.image = image;
		this.position = position;		
		this.thickness = thickness;
		this.edge = edge;
		this.threshold = threshold;
		edge.threshold = threshold;
	}

	public Point getPosition() {
		return position;
	}

	public BufferedImage getImage() {
		return image;
	}

	public double getThickness() {
		return thickness;
	}

	public double getThreshold() {
		return threshold;
	}

	public Line getEdge() {
		return edge;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
		edge.score = score;
	}

	public void changePosition(Point p) {
		origPos = position;
		position = p;
		disp = new Vector(origPos, p);
		origEdge = edge;
		edge = Line.add(origEdge, disp);
		edge.score = score;
	}

	public String toString() {
		if (disp != null) {
			return edge.toString() + " " + disp.getLength() + " " + thickness + " " + score;
		}
		else {
			return edge.toString() + " 0.0 " + thickness + " " + score;
		}
	}

	public void displayEdge(BufferedImage img, Color color) {
		Graphics g = img.createGraphics();
		g.drawImage(image, round(position.x), round(position.y), null);		
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(color);        		
	    Shape l = new Line2D.Double(edge.p1.x, edge.p1.y, edge.p2.x, edge.p2.y);
        g2.draw(l);
        g.dispose();
	}

	public static List<EdgeBorder> getEdges(SignImage image, List<Line> lines) {
		Logger logger = new Logger(LoggingTag.Border, "EdgeBorder", "getEdges");
		List<EdgeBorder> edges = new ArrayList<EdgeBorder>();
		double maxScore = 0.0;
		for (Line line : lines) {
			EdgeBorder edge = createEdgeBorder(image.getImage(), line);
			if (edge != null) {
				computeScore(image.getImage(), edge);
				edges.add(edge);
				maxScore = edge.getScore() > maxScore ? edge.getScore() : maxScore;
			}
		}
		EdgeComparator comparator = new EdgeComparator();
		Collections.sort(edges, comparator);
		logger.log("Found " + edges.size() + " edges");		
		for (EdgeBorder e : edges) {
//			e.setScore(e.getScore()/maxScore);
			e.setScore(e.getScore());
//			logger.log(e.toString());
		}
		
		return edges;
	}

	public static double computeScore(BufferedImage image, EdgeBorder edgeBorder) {
		final int initWindowSize = 10;
		final int maxWindowSize = 100;
		ScoreStats ss = null;
		double initScore = computeScoreAtPoint(image, edgeBorder, edgeBorder.getPosition());
		edgeBorder.setScore(initScore);
//		System.out.println("Compute score for border edge "+edgeBorder.getEdge()+" image "+edgeBorder.getImage().getWidth()+" x "+
//			edgeBorder.getImage().getHeight()+" @ "+edgeBorder.getPosition()+" init score = "+initScore);
		do {
			ScoreWindow window = ss == null ? new ScoreWindow(-1*initWindowSize, initWindowSize) : ss.next();			
			ss = computeScoreWindow(image, edgeBorder, window);
//			System.out.println(ss);
		} while (ss.next() != null && ss.next().within(maxWindowSize));
		edgeBorder.setScore(ss.max);
		if (ss.maxPos != null) {
			edgeBorder.changePosition(ss.maxPos);
		}
//		System.out.println("Edge adjusted to "+edgeBorder);		
		return ss.max;
	}

	private static ScoreStats computeScoreWindow(BufferedImage image, EdgeBorder edgeBorder, ScoreWindow window) {
		int winLen = window.upper - window.lower;
		double[] score = new double[winLen+1];
		StringBuilder sb = new StringBuilder(10);		
		double max = 0.0;
		Point maxPos = null;
		int maxi = 0;
		int i;
		double x,y;
		if (Math.abs(Math.sin(edgeBorder.getEdge().angle)) > 1.0 / Math.sqrt(2.0) ) {

			for ( i=0, x = edgeBorder.getPosition().x+window.lower ; x<edgeBorder.getPosition().x+window.upper ; x++, i++ ) {
				Point p = new Point(x, edgeBorder.getPosition().y);
				score[i] = computeScoreAtPoint(image, edgeBorder, p);
				sb.append(score[i]+" ");
				if (max < score[i]) {
					max = score[i];
					maxPos = p;
					maxi = i;
				}				
			}
		}
		else {
			for ( i=0, y = edgeBorder.getPosition().y+window.lower ; y<edgeBorder.getPosition().y+window.upper ; y++, i++ ) {
				Point p = new Point(edgeBorder.getPosition().x, y);
				score[i] = computeScoreAtPoint(image, edgeBorder, p);
				sb.append(score[i]+" ");
				if (max < score[i]) {
					max = score[i];
					maxPos = p;
					maxi = i;
				}
			}
		}
//		System.out.println(sb+" max = "+max+" maxi="+maxi);
//		System.out.println(getSlope(score));

		return new ScoreStats(max, maxi, maxPos, window);

	}

	private static String getSlope(double[] scores) {
		int len = scores.length;
		double[] slope = new double[len];
		StringBuilder sb = new StringBuilder(10);
		for ( int i=0 ; i<len ; i++ ) {
			if ( i<len-1) {
				slope[i] = scores[i+1] - scores[i];
				sb.append(slope[i]+" ");
			}
		}
		return sb.toString();
	}
	
	private static double computeScoreAtPoint(BufferedImage image, EdgeBorder edgeBorder, Point pos) {
		BufferedImage bimage = edgeBorder.getImage();		
		double score = 0.0;
		double maxScore = 0.0;
		int k=0;
		int kmax = 0;
		for ( int x=0 ; x<bimage.getWidth() ; x++) {
			for ( int y=0 ; y<bimage.getHeight() ; y++ ) {
				int xi = round(pos.x + x);
				int yi = round(pos.y + y);	
				Color bVal = new Color(bimage.getRGB(x,y));			
				if (xi>=0 && xi < image.getWidth() && yi>=0 && yi < image.getHeight() && bVal.getAlpha() > 0) {				
					Color iVal = new Color(image.getRGB(xi,yi));
					score += getIntensityDiff(iVal,bVal);
//					score += getColorCloseness(iVal,bVal);
					maxScore += 1.0;
					k++;
				}
				kmax++;
			}
			
		}
//		System.out.println(k+" of "+kmax+" points found in image");
/*		
		if (maxScore > 0.0) {			
			double normalScore = score/maxScore;
			edgeBorder.setScore(normalScore);
			return normalScore;
		}
		else {
			return 0.0;
		}
*/
		return score;		
	}

	private static double getIntensityDiff(Color iVal, Color bVal) {
		return 1.0 - Math.abs(getIntensity(iVal) - getIntensity(bVal));
	}

	private static double getIntensity(Color color) {
		double intensity = Math.sqrt(color.getRed()*color.getRed() + color.getBlue()*color.getBlue() + color.getGreen()*color.getGreen());
		return intensity/(255 * Math.sqrt(3));
	}

	private static double getColorCloseness(Color iVal, Color bVal) {
		double blueScore = 1.0 - Math.abs(iVal.getBlue() - bVal.getBlue())/255.0;
		double greenScore = 1.0 - Math.abs(iVal.getGreen() - bVal.getGreen())/255.0;
		double redScore = 1.0 - Math.abs(iVal.getRed() - bVal.getRed())/255.0;
//		System.out.println(iVal.getRed()+" "+bVal.getRed()+":"+iVal.getBlue()+" "+bVal.getBlue()+":"+iVal.getGreen()+" "+bVal.getGreen());
		return (redScore + blueScore + greenScore)/3;
	}

	//
	//  FIXME: sometimes the edge is perp to the mask image
	//
	public static EdgeBorder createEdgeBorder(BufferedImage image, Line line) {
		
		final double cornerRatio = 0.1; // ratio of sign edge taken up by the rounded corner of the sign
		final double bimageRatio = 0.1;
		final double aspectRatio = 0.67;
		final Color defaultSignColor = new Color(0xffffffff);
		final Color defaultBorderColor = new Color(0xff000000);
		final Color transparent = new Color(0);
//		System.out.println("Create edge border for " + line);
		List<ImageBorderParams> params = new ArrayList<ImageBorderParams>();
		int found = 0;
		double tSum = 0.0;
		double dSum = 0.0;
		double fSum = 0.0;
		double bSum = 0.0;
		double threshSum = 0.0;
		for ( double pp = 0.1 ; pp < 1.0 ; pp += 0.1) {
			ImageBorderParams ibp = new ImageBorderParams(line, pp);
			int trans = ibp.compute(image);
//			System.out.println(ibp);
			if (trans == 2) {
				found++;
				tSum += ibp.getThickness();
				dSum += ibp.getDisplacement();
				fSum += ibp.getForeground();
				bSum += ibp.getBackground();
				threshSum += ibp.getThreshold();
			}
		}

		if (found == 0) {
			return null;
		}

		double t = tSum / found;
		if ( t < 1.0) {
			return null;
		}
		double displacement = dSum / found;
//		System.out.println("displacement is "+ displacement);
		Double fg = new Double(fSum / found);
		Double bg = new Double(bSum / found);
		float foreground = fg.floatValue();
		float background = bg.floatValue();
		double threshold = threshSum / found;

//		System.out.println("Set background to " + background);
		final Color signColor = new Color(background, background, background);
		final Color borderColor = new Color(foreground, foreground, foreground);
			
		double h = line.p2.y - line.p1.y;
		double w = line.p2.x - line.p1.x;
		double hw = h*w;
		
		int height, width;
		double x,y; // FIXME: need to account for the corners!
		Line adjusted = null;
		if (Math.abs(w) > Math.abs(h) ) {
			adjusted = Line.add(line, new Vector(0.0, displacement));
		}
		else {
			adjusted = Line.add(line, new Vector(displacement, 0.0));
		}
		Point p1 = adjusted.findPoint(cornerRatio);
		Point p2 = adjusted.findPoint(1.0-cornerRatio);
		h = p2.y - p1.y;
		w = p2.x - p1.x;
		if (Math.abs(w) > Math.abs(h) ) {			
			height = round(3*t + Math.abs(h));
			width = Math.abs(round(w));			
		}
		else {
			width = round(3*t + Math.abs(w));
			height = Math.abs(round(h));
		}
		
		BufferedImage bimage = new BufferedImage ( width, height, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g2 = bimage.createGraphics();
		g2.setColor( transparent) ;
		g2.fillRect(0, 0, width, height);
		Polygon brdr = new Polygon();
		Polygon bck = new Polygon();
		
		if (Math.abs(w) > Math.abs(h)) {
//			System.out.println("Horizontal edge");			
			x = p1.x > p2.x ? p2.x : p1.x;
			y = p1.y > p2.y ? p2.y : p1.y;
			y -= 3*t/2;
			if (hw<0.0) {
//				System.out.println("rising");				
//				y -= Math.abs(h)+3*t/2;
				brdr.addPoint(0,round(Math.abs(h)+2*t));
				brdr.addPoint(0,round(Math.abs(h)+t));
				brdr.addPoint(Math.abs(round(w)),round(t));
				brdr.addPoint(Math.abs(round(w)),round(2*t));

				bck.addPoint(0,round(Math.abs(h)+3*t));
				bck.addPoint(0,round(Math.abs(h)));
				bck.addPoint(Math.abs(round(w)),0);
				bck.addPoint(Math.abs(round(w)),round(3*t));
//				System.out.println("Border Image: (0,"+round(t+Math.abs(h))+") (0,"+round(2*t+Math.abs(h))+") ("+round(w)+","+round(t)+") ("+round(w)+","+round(2*t)+")");
					
			}
			else {
//				System.out.println("falling");
//				y -= 3*t/2;
				brdr.addPoint(0,round(2*t));
				brdr.addPoint(0,round(t));
				brdr.addPoint(Math.abs(round(w)),round(t+Math.abs(h)));
				brdr.addPoint(Math.abs(round(w)),round(2*t+Math.abs(h)));

				bck.addPoint(0,round(3*t));
				bck.addPoint(0,0);
				bck.addPoint(Math.abs(round(w)),round(Math.abs(h)));
				bck.addPoint(Math.abs(round(w)),round(3*t+Math.abs(h)));


//				System.out.println("Border Image: (0,"+round(t)+") (0,"+round(2*t)+") ("+round(w)+","+round(t+Math.abs(h))+") ("+round(w)+","+round(2*t+Math.abs(h))+")");
			}
		}
		else {
//			System.out.println("vertical edge");			
			y = p1.y > p2.y ? p2.y : p1.y;
			if (hw<0.0) {
				x = p1.x > p2.x ? p1.x : p2.x;
				x -= Math.abs(w)+3*t/2;
				brdr.addPoint(round(Math.abs(w)+2*t),0);
				brdr.addPoint(round(Math.abs(w)+t),0);
				brdr.addPoint(round(t),round(Math.abs(h)));
				brdr.addPoint(round(2*t), round(Math.abs(h)));

				bck.addPoint(round(Math.abs(w)+3*t),0);
				bck.addPoint(round(Math.abs(w)),0);
				bck.addPoint(0,round(Math.abs(h)));
				bck.addPoint(round(3*t), round(Math.abs(h)));
//				System.out.println("Border Image: ("+round(t+Math.abs(w))+",0) ("+round(2*t+Math.abs(w))+",0) ("+round(t)+","+round(h)+") ("+round(2*t)+","+round(h)+")");
				
			}
			else {
				x = p1.x < p2.x ? p1.x : p2.x;
				x -= 3*t/2;
				brdr.addPoint(round(2*t),0);
				brdr.addPoint(round(t),0);
				brdr.addPoint(round(t+Math.abs(w)),round(Math.abs(h)));
				brdr.addPoint(round(2*t+Math.abs(w)), round(Math.abs(h)));

				bck.addPoint(round(3*t),0);
				bck.addPoint(0,0);
				bck.addPoint(round(Math.abs(w)),round(Math.abs(h)));
				bck.addPoint(round(3*t+Math.abs(w)), round(Math.abs(h)));
//				System.out.println("Border Image: ("+round(t)+",0) ("+round(2*t)+",0) ("+round(t+Math.abs(w))+","+round(h)+") ("+round(2*t+Math.abs(w))+","+round(h)+")");
			}
		}
		g2.setColor(signColor);
        g2.fillPolygon(bck);
		g2.setColor(borderColor);
        g2.fillPolygon(brdr);
        g2.dispose();
        	
		EdgeBorder border = new EdgeBorder(bimage, new Point(x,y), adjusted, t, threshold);
//		System.out.println("created new EdgeBorder "+border);
		return border;
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}