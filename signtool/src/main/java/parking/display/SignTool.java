package parking.display;

import parking.opencv.CannyEdgeDetector;
import parking.opencv.ProcessOpenCV;
import parking.opencv.OpenCVConvert;
import parking.opencv.Line;
import parking.opencv.Circle;
import parking.opencv.Ellipse;
import parking.opencv.Rectangle;
import parking.opencv.SignBorder;
import parking.opencv.EdgeBorder;
import parking.opencv.TextShape;
import parking.opencv.TextChoice;
import parking.opencv.TextSegment;
import parking.opencv.TextGroup;
import parking.opencv.SignSegmenter;
import parking.opencv.BinarySegmenter;
import parking.opencv.ShapeGenMode;

import parking.schedule.ParkingSchedule;
import parking.schedule.ParkingSign;
import parking.schedule.ParkingSignType;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;  

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFileChooser;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class SignTool extends JFrame {

	private JFrame cannyFrame;
	private JFrame lineFrame;
	private JFrame circleFrame;
	private JFrame edgeFrame;
	private JFrame rectFrame;
	private JFrame shapeFrame;
	private JFrame textSegmentFrame;
	private JFrame signFrame;
	private JFrame binaryFrame;
	private JFrame graphFrame;
	private JPanel imagePanel;

	private SignToolMenu menu;
	private SignImage origImage;
	private SignImage zoomImage;
	private Mat cannyMat;
	private SignImage cannyImage;
	private SignImage lineImage;
	private SignImage circleImage;
	private SignImage edgeImage;
	private SignImage rectImage;
	private SignImage shapeImage;
	private SignImage textSegmentImage;
	private List<SignImage> signImages;
	private SignImage viewedImage;
	private SignImage binaryImage;
	private SignImage graphImage;

	private SpinSlider spinSlider;
	private boolean sliderVisible;	
	private CannyEdgeDetector canny;
	private List<CentroidCluster<Line>> clusters;
	private KMeansPlusPlusClusterer<Line> kmeans;
	private File imageDir;

	private List<Line> allLines;
	private List<Line> verifiedLines;
	private List<Line> edgeLines;
	private List<Circle> circles;
	private List<Rectangle> rectangles;
	private List<SignBorder> borders;
	private List<EdgeBorder> edges;
	private List<TextShape> shapes;
	
	private final long tomsec = 1000000;
	private final String imagePath = "C:\\Users\\mike\\java-dev\\parking\\sign-pics";

	public SignTool() {
		setTitle("Sign Image Processing Tool");
		setBackground( Color.gray );
		setLayout(new GridLayout(1,1));
		imagePanel = new JPanel();
		imagePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		
      	getContentPane().add(imagePanel);

		menu = new SignToolMenu(this);
		imageDir = new File(imagePath);

		setSize(400, 400);
		setVisible(true);

		spinSlider = new SpinSlider(this);
		sliderVisible = false;

		cannyFrame = new JFrame();
		cannyFrame.setTitle("canny Image");
		cannyFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		cannyFrame.setVisible(false);

		lineFrame = new JFrame();
		lineFrame.setTitle("Line Image");
		lineFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		lineFrame.setVisible(false);

		circleFrame = new JFrame();
		circleFrame.setTitle("Circle Image");
		circleFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		circleFrame.setVisible(false);

		edgeFrame = new JFrame();
		edgeFrame.setTitle("Edge Image");
		edgeFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		edgeFrame.setVisible(false);

		rectFrame = new JFrame();
		rectFrame.setTitle("Rectangles");
		rectFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		rectFrame.setVisible(false);

		shapeFrame = new JFrame();
		shapeFrame.setTitle("Shapes");
		shapeFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		shapeFrame.setVisible(false);

		textSegmentFrame = new JFrame();
		textSegmentFrame.setTitle("Text Segments");
		textSegmentFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		textSegmentFrame.setVisible(false);

		signFrame = new JFrame();
		signFrame.setTitle("Sign");
		signFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		signFrame.setVisible(false);

		binaryFrame = new JFrame();
		binaryFrame.setTitle("Binary");
		binaryFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		binaryFrame.setVisible(false);

		graphFrame = new JFrame();
		graphFrame.setTitle("Graph");
		graphFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		graphFrame.setVisible(false);

//		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		ProcessOpenCV.initOpenCV();
	}

	public static void main( String args[]) {
		SignTool signTool = new SignTool();
		signTool.pack();
		signTool.setVisible(true);
	}
	
	public void loadImage() {		
		JFileChooser c = new JFileChooser(imageDir);
		int rVal = c.showOpenDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
          	File file = c.getSelectedFile();
        	origImage = new SignImage(file);
//        	imagePanel.add(origImage);
        	zoom(0.25);
//        	zoom(0.5);
//        	zoom(1.0);
      	}
	}

	public void zoom(double factor) {
		if (origImage != null) {
			imagePanel.remove(origImage);
		}
		if (zoomImage != null) {
			imagePanel.remove(zoomImage);
		}
		zoomImage = origImage.scale(factor);
//		zoomImage = new SignImage(ProcessOpenCV.nop(zoomImage.getImage()));
		imagePanel.add(zoomImage);
		setSize(zoomImage.getWidth(), zoomImage.getHeight());
		System.out.println("Zoom image is " + zoomImage.getWidth() + " x " +zoomImage.getHeight());
	}

	public void invoke(int value) {
		if (cannyImage != null) {
			cannyFrame.getContentPane().remove(cannyImage);
		}
		System.out.println("Invoke with value " + value);
		cannyMat = canny.doCannyThreshold(value);
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null));
		cannyFrame.getContentPane().add(cannyImage);
		cannyFrame.setSize(cannyImage.getWidth(), cannyImage.getHeight());
	}

	public void toggleSlider() {
		if (sliderVisible) {
			spinSlider.setVisible(false);
			sliderVisible = false;
		}
		else {
			spinSlider.setVisible(true);
			sliderVisible = true;
		}
	}

	public void doAll() {		
		canny();
//		getShapes();
		getLines();
//		getCircles();		
		getSignEdges();
		getRect();
		signImages();
//		segmentSign();
		binarySign();
	}
/*
	public void doAll() {	
		canny();
		testTextRecog();
	}
*/
	public void canny() {
		long startTime = System.nanoTime();
		if (cannyImage != null) {
			cannyFrame.getContentPane().remove(cannyImage);
		}
		canny = new CannyEdgeDetector(zoomImage.getImage());
//		spinSlider.setSpinner(10, 10, 100, 10);
//		spinSlider.setVisible(true);
//		sliderVisible = true;
		int lowThresh = 50;
		cannyMat = canny.doCannyThreshold(lowThresh);		 
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null));
		cannyFrame.getContentPane().add(cannyImage);
		cannyFrame.setSize(cannyImage.getWidth(), cannyImage.getHeight());
		cannyFrame.setVisible(true);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("canny took "+ duration + " msec");
	}

	public void getShapes() {
		if (shapeImage != null) {
			shapeFrame.getContentPane().remove(shapeImage);
		}
		List<TextShape> shapes = TextShape.getShapes(cannyImage.getImage(), ShapeGenMode.EDGESCAN);
		if (shapes.size() > 0) {
			shapeImage = new SignImage(shapes.get(0).getImage());
			shapeFrame.getContentPane().add(shapeImage);
			shapeFrame.setSize(cannyImage.getWidth(), cannyImage.getHeight());
			shapeFrame.setVisible(true);
			for ( int i=0 ; i<shapes.size() ; i++) {
//				System.out.println(shapes.get(i));
			}
		}
	}

	//
	//  FIXME: these can help when the camera is square but they are not used
	//
	public void getCircles() {
		long startTime = System.nanoTime();
		if (circleImage != null) {
			circleFrame.getContentPane().remove(circleImage);
		}
//		circles = ProcessOpenCV.getCircles(cannyMat, cannyImage.getWidth()/5);
		circles = ProcessOpenCV.getCircles(zoomImage.getImage(), cannyImage.getWidth()/4);
		circleImage = new SignImage(cannyImage.getWidth(), cannyImage.getHeight() );
		circleImage.setCircles(circles);
		circleFrame.getContentPane().add(circleImage);
		circleFrame.setSize(lineImage.getWidth(), lineImage.getHeight());
//		circleFrame.setVisible(true);	
	}

	public void getLines() {
		long startTime = System.nanoTime();
		if (lineImage != null) {
			lineFrame.getContentPane().remove(lineImage);
		}	
		
		allLines = ProcessOpenCV.getLinesP(cannyMat);			
		System.out.println("Raw lines from Hough:");
		for (Line line : allLines) {
//			System.out.println(line);
		}	
		verifiedLines = Line.verifyLines(allLines, cannyImage);

		lineImage = new SignImage( cannyImage.getWidth(), cannyImage.getHeight() );
		lineFrame.getContentPane().add(lineImage);
		lineFrame.setSize(lineImage.getWidth(), lineImage.getHeight());
		lineFrame.setVisible(true);

		lineImage.setLines(verifiedLines);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("hough lines took "+ duration + " msec");
	}

	public void getSignEdges() {
		long startTime = System.nanoTime();
		final Color[] colors = new Color[] { Color.red, Color.green, Color.orange, Color.cyan, Color.pink, Color.yellow, Color.magenta,  Color.blue, Color.lightGray};
		
		if (edgeImage != null) {
			edgeFrame.getContentPane().remove(edgeImage);
		}
		edgeImage = new SignImage( cannyImage.getWidth(), cannyImage.getHeight() );
		edgeFrame.getContentPane().add(edgeImage);
		edgeFrame.setSize(cannyImage.getWidth(), cannyImage.getHeight() );
//		edgeFrame.setVisible(true);
		edges = EdgeBorder.getEdges(zoomImage, verifiedLines);
		int i=0;
		edgeLines = new ArrayList<Line>();
		for (EdgeBorder edge : edges) {
//			System.out.println(edge.getScore()+":"+edge.getEdge());			
			edgeLines.add(edge.getEdge());
			int k = (int)(edge.getScore()*8 + 0.5);
			Color color = colors[k];
			if (i==0) {
				edge.displayEdge(edgeImage.getImage(), color);
			}
			i++;
		}
		edgeImage.setLines(edgeLines);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("sign edges took "+ duration + " msec");
	}

	public void getRect() {
		 
		long startTime = System.nanoTime();
		if (rectImage != null) {
			rectFrame.getContentPane().remove(rectImage);
		}
		int minDim = cannyImage.getWidth() > cannyImage.getHeight() ? cannyImage.getHeight() : cannyImage.getWidth();
		rectImage = new SignImage( cannyImage.getWidth(), cannyImage.getHeight() );
		clusters = clusterLines(edgeLines);
/*		
		List<Line> mergedLines = Line.mergeLines(edgeLines);
		System.out.println("Merged lines:");
		for (Line line : mergedLines) {
			System.out.println(line.score+":"+line);
		}

		clusters = clusterLines(mergedLines);
*/

		rectangles = Rectangle.getRectangles(clusters, minDim);
		rectImage.setRectangles(rectangles);
		rectFrame.getContentPane().add(rectImage);
		rectFrame.setSize(cannyImage.getWidth(), cannyImage.getHeight());
//		rectFrame.setVisible(true);				
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("rectangles took "+ duration + " msec");
		for (Rectangle rect : rectangles) {
//			System.out.println(rect+" "+rect.getScore()+" "+rect.getPerimeter());
		}
	}

	public List<CentroidCluster<Line>> clusterLines(List<Line> lines) {
		final int maxClusters = 6;
		final int maxIterations = 100;
		int nbClusters = lines.size() > maxClusters ? maxClusters : lines.size();
		kmeans = new KMeansPlusPlusClusterer<Line>(nbClusters, maxIterations);
		return kmeans.cluster(lines);
	}
/*
	public void getBorder() {
		long startTime = System.nanoTime();
		if (borderImage != null) {
			borderFrame.getContentPane().remove(borderImage);
		}
		
		borderImage = new SignImage( cannyImage.getWidth(), cannyImage.getHeight() );
		borders = SignBorder.findBorder(zoomImage, rectangles, borderImage.getImage());
		borderImage.setSignBorders(borders);
		borderFrame.getContentPane().add(borderImage);
		borderFrame.setSize( cannyImage.getWidth(), cannyImage.getHeight());
		borderFrame.setVisible(true);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("sign borders took "+ duration + " msec");
	}
*/
	public void signImages() {
		int numTopChoice = 10;
		long startTime = System.nanoTime();
		if (viewedImage != null) {
			signFrame.getContentPane().remove(viewedImage);			
		}

		signImages = SignImage.getSigns(zoomImage, cannyImage, rectangles, true);
/*		
		for ( int i=0 ; i<100 && i<signImages.size(); i++) {
			System.out.println("score="+signImages.get(i).getScoreDetails());
			System.out.println("border="+signImages.get(i).getTheBorder());
			System.out.println("ban="+signImages.get(i).getTheBorder().ancestor());
			System.out.println("orig="+signImages.get(i).getOrigBorder());
			System.out.println("");
		}
*/		
		if (signImages.size() > 0) {			
			viewedImage = signImages.get(0);
			signFrame.getContentPane().add(viewedImage);
			signFrame.setSize( viewedImage.getWidth(), viewedImage.getHeight());
			signFrame.setVisible(true);			
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("sign images took "+ duration + " msec");
	}

	public void segmentSign() {
		long startTime = System.nanoTime();
		if (graphImage != null) {
			graphFrame.getContentPane().remove(graphImage);
		}
		if (signImages.size() > 0) {
			SignImage img = signImages.get(0);
			System.out.println("Top choice is " + img.getWidth() + " x " + img.getHeight());
			Rectangle border =  img.getOrigBorder();				
/*
			SignSegmenter segmenter = new SignSegmenter(img.getImage(), img.getTheBorder());
			segmenter.getSegments();
			List<Rectangle> textBorders = segmenter.getTextBorders();
			img.setRectangles(textBorders);
			showGraph(segmenter.getGraph());
*/			
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("segment image took "+ duration + " msec");
	}

	private void showGraph(BufferedImage graph) {
		if (graph == null) {
			System.out.println("Graph is null...exiting");
			return;
		}
		graphImage = new SignImage(graph);
		graphFrame.getContentPane().add(graphImage);
		System.out.println("Got graph of " + graphImage.getWidth() + " " + graphImage.getHeight());
		graphFrame.setSize(graphImage.getWidth(), graphImage.getHeight());
		graphFrame.setVisible(true);
	}

	public void binarySign() {
		
		long startTime = System.nanoTime();
		if (binaryImage != null) {
			binaryFrame.getContentPane().remove(binaryImage);			
		}
		if (signImages.size() > 0) {
			SignImage img = signImages.get(0);
			BinarySegmenter segmenter = new BinarySegmenter(img.getImage(), cannyImage.getImage(), img.getTheBorder(), img.getOrigBorder());
			binaryImage = new SignImage(segmenter.getBinary());
			binaryImage.setTheBorder(img.getTheBorder());
			binaryFrame.getContentPane().add(binaryImage);
			binaryFrame.setSize( binaryImage.getWidth(), binaryImage.getHeight());
			binaryFrame.setVisible(true);
			binaryShapes();			
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("binary image took "+ duration + " msec");
	}

	public void testTextRecog() {
		if (binaryImage != null) {
			binaryFrame.getContentPane().remove(binaryImage);			
		}
		Point[] corner = new Point[4];
		corner[0] = new Point(0,0);
		corner[1] = new Point(0,zoomImage.getHeight()-1);
		corner[2] = new Point(zoomImage.getWidth()-1, zoomImage.getHeight()-1);
		corner[3] = new Point(zoomImage.getWidth()-1,0);
		Rectangle border = new Rectangle( corner, null );
		BinarySegmenter segmenter = new BinarySegmenter(zoomImage.getImage(), cannyImage.getImage(), border, border);
		binaryImage = new SignImage(segmenter.getBinary());
		binaryImage.setTheBorder(border);
		binaryFrame.getContentPane().add(binaryImage);
		binaryFrame.setSize( binaryImage.getWidth(), binaryImage.getHeight());
		binaryFrame.setVisible(true);
		binaryShapes();	
	}

	private void binaryShapes() {
		if (shapeImage != null) {
			shapeFrame.getContentPane().remove(shapeImage);
		}
		if (textSegmentImage != null) {
			textSegmentFrame.getContentPane().remove(textSegmentImage);
		}

		List<TextShape> shapes = TextShape.getShapes(binaryImage.getImage(),  ShapeGenMode.BINREGION);
		if (shapes.size() > 0) {
			shapeImage = new SignImage(shapes.get(0).getImage());			
			shapeFrame.getContentPane().add(shapeImage);			
			shapeFrame.setSize(binaryImage.getWidth(), binaryImage.getHeight());
			shapeFrame.setVisible(true);
			List<Rectangle> bounds = new ArrayList<Rectangle>(); 
			for ( int i=0 ; i<shapes.size() ; i++) {
				bounds.add(shapes.get(i).getBound());
//				System.out.println(shapes.get(i));
			}
			shapeImage.setRectangles(bounds);			
			List<TextGroup> textGroups = TextGroup.getTextGroups(shapes);
			List<Line> groupsBase = new ArrayList<Line>();
			for (TextGroup group : textGroups) {
				groupsBase.add(group.getBaseline());
				if ( group.size() > 1) {
					System.out.println("Group:"+group);
					for ( TextShape shape : group.getShapes() ) {
						System.out.println(shape);
						if (shape.getTextSegments() != null) {
							for (TextSegment s : shape.getTextSegments()) {
								System.out.println("     "+s);
							}
						}
						if (shape.getTextMatches() != null) {
							for (TextChoice choice : shape.getTextMatches()) {
								System.out.println("     choice "+choice);
							}
						}
					}
				}
			}
			binaryImage.setLines(groupsBase);
			double zoom = 2;
			int zw = round(zoom*binaryImage.getWidth());
			int zh = round(zoom*binaryImage.getHeight());
			textSegmentImage = new SignImage(binaryImage.getWidth(), binaryImage.getHeight(), zoom);
			textSegmentFrame.getContentPane().add(textSegmentImage);			
			textSegmentFrame.setSize(zw, zh);
			textSegmentFrame.setVisible(true);
			textSegmentImage.setLetters(shapes);
		}
	}

	public void showAllLines() {
		cannyImage.setLines(allLines);
	}

	public void showVerifiedLines() {
		cannyImage.setLines(verifiedLines);
	}

	public void showEdgeLines() {
		cannyImage.setLines(edgeLines);
	}

	public void getHistogram() {
		
		Map<Integer, Integer> histogram = cannyImage.getColorDistribution();
		for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
			System.out.println(Integer.toHexString(entry.getKey()) + " count " + entry.getValue());
		}

	}	

	private void displayMergedLines(List<CentroidCluster<Line>> clusters) {
		List<Map<Line, List<Line>>> mergesList = Line.mergeLinesDebug(clusters);
		int c = 0;
		for ( Map<Line, List<Line>> merges : mergesList) {
			int ln = 0;
			for (Map.Entry<Line, List<Line>> entry : merges.entrySet()) {
				Line line = entry.getKey();
				List<Line> ml = entry.getValue();
				if ( ml != null && ml.size() > 0) {
					JFrame frame = new JFrame();
					frame.setTitle("cluster " + c + " line " + ln + " has "+ ml.size() + " merges");
					frame.setLayout(new FlowLayout(FlowLayout.LEFT));
					frame.setVisible(true);
					frame.setSize(cannyImage.getWidth(), cannyImage.getHeight());
					SignImage image = new SignImage(cannyImage.getWidth(), cannyImage.getHeight());
					frame.getContentPane().add(image);
					List<Line> mLines = new ArrayList<Line>();
					mLines.add(line);
					for (Line l : ml) {
						mLines.add(l);

					}
					image.setLines(mLines);
				}
				ln++;
			}
			c++;
		}
	}

	void readSign() {
		SignBuilder builder = new SignBuilder(zoomImage);
		ParkingSchedule schedule = builder.readSign(0);
	}

	private static int round(double val) {
		return (int)(val +0.5);
	}
}