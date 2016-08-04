package parking.opencv;

import parking.display.SignImage;

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


class BorderComparator implements Comparator<SignBorder> {
	@Override
	public int compare(SignBorder b1, SignBorder b2) {
		if (b1.getScore() > b2.getScore()) {
			return -1;
		}
		else if (b1.getScore() < b2.getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

public class SignBorder {

	private Rectangle border;
	private double thickness;
	private double score;    // how well does the border pmatch the input image

	private final static Color[] colors = new Color[] { Color.red, Color.green, Color.orange, Color.cyan, Color.pink, 
		Color.yellow, Color.magenta,  Color.blue};

	public SignBorder(Rectangle rectangle, double thickness, double score) {
		this.border = rectangle;
		this.thickness = thickness;
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	public Rectangle getBorder() {
		return border;
	}

	public static List<SignBorder> findBorder(SignImage image, List<Rectangle> rectangles, BufferedImage img) {
		final double minScore = 0.0;
		
		List<SignBorder> borders = new ArrayList<SignBorder>();
		boolean didIt = false;
		SignBorder bestBorder = null;
		int i=0;
		for (Rectangle rect : rectangles) {
			SignBorder border = getBorder(image, rect);
			if (border != null) {
				if (border.getScore() > minScore) {
					borders.add(border);
				}
				if (bestBorder == null || border.getScore() > bestBorder.getScore()) {
					bestBorder = border;
				}
			}
			if (i<8) {
//				showBorder(img, rect, i);
			}
			else {
//				return null;
			}
			i++;
		}
		System.out.println("Found "+borders.size()+" sign borders, computed "+i);
		sortBorders(borders);
		i=0;
		for (SignBorder border : borders) {
			if ( i<8) {
				showBorder(img, border.getBorder(), i);
			}
//			System.out.println(border.getBorder()+" "+border.getScore()+" "+border.getBorder().getAspectRatio());
			i++;
		}

		if (bestBorder != null) {
			showBorder(img, bestBorder.getBorder(), 0);
			System.out.println("best score "+bestBorder.getScore());
			
		}
		return borders;
	}

	private static void sortBorders(List<SignBorder> borders) {
		BorderComparator comparator = new BorderComparator();
		Collections.sort(borders, comparator);			
	}

	private static void showBorder(BufferedImage image, Rectangle rect, int index) {
		Graphics2D g2 = (Graphics2D)image.createGraphics();
		int k = index % 8;
        g2.setColor(colors[k]);    	
		Point[] corner = rect.getCorners();
		for ( int j=0 ; j<4 ; j++) {
        	int next = j==3 ? 0 : j+1;
        	Shape l = new Line2D.Double(corner[j].x, corner[j].y, corner[next].x, corner[next].y);
        	g2.draw(l);
        		
		}
		g2.dispose();
	}

	//
	//  The sign border is reprsented using bimage which is 3 x the border thickness. 
	//  this should place the edge of the bimage at the edge of the sign when aligned
	//  with the border. 
	//        bimageRatio is the width of the bimage as a fraction of the width of the sign
	//        aspectRatio is the aspect ratio of the sign i.e width / height
	//
	public static SignBorder getBorder(SignImage image, Rectangle rect) {
		double[] score = new double[4];
		double totalScore = 0.0;
		double[] thickness = new double[4];
		double avgThickness = 0.0;
		int i=0;
		StringBuilder sb = new StringBuilder(4);
		sb.append(rect);
		sb.append(" scores: ");
		int edgesFound = 0;
		for (Line line : rect.getSides()) {
			EdgeBorder edgeBorder = EdgeBorder.createEdgeBorder(image.getImage(), line);
			if (edgeBorder != null) {
				thickness[i] = edgeBorder.getThickness();
				avgThickness += thickness[i];
				score[i] = EdgeBorder.computeScore(image.getImage(), edgeBorder);
				totalScore += score[i];
				sb.append(score[i]+", ");
				edgesFound++;
			}
			i++;
		}
		sb.append(" total = " + totalScore + " found " + edgesFound + " edges");
//		System.out.println(sb);
		if (edgesFound > 0) {
			avgThickness = avgThickness / edgesFound;
		}

		return new SignBorder(rect, avgThickness, totalScore/4);
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}