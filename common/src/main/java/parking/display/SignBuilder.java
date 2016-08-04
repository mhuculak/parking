package parking.display;

import parking.opencv.Rectangle;
import parking.opencv.Line;
import parking.opencv.EdgeBorder;
import parking.opencv.TextShape;
import parking.opencv.TextGroup;
import parking.opencv.BinarySegmenter;
import parking.opencv.ShapeGenMode;
import parking.opencv.ProcessOpenCV;
import parking.opencv.CannyEdgeDetector;
import parking.opencv.OpenCVConvert;

import parking.schedule.ParkingSchedule;
import parking.schedule.ParkingSign;
import parking.schedule.ParkingSignType;

import org.opencv.core.Mat;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;

import java.util.List;
import java.util.ArrayList;
import java.io.File;


public class SignBuilder {

	private SignImage origImage;
	private SignImage workingImage;
	private SignImage cannyImage;
	private List<SignImage> signImages;
	private List<Line> allLines;

	static {
		System.out.println("init open cv");
		ProcessOpenCV.initOpenCV();
		System.out.println("done init open cv");
	}
	
	public SignBuilder(File file, double scaleFactor) {
		System.out.println("SignBuilder enter");
		origImage = new SignImage(file);
		workingImage = origImage.scale(scaleFactor);
		System.out.println("SignBuilder load sign");
		loadSign();
		System.out.println("SignBuilder created");
	}

	public SignBuilder(SignImage working) {
		workingImage = working;
		try {
			loadSign();
		}
		catch (Exception ex) {
			System.out.println("Caught exception "+ex);
			ex.printStackTrace();
		}
	}

	public ParkingSchedule readSign(int index) {
		System.out.println("SignBuilder read sign enter");
		SignImage img = signImages.get(index);
		BinarySegmenter segmenter = new BinarySegmenter(img.getImage(), cannyImage.getImage(), img.getTheBorder(), img.getOrigBorder());
		SignImage binaryImage = new SignImage(segmenter.getBinary());
		List<TextShape> shapes = TextShape.getShapes(binaryImage.getImage(),  ShapeGenMode.BINREGION);

		if (shapes != null && shapes.size() > 0) {
			List<TextGroup> textGroups = TextGroup.getTextGroups(shapes);
			ParkingSignType signType = img.getSignType(cannyImage, allLines, textGroups);

			ParkingSchedule schedule = ParkingSign.readSchedule(signType, textGroups);
			System.out.println(schedule);
			return schedule;
		}
		return null;
	}

	public int size() {
		if (signImages != null) {
			return signImages.size();
		}
		return 0;
	}

	private void loadSign() {

		CannyEdgeDetector canny = new CannyEdgeDetector(workingImage.getImage());
		
		int lowThresh = 50;
		Mat cannyMat = canny.doCannyThreshold(lowThresh);
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null));		 
		allLines = ProcessOpenCV.getLinesP(cannyMat);
		List<Line> verifiedLines = Line.verifyLines(allLines, cannyImage);
		List<EdgeBorder> edges = EdgeBorder.getEdges(workingImage, verifiedLines);		
		List<Line> edgeLines = new ArrayList<Line>();
		for (EdgeBorder edge : edges) {
			edgeLines.add(edge.getEdge());
		}
		List<CentroidCluster<Line>> clusters = clusterLines(edgeLines);

		int minDim = workingImage.getWidth() > workingImage.getHeight() ? workingImage.getHeight() : workingImage.getWidth();
		List<Rectangle> rectangles = Rectangle.getRectangles(clusters, minDim);
		
		signImages = SignImage.getSigns(workingImage, cannyImage, rectangles, false);

	}

	private List<CentroidCluster<Line>> clusterLines(List<Line> lines) {
		final int maxClusters = 6;
		final int maxIterations = 100;
		int nbClusters = lines.size() > maxClusters ? maxClusters : lines.size();
		KMeansPlusPlusClusterer<Line> kmeans = new KMeansPlusPlusClusterer<Line>(nbClusters, maxIterations);
		return kmeans.cluster(lines);
	}
}