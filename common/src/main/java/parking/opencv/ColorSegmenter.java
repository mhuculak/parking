package parking.opencv;

import parking.display.SignImage;
import parking.display.Boundary;
import parking.util.Logger;

import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.CvType;

import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.CentroidCluster;  
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class MooreWin {
	public ColorWindow cwin;
	public ColorWindow prev;
	public int k;
	public int x;
	public int y;
	public int px;
	public int py;
	public int b;
	public int c;
	public String s;

	public MooreWin(ColorWindow cwin, ColorWindow prev, int k) {
		this.cwin = cwin;
		this.prev = prev;
		this.k = k;
		x = cwin.xIndex;
		y = cwin.yIndex;
		if (prev != null) {
			px = prev.xIndex;
			py = prev.yIndex;
		}
		else {
			px = -1;
			py = -1;
		}
		b = cwin.boundary;
		c = cwin.clusterNum;
		s = "curr:"+x+" "+y+" k:"+k+" prev:"+px+" "+py+" cn:"+c+" b:"+b;
	}
}

class MyColor implements Clusterable {

	public Color color;

	public MyColor(int rgb) {
		this.color = new Color(rgb);
	}

	public MyColor(Color color) {
		this.color = color;
	}

	public double[] getPoint() {
		float[] comp = color.getRGBColorComponents(null);
		double[] arr = new double[3];
		for ( int i=0 ; i<3 ; i++) {
			arr[i] = comp[i];
		}
		return arr;
	}
}

public class ColorSegmenter {
	
	private BufferedImage original;
	private BufferedImage colorImg;
	private ColorWindow[][] cwindow;
	private int xNominalWinSize;
	private int yNominalWinSize;
	private int numWindows;
	private List<CentroidCluster<ColorWindow>> winClusters;

	private static Pos[] clockwise = { new Pos(-1,0), new Pos(-1,-1), new Pos(0,-1), new Pos(1,-1),
								new Pos(1,0), new Pos(1,1), new Pos(0,1), new Pos(-1,1)};

	public ColorSegmenter(BufferedImage original, int numWindows) {
		this.original = original;
		this.numWindows = numWindows;
		setupWindows();		
		colorImg = new BufferedImage ( original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB );
		Graphics g = colorImg.createGraphics();
		scanImage(g);	
		g.dispose();
	}

	public ColorWindow[][] getWindows() {
		return cwindow;
	}

	public void mergeWindows(double minColorDist) {
		for ( int i=0 ; i<cwindow.length ; i++ ) {
			int nexti = i == cwindow.length - 1 ? -1 : i+1;
			for ( int j=0 ; j<cwindow[i].length ; j++ ) {
				ColorWindow curr = cwindow[i][j];
				int nextj = j == cwindow[i].length - 1 ? -1 : j+1;
				if ( nexti > 0 ) {
					ColorWindow right = cwindow[nexti][j];
					curr.mergeWith(right, minColorDist);
				}
				if (nextj > 0) {
					ColorWindow down = cwindow[i][nextj];
					curr.mergeWith(down, minColorDist);
				}
			}
		}
	}

	public BufferedImage applyColorWindows(BufferedImage inputImage) {
		BufferedImage outputImage = new BufferedImage ( inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB );
		Graphics g = outputImage.createGraphics();	
		for ( int i=0 ; i<cwindow.length ; i++ ) {
			for ( int j=0 ; j<cwindow[i].length ; j++ ) {
				ColorWindow curr = cwindow[i][j];
				curr.apply(g, inputImage);
			}
		}
		g.dispose();
		return outputImage;
	}

	public void computeWindowStats(int kmeansNumColors) {
		for ( int i=0 ; i<cwindow.length ; i++ ) {
			for ( int j=0 ; j<cwindow[i].length ; j++ ) {
				ColorWindow curr = cwindow[i][j];
				curr.setStandardColors();
				curr.computeStats(original, kmeansNumColors);
			}
		}
	}
/*
	public BufferedImage hideBackground(BufferedImage inputImage, List<Color> foreground) {
		BufferedImage outputImage = new BufferedImage ( inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB );
		Graphics g = outputImage.createGraphics();	
		for ( int i=0 ; i<cwindow.length ; i++ ) {
			for ( int j=0 ; j<cwindow[i].length ; j++ ) {
				ColorWindow curr = cwindow[i][j];
				curr.applyAlpha(g, inputImage, curr.getDistFrom(foreground));
			}
		}
		g.dispose();
		return outputImage;
	}
*/
	public BufferedImage hideBackground(BufferedImage inputImage) {
		final double maxVariance = 100000;
		BufferedImage outputImage = new BufferedImage ( inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB );
		Graphics g = outputImage.createGraphics();	
		for ( int i=0 ; i<cwindow.length ; i++ ) {
			for ( int j=0 ; j<cwindow[i].length ; j++ ) {
				ColorWindow curr = cwindow[i][j];
				double alpha = curr.getPosVariance() / maxVariance;
				curr.applyAlpha(g, inputImage, alpha);
			}
		}
		g.dispose();
		return outputImage;
	}

	private void setupWindows() {
		double xnom = original.getWidth();
		xnom = xnom/numWindows + 0.5;
		xNominalWinSize = (int)xnom;
		double ynom = original.getHeight();
		ynom = ynom/numWindows + 0.5;
		yNominalWinSize = (int)ynom;		
		cwindow = new ColorWindow[numWindows+1][numWindows+1];
		for (int xWin = 0 ; xWin<=numWindows ; xWin ++) {
			int x = xWin*xNominalWinSize;
			for ( int yWin = 0 ; yWin<=numWindows ; yWin++) {
				int y = yWin*yNominalWinSize;				 
				cwindow[xWin][yWin] = new ColorWindow(x,y,xNominalWinSize,yNominalWinSize);			
			}
		}
	}

	private void scanImage(Graphics g) {		
		for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
			for ( int yWin = 0 ; yWin<numWindows ; yWin++) {				
				segmentWindow(cwindow[xWin][yWin], g);
			}
		}
	}


	private void segmentWindow(ColorWindow cwin, Graphics g) {
		List<MyColor> colorList = new ArrayList<MyColor>();
		for ( int x=cwin.xpos ; x<cwin.xpos+cwin.xWinSize ; x++) {
			for ( int y=cwin.ypos ; y<cwin.ypos+cwin.yWinSize ; y++) {
				if (x>=0 && y>=0 && x<original.getWidth() && y<original.getHeight() ) {
					colorList.add(new MyColor(original.getRGB(x,y)));
				}
			}
		}
		KMeansPlusPlusClusterer<MyColor> kmeans = new KMeansPlusPlusClusterer<MyColor>(3, 10);
		List<CentroidCluster<MyColor>> clusters = kmeans.cluster(colorList);	
		Map<Color, Color> colorMap = new HashMap<Color, Color>();
		for (CentroidCluster<MyColor> cl : clusters) {
			double[] iarr = cl.getCenter().getPoint();
			Color color = new Color((float)iarr[0], (float)iarr[1], (float)iarr[2]);
			cwin.add(color);
			for ( MyColor my : cl.getPoints()) {
				colorMap.put(my.color, color);
			}
		}
		for ( int x=cwin.xpos ; x<cwin.xpos+cwin.xWinSize ; x++) {
			for ( int y=cwin.ypos ; y<cwin.ypos+cwin.yWinSize ; y++) {
				if (x>=0 && y>=0 && x<original.getWidth() && y<original.getHeight() ) {
					Color color = new Color(original.getRGB(x,y));
					g.setColor(colorMap.get(color));				
					g.fillRect(x,y,1,1);
				}
			}
		}
				
	}

	public List<WindowCluster> clusterWindows(int numClusters) {
		List<ColorWindow> windowList = new ArrayList<ColorWindow>();
		List<WindowCluster> clusterList = new ArrayList<WindowCluster>();
		for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
			for ( int yWin = 0 ; yWin<numWindows ; yWin++) {				
				windowList.add(cwindow[xWin][yWin]);
			}
		}
		KMeansPlusPlusClusterer<ColorWindow> kmeans = new KMeansPlusPlusClusterer<ColorWindow>(numClusters, 10);
		winClusters = kmeans.cluster(windowList);
		int clusterNum = 0;
		for (CentroidCluster<ColorWindow> cl : winClusters) {
			double[] vec = cl.getCenter().getPoint();
			Color clusterColor = new Color( (int)vec[0], (int)vec[1], (int)vec[2]);
			Point clusterCentroid = new Point( vec[3], vec[4]);
			for ( ColorWindow cwin : cl.getPoints() ) {
				cwin.setCluster(clusterNum);
			}
			clusterList.add( new WindowCluster( clusterNum, clusterCentroid, clusterColor, cl.getPoints()));
			clusterNum++;
		}
		return clusterList;
	}

	private void computeWindowThreshold(ColorWindow cwin) {
		List<Intensity> intensity = new ArrayList<Intensity>();
		for ( int x=cwin.xpos ; x<cwin.xpos+cwin.xWinSize ; x++) {
			for ( int y=cwin.ypos ; y<cwin.ypos+cwin.yWinSize ; y++) {
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
		cwin.setThreshold(maxForeground);
	}

	public List<String> labelClustersByColor() {
		List<String> clusterLabels = new ArrayList<String>();
//		List<Map<String, Integer>> clusterFuck = new ArrayList<Map<String, Integer>>(); // who needs it?
		for (CentroidCluster<ColorWindow> cl : winClusters) {
			Map<String, Integer> labelMap = new HashMap<String, Integer>();			
			for ( ColorWindow cwin : cl.getPoints() ) {
				String label = cwin.getRawLabel();
				Integer count = labelMap.get(label);
				if ( count == null) {
					labelMap.put(label, 1);
				}
				else {
					labelMap.put(label, ++count);
				}
			}
			int max = 0;
			String best = null;
			for (String label : labelMap.keySet()) {
				int count = labelMap.get(label);
				if (best == null || count > max) {
					best = label;
					max = count;
				}
			}
			clusterLabels.add(best);
		}
		return clusterLabels;		
	}

	public boolean labelClustersByProximity(List<WindowCluster> windowClusterList) {
		for (WindowCluster wc : windowClusterList) {
			wc.computeBorderProximity(original);
		}
		KMeansPlusPlusClusterer<WindowCluster> kmeans = new KMeansPlusPlusClusterer<WindowCluster>(2, 10);
		List<CentroidCluster<WindowCluster>> clusters = kmeans.cluster(windowClusterList);
		if (clusters.size() == 2) {
			CentroidCluster<WindowCluster> foreground = clusters.get(0);
			CentroidCluster<WindowCluster> background = clusters.get(1);
			if (getProximity(foreground) < getProximity(background)) {
				background = clusters.get(0);
				foreground = clusters.get(1);
			}
			setLabel(background, "B");
			setLabel(foreground, "F");
			return true;
		}		
		return false;
	}

	public List<Boundary> findClusterBoundaries(List<WindowCluster> clusterList) {
		List<Boundary> boundaryList = new ArrayList<Boundary>();
		System.out.println(numWindows+" x "+numWindows);
		for (WindowCluster wc : clusterList) {
			boundaryList.add(findBoundary(wc));
		}
		return boundaryList;
	}

	private Boundary findBoundary(WindowCluster wc) {
		List<ColorWindow> boundaryWindows = new ArrayList<ColorWindow>();
		MooreWin start = scanForCluster(wc.getClusterNum());
//		System.out.println("Start: cluster = "+wc.getClusterNum()+"start = "+start.s);
		MooreWin curr = start;
		do {
			boundaryWindows.add(curr.cwin);
			curr.cwin.boundary++;
			curr = findNext(curr, start);
//			System.out.println("Got next boundary window at "+curr.s);
		} while ( curr.cwin != start.cwin);

//		System.out.println("Found "+boundaryWindows.size()+" boundary windows");		
//		System.out.println(clustersAsString());

		Boundary boundary = new Boundary();
		int x = boundaryWindows.get(0).xpos;
		int y = boundaryWindows.get(0).ypos;
		Pos currPos = new Pos(x,y);
		boundary.add( currPos );
		for ( int i=0 ; i<boundaryWindows.size()-1 ; i++) {
			ColorWindow currWin = boundaryWindows.get(i);
			ColorWindow nextWin = boundaryWindows.get(i+1);
//			System.out.println("curr = "+currWin.xIndex+" "+currWin.yIndex+" next = "+nextWin.xIndex+" "+nextWin.yIndex);
			int corner = getCorner(currPos, currWin);
			do {
				int nextCorner = corner == 3 ? 0 : corner+1;
				currPos = getPos( currWin, nextCorner);
//				System.out.println("got pos "+currPos+" at corner "+nextCorner);
				boundary.add( currPos );
				corner = nextCorner;
			} while ( !nextWin.contains(currPos));			
		}
//		System.out.println("Created boundary with "+boundary.size()+" points");
		return boundary;	
	}

	

	private MooreWin findNext(MooreWin curr, MooreWin start) {
		int clusterNum = curr.cwin.getClusterNum();
//		System.out.println(curr.s);
		int k=nextk(curr);		
		ColorWindow cwin = curr.cwin;
		ColorWindow prev = curr.prev;
//		System.out.println("start at direction = "+k);
		int startk = k;
		do {			
			do {
				k = (k+1) % clockwise.length;
				prev = cwin;
				cwin = getColorWindowAt( curr.cwin, k);				
				if (cwin != null ) {
//					System.out.println("got "+cwin.xIndex+" "+cwin.yIndex+" for  k = "+k);
				}
				else {
//					System.out.println("got null for k = "+k);
				}
			} while( cwin == null && k != startk);
		} while( cwin.getClusterNum() != clusterNum && k != startk);
		if ( k == startk ) { // case of an isolated point
			return start;
		}
		return new MooreWin( cwin, prev, k);
	}

	private int nextk(MooreWin curr) {
		int k=curr.k;
		if (k == -1) {
			Pos rel = getRelativePos( curr.cwin, curr.prev );
		
			for ( k=0 ; k<clockwise.length && !clockwise[k].equals(rel)  ; k++ ) {
			}
			if (k == clockwise.length) {
				System.out.println("ERROR: unable to find "+rel);
				return -1;
			}
			else {
				return k;
			}
		}
		else {
			int k2 = k/2;
			int k3 = k2 == 0 ? 3 : k2-1;
			int k4 = k3*2;
//			System.out.println("k = "+k+" k2 = "+k2+" k3 = "+k3+" k4 = "+k4);
			return k4;
		}
	}
	private int getCorner(Pos p, ColorWindow cwin) {
		int dx = p.x - cwin.xpos;
		int dy = p.y - cwin.ypos;
		if (dx < 0 || dy < 0) {
			System.out.println("ERROR: point "+p+" is not on cwin "+cwin.xIndex+" "+cwin.yIndex+" "+cwin.xpos+" "+cwin.ypos);
		}
		if (dx == 0) {
			if (dy == 0) {
				return 0;
			}
			else {
				return 3;
			}
		}
		else {
			if (dy == 0) {
				return 1;
			}
			else {
				return 2;
			}
		}
	}

	private Pos getPos( ColorWindow cwin, int corner) {
		if (corner == 0) {
			return new Pos( cwin.xpos, cwin.ypos);
		}
		else if (corner == 1) {
			return new Pos( cwin.xpos+cwin.xWinSize, cwin.ypos);
		}
		else if (corner == 2) {
			return new Pos( cwin.xpos+cwin.xWinSize, cwin.ypos+cwin.yWinSize);
		}
		else if (corner == 3) {
			return new Pos( cwin.xpos, cwin.ypos+cwin.yWinSize);
		}
		return null;
	}

	private ColorWindow getColorWindowAt( ColorWindow cwin, int k) {
		Pos d = clockwise[k];
		int xWin = cwin.xpos / cwin.xWinSize;
		int yWin = cwin.ypos / cwin.yWinSize;
		xWin += d.x;
		yWin += d.y;
		if (xWin >= 0 && yWin >=0 && xWin<numWindows && yWin<numWindows) {
			return cwindow[xWin][yWin];
		}
		return null;
	}
	private Pos getRelativePos(ColorWindow from, ColorWindow to) {
		if ( to == null) {
			return new Pos( 0, -1); // because we scan top to bottom
		}
		return new Pos ( (to.xpos - from.xpos)/to.xWinSize,  (to.ypos - from.ypos)/to.yWinSize );
	}

	private MooreWin scanForCluster(int clusterNum) {
		ColorWindow prev = null;
		for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
			for ( int yWin = 0 ; yWin<numWindows ; yWin++) {				
				ColorWindow cwin = cwindow[xWin][yWin];
				if (cwin.getClusterNum() == clusterNum) {
					return new MooreWin( cwin, prev, -1);
				}
				prev = yWin == numWindows-1 ? null : cwin;
			}
		}
		return null;
	}

	private void setLabel(CentroidCluster<WindowCluster> cl, String label ) {
		for (WindowCluster wc : cl.getPoints()) {
			wc.setClusterLabel(label);
		}
	}

	private double getProximity(CentroidCluster<WindowCluster> cl) {
		double proximity = 0;
		for (WindowCluster wc : cl.getPoints()) {
			proximity += wc.getProximity();
		}
		return proximity;
	}

	public void applyLabels(List<String> clusterLabels) {
		int k=0;
		for (CentroidCluster<ColorWindow> cl : winClusters) {
			for ( ColorWindow cwin : cl.getPoints() ) {
				cwin.setClusterLabel(clusterLabels.get(k));
			}
			k++;
		}
	}

	public double scoreSign(Rectangle border) {
		double maxScore = numWindows * numWindows;
		double scoreSum = 0.0;
		for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
			for ( int yWin = 0 ; yWin<numWindows ; yWin++) {
				ColorWindow cwin = cwindow[xWin][yWin];			
				double common = cwin.getBorder().getIntersection(border);
				double coverage = common / cwin.getBorder().getArea();
				if (cwin.isForeground()) {
					scoreSum += coverage;
				}
				else {
					scoreSum += 1.0 - coverage;
				}
			}
		}
		return scoreSum	/ maxScore;
	}

	public BufferedImage getImage() {
		return colorImg;
	}

	public void logWindows(Logger logger) {
		for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
			for ( int yWin = 0 ; yWin<numWindows ; yWin++) {				
				logger.log(cwindow[xWin][yWin].toString());
			}
		}
	}

	public void logWindow(int x, int y, Logger logger) {
		int xWin = x/xNominalWinSize;
		int yWin = y/yNominalWinSize;
		logger.log(cwindow[xWin][yWin].toString());
	}

	public String clustersAsString() {
		StringBuilder sb = new StringBuilder(10);
		for ( int yWin = 0 ; yWin<numWindows ; yWin++) {
			for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
				if (cwindow[xWin][yWin].getClusterNum() < 10) {
					sb.append(" ");
				}
				sb.append(cwindow[xWin][yWin].getClusterNum()+" ");
/*				
				if (cwindow[xWin][yWin].boundary > 0) {
					sb.append("* ");
				}
				else {
					sb.append("  ");
				}
*/				
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String rawLabelAsString() {
		StringBuilder sb = new StringBuilder(10);
		for ( int yWin = 0 ; yWin<numWindows ; yWin++) {
			for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
				sb.append(cwindow[xWin][yWin].getRawLabel()+" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String clusterLabelAsString() {
		StringBuilder sb = new StringBuilder(10);
		for ( int yWin = 0 ; yWin<numWindows ; yWin++) {
			for (int xWin = 0 ; xWin<numWindows ; xWin ++) {
				sb.append(cwindow[xWin][yWin].getClusterLabel()+" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}