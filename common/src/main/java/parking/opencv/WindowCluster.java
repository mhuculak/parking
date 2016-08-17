package parking.opencv;

import parking.display.Boundary;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.opencv.core.Point;

import java.util.Collections;
import java.util.Comparator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

class WindowComparator implements Comparator<ColorWindow> {
	@Override
	public int compare(ColorWindow w1, ColorWindow w2) {
		if (w1.ypos < w2.ypos) {
			return -1;
		}
		else if (w1.ypos > w2.ypos) {
			return 1;
		}
		else {
			if (w1.xpos < w2.xpos) {
				return -1;
			}
			else if (w1.xpos > w2.xpos) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
}

public class WindowCluster implements Clusterable {
	private Point centroid;
	private Color color;
	private double intensity;
	private List<ColorWindow> windowList;
	private int clusterNum;
	private String clusterLabel;
	private double borderProximity;
	private Boundary bounday;
	private boolean foreground;

	private final double foregroundThreshold = 0.7;

	public WindowCluster( int clusterNum, Point centroid, Color color, List<ColorWindow> windowList) {
		this.clusterNum = clusterNum;
		this.centroid = centroid;
		this.color = color;
		intensity = ColorClassifier.getGreyLevel(color);
		this.windowList = windowList;
//		label = computeLabel();
	}
/*
	public double[] getPoint() {
		double[] arr = new double[1];
		arr[0]= borderProximity;
		return arr;
	}
*/
	public double[] getPoint() {
		double[] arr = new double[4];
		arr[0]= borderProximity;
		float[] comp = color.getRGBColorComponents(null);
		int k=0;
		for ( int j=0 ; j<3 ; j++ ) {
			arr[k] = 255*(double)comp[j];
			k++;
		}
		return arr;
	}

	public int getClusterNum() {
		return clusterNum;
	}

	public String getLabel() {
		return clusterLabel;
	}

	public boolean isForeground() {
		return foreground;
	}

	public double getIntensity() {
		return intensity;
	}

	public Point getCentroid() {
		return centroid;
	}

	public double getProximity() {
		return borderProximity;
	}

	public void setClusterLabel(String clusterLabel) {
		this.clusterLabel = clusterLabel;
		if (clusterLabel.equals("F")) {
			foreground = true;
		}
		else {
			foreground = false;
		}
		
	}

	public String toString() {
		return clusterNum+" "+clusterLabel+" "+color+" "+intensity;
	}
/*
	private String computeLabel() {
		String foreground = "F";
		String background = "B";
	
		if (ColorClassifier.isWhite(color, foregroundThreshold)) {
			return foreground;
		}
		return background;
	}
*/
	public void computeBorderProximity(BufferedImage image) {
		borderProximity = 0.0;
		for (ColorWindow cw : windowList) {
			borderProximity += cw.computeBorderProximity(image);
		}
	}

}