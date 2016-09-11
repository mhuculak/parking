package parking.opencv;

import parking.display.SignImage;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.CvType;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;  
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;

import java.util.List;
import java.util.ArrayList;

class BinaryWindow {
	public double threshold;
	public int cannyEdgeCount;
	public int xWinSize;
	public int yWinSize;
	public int xpos;
	public int ypos;

	private final int WHITE = 0xffffffff;

	public BinaryWindow( int xpos, int ypos, int xWinSize, int yWinSize) {
		this.xpos = xpos;
		this.ypos = ypos;
		this.xWinSize = xWinSize;
		this.yWinSize = yWinSize;
	}	

	public String toString() {
		return threshold+" "+cannyEdgeCount+" "+xpos+","+ypos+" "+xWinSize+" x "+yWinSize;
	}

	public void adjustSize(BufferedImage canny, int minEdgeCount, int windowIncrement) {
		int count = getCountForWindow(canny);
		while (count < minEdgeCount && xWinSize < canny.getWidth() && yWinSize < canny.getHeight()) {
			xpos -= windowIncrement;
			ypos -= windowIncrement;
			xWinSize += 2*windowIncrement;
			yWinSize += 2*windowIncrement;
			count += getCountForAddedWindow(canny, windowIncrement);
		}
		cannyEdgeCount = count;
	}

	private int getCountForAddedWindow(BufferedImage canny, int incr) {
		int count = 0;
		for ( int x=xpos ; x<xpos+incr ; x++) {
			for ( int y=ypos ; y<ypos+yWinSize ; y++ ) {
				count += getCount(canny, x, y);
				count += getCount(canny, xpos+xWinSize-x-1, y);
			}
		}
		for ( int x=xpos+incr ; x<xpos+xWinSize+incr ; x++) {
			for ( int y=ypos ; y<ypos+incr ; y++) {
				count += getCount(canny, x, y);
				count += getCount(canny, x, ypos+xWinSize-y-1);
			}
		}
		return count;
	}

	private int getCountForWindow(BufferedImage canny) {
		
		int count = 0;
		for ( int x=xpos ; x<xpos+xWinSize ; x++ ) {
			for ( int y=ypos ; y<ypos+yWinSize ; y++ ) {
				count += getCount(canny, x, y);
			}
		}
		return count;
	}

	private int getCount(BufferedImage canny, int x, int y) {
		if (x>=0 && y>=0 && x<canny.getWidth() && y<canny.getHeight() ) {
			int value = canny.getRGB(x,y);
			if (value == WHITE) {
				return 1;
			}
		}
		return 0;
	}
}

public class BinarySegmenter {
	
	private Rectangle border;
	private BufferedImage original;
	private BufferedImage canny;
	private BufferedImage binary;
	private BinaryWindow[][] bwindow;
	private int xNominalWinSize;
	private int yNominalWinSize;
	
	private static final int numWindows = 10;
	private static final int minEdgeCount = 100;
	private static final int windowAdjustIncrement = 1;

	public BinarySegmenter(BufferedImage original, BufferedImage canny, Rectangle border, Rectangle origBorder) {
		this.original = original;
		if (border != origBorder) {
			this.canny = SignImage.applyBorderTransform(canny, origBorder);
		}
		else {
			this.canny = canny;
		}
		this.border = border;
//		System.out.println("binary segment image of size "+original.getWidth()+" x "+original.getHeight());
		setupWindows();		
		computeBinary();
	}
	
	private void setupWindows() {
		double xnom = original.getWidth();
		xnom = xnom/numWindows + 0.5;
		xNominalWinSize = (int)xnom;
		double ynom = original.getHeight();
		ynom = ynom/numWindows + 0.5;
		yNominalWinSize = (int)ynom;		
		bwindow = new BinaryWindow[numWindows+1][numWindows+1];
		for (int xWin = 0 ; xWin<=numWindows ; xWin ++) {
			int x = xWin*xNominalWinSize;
			for ( int yWin = 0 ; yWin<=numWindows ; yWin++) {
				int y = yWin*yNominalWinSize;
				BinaryWindow bwin = new BinaryWindow(x,y,xNominalWinSize,yNominalWinSize);
				bwin.adjustSize(canny, minEdgeCount, windowAdjustIncrement);
				bwindow[xWin][yWin] = bwin;				
			}
		}
	}

	private void computeThreshold() {		
		for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
			for ( int yWin = 0 ; yWin<numWindows ; yWin++) {				
//				System.out.println(xWin+","+yWin+"="+bwindow[xWin][yWin]);
				computeWindowThreshold(bwindow[xWin][yWin]);
			}
		}
	}

	private void computeWindowThreshold(BinaryWindow bwin) {
		List<Intensity> intensity = new ArrayList<Intensity>();
		for ( int x=bwin.xpos ; x<bwin.xpos+bwin.xWinSize ; x++) {
			for ( int y=bwin.ypos ; y<bwin.ypos+bwin.yWinSize ; y++) {
				if (x>=0 && y>=0 && x<original.getWidth() && y<original.getHeight() ) {
					Intensity ival = ImageBorderParams.getIntensity(new Color(original.getRGB(x,y)));
					intensity.add(ival);
				}
			}
		}
		KMeansPlusPlusClusterer<Intensity> kmeans = new KMeansPlusPlusClusterer<Intensity>(2, 10);
		List<CentroidCluster<Intensity>> clusters = kmeans.cluster(intensity);
		double foreground = 1.0;
		double maxForeground = 0.0;
		for (CentroidCluster<Intensity> cl : clusters) {
			double[] iarr = cl.getCenter().getPoint();
			double inten = iarr[0];
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
		bwin.threshold = maxForeground;
	}

	private void computeBinary() {
		computeThreshold();
		binary = new BufferedImage ( original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB );
		Graphics g = binary.createGraphics();
		for ( int x=0 ; x<original.getWidth() ; x++) {
			for ( int y=0 ; y<original.getHeight() ; y++) {
				double value = ImageBorderParams.getGreyLevel(new Color(original.getRGB(x,y)));
				double threshold = getThreshold(x,y);
				if (value > threshold) {
					g.setColor(Color.white);				
				}
				else {
					g.setColor(Color.black);
				}
				g.fillRect(x,y,1,1);
			}
		}
		g.dispose();
	}

	private double getThreshold(int x, int y) {
		int xWin = x/xNominalWinSize;
		int yWin = y/yNominalWinSize;
		double thr = 0.0;
		thr = bwindow[xWin][yWin].threshold;
		return thr;
	}

	public BufferedImage getBinary() {
		return binary;
	}
}