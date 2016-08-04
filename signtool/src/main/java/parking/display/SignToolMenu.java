package parking.display;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class SignToolMenu {

	private SignTool signTool;
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu viewMenu;
	private JMenu imageMenu;
	private JMenu lineMenu;
	private JMenuItem fileLoadImage;
	private JMenuItem viewZoom;
	private JMenuItem viewZoom10;
	private JMenuItem viewZoom25;
	private JMenuItem viewSlider;
	private JMenuItem imageAll;
	private JMenuItem imageReadSign;
	private JMenuItem imageCanny;
	private JMenuItem imageLines;
	private JMenuItem imageRect;
	private JMenuItem imageBorder;
	private JMenuItem imageHistogram;
	private JMenuItem lineAll;
	private JMenuItem lineVerified;
	private JMenuItem lineMerged;
	
	private FileMenuListener fileMenuListener;
	private ViewMenuListener viewMenuListener;
	private ImageMenuListener imageMenuListener;
	private LineMenuListener lineMenuListener;
	
	public SignToolMenu(SignTool signTool) {

		this.signTool = signTool;

		menuBar = new JMenuBar();
		signTool.setJMenuBar(menuBar);

		fileMenu = new JMenu("File");
		viewMenu = new JMenu("View");
		imageMenu = new JMenu("Image");
		lineMenu = new JMenu("Line");

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(imageMenu);
		menuBar.add(lineMenu);

		fileMenuListener = new FileMenuListener();
		viewMenuListener = new ViewMenuListener();
		imageMenuListener = new ImageMenuListener();
		lineMenuListener = new LineMenuListener();

		fileLoadImage = new JMenuItem("Load Image");
		fileLoadImage.setActionCommand("Load");
		fileLoadImage.addActionListener(fileMenuListener);
		fileMenu.add(fileLoadImage);


		viewZoom10 = new JMenuItem("Zoom 10%");
		viewZoom10.setActionCommand("10");
		viewZoom10.addActionListener(viewMenuListener);
		viewMenu.add(viewZoom10);

		viewZoom25 = new JMenuItem("Zoom 25%");
		viewZoom25.setActionCommand("25");
		viewZoom25.addActionListener(viewMenuListener);
		viewMenu.add(viewZoom25);

		viewSlider = new JMenuItem("Slider");
		viewSlider.setActionCommand("Slider");
		viewSlider.addActionListener(viewMenuListener);
		viewMenu.add(viewSlider);

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
         }
	}

	class ViewMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("10")){
         		signTool.zoom(0.1);
         	}
         	else if (cmd.equals("25")){
         		signTool.zoom(0.25);
         	}
         	else if (cmd.equals("Slider")) {
         		signTool.toggleSlider();
         	}
         }
	}

	class ImageMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("Read Sign")) {
				signTool.readSign();
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