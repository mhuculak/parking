package parking.display;

import parking.opencv.SignRecognizer;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class SignToolMenu {

	private SignTool signTool;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu testMenu;
	private JMenu viewMenu;
	private JMenu imageMenu;
	private JMenu lineMenu;
	private JMenuItem fileLoadImage;
	private JMenuItem fileLoadImage4;
	private JMenuItem fileReadSign;
	private JMenuItem testSignDB;
	private JMenuItem verifySignDB;
	private JMenuItem cleanupDB;
	private JMenuItem viewDB;
	private JMenuItem viewWorking;
	private JMenuItem viewLines;
	private JMenuItem viewEdges;
	private JMenuItem viewRectangles;
	private JMenuItem viewClusterBoundaries;
	private JMenuItem viewResized;
	private JMenuItem viewCannyOrig;
	private JMenuItem viewCannyRaw;
	private JMenuItem viewTransCanny;
	private JMenuItem viewResizedCanny;
	private JMenuItem viewBinary;
	private JMenuItem viewShape;
	private JMenuItem viewTextSegments;
	private JMenuItem viewTextSegments2;
	private JMenuItem imageAll;
	private JMenuItem imageReadSign;
	private JMenuItem imageCanny;
	private JMenuItem imageLines;
	private JMenuItem imageRect;
	private JMenuItem imageBorder;
	private JMenuItem imageHistogram;
	private JMenuItem imageColorSegment;
//	private JMenuItem imageColorMerge;
	private JMenuItem imageHideBackground;
	private JMenuItem lineAll;
	private JMenuItem lineVerified;
	private JMenuItem lineMerged;
	
	private FileMenuListener fileMenuListener;
	private TestMenuListener testMenuListener;
	private ViewMenuListener viewMenuListener;
	private ImageMenuListener imageMenuListener;
	private LineMenuListener lineMenuListener;
	
	public SignToolMenu(SignTool signTool) {

		this.signTool = signTool;

		menuBar = new JMenuBar();
		signTool.setJMenuBar(menuBar);

		fileMenu = new JMenu("File");
		testMenu = new JMenu("Database");
		viewMenu = new JMenu("View");
		imageMenu = new JMenu("Image");
		lineMenu = new JMenu("Line");

		menuBar.add(fileMenu);
		menuBar.add(testMenu);
		menuBar.add(viewMenu);
		menuBar.add(imageMenu);
		menuBar.add(lineMenu);

		fileMenuListener = new FileMenuListener();
		testMenuListener = new TestMenuListener();
		viewMenuListener = new ViewMenuListener();
		imageMenuListener = new ImageMenuListener();
		lineMenuListener = new LineMenuListener();

		fileLoadImage = new JMenuItem("Load Image");
		fileLoadImage.setActionCommand("Load");
		fileLoadImage.addActionListener(fileMenuListener);
		fileMenu.add(fileLoadImage);

		fileLoadImage4 = new JMenuItem("Load Image 0.25");
		fileLoadImage4.setActionCommand("Load4");
		fileLoadImage4.addActionListener(fileMenuListener);
		fileMenu.add(fileLoadImage4);

		fileReadSign = new JMenuItem("Read Sign");
		fileReadSign.setActionCommand("Read Sign");
		fileReadSign.addActionListener(fileMenuListener);
		fileMenu.add(fileReadSign);

		testSignDB = new JMenuItem("Test Signs");
		testSignDB.setActionCommand("SignDatabase");
		testSignDB.addActionListener(testMenuListener);
		testMenu.add(testSignDB);

		verifySignDB = new JMenuItem("Verify Signs");
		verifySignDB.setActionCommand("VerifySigns");
		verifySignDB.addActionListener(testMenuListener);
		testMenu.add(verifySignDB);

		cleanupDB = new JMenuItem("Cleanup DB");
		cleanupDB.setActionCommand("CleanupDB");
		cleanupDB.addActionListener(testMenuListener);
		testMenu.add(cleanupDB);

		viewDB = new JMenuItem("View DB");
		viewDB.setActionCommand("ViewDB");
		viewDB.addActionListener(testMenuListener);
		testMenu.add(viewDB);

		viewWorking = new JMenuItem("Working");
		viewWorking.setActionCommand("Working");
		viewWorking.addActionListener(viewMenuListener);
		viewMenu.add(viewWorking);

		viewLines = new JMenuItem("Lines");
		viewLines.setActionCommand("Lines");
		viewLines.addActionListener(viewMenuListener);
		viewMenu.add(viewLines);

		viewEdges = new JMenuItem("Edges");
		viewEdges.setActionCommand("Edges");
		viewEdges.addActionListener(viewMenuListener);
		viewMenu.add(viewEdges);

		viewRectangles = new JMenuItem("Rectangles");
		viewRectangles.setActionCommand("Rectangles");
		viewRectangles.addActionListener(viewMenuListener);
		viewMenu.add(viewRectangles);

		viewClusterBoundaries = new JMenuItem("ClusterBoundaries");
		viewClusterBoundaries.setActionCommand("ClusterBoundaries");
		viewClusterBoundaries.addActionListener(viewMenuListener);
		viewMenu.add(viewClusterBoundaries);

		

		viewResized = new JMenuItem("Resized");
		viewResized.setActionCommand("Resized");
		viewResized.addActionListener(viewMenuListener);
		viewMenu.add(viewResized);

		viewCannyOrig = new JMenuItem("CannyOrig");
		viewCannyOrig.setActionCommand("CannyOrig");
		viewCannyOrig.addActionListener(viewMenuListener);
		viewMenu.add(viewCannyOrig);

		viewCannyRaw = new JMenuItem("CannyRaw");
		viewCannyRaw.setActionCommand("CannyRaw");
		viewCannyRaw.addActionListener(viewMenuListener);
		viewMenu.add(viewCannyRaw);

		viewTransCanny = new JMenuItem("TransCanny");
		viewTransCanny.setActionCommand("TransCanny");
		viewTransCanny.addActionListener(viewMenuListener);
		viewMenu.add(viewTransCanny);

		viewResizedCanny = new JMenuItem("ResizedCanny");
		viewResizedCanny.setActionCommand("ResizedCanny");
		viewResizedCanny.addActionListener(viewMenuListener);
		viewMenu.add(viewResizedCanny);

		viewBinary = new JMenuItem("Binary");
		viewBinary.setActionCommand("Binary");
		viewBinary.addActionListener(viewMenuListener);
		viewMenu.add(viewBinary);

		viewShape = new JMenuItem("Shape");
		viewShape.setActionCommand("Shape");
		viewShape.addActionListener(viewMenuListener);
		viewMenu.add(viewShape);

		viewTextSegments = new JMenuItem("TextSegments");
		viewTextSegments.setActionCommand("TextSegments");
		viewTextSegments.addActionListener(viewMenuListener);
		viewMenu.add(viewTextSegments);

		viewTextSegments2 = new JMenuItem("TextSegments2");
		viewTextSegments2.setActionCommand("TextSegments2");
		viewTextSegments2.addActionListener(viewMenuListener);
		viewMenu.add(viewTextSegments2);

		imageReadSign = new JMenuItem("Read Sign");
		imageReadSign.setActionCommand("Read Sign");
		imageReadSign.addActionListener(imageMenuListener);
		imageMenu.add(imageReadSign);

		imageAll = new JMenuItem("All");
		imageAll.setActionCommand("All");
		imageAll.addActionListener(imageMenuListener);
		imageMenu.add(imageAll);

		imageCanny = new JMenuItem("Canny");
		imageCanny.setActionCommand("Canny");
		imageCanny.addActionListener(imageMenuListener);
		imageMenu.add(imageCanny);

		imageLines = new JMenuItem("Lines");
		imageLines.setActionCommand("Lines");
		imageLines.addActionListener(imageMenuListener);
		imageMenu.add(imageLines);

		imageRect = new JMenuItem("Rect");
		imageRect.setActionCommand("Rect");
		imageRect.addActionListener(imageMenuListener);
		imageMenu.add(imageRect);

		imageHistogram = new JMenuItem("Histogram");
		imageHistogram.setActionCommand("Histogram");
		imageHistogram.addActionListener(imageMenuListener);
		imageMenu.add(imageHistogram);
		
		imageColorSegment = new JMenuItem("ColorSegment");
		imageColorSegment.setActionCommand("ColorSegment");
		imageColorSegment.addActionListener(imageMenuListener);
		imageMenu.add(imageColorSegment);
/*
		imageColorMerge = new JMenuItem("ColorMerge");
		imageColorMerge.setActionCommand("ColorMerge");
		imageColorMerge.addActionListener(imageMenuListener);
		imageMenu.add(imageColorMerge);
*/

		imageHideBackground = new JMenuItem("HideBackground");
		imageHideBackground.setActionCommand("HideBackground");
		imageHideBackground.addActionListener(imageMenuListener);
		imageMenu.add(imageHideBackground);

		imageBorder = new JMenuItem("Border");
		imageBorder.setActionCommand("Border");
		imageBorder.addActionListener(imageMenuListener);
		imageMenu.add(imageBorder);

		lineAll = new JMenuItem("All Lines");
		lineAll.setActionCommand("All");
		lineAll.addActionListener(lineMenuListener);
		lineMenu.add(lineAll);

		lineVerified = new JMenuItem("Verified");
		lineVerified.setActionCommand("Verified");
		lineVerified.addActionListener(lineMenuListener);
		lineMenu.add(lineVerified);

		lineMerged = new JMenuItem("Edge");
		lineMerged.setActionCommand("Edge");
		lineMerged.addActionListener(lineMenuListener);
		lineMenu.add(lineMerged);
	}

	class FileMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("Load")){
         		signTool.loadImage();
         	}
         	else if (cmd.equals("Load4")) {
         		signTool.loadImage4();
         	}
         	else if(cmd.equals("Read Sign")) {
         		signTool.readSign();
         	}
         }
	}
 
	class TestMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("SignDatabase")){
         		signTool.testSignDatabase();
         	}
         	else if (cmd.equals("VerifySigns")){
         		signTool.verifySignDatabase();
         	}
         	else if (cmd.equals("CleanupDB")){
         		signTool.cleanupSignDatabase();
         	}
         	else if (cmd.equals("ViewDB")){
         		signTool.viewSignDatabase();
         	}
         }
    }

	class ViewMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			SignRecognizer recognizer = signTool.getRecognizer();
			if (cmd.equals("Working")){
       			signTool.show(cmd, recognizer.getWorkingImage());
         	}
         	else if (cmd.equals("Lines")){
       			signTool.show(cmd, recognizer.getLineImage());
         	}
         	else if (cmd.equals("Edges")){
       			signTool.show(cmd, recognizer.getEdgeImage());
         	}
         	else if (cmd.equals("Rectangles")){
       			signTool.show(cmd, recognizer.getRectangleImage());
         	}
         	else if (cmd.equals("ClusterBoundaries")){
       			signTool.show(cmd, recognizer.showClusterBoundaries());
         	}         	
         	else if (cmd.equals("Resized")){
       			signTool.show(cmd, recognizer.getResizedImage());
         	}
         	else if (cmd.equals("CannyOrig")){
         		signTool.show(cmd, recognizer.getCannyOrig());
         	}
         	else if (cmd.equals("CannyRaw")) {
         		signTool.show(cmd, recognizer.getRawCanny());
         	}
         	else if (cmd.equals("TransCanny")) {
         		signTool.show(cmd, recognizer.getTransCanny());
         	}
         	else if (cmd.equals("ResizedCanny")) {
         		signTool.show(cmd, recognizer.getResizedCanny());
         	}
         	else if (cmd.equals("Binary")) {
         		signTool.show(cmd, recognizer.getBinaryImage());
         	}
         	else if (cmd.equals("Shape")) {
         		signTool.show(cmd, recognizer.getShapeImage());
         	}
         	else if (cmd.equals("TextSegments")) {
         		signTool.show(cmd, recognizer.getTextSegmentImage(1.0));
         	}
         	else if (cmd.equals("TextSegments2")) {
         		signTool.show(cmd, recognizer.getTextSegmentImage(2.0));
         	}
         }
	}

	class ImageMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("Read Sign")) {
				signTool.readSignFromLoadedImage();
			}
			else if (cmd.equals("All")) {
				signTool.doAll();
			}
			else if (cmd.equals("Canny")){
         		signTool.canny();
         	}
         	else if (cmd.equals("Lines")) {
         		signTool.getLines();
         	}
         	else if (cmd.equals("Rect")) {
         		signTool.getRect();
         	}
         	else if (cmd.equals("Border")) {
 //        		signTool.getBorder();
         	}
         	else if (cmd.equals("Histogram")) {
         		signTool.getHistogram();
         	}
         	else if (cmd.equals("ColorSegment")) {
         		signTool.doColorSegment();
         	}
/*
         	else if (cmd.equals("ColorMerge")) {
         		signTool.doColorMerge();
         	}
*/         	
         	else if (cmd.equals("HideBackground")) {
         		signTool.doHideBackground();
         	}
         }
	}

	class LineMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("All")){
         		signTool.showAllLines();
         	}
         	else if (cmd.equals("Verified")) {
         		signTool.showVerifiedLines();
         	}
         	else if (cmd.equals("Edge")) {
         		signTool.showEdgeLines();
         	}
        }
    }
}