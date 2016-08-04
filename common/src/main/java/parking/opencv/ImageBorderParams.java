package parking.opencv;

import org.opencv.core.Point;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;  
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.awt.image.BufferedImage;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;



public class ImageBorderParams {
	
	private double thickness;
	private double foreground;
	private double threshold;
	private double background;
	private double displacement;
	private Vector direction;
	private Point pos; 
	private int numTrans;
	private int window;
	private int[] trans;
	

	public ImageBorderParams(Line edge, double placement) {
		direction = new Vector(edge.p1, edge.p2);
		pos = edge.findPoint(placement);
	}

	public double getThickness() {
		return thickness;
	}

	public double getDisplacement() {
		return displacement;
	}

	public double getForeground() {
		return foreground;
	}

	public double getBackground() {
		return background;
	}

	public double getThreshold() {
		return threshold;
	}

	public String toString() {
		if (numTrans == 2 && trans != null) {
			return thickness+" "+displacement+" "+window+" "+numTrans+" "+trans[0]+" "+foreground+" "+background;
		}
		else {
			return "no edge found, num trans = "+numTrans;
		}
	}

	public int compute(BufferedImage image) {
		final int maxWin = 100;
		int win = 5;
		int transitions = 0;
		do {
			transitions = computeWin(image, win);
			win = win*2;
		} while ( transitions == 1 && win < maxWin);

		return transitions;
	}

	private int computeWin(BufferedImage image, int win) {
		window = win;
		Vector ortho = new Vector(-1*direction.y, direction.x);
//		System.out.println("compute params at "+pos.x+","+pos.y+" along "+ortho);
		
		List<Intensity> intensity = new ArrayList<Intensity>();
		int i;
		double x,y;
		StringBuilder sb = new StringBuilder(100);
		sb.append("intenities:");
		if (Math.abs(ortho.x) > Math.abs(ortho.y)) {
			for ( i=0, x = pos.x-win ; x < pos.x+win ; x++, i++ ) {
				if (round(x)>=0 && round(x)<image.getWidth() && round(pos.y)>=0 && round(pos.y)<image.getHeight()) {
					Intensity ival = getIntensity(new Color(image.getRGB(round(x),round(pos.y))));
					intensity.add(ival);
					sb.append(ival.val()+" ");
				}
			}
		}
		else {
			for ( i=0, y = pos.y-win ; y < pos.y+win ; y++, i++ ) {
				if (round(y)>=0 && round(y)<image.getHeight() ) {
					Intensity ival = getIntensity(new Color(image.getRGB(round(pos.x),round(y))));
					intensity.add(ival);
					sb.append(ival.val()+" ");
				}
			}
		}	
//		System.out.println(sb);	
		KMeansPlusPlusClusterer<Intensity> kmeans = new KMeansPlusPlusClusterer<Intensity>(3, 10);
		List<CentroidCluster<Intensity>> clusters = kmeans.cluster(intensity);
		foreground = 1.0;
		double maxForeground = 0.0;
//		System.out.println("Found " + clusters.size() + " clusters");
		for (CentroidCluster<Intensity> cl : clusters) {
			double[] iarr = cl.getCenter().getPoint();
			double inten = iarr[0];
//			System.out.println("Found cluser with "+cl.getPoints().size()+" points  value = "+inten);
			if (inten < foreground) {
				foreground = inten;
				maxForeground = 0.0;
				for (Intensity inval : cl.getPoints()) {
					if (inval.val() > maxForeground) {
						maxForeground = inval.val();
					}
				}
			}		
		}		
		threshold = maxForeground;
		numTrans = 0;
		int maxTrans = 100;
		int[] location = new int[maxTrans];
		double[] intensities = new double[maxTrans];
		i=0;
		boolean inBackground = intensity.get(0).val() > threshold;
		for (Intensity inten : intensity) {
			if ( inBackground && inten.val() < threshold) {
				inBackground = false;
				intensities[numTrans] = 1.0;
				location[numTrans++] = i;				
			}
			else if (inBackground == false && inten.val() > threshold) {
				inBackground = true;
				intensities[numTrans] = foreground;
				location[numTrans++] = i;
				for (CentroidCluster<Intensity> cl : clusters) {
					for (Intensity inval : cl.getPoints()) {
						if (inval.val()==inten.val()) {
							background = cl.getCenter().getPoint()[0];
						}
					}
				}
			}
			i++;
		}
//		System.out.println("foreground = "+foreground+" threshold = "+threshold+" num trans = "+numTrans);
		sb = new StringBuilder(100);
		sb.append("transitions = ");
		for ( i=0 ; i<numTrans ; i++) {
			sb.append(intensities[i]+" @ "+location[i]+",");
		}
//		System.out.println(sb);
		if (numTrans==2 && intensities[1]==foreground) {
			thickness = location[1]-location[0];
			trans = location;
			displacement = location[0]-win;		
//			System.out.println("edge found at "+ location[0] + " thickness = "+ thickness);
		}
		return numTrans;
	}

	public static Intensity getIntensity(Color color) {
		return new Intensity(getGreyLevel(color));
	}

	public static double getGreyLevel(Color color) {
		return Math.sqrt(color.getRed()*color.getRed() + color.getBlue()*color.getBlue() + color.getGreen()*color.getGreen())/(255 * Math.sqrt(3));
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}