package parking.display;

import parking.opencv.Rectangle;
import parking.opencv.Line;
import parking.opencv.PolarLine;
import parking.opencv.EdgeBorder;
import parking.opencv.TextShape;
import parking.opencv.TextGroup;
import parking.opencv.BinarySegmenter;
import parking.opencv.ShapeGenMode;
import parking.opencv.ProcessOpenCV;
import parking.opencv.CannyEdgeDetector;
import parking.opencv.OpenCVConvert;
import parking.opencv.ColorSegmenter;
import parking.opencv.WindowCluster;

import parking.schedule.ParkingSchedule;
import parking.schedule.ParkingSign;
import parking.schedule.ParkingSignType;

import parking.util.Profiler;
import parking.util.Logger;
import parking.util.LoggingTag;

import org.opencv.core.Mat;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.util.Collections;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;

class LineComparator implements Comparator<Line> {
	@Override
	public int compare(Line l1, Line l2) {
		double d1 = l1.diagonal();
		double d2 = l2.diagonal();
		if (d1 > d2) {
			return -1;
		}
		else if (d1 < d2) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class ClusterComparator implements Comparator<WindowCluster> {
	@Override
	public int compare(WindowCluster wc1, WindowCluster wc2) {
		if (wc1.isForeground() && !wc1.isForeground()) {
			return -1;
		}
		else if (!wc1.isForeground() && wc1.isForeground()) {
			return 1;
		}
		else {
			if (wc1.getIntensity() > wc2.getIntensity()) {
				return -1;
			}
			else if (wc1.getIntensity() < wc2.getIntensity()) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}
}

public class SignBuilder {

	private SignImage origImage;
	private SignImage workingImage;
	private List<SignImage> signImages;
	private SignImage cannyImage;
	private SignImage rawCanny;
	private SignImage transCanny;
	private SignImage resizedCanny;
	private SignImage resizedImage;
	private SignImage binaryImage;
	private List<Line> allLines;
	private List<Line> verifiedLines;
	private List<Line> edgeLines;
	private List<Line> clusteredLines;
	private List<TextShape> shapes;
	private List<Rectangle> rectangles;
	private List<WindowCluster> clusterList;
	private ColorSegmenter colorSegmenter;
	private double scaleFactor;

	private Logger m_logger;
	private Profiler m_profiler;

	
	private final int lowThresh = 50;

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

	public SignBuilder(InputStream inStream, Logger logger, Profiler profiler) throws Exception {
		m_profiler = profiler;
		m_logger = logger;
		BufferedImage img = ImageIO.read(inStream);
		origImage = new SignImage(img, m_logger);
		loadSign();
	}

	public ParkingSchedule readSign(int index) {
		Logger logger = new Logger(m_logger, "readSign");
		Rectangle origBorder = signImages.get(index).getOrigBorder();
		resizedImage = restoreSize(signImages.get(index), origImage);
//		SignImage img = signImages.get(index);
		logger.log("get sign image "+index+" size is "+resizedImage.getWidth()+" x "+resizedImage.getHeight());
		logger.log("Border = "+origBorder);

		BinarySegmenter segmenter = null;
		if (resizedImage.getMinDim() != signImages.get(index).getMinDim()) {		
			CannyEdgeDetector cannyDetector = new CannyEdgeDetector(origImage.getImage());		
			Mat cannyMat = cannyDetector.doCannyThreshold(lowThresh);
			rawCanny = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null), logger);
			transCanny = new SignImage( SignImage.applyBorderTransform(rawCanny.getImage(), resizedImage.getOrigBorder()), logger);
			resizedCanny = transCanny.scale(resizedImage.getOrigScale());
			allLines = ProcessOpenCV.getLinesP(cannyMat);
			segmenter = new BinarySegmenter(resizedImage.getImage(), resizedCanny.getImage(), resizedImage.getTheBorder(), resizedImage.getTheBorder());
		}
		else {
			segmenter = new BinarySegmenter(resizedImage.getImage(), cannyImage.getImage(), resizedImage.getTheBorder(), origBorder);
		}
		
		binaryImage = new SignImage(segmenter.getBinary(), m_logger);
		shapes = TextShape.getShapes(binaryImage.getImage(),  ShapeGenMode.BINREGION, m_logger);
		if (shapes != null && shapes.size() > 0) {
			List<TextGroup> textGroups = TextGroup.getTextGroups(shapes, logger);
			ParkingSignType signType = resizedImage.getSignType(cannyImage, allLines, origBorder, textGroups); // FIXME
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

	public List<SignImage> getTopChoices() {
		return signImages;
	}

	private SignImage restoreSize(SignImage img, SignImage orig) {
		Logger logger = new Logger(m_logger, "restoreSize");
		if (scaleFactor < 1.0) {
			logger.log("sign image is "+img.getWidth()+" x "+img.getHeight());
			Rectangle border = img.getOrigBorder();
			Rectangle bigBorder = border.scale(1.0/scaleFactor); // this reverses the scaling from loadSign()
			SignImage bigImage = new SignImage( SignImage.applyBorderTransform(orig.getImage(), bigBorder), logger); // apply scaled up border transform to orig image
			int minDim = bigImage.getMinDim();
			double newScaleFactor = desiredMinDim/minDim;
			SignImage restoredImage = bigImage.scale(newScaleFactor);
			restoredImage.setOrigBorder(bigBorder);
			restoredImage.setOrigScale(newScaleFactor);
			restoredImage.setTheBorder(img.getTheBorder().scale(newScaleFactor/scaleFactor));
			logger.log("restored image is "+restoredImage.getWidth()+" x "+restoredImage.getHeight());
			return restoredImage;
		}
		else {
			return img;  // no previous scaling to restore
		}
	}

	private void loadSign() {
		Logger logger = new Logger(m_logger, "loadSign");
		int minDim = origImage.getMinDim();
		scaleFactor = desiredMinDim/minDim;
		if (scaleFactor < 1.0) {
			logger.log("Scale image with minDim "+minDim+" by factor "+scaleFactor);
			workingImage = origImage.scale(scaleFactor); // scale down to save resource			
		}
		else {
			workingImage = origImage; // no benefit in scaling up
		}
		if (workingImage.getWidth() > workingImage.getHeight()) {
			workingImage = workingImage.rotate90();
		}
		logger.log("Working image is "+workingImage.getWidth()+" x "+workingImage.getHeight());
		CannyEdgeDetector canny = new CannyEdgeDetector(workingImage.getImage());
		
		int lowThresh = 50;
		Mat cannyMat = canny.doCannyThreshold(lowThresh);
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null), m_logger);		 
		allLines = ProcessOpenCV.getLinesP(cannyMat);
		logger.log("Found "+allLines.size()+" total lines");
		
		verifiedLines = Line.verifyLines(allLines, cannyImage);
		logger.log("Found "+allLines.size()+" verified lines");

		clusteredLines = colinearCluster(verifiedLines);
		logger.log("Found "+clusteredLines.size()+" clustered lines");

		rectangles = Rectangle.getRectanglesFromLines(clusteredLines, workingImage.getMinDim());
//		rectangles = Rectangle.getRectanglesFromLines(verifiedLines, workingImage.getMinDim());


		ColorSegmenter colorSegmenter = doColorSegment();
		signImages = SignImage.getSigns(workingImage, cannyImage, rectangles, colorSegmenter, false, m_logger);

		logger.log("top border is "+signImages.get(0).getOrigBorder().toString());		
			
	}

	public ColorSegmenter doColorSegment() {
		int numWindows = 20;
		int numWindowClusters = 20;
		colorSegmenter = new ColorSegmenter(workingImage.getImage(), numWindows);
		double minColorDist = 0.1;
		colorSegmenter.mergeWindows(minColorDist);	
		colorSegmenter.computeWindowStats(3);
		clusterList = colorSegmenter.clusterWindows(numWindowClusters);
		colorSegmenter.labelClustersByProximity(clusterList);		
		List<String> clusterLabels = new ArrayList<String>();
		for (WindowCluster cl :  clusterList) {
			clusterLabels.add(cl.getLabel());
		}
		colorSegmenter.applyLabels(clusterLabels);
		ClusterComparator comparator = new ClusterComparator();
		Collections.sort(clusterList, comparator);
		return colorSegmenter;
		
	}

	//
	//  clustering lines into colinear groups reduces the number of candidate rectangles which saves resources
	//
	private List<Line> colinearCluster(List<Line> lines) {
		double width = workingImage.getWidth();
		double height = workingImage.getHeight();
		final int maxClusters = 20;
		final int maxIterations = 100;
		final int maxLines = 100; // removing diagonal lines improves the accuracy of the resulting clusters cuz border lines are horizontal & vertical
		final double minDiagonal = Math.PI/4 - 0.1;;
		System.out.println("cluster "+lines.size()+" lines");
		int nbLines = Math.min(lines.size(), maxLines);
		if (lines.size() > maxLines) {
			LineComparator comparator = new LineComparator();
			Collections.sort(lines, comparator);
			while ( nbLines < lines.size() && lines.get(nbLines).diagonal() > minDiagonal ) {
				nbLines++;
			}
		}
		List<PolarLine> polarLines = new ArrayList<PolarLine>();
		for ( int i=0 ; i<nbLines ; i++) {
			PolarLine pline = new PolarLine(lines.get(i), width, height);
			polarLines.add(pline);
		}
		int nbClusters = polarLines.size() > maxClusters ? maxClusters : polarLines.size();
		KMeansPlusPlusClusterer<PolarLine> kmeans = new KMeansPlusPlusClusterer<PolarLine>(nbClusters, maxIterations);
		List<CentroidCluster<PolarLine>> colinearClusters = kmeans.cluster(polarLines);
		List<Line> combinedLines = new ArrayList<Line>();
		for (CentroidCluster<PolarLine> cl : colinearClusters) {
			int size = cl.getPoints().size();
			double[][] points = new double[2*size][2];
			double[] scores = new double[size];
			double[] theta = cl.getCenter().getPoint();
			int k=0;
			boolean xdir = Math.abs(theta[0]+theta[1]) < 0.1 ? false : true;					
			for ( int i=0 ; i<size ; i++  ) {
				PolarLine pl = cl.getPoints().get(i);
				PolarLine pline = new PolarLine( theta, pl.getLine(), width, height);
				if (xdir) {
					points[k][0] = pline.getLine().p1.x;
					points[k++][1] = pline.getLine().p1.y;
					points[k][0] = pline.getLine().p2.x;
					points[k++][1] = pline.getLine().p2.y;					
				}
				else {
					points[k][0] = pline.getLine().p1.y;
					points[k++][1] = pline.getLine().p1.x;
					points[k][0] = pline.getLine().p2.y;
					points[k++][1] = pline.getLine().p2.x;	
				}
				scores[i] = pline.getLine().length; // FIXME: perhaps better to use perp dist as a cost?
			}
			Line combined = new Line(points, xdir, scores); // FIXME: using linear regression is overkill cuz PolarLine ctor projects the points
//			System.out.println("Got line cluster "+theta[0]+" "+theta[1]+" of size "+cl.getPoints().size()+" "+combined+" "+combined.score);
			combinedLines.add(combined);
		}
		return combinedLines;
	}

	private List<CentroidCluster<Line>> clusterLinesbyAngle(List<Line> lines) {
		final int maxClusters = 6;
		final int maxIterations = 100;
		int nbClusters = lines.size() > maxClusters ? maxClusters : lines.size();
		KMeansPlusPlusClusterer<Line> kmeans = new KMeansPlusPlusClusterer<Line>(nbClusters, maxIterations);
		return kmeans.cluster(lines);
	}

	public SignImage getWorkingImage() {
		if (rectangles.size() > 0) {
			List<Rectangle> bestBorder = new ArrayList<Rectangle>();
//			bestBorder.add(rectangles.get(0));
			bestBorder.add(signImages.get(0).getOrigBorder());
			workingImage.setRectangles(bestBorder);
		}
		return workingImage;
	}

	public SignImage getLineImage() {
		SignImage lineImage = new SignImage( workingImage.getWidth(), workingImage.getHeight() );
		lineImage.setLines(verifiedLines);
		return lineImage;
	}

	public SignImage getEdgeImage() {
		SignImage edgeImage = new SignImage( workingImage.getWidth(), workingImage.getHeight() );
//		edgeImage.setLines(edgeLines);
		edgeImage.setLines(clusteredLines);
		for ( Line l : clusteredLines) {
			m_logger.log("edge: "+l.toString()+" "+l.score);
		}
		return edgeImage;
	} 

	public SignImage getRectangleImage() {
		SignImage rectImage = new SignImage( workingImage.getWidth(), workingImage.getHeight() );
		rectImage.setRectangles(rectangles);
		m_logger.log("got "+signImages.size()+" candidate signs ");
		for (SignImage si : signImages) {
			m_logger.log("border: "+si.getOrigBorder().toString()+" score = "+si.getScore()+" "+si.getScoreDetails());			
			for ( Line l : si.getOrigBorder().getOrigLines()) {
				m_logger.log("  orig edge:" +l.toString()+" score = "+l.score);
			}			
		}
		return rectImage;
	} 

	public SignImage showClusterBoundaries() {
		List<Boundary> clusterBoundaryList = colorSegmenter.findClusterBoundaries(clusterList);
		workingImage.setBoundaries(clusterBoundaryList);
		m_logger.log(colorSegmenter.clustersAsString());
		for (WindowCluster cl : clusterList) {
			m_logger.log(cl.toString());
		}
		m_logger.log(colorSegmenter.clusterLabelAsString());
		return workingImage;
	}

	public SignImage getResizedImage() {
		return resizedImage;
	}

	public SignImage getCannyOrig() {
		return cannyImage;
	}

	public SignImage getRawCanny() {
		return rawCanny;
	}

	public SignImage getTransCanny() {
		return transCanny;
	}

	public SignImage getResizedCanny() {
		return resizedCanny;
	}

	public SignImage getBinaryImage() {
		return binaryImage;
	}

	public SignImage getShapeImage() {
		SignImage shapeImage = new SignImage(shapes.get(0).getImage(), m_logger);
		List<Rectangle> bounds = new ArrayList<Rectangle>(); 
		for ( int i=0 ; i<shapes.size() ; i++) {
			bounds.add(shapes.get(i).getBound());
		}
		shapeImage.setRectangles(bounds);
		return shapeImage;
	}

	public SignImage getTextSegmentImage(double zoom) {
		SignImage textSegmentImage = new SignImage(binaryImage.getWidth(), binaryImage.getHeight(), zoom);
		textSegmentImage.setLetters(shapes);
		return textSegmentImage;
	}
}