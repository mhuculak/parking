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
import parking.opencv.ColorSegmenter;
import parking.opencv.ShapeGenMode;
import parking.opencv.WindowCluster;
import parking.opencv.SignRecognizer;

import parking.schedule.ParkingSchedule;
import parking.schedule.ParkingSign;
import parking.schedule.ParkingSignType;

import parking.database.DbCleanup;

import parking.util.Profiler;
import parking.util.Logger;
import parking.util.LoggingTag;
import parking.util.Utils;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;  

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
	private SignRecognizer signRecognizer;
	private ColorSegmenter colorSegmenter;

	private Logger logger;
	
	private final long tomsec = 1000000;
	private final String imagePath = "C:\\Users\\mike\\java-dev\\parking\\sign-pics";

	public SignTool() {
		File tagMapsFile = new File("tagMaps.txt");
		logger = new Logger( tagMapsFile, LoggingTag.Image, this);
		setTitle("Sign Image Processing Tool");
		setBackground( Color.gray );
		setLayout(new GridLayout(1,1));
		imagePanel = new JPanel();
		imagePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                super.mouseClicked(me);
                System.out.println("Got mouse event " +  me.getX() + " " + me.getY());
                if (colorSegmenter != null) {
                	colorSegmenter.logWindow(me.getX(), me.getY(), logger);
                }
            }
        });
		
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
	
	public void loadImage4() {		
		JFileChooser c = new JFileChooser(imageDir);
		int rVal = c.showOpenDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
          	File file = c.getSelectedFile();
        	origImage = new SignImage(file, logger);
        	zoom(0.25);
      	}
	}

	public void loadImage() {		
		JFileChooser c = new JFileChooser(imageDir);
		int rVal = c.showOpenDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
          	File file = c.getSelectedFile();
        	origImage = new SignImage(file, logger);
        	zoom(1.0);
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
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null), logger);
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
//		getRect();
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
		cannyImage = new SignImage(OpenCVConvert.matToBufferedImage(cannyMat, null), logger);
		cannyFrame.getContentPane().add(cannyImage);
		cannyFrame.setSize(cannyImage.getWidth(), cannyImage.getHeight());
//		cannyFrame.setVisible(true);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/tomsec;
		System.out.println("canny took "+ duration + " msec");
	}

	public void getShapes() {
		if (shapeImage != null) {
			shapeFrame.getContentPane().remove(shapeImage);
		}
		List<TextShape> shapes = TextShape.getShapes(cannyImage.getImage(), ShapeGenMode.EDGESCAN, logger);
		if (shapes.size() > 0) {
			shapeImage = new SignImage(shapes.get(0).getImage(), logger);
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
/*		
		clusters = clusterLines(edgeLines);
		
		List<Line> mergedLines = Line.mergeLines(edgeLines);
		System.out.println("Merged lines:");
		for (Line line : mergedLines) {
			System.out.println(line.score+":"+line);
		}

		clusters = clusterLines(mergedLines);


		rectangles = Rectangle.getRectanglesFromClusters(clusters, minDim);
*/		
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
/*
	public List<CentroidCluster<Line>> clusterLines(List<Line> lines) {
		final int maxClusters = 6;
		final int maxIterations = 100;
		int nbClusters = lines.size() > maxClusters ? maxClusters : lines.size();
		kmeans = new KMeansPlusPlusClusterer<Line>(nbClusters, maxIterations);
		return kmeans.cluster(lines);
	}

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

		signImages = SignImage.getSigns(zoomImage, cannyImage, rectangles, null, true, logger);
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
		graphImage = new SignImage(graph, logger);
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
			binaryImage = new SignImage(segmenter.getBinary(), logger);
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

	public void doColorSegment() {
		canny();
		int numWindows = 20;
		int numWindowClusters = 20;
		colorSegmenter = new ColorSegmenter(zoomImage.getImage(), numWindows);
		SignImage colorImage = new SignImage(colorSegmenter.getImage(), logger);
		double minColorDist = 0.1;
		colorSegmenter.mergeWindows(minColorDist);
		SignImage mergedImage = new SignImage( colorSegmenter.applyColorWindows(zoomImage.getImage()), logger );		
		colorSegmenter.computeWindowStats(3);
		List<WindowCluster> clusterList = colorSegmenter.clusterWindows(numWindowClusters);
		List<Boundary> clusterBoundaryList = colorSegmenter.findClusterBoundaries(clusterList);
		mergedImage.setBoundaries(clusterBoundaryList);
		show("Color Segmentation", mergedImage);
		colorSegmenter.labelClustersByProximity(clusterList);		
		logger.log(colorSegmenter.clustersAsString());
		for (WindowCluster cl : clusterList) {
			logger.log(cl.toString());
		}
		List<String> clusterLabels = new ArrayList<String>();
		for (WindowCluster cl :  clusterList) {
			clusterLabels.add(cl.getLabel());
		}
		colorSegmenter.applyLabels(clusterLabels);
		logger.log(colorSegmenter.clusterLabelAsString());
		
	}


	public void doHideBackground() {
		List<Color> foreground = new ArrayList<Color>();
/*		
		foreground.add(Color.black);
		foreground.add(Color.white);
		foreground.add(Color.red);
		SignImage foregroundImage = new SignImage( colorSegmenter.hideBackground( zoomImage.getImage(), foreground ), logger );
*/
		SignImage foregroundImage = new SignImage( colorSegmenter.hideBackground( zoomImage.getImage()), logger );		
		show("Foreground colors", foregroundImage);
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
		binaryImage = new SignImage(segmenter.getBinary(), logger);
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

		List<TextShape> shapes = TextShape.getShapes(binaryImage.getImage(),  ShapeGenMode.BINREGION, logger);
		if (shapes.size() > 0) {
			shapeImage = new SignImage(shapes.get(0).getImage(), logger);			
			shapeFrame.getContentPane().add(shapeImage);			
			shapeFrame.setSize(binaryImage.getWidth(), binaryImage.getHeight());
			shapeFrame.setVisible(true);
			List<Rectangle> bounds = new ArrayList<Rectangle>(); 
			for ( int i=0 ; i<shapes.size() ; i++) {
				bounds.add(shapes.get(i).getBound());
//				System.out.println(shapes.get(i));
			}
			shapeImage.setRectangles(bounds);			
			List<TextGroup> textGroups = TextGroup.getTextGroups(shapes, logger);
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
			SignImage img = signImages.get(0);
			ParkingSignType signType = img.getSignType(cannyImage, allLines, img.getOrigBorder(), textGroups);
			ParkingSchedule schedule = ParkingSign.readSchedule(signType, textGroups, logger);
			logger.log(schedule.toString());
			binaryImage.setLines(groupsBase);
			double zoom = 2;
			int zw = Utils.round(zoom*binaryImage.getWidth());
			int zh = Utils.round(zoom*binaryImage.getHeight());
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

	public void readSign() {
		SignImage image = null;
		JFileChooser c = new JFileChooser(imageDir);
		int rVal = c.showOpenDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
          	File file = c.getSelectedFile();
        	image = new SignImage(file, logger);
      	}
      	Profiler profiler = new Profiler("readSign", null, this);
		signRecognizer = new SignRecognizer(image, logger);
		ParkingSchedule schedule = signRecognizer.readSign(0);
		profiler.stop();
		logger.logProfile(profiler);
	}

	public void readSignFromLoadedImage() {
		signRecognizer = new SignRecognizer(origImage, logger);
		ParkingSchedule schedule = signRecognizer.readSign(0);
	}

	public SignRecognizer getRecognizer() {
		return signRecognizer;
	}

	public void show(String title, SignImage image) {
		int width = image.getWidth() > 800 ? 800 : image.getWidth();
		int height = image.getHeight() > 1000 ? 1000 : image.getHeight();		
		double scaleFactor = 800.0 /image.getMinDim();
		logger.log("got scale factor = "+scaleFactor);
		SignImage displayImage = scaleFactor < 1.0 ? image.scale(scaleFactor) : image;
		logger.log("display "+title+" size "+image.getWidth()+" x "+image.getHeight()+" as "+displayImage.getWidth()+" x "+displayImage.getHeight());
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout(FlowLayout.LEFT));
		frame.setTitle(title);
		frame.setSize(width, height);
		frame.add(displayImage);
		frame.setVisible(true);    
	}

	public void testSignDatabase() {
		SignTester signTester = new SignTester(logger);
//		signTester.run();
	}

	public void verifySignDatabase() {
		SignVerifier signVerifier = new SignVerifier(logger);
//		signTester.run();
	}

	public void cleanupSignDatabase() {
		DbCleanup dbCleanup = new DbCleanup(logger);
		dbCleanup.findMissingPictures("demo_test");
		dbCleanup.removeDuplicatePictures("demo_test");
		dbCleanup.addMissingSigns("demo_test");
		dbCleanup.findMissingPictures("demo_test");
	}

	public void viewSignDatabase() {
		DBViewer dbViewer = new DBViewer(logger);
		dbViewer.viewDatabase();
	}
}