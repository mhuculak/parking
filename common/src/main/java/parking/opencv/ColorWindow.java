package parking.opencv;

import org.opencv.core.Point;
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.Collections;
import java.util.Comparator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class IntensityComparator implements Comparator<MyColor2> {
	@Override
	public int compare(MyColor2 c1, MyColor2 c2) {
		if (c1.intensity > c2.intensity) {
			return -1;
		}
		else if (c1.intensity < c2.intensity) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class FrequencyComparator implements Comparator<MyColor2> {
	@Override
	public int compare(MyColor2 c1, MyColor2 c2) {
		if (c1.n > c2.n) {
			return -1;
		}
		else if (c1.n < c2.n) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

class MyColor2 {

	public Color color;
	public double intensity;
	public int n;

	public MyColor2(Color color, int count) {
		this.color = color;
		this.n = count;
		intensity = ImageBorderParams.getGreyLevel(color);
	}
}


class ColorAccum {
	private float[] comp;	
	private int n;

	public ColorAccum() {
		comp = new float[3];
	}

	public ColorAccum(Color color) {
		comp = new float[3];
		add(color);
	}

	public void add(int rgb) {
		add(new Color(rgb));
	}

	public void add(Color color) {
		float[] c = color.getRGBColorComponents(null);
		for ( int i=0 ; i<3 ; i++) {
			comp[i] += c[i];
		}
		n++;
	}

	public Color getColor() {
		if (n > 0) {
			float[] c = new float[3];
			for ( int i=0 ; i<3 ; i++ ) {
				c[i] = comp[i]/n;
			}
			return new Color( c[0], c[1], c[2] );
		}
		return null;
	}
}

class WindowStats {
	public Point meanPos;
	public Point variancePos;
	public Color meanColor;
	public float[] varianceColor;
	public int n;
	public double portion;

	private List<Point> posList;
	private List<Color> colorList;
	private ColorAccum colorAccum;
	private int winSize;

	public WindowStats( int winSize ) {
		this.winSize = winSize;
		meanPos = new Point(0,0);
		variancePos = new Point(0,0);
		varianceColor = new float[3];
		colorAccum = new ColorAccum();
		colorList = new ArrayList<Color>();
		posList = new ArrayList<Point>();
	}

	public void add(int x, int y, Color c) {
		meanPos.x += x;
		meanPos.y += y;
		posList.add( new Point(x,y));
		colorAccum.add(c);
		colorList.add(c);
	}

	public void compute() {
		n = posList.size();
		if ( n == 0) {
			return;
		}
		portion = (double)n / (double)winSize;
		meanColor = colorAccum.getColor();
		meanPos = new Point( meanPos.x / n, meanPos.y / n);		
		float[] meanComp = meanColor.getRGBColorComponents(null);		
		
		for ( int i=0 ; i<n ; i++ ) {
			float[] comp = colorList.get(i).getRGBColorComponents(null);
			for ( int j=0 ; j<3 ; j++ ) {				
				float dev = meanComp[j] - comp[j];
				varianceColor[j] += dev*dev;
			}
			double dx = meanPos.x - posList.get(i).x;
			variancePos.x += dx*dx;
			double dy = meanPos.y - posList.get(i).y;
			variancePos.y += dy*dy;
		}
		for ( int j=0 ; j<3 ; j++ ) {
			varianceColor[j] = varianceColor[j] / n;
		}
		variancePos.x = variancePos.x/n;
		variancePos.y = variancePos.y/n;
	}
}

public class ColorWindow implements Clusterable {

	
	public int xWinSize;
	public int yWinSize;
	public int xpos;
	public int ypos;
	public int clusterNum;

	public int xIndex;
	public int yIndex;

	public int internalChecks;
	public int internalChanges;
	public int externalChecks;
	public int externalChanges;
	public int boundary;

	private String rawLabel;
	private String clusterLabel;
	private boolean foreground;
	private Rectangle border;

	private Map<Color, ColorAccum> colorsFound;
	private Map<ColorWindow, Boolean> alreadyMerged;
	private Map<Color, WindowStats> stats;
	private Map<Color, Color> standardMap;
	private double[] colorVector;
	private Point centroid;
	private Color mostFrequent;
	private Color lightest;
	private Color darkest;
	private double threshold;
	private int kmeansNumColors;

	private final double redBiasThreshold = 2.0;
	private final double foregroundThreshold = 0.9;

	public ColorWindow( int xpos, int ypos, int xWinSize, int yWinSize) {
		this.xpos = xpos;
		this.ypos = ypos;
		this.xWinSize = xWinSize;
		this.yWinSize = yWinSize;
		centroid = new Point( xpos + xWinSize/2.0, ypos + yWinSize/2.0 );
		alreadyMerged = new HashMap<ColorWindow, Boolean>();
		boundary = 0;
		xIndex = xpos/xWinSize;
		yIndex = ypos/yWinSize;
		Point[] c = new Point[4];
		c[0] = new Point( xpos, ypos);
		c[1] = new Point( xpos, ypos+yWinSize);
		c[2] = new Point( xpos + xWinSize, ypos+yWinSize);
		c[3] = new Point( xpos + xWinSize, ypos);
		border = new Rectangle( c, null );
	}

	public double[] getPoint() {
		return getPoint(kmeansNumColors);
	}

	public double[] getPoint(int size) { 
		double[] vector = new double[3*size+2];
		for ( int i=0 ; i<3*size ; i++) {
			vector[i] = colorVector[i];
		}
		vector[3] = centroid.x;
		vector[4] = centroid.y;		
		return vector;
	}	
/*
	//
	// 11 eleement vector of 3 colors x RGB + centroid x,y
	//
	public double[] getPoint11() { 
		double[] vector = new double[11];
		for ( int i=0 ; i<9 ; i++) {
			vector[i] = colorVector[i];
		}
		vector[9] = centroid.x;
		vector[10] = centroid.y;		
		return vector;
	}	
*/
	public List<Color> getColors() {
		if (colorsFound != null) {
			return new ArrayList<Color>(colorsFound.keySet());
		}
		return new ArrayList<Color>();
	}

	public void add(Color color) {
		if (colorsFound == null) {
			colorsFound = new HashMap<Color, ColorAccum>();
		}
		ColorAccum accum = new ColorAccum(color);
		colorsFound.put(color, accum);		
	}

	public String getRawLabel() {
		return rawLabel;
	}

	public Rectangle getBorder() {
		return border;
	}

	public void setClusterLabel(String clusterLabel) {
		this.clusterLabel = clusterLabel;
		if (clusterLabel.equals("F")) {
			foreground = true;
		}
		else {
			foreground = false;
		}
		
	}

	public String getClusterLabel() {
		return clusterLabel;
	}

	public boolean isForeground() {
		return foreground;
	}

	public void computeStats(BufferedImage image, int kmeansNumColors) {
		this.kmeansNumColors = kmeansNumColors;
		List<Color> colorList = this.getColors();
		if (colorList.size() == 0) {
			return;
		}
		stats = new HashMap<Color, WindowStats>();
		for ( Color c: colorList) {
			stats.put(c, new WindowStats( xWinSize*yWinSize ));
		}
		try {
		for ( int x=xpos ; x<xpos+xWinSize ; x++) {
			for ( int y=ypos ; y<ypos+yWinSize ; y++) {
				if (x>=0 && y>=0 && x<image.getWidth() && y<image.getHeight()) {
					Color real = new Color(image.getRGB(x,y));
					Color c = select(real);
					WindowStats cStats = stats.get(c);
					cStats.add(x,y,real);
				}
			}
		}
		}
		catch (Exception ex) {
			System.out.println("caught "+ex+" for "+this.toString());
		}
		for ( Color c: colorList) {
			WindowStats cStats = stats.get(c);
			cStats.compute();
		}
		//
		//  needed for kmeans clustering 
		//
		colorVector = getColorVector(kmeansNumColors);
		rawLabel = computeRawLabel();
	}

	//
	//   F - foreground i.e. we think it could be part of a sign
	//   B - background i.e. nope
	//
	private String computeRawLabel() {
		String foreground = "F";
		String background = "B";
		
		int redIndex = 0;
		Color white = ColorClassifier.isWhite(lightest, threshold) ? lightest : null;
		Color black = ColorClassifier.isBlack(darkest, threshold) ? darkest : null;
		double maxRedBias = 0.0;
		Color red = null;
		for ( Color c : this.getColors()) {
			double redBias = ColorClassifier.getBias(c, redIndex);
			if (redBias > maxRedBias) {
				maxRedBias = maxRedBias;
				red = c;
			}
		}
		if (maxRedBias < redBiasThreshold) {
			red = null;
		}

		double whitePortion = white == null ? 0 : stats.get(white).portion;
		double blackPortion = black == null ? 0 : stats.get(black).portion;
		double redPortion = red == null ? 0 : stats.get(red).portion;
		double total = whitePortion + blackPortion + redPortion;

		if (total > foregroundThreshold) {
			return foreground;
		}

		return background;
	}

/*
	private double[] getColorVector5() {
		List<Color> colorList = getColors();
		List<MyColor2> myColorList = new ArrayList<MyColor2>();
		for (Color c: colorList) {
			myColorList.add(new MyColor2(c, stats.get(c).n));
		}

		FrequencyComparator frequency = new FrequencyComparator();
		Collections.sort(myColorList, frequency);
		mostFrequent = myColorList.get(0).color;

		IntensityComparator comparator = new IntensityComparator();
		Collections.sort(myColorList, comparator);
		lightest = myColorList.get(0).color;
		darkest = myColorList.get(myColorList.size()-1).color;

		double[] vec = new double[3];
		
		float[] comp = mostFrequent.getRGBColorComponents(null);
		for ( int j=0 ; j<3 ; j++ ) {
			vec[j] = 255*(double)comp[j];
		}
		
		return vec;
	}
*/
	private List<MyColor2> processColorList() {
		List<Color> colorList = getColors();
		List<MyColor2> myColorList = new ArrayList<MyColor2>();
		for (Color c: colorList) {
			myColorList.add(new MyColor2(c, stats.get(c).n));
		}

		FrequencyComparator frequency = new FrequencyComparator();
		Collections.sort(myColorList, frequency);
		MyColor2 mostFrequent2 = myColorList.get(0);
		mostFrequent = mostFrequent2.color;

		IntensityComparator comparator = new IntensityComparator();
		Collections.sort(myColorList, comparator);
		lightest = myColorList.get(0).color;
		darkest = myColorList.get(myColorList.size()-1).color;
		
		if ( myColorList.size() == 1) {
			myColorList.add(mostFrequent2);
			myColorList.add(mostFrequent2);
		}
		else if ( myColorList.size() == 2) {
			if (mostFrequent2.intensity == myColorList.get(0).intensity) {
				myColorList.add(0, mostFrequent2);
			}
			else {
				myColorList.add(mostFrequent2);
			}
		}
		return myColorList;
	}

	//
	//  create 9 element vector from three colors ordered by intensity.
	//  if less than 3 colors available, insert the most frequent
	//  in the apprpriate spot so as to preserve intensity order
	//	
	private double[] getColorVector(int size) {		
		List<MyColor2> myColorList = processColorList();
		return buildVector(myColorList, size);
	}

	private double[] buildVector(List<MyColor2> myColorList, int size) {
		double[] vec = new double[3*size];
		int k=0;
		for ( int i=0 ; i<size ; i++ ) {
			MyColor2 my = myColorList.get(i);
			float[] comp = my.color.getRGBColorComponents(null);
			for ( int j=0 ; j<3 ; j++ ) {
				vec[k] = 255*(double)comp[j];
				k++;
			}
		}
		return vec;
	}
	
	public double computeBorderProximity(BufferedImage image) {
		double[] prox = new double[4];
		prox[0] = xpos;
		prox[1] = image.getWidth() - xpos - xWinSize;
		prox[2] = ypos;
		prox[3] = image.getHeight() - ypos - yWinSize;
		double min = prox[0];
		for ( int i=1 ; i<4 ; i++) {
			min = Math.min( prox[i], min);
		}
		return min;
	}
	//
	//  paint graphics with the window color that is the closest match to the input pixel color
	//
	public void apply(Graphics g, BufferedImage image) {
		for ( int x=xpos ; x<xpos+xWinSize ; x++) {
			for ( int y=ypos ; y<ypos+yWinSize ; y++) {
				if (x>=0 && y>=0 && x<image.getWidth() && y<image.getHeight()) {
					g.setColor(select(new Color(image.getRGB(x,y))));				
					g.fillRect(x,y,1,1);
				}
			}
		}
	}

	public void applyAlpha(Graphics g, BufferedImage image, double a) {
		float alpha = (float)a;
		if (alpha > 1) {
			alpha = 1;
		}
		for ( int x=xpos ; x<xpos+xWinSize ; x++) {
			for ( int y=ypos ; y<ypos+yWinSize ; y++) {
				if (x>=0 && y>=0 && x<image.getWidth() && y<image.getHeight()) {
					Color color = new Color(image.getRGB(x,y));
					float[] comp = color.getRGBColorComponents(null);
					Color colorWithAlpha = new Color( comp[0], comp[1], comp[2], alpha );
					g.setColor(colorWithAlpha);				
					g.fillRect(x,y,1,1);
				}
			}
		}
	}

	public void mergeWith(ColorWindow cwin, double minColorDist) {
		if (alreadyMerged.get(cwin) == null || alreadyMerged.get(cwin) == false) {
			selfMerge( minColorDist);
			Map<Color, Boolean> merges = new HashMap<Color, Boolean>();
			for ( Color c1 : this.getColors()) {
				for ( Color c2 : cwin.getColors()) {
					if (c1 != c2) {
						double dist = ColorClassifier.getDist(c1, c2);
						externalChecks++;
						if (dist < minColorDist) {
//							System.out.println("Merge "+c1+" from "+this+" with "+c2+" from "+cwin+" dist = "+dist);
							accumulate(c1, c2);
							externalChanges++;
						}
					}
				} 
			}
			List<Color> toUpdate = new ArrayList<Color>(merges.keySet());			
			update(toUpdate);
			alreadyMerged.put(cwin, true);
			cwin.mergeWith(this, minColorDist);
		}		
	}

	public double getDistFrom(List<Color> colorList) {
		double sum2 = 0.0;
		int size = this.getColors().size();
		for ( Color c1 : this.getColors()) {
			double minDist = -1;
			for (Color c : colorList) {
				double dist = ColorClassifier.getDist(c, c1);
				if (minDist < 0 || minDist > dist) {
					minDist = dist;
				}
			}
			sum2 += minDist * minDist;
		}
		return Math.sqrt(sum2);
	}

	public double getPosVariance() {
		double sumx2 = 0;
		double sumy2 = 0;
		for ( Color c: this.getColors()) {
			WindowStats cStats = stats.get(c);
			sumx2 += cStats.variancePos.x * cStats.variancePos.x;
			sumy2 += cStats.variancePos.y * cStats.variancePos.y;
		}
		return Math.sqrt( sumx2 + sumy2);
	}
	
	private void selfMerge(double  minColorDist) {
		Map<Color, Color> mergedToMap = new HashMap<Color, Color>();
		List<Color> colorList = this.getColors();
		Map<Color, Boolean> merges = new HashMap<Color, Boolean>();
		for ( int i=0 ; i<colorList.size() ; i++ ) {	
			Color c1 = colorList.get(i);			
			Color mergeTo = mergedToMap.get(c1) == null ? c1 : mergedToMap.get(c1);
			for ( int j=i+1 ; j<colorList.size() ; j++ ) {
				Color c2 = colorList.get(j);
				double dist = ColorClassifier.getDist(c1, c2);
				internalChecks++;
				if (dist < minColorDist) {
//					System.out.println("Merge "+c1+" with "+c2+" in "+this+" dist = "+dist);
					mergedToMap.put(c2, mergeTo);
					merges.put(mergeTo, true);
					internalChanges++;
				}
			}
		}
	
		for ( Color removed : mergedToMap.keySet()) {
			Color keep = mergedToMap.get(removed);			
			transfer( keep, removed);			
		}
		List<Color> toUpdate = new ArrayList<Color>(merges.keySet());
		update(toUpdate);
	}

	private void update(List<Color> updates) {
		for (Color c : updates) {
//			System.out.println("updating "+c);
			ColorAccum accum = colorsFound.get(c);
			Color mergedColor = accum.getColor();
//			System.out.println("updated color is "+mergedColor);
			colorsFound.remove(c);
			colorsFound.put(mergedColor, accum);
		}
	}

	private void accumulate(Color c1, Color c2) {
		ColorAccum accum = colorsFound.get(c1);
		accum.add(c2);
	}


	private void transfer(Color toKeep, Color toRemove) {
		accumulate(toKeep, toRemove);
		colorsFound.remove(toRemove);		 
	}

	private Color select(Color color) {
		Color best = null;
		double minDist = 0;
		for (Color c : getColors()) {
			double dist = ColorClassifier.getDist(color, c);
			if (best == null || minDist > dist) {
				minDist = dist;
				best = c;
			}
		}
		return best;
	}

	public boolean contains(Pos p) {
		if (p.x >= xpos && p.y >= ypos && p.x <= xpos+xWinSize && p.y <= ypos+yWinSize) {
			return true;
		}
		return false;
	}

	public boolean contains(Point p) {
		if (p.x >= xpos && p.y >= ypos && p.x <= xpos+xWinSize && p.y <= ypos+yWinSize) {
			return true;
		}
		return false;
	}

	public void setStandardColors() {
		standardMap = ColorClassifier.classifyWindowColors(this);

	}

	public void setCluster(int clusterNum) {
		this.clusterNum = clusterNum;
	}

	public int getClusterNum() {
		return clusterNum;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(10);
		List<Color> colorList = getColors();
		sb.append(xpos+","+ypos+" "+clusterNum+" "+colorList.size()); // +" "+xWinSize+" x "+yWinSize;
		for (Color c : colorList) {
			if (stats != null) {		
				String standard = ColorClassifier.getColorName(standardMap.get(c));
				sb.append("\n  "+standard+" "+c);
				WindowStats cStats = stats.get(c);
				sb.append(" stats: size: "+cStats.n+" pos "+cStats.variancePos.x+","+cStats.variancePos.y+" color ");
				for ( int j=0 ; j<3 ; j++) {
					sb.append(cStats.varianceColor[j]+",");
				}				
			}
			else {
				sb.append("\n  "+c);
			}
		}
		if (stats != null) {	 
			sb.append("\ntotal pos variance = "+getPosVariance());
		}
		sb.append("\n  dist: ");
		for ( int i=0 ; i<colorList.size()-1 ; i++ ) {
			for ( int j=i+1 ; j<colorList.size() ; j++ ) {
				sb.append("d"+i+":"+j+"="+ColorClassifier.getDist(colorList.get(i), colorList.get(j)));
			}
		}
		sb.append("\n  num merges internal: "+internalChanges+"/"+internalChecks+" external: "+externalChanges+"/"+externalChecks);

		return sb.toString();
	}
}