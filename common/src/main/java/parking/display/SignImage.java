package parking.display;

import parking.util.HttpClient;
import parking.opencv.ProcessOpenCV;
import parking.opencv.Line;
import parking.opencv.Circle;
import parking.opencv.Ellipse;
import parking.opencv.Vector;
import parking.opencv.Rectangle;
import parking.opencv.Transformation;
import parking.opencv.SignBorder;
import parking.opencv.TextShape;
import parking.opencv.TextGroup;
import parking.opencv.TextSegment;
import parking.opencv.StraightSegment;

import parking.schedule.ParkingSignType;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import org.opencv.core.Point;

import javax.swing.JComponent;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;

import java.util.Collections;
import java.util.Comparator;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class SignComparator implements Comparator<SignImage> {
	@Override
	public int compare(SignImage s1, SignImage s2) {
		if (s1.getScore() > s2.getScore()) {
			return -1;
		}
		else if (s1.getScore() < s2.getScore()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

public class SignImage extends JComponent {

	private BufferedImage img;
	private List<Line> lines;
	private List<Circle> circles;
	private List<Rectangle> rectangles;
	private List<TextShape> letters;
	private List<SignBorder> borders;
	private Rectangle theBorder;  //  border in sign ref reframe
	private Rectangle origBorder; // border in original ref frame
	private Ellipse outerEllipse;
	private double score;
	private String scoreDetails;
	private double zoom;

	private static final int maxSigns = 100;

	private final Color[] colors = new Color[] { Color.red, Color.green, Color.orange, Color.cyan, Color.pink, 
		Color.yellow, Color.magenta,  Color.blue};

	public SignImage(String uri) {
		try {
    		HttpClient httpClient = new HttpClient();
    		httpClient.doGet(uri);
    		img = ImageIO.read(httpClient.getInputStream());
    	} 
    	catch (IOException e) {
            e.printStackTrace();
        }
        zoom = 1.0;
	}

	public SignImage(File imageFile) {
		try {    		
    		img = ImageIO.read(imageFile);
    		System.out.println("Read new image "+ img);
    	} 
    	catch (IOException e) {
            e.printStackTrace();
        }
        zoom = 1.0;
	}

	public SignImage( int width, int height) {
		img = new BufferedImage ( width, height, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = img.createGraphics();
		g.setColor( Color.black) ;
		g.fillRect(0, 0, width, height);
		g.dispose();
		zoom = 1.0;
	}

	public SignImage( int width, int height, double zoom) {
		this.zoom = zoom;
		int w = round(width*zoom);
		int h = round(height*zoom);
		img = new BufferedImage ( w, h, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = img.createGraphics();
		g.setColor( Color.black) ;
		g.fillRect(0, 0, w, h);
		g.dispose();
	}

	public SignImage(BufferedImage img) {
		this.img = img;
		zoom = 1.0;
	}

	public int getType() {
		int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
		return type;
	}

	public int getHeight() {
		return img.getHeight();
	}

	public int getWidth() {
		return img.getWidth();
	}

	public double getScore() {
		return score;
	}

	public String getScoreDetails() {
		return scoreDetails;
	}

	public BufferedImage getImage() {
		return img;
	}

	public Rectangle getTheBorder() {
		return theBorder;
	}

	public Rectangle getOrigBorder() {
		return origBorder;
	}

	public SignImage scale(double factor) {
		int width = (int)(img.getWidth() * factor + 0.5);
		int height = (int)(img.getHeight() * factor + 0.5);
		BufferedImage scaledImage = new BufferedImage( width, height, getType());
		Graphics2D g = scaledImage.createGraphics();
		g.drawImage(img, 0, 0, width, height, null);
		g.dispose();
		
		return new SignImage(scaledImage);

	}

	public void setLetters(List<TextShape> letters) {
		this.letters = letters;
	}

	public void setLines(List<Line> lines) {
		this.lines = lines;
	}

	public void setRectangles(List<Rectangle> rectangles) {
		this.rectangles = rectangles;
	}

	public void setCircles(List<Circle> circles) {
		this.circles = circles;
	}

	public void setSignBorders(List<SignBorder> borders) {
		this.borders = borders;
	}

	public void setTheBorder(Rectangle rect) {
		theBorder = rect;
	}

	public void setOrigBorder(Rectangle rect) {
		origBorder = rect;
	}

	public void setOuterEllipse(Ellipse ellipse) {
		this.outerEllipse = ellipse;
	}

	//
	//  FIXME: we multiply the score by the perimeter to favor larger borders over smaller
	//         but we dont want to simply pick the largest border...so we need a better
	//         way of balancing the quality of the borders and the size...needs further investigation
	//
	private void computeScore() {
		
		double[] borderScore = theBorder.getScoreVector();
		double ellipseScore = 0.0;
		if (outerEllipse != null) {
			double err = theBorder.getDisplacmentFromVerticalAxis(outerEllipse.getCenter());
			double maxError = theBorder.getWidth()/2;
			err = err > maxError ? 1.0 : err/maxError;
			ellipseScore = Math.sqrt(1 - err*err);
		}
		double scoreSum = ellipseScore;
		for ( int i=0 ; i<borderScore.length ; i++) {
			scoreSum += borderScore[i];
		}
		score = scoreSum/(borderScore.length+1)*theBorder.getPerimeter();
		StringBuilder sb = new StringBuilder(100);
		sb.append(score+" ");
		for ( int i=0 ; i<4 ; i++) {
			sb.append(i+":"+borderScore[i]+" ");
		}
		sb.append("Asp:"+borderScore[4]+" ");
		sb.append("Cov:"+borderScore[5]+" ");
		sb.append("Eli:"+ellipseScore+" ");
		sb.append("Tot:"+scoreSum+" ");
		sb.append("Per:"+theBorder.getPerimeter()+" ");
		scoreDetails = sb.toString();
	}

	

	public ParkingSignType getSignType(SignImage canny, List<Line> lines, List<TextGroup> textGroups) {
		List<Line> vertAxisList = estimateVertAxisFromTextGroups(textGroups);
//		System.out.println("border center is "+origBorder.getCentroid());
		for ( Line vertAxis : vertAxisList ) {
			vertAxis = origBorder.extendVertAxis(vertAxis);	
			Ellipse ellipse = Ellipse.getEllipse(canny.getImage(), origBorder, vertAxis);
			if (ellipse == null) {
				ellipse = Ellipse.getEllipse(canny.getImage(), origBorder, null);
			}
			boolean signIsRestriction = findRestrictionBar(lines, origBorder, vertAxis, ellipse);											
			if (signIsRestriction) {
				return ParkingSignType.NOPARKING;
			}			
		}
		return ParkingSignType.PARKING;
	}

	//
	//  find bar inclined 45 deg from sign axis
	//
	private boolean findRestrictionBar(List<Line> lines, Rectangle border, Line vertAxis, Ellipse ellipse) {		
		final double maxAngleDeviation = 0.1;
		final double maxBarWidthFactor = 0.1;
		Point loc = border.getCirclePos(vertAxis);		
		if (ellipse != null) {
//			System.out.println("Found ellipse at "+ellipse.getCenter());
			loc = ellipse.getCenter();
		}
		double expectedBarAngle = vertAxis.angle - Math.PI/4;
		double maxBarWidth = border.getWidth() * maxBarWidthFactor;
//		System.out.println("Looking for restriction bar at "+loc+" expected angle = "+expectedBarAngle+" within "+maxBarWidth+" center = "+border.getCentroid());
		List<Line> barEdges = new ArrayList<Line>();
		int numDistChecks = 0;
		double minDist = 1000000;
		for ( Line line : lines) {
			double angleDev = getAngleDeviation(line.angle, expectedBarAngle);
//			System.out.println(line.angle+" "+angleDev);
			if (Math.abs(angleDev) < maxAngleDeviation) {
				numDistChecks++;
				double dist = Line.getDistance(line, loc);
				if (dist < maxBarWidth) {
//					System.out.println("Found restr bar edge at "+line);
					barEdges.add(line);
				}
				minDist = minDist > dist ? dist : minDist;
			}
		}
//		System.out.println("Performed "+numDistChecks+" dist checks min dist = "+minDist);
//		System.out.println("Found "+barEdges.size()+" restriction bar edges");
		return barEdges.size() > 0;
	}
//    a = targetA - sourceA
//    a = (a + 180) % 360 - 180
	private double getAngleDeviation( double angle1, double angle2) {
		double dev = angle1 - angle2;
		double devPlus = dev + Math.PI;
		if (devPlus > 2*Math.PI) {
			devPlus = devPlus - 2*Math.PI;
		}
		return devPlus - Math.PI;
	}

	private List<Line> estimateVertAxisFromTextGroups( List<TextGroup> textGroups) {
		final int minGroupSize = 4;
		int size = textGroups.size();	
		Vector v = new Vector( Math.cos( theBorder.getVertAxis().angle ), Math.sin( theBorder.getVertAxis().angle ) );
		List<Line> vertAxisList = new ArrayList<Line>();
		double avgX = 0.0;
		double avgY = 0.0;
		int n = 0;
		for (int i=0 ; i<size ; i++) {
			if (textGroups.get(i).size() >= minGroupSize) {
				Line b = textGroups.get(i).getBaseline();
//				System.out.println("got baseline "+b+" for group of size "+textGroups.get(i).size());
				Point m = b.findPoint(0.5);
//				System.out.println("Mid point "+m);
				Line base = theBorder.getTransform().reverse(b);
//				System.out.println("transformed baseline is"+base);
				Point p = base.findPoint(0.5);
//				System.out.println("Got transformed mid point "+p);
				Line vertAxis = new Line( p, v );
				vertAxisList.add(vertAxis);
				avgX += p.x;
				avgY += p.y;
				n++;
			}
		}
		if (n > 1) {
			avgX = avgX / n;
			avgY = avgY / n;
			vertAxisList.add( new Line( new Point(avgX, avgY), v));
		}
		return vertAxisList;
	}

	private boolean verifyPixel(Line line, double xp, double yp) {
		final int WHITE = 0xffffffff;
		final int BLACK = 0xff000000;

		int x = (int)(line.p1.x + xp + 0.5);
		int y = (int)(line.p1.y + yp + 0.5);
		if ( x<0 || y<0 || x >= img.getWidth() || y>=img.getHeight() ) {
			return false;
		}
		int value = img.getRGB(x,y);
//		g.fillRect(x,y,1,1);
		if (value == WHITE) {
			return true;			
		}	
		else {
			return false;
		}
	}

//	public boolean verifyLine(Line line, double threshold, Graphics g)
	public boolean verifyLine(Line line, double threshold, double minLineLength) {
		
		double dx = line.p2.x - line.p1.x;
		double dy = line.p2.y - line.p1.y;
		double pointsVerified = 0;
		double numPoints = 0;		
		if (Math.abs(dx) > Math.abs(dy)) {
			double s = dx > 0 ? 1.0 : -1.0;
			double m = dy / dx;
			for ( double xp = 0.0 ; xp <= dx*s ; xp++, numPoints++) {
				double x = s * xp;
				double y = x * m;
				pointsVerified = verifyPixel(line, x, y) ? pointsVerified + 1 : pointsVerified;
			}
		}
		else {
			double s = dy > 0 ? 1.0 : -1.0;
			double m = dx / dy;
			for ( double yp = 0.0 ; yp <= dy*s ; yp++, numPoints++ ) {
				double y = s * yp;
				double x = y * m;
				pointsVerified = verifyPixel(line, x, y) ? pointsVerified + 1 : pointsVerified;
			}
		}
		double score = pointsVerified / numPoints;
//		System.out.println("verified " + pointsVerified + " of " + numPoints);
/*		
		int k = (int)( score*4 + 0.5);
		Graphics2D g2 = (Graphics2D) g;
//		g2.setColor(colors[k]);
		if (score > threshold) {
			g2.setColor(Color.green);
		}
		else {
			g2.setColor(Color.red);
		}
		Shape l = new Line2D.Double(line.p1.x, line.p1.y, line.p2.x, line.p2.y);
		g2.draw(l);
*/		
		if (score > threshold && line.length > minLineLength) {
			return true;
		}
		return false;		
//		return true;
	}

	public Map<Integer, Integer> getColorDistribution() {
		Map<Integer, Integer> dist = new HashMap<Integer, Integer>();
		for ( int x=0 ; x<img.getWidth() ; x++) {
			for ( int y=0 ; y<img.getHeight() ; y++) {
				int value = img.getRGB(x,y);
				Integer count = dist.get(value);
				if (count == null) {
					dist.put(value, 1);
				}
				else {
					int c = count;
					dist.put(value, ++c);
				}
			}
		}
		return dist;
	}
	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;  
        g.drawImage(img, 0, 0, null);
        boolean didOne = false;
        int i;

        if (letters != null) {
        	i=0;
        	for (TextShape letter : letters) {
        		g2.setColor(colors[i % 8]);
 //       		drawRect(letter.getBound(), g2);
        		if (letter.getTextSegments() != null) {
        			for (TextSegment seg : letter.getTextSegments()) {
        				i++;
        				g2.setColor(colors[i % 8]);
        				if (seg instanceof StraightSegment) {
        					StraightSegment ss = (StraightSegment)  seg;
        					Point p11 = ss.startEdge.p1;
        					Point p12 = ss.startEdge.p2;
        					Point p21 = ss.endEdge.p1;
        					Point p22 = ss.endEdge.p2;
        					Shape l = new Line2D.Double(p11.x * zoom, p11.y * zoom, p12.x * zoom, p12.y * zoom);
        					g2.draw(l);
        					l = new Line2D.Double(p21.x * zoom, p21.y * zoom, p22.x * zoom, p22.y * zoom);
        					g2.draw(l);
        					l = new Line2D.Double(p11.x * zoom, p11.y * zoom, p21.x * zoom, p21.y * zoom);
        					g2.draw(l);
        					l = new Line2D.Double(p12.x * zoom, p12.y * zoom, p22.x * zoom, p22.y * zoom);
        					g2.draw(l);
                		}                		
                	}
        		}
        		i++;
        	}
        }
        if (lines != null) {
        	      	
        	
//        	System.out.println("Display lines of minimum length " + minLineLength);
        	i = 0;
        	for (Line line: lines) {
        		i++;
        		int k = i % 8;
        		g2.setColor(colors[k]);        						        		
//				g2.setColor(colors[line.cluster]);        		
	       		Shape l = new Line2D.Double(line.p1.x, line.p1.y, line.p2.x, line.p2.y);
                g2.draw(l);                
	       	}
//	       	System.out.println("drew " + lines.size() + " lines ");
       	} 

       	if (rectangles != null) {
       		i = 0;
       		for (Rectangle rect : rectangles) {
       			if (i>=0) {
        			int k = i % 8;
        			g2.setColor(colors[k]);
        			drawRect(rect, g2);
        		}
       			i++;
       		}
       	}

       	if (borders != null) {
       		i = 0;
       		for (SignBorder border : borders) {       			
       			if (i<8) {
       				Rectangle rect = border.getBorder();
        			int k = i % 8;
        			g2.setColor(colors[k]);
        			Point[] corner = rect.getCorners();
        			for ( int j=0 ; j<4 ; j++) {
        				int next = j==3 ? 0 : j+1;
        				Shape l = new Line2D.Double(corner[j].x, corner[j].y, corner[next].x, corner[next].y);
        				g2.draw(l);
        			} 
        		}
       			i++;
       		}

       	}
       	if (theBorder != null) {
       		g2.setColor(Color.red);
       		g2.setStroke(new BasicStroke(4));
       		Point[] corner = theBorder.getCorners();
        	for ( int j=0 ; j<4 ; j++) {
        		int next = j==3 ? 0 : j+1;
        		Shape l = new Line2D.Double(corner[j].x, corner[j].y, corner[next].x, corner[next].y);
        		g2.draw(l);
        	}
       	}
       	if (circles != null) {
       		i=0;
       		for (Circle c : circles) {
       			int k = i % 8;
        		g.setColor(colors[k]);
        		g.drawOval(round(c.getCenter().x - c.getRadius()), round(c.getCenter().y - c.getRadius()), round(2*c.getRadius()), round(2*c.getRadius()));
       		}
       		i++;
       	}
       	if (outerEllipse != null) {
       		g2.setStroke(new BasicStroke(4));
       		g.setColor(Color.blue);
//       		System.out.println("display outerEllipse "+outerEllipse);
       		if (Math.abs(Math.sin(outerEllipse.getAngle())) < 1/Math.sqrt(2)) {
       			g.drawOval(round(outerEllipse.getCenter().x - outerEllipse.getMajor()), round(outerEllipse.getCenter().y - outerEllipse.getMinor()), round(2*outerEllipse.getMajor()), round(2*outerEllipse.getMinor()));
       		}
       		else {
       			g.drawOval(round(outerEllipse.getCenter().x - outerEllipse.getMinor()), round(outerEllipse.getCenter().y - outerEllipse.getMajor()), round(2*outerEllipse.getMinor()), round(2*outerEllipse.getMajor()));
       		}
       	}
    }

    private void drawRect(Rectangle rect, Graphics2D g2) {
    	Point[] corner = rect.getCorners();
        for ( int j=0 ; j<4 ; j++) {
        	int next = j==3 ? 0 : j+1;
        	Shape l = new Line2D.Double(zoom*corner[j].x, zoom*corner[j].y, zoom*corner[next].x, zoom*corner[next].y);
        	g2.draw(l);
        } 
    }

    public static List<SignImage> getSigns(SignImage image, SignImage canny, List<Rectangle> rectangles, boolean findEllipse) {
    	List<SignImage> signs = new ArrayList<SignImage>();    	
    	for ( int i=0 ; i<rectangles.size() && i<maxSigns; i++) {			
    		SignImage sign = createSign(image, canny, rectangles.get(i), findEllipse);    		
    		signs.add(sign);
    	}
    	SignComparator comparator = new SignComparator();
		Collections.sort(signs, comparator);
		List<SignImage> reduced = eliminateDuplicates(signs, 0.1);
    	return reduced;
    }

    private static List<SignImage> eliminateDuplicates(List<SignImage> full, double dupDistThresh) {
		double scoreThresh = 10;
		List<SignImage> reduced = new ArrayList<SignImage>();
		Map<SignImage, Boolean> eliminated = new HashMap<SignImage, Boolean>();
		for ( int i=0 ; i<full.size() ; i++) {
			SignImage sign = full.get(i);
			Rectangle rect = sign.getTheBorder();
			Boolean isEliminated = eliminated.get(sign);
			if (isEliminated == null || isEliminated == false) {
				// no need to go thru the entire list assuming it is sorted by score
				for ( int j=i+1 ; j<full.size() && full.get(j).getScore() > sign.getScore() - scoreThresh ; j++) {
					if (Rectangle.areDuplicate(rect, full.get(j).getTheBorder(), dupDistThresh)) {
						eliminated.put(full.get(j), true);
					}
				}
				reduced.add(sign);
			}			
		}
		System.out.println("full size is "+full.size()+" reduced is "+reduced.size());
		return reduced;
	}

    public static SignImage createSign(SignImage image, SignImage canny, Rectangle rect, boolean findEllipse) {
    	SignImage signImage = new SignImage(applyBorderTransform(image.getImage(), rect));
    	Ellipse ellipse = null;
    	if (findEllipse) {
    	 	ellipse = Ellipse.getEllipse(canny.getImage(), rect, null);
//    	 	System.out.println("ellips is "+ellipse);
    	}   	
		signImage.setOrigBorder(rect);
		Graphics2D g2 = (Graphics2D)signImage.getImage().createGraphics();
		Point pos = rect.getBoundPos();
		int xoff = -1*round(pos.x);
		int yoff = -1*round(pos.y);
		double correction = rect.getCorrectAngle();
        g2.setColor(Color.red);
        g2.setStroke(new BasicStroke(4));
        Transformation transformation = new Transformation(  new Vector(xoff, yoff), correction, rect.getCentroid() );
        Rectangle nBorder = transformation.apply(rect);
        if (ellipse != null) {        	
        	Ellipse nEllipse = transformation.apply(ellipse);
        	signImage.setOuterEllipse(nEllipse);
        }
        nBorder.setThreshold(rect.getThreshold());        
        signImage.setTheBorder(nBorder);        
        signImage.computeScore();     
		g2.dispose();
		return signImage;
    }

    public static BufferedImage applyBorderTransform(BufferedImage image, Rectangle border) {
    	Point boundDim = border.getBoundDim();
    	BufferedImage outImage = new BufferedImage(round(boundDim.x), round(boundDim.y), BufferedImage.TYPE_INT_ARGB );
    	Graphics2D g2 = (Graphics2D)outImage.createGraphics();
		Point pos = border.getBoundPos();
		int xoff = -1*round(pos.x);
		int yoff = -1*round(pos.y);
		double correction = border.getCorrectAngle();
		Point centroid = border.getCentroid();
		AffineTransform transform = new AffineTransform();
		transform.setToTranslation(xoff, yoff);
        transform.rotate( correction, round(centroid.x), round(centroid.y));
        g2.drawImage( image, transform, null);
        g2.dispose();
        return outImage;
    }


	private static int round(double val) {
		return (int)(val +0.5);
	}
}