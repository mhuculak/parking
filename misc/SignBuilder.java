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

import parking.util.Logger;
import parking.util.LoggingTag;

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
	private Logger m_logger;

	private final double desiredMinDim = 800.0;

	static {
		ProcessOpenCV.initOpenCV();
	}
	
	public SignBuilder(File file, Logger logger) {
		m_logger = new Logger(logger, this, LoggingTag.Image);
		origImage = new SignImage(file, m_logger);
		loadSign();
	}

	public SignBuilder(SignImage img, Logger logger) {
		origImage = img;
		m_logger = logger;
		try {
			loadSign();
		}
		catch (Exception ex) {
			System.out.println("Caught exception "+ex);
			ex.printStackTrace();
		}
	}

	public ParkingSchedule readSign(int index) {
		Logger logger = new Logger(m_logger, "readSign");
		SignImage img = signImages.get(index);
		logger.log("sign image is "+img.getWidth()+" x "+img.getHeight());
		BinarySegmenter segmenter = new BinarySegmenter(img.getImage(), cannyImage.getImage(), img.getTheBorder(), img.getOrigBorder());
		SignImage binaryImage = new SignImage(segmenter.getBinary(), m_logger);
		List<TextShape> shapes = TextShape.getShapes(binaryImage.getImage(),  ShapeGenMode.BINREGION, m_logger);

		if (shapes != null && shapes.size() > 0) {
			List<TextGroup> textGroups = TextGroup.getTextGroups(shapes, logger);
			ParkingSignType signType = img.getSignType(cannyImage, allLines, textGroups);

			ParkingSchedule schedule = ParkingSign.readSchedule(signType, textGroups, m_logger);
			logger.log(schedule.toString());
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
		int minDim = origImage.getWidth() < origImage.getHeight() ? origImage.getWidth() : origImage.getHeight();
		double scaleFactor = desiredMinDim/minDim;
		if (scaleFactor < 1.0) {
			workingImage = origImage.scale(scaleFactor);
		}
		else {
			workingImage = origImage;
		}
		CannyEdgeDetector canny = new CannyEdgeDetector(workingImage.getImage());
		
		int lowThresh = 50;
		Mat cannyMat = canny.doCannyThreshold(lowThresh);
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null), m_logger);		 
		allLines = ProcessOpenCV.getLinesP(cannyMat);
		List<Line> verifiedLines = Line.verifyLines(allLines, cannyImage);
		List<EdgeBorder> edges = EdgeBorder.getEdges(workingImage, verifiedLines);		
		List<Line> edgeLines = new ArrayList<Line>();
		for (EdgeBorder edge : edges) {
			edgeLines.add(edge.getEdge());
		}
		List<CentroidCluster<Line>> clusters = clusterLines(edgeLines);

		minDim = workingImage.getWidth() > workingImage.getHeight() ? workingImage.getHeight() : workingImage.getWidth();
		List<Rectangle> rectangles = Rectangle.getRectangles(clusters, minDim);
		
		signImages = SignImage.getSigns(workingImage, cannyImage, rectangles, false, m_logger);

	}

	private List<CentroidCluster<Line>> clusterLines(List<Line> lines) {
		final int maxClusters = 6;
		final int maxIterations = 100;
		int nbClusters = lines.size() > maxClusters ? maxClusters : lines.size();
		KMeansPlusPlusClusterer<Line> kmeans = new KMeansPlusPlusClusterer<Line>(nbClusters, maxIterations);
		return kmeans.cluster(lines);
	}
}