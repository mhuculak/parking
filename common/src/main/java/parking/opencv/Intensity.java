package parking.opencv;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class Intensity implements Clusterable {
	private double intensity;

	public Intensity(double i) {
		intensity = i;
	}

	public double val() {
		return intensity;
	}

	public double[] getPoint() {
		double[] arr = new double[1];
		arr[0]= intensity;
		return arr;
	}
}