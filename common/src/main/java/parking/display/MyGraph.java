package parking.display;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

public class MyGraph {

	private double[] x1;
	private double[] y1;
	private double[] values1;
	private double[] values2;
	private int[] vals1;
	private int[] vals2;
	private BufferedImage image;
	private int width;
	private int height;
	private final int maxRange = 1000;
	private final int maxWidth = 1500;

	public MyGraph( double[] x1, double[] y1,  double[] x2, double[] y2) {
		this.x1 = x1;
		this.y1 = y1;
		height = maxRange;
		width = maxWidth;
	}
	
	public MyGraph( double[] values) {
		this.values1 = values;
		      	
      	height = maxRange;
      	width = values1.length;
	}

	public MyGraph( double[] values1, double[] values2) {
		this.values1 = values1;
		this.values2 = values2;     	
      	
      	height = maxRange;
      	width = 2*values1.length;
	}

	public MyGraph( int[] values1, int[] values2) {
		this.vals1 = values1;
		this.vals2 = values2;     	
      	
      	height = maxRange;
      	width = 2*values1.length;
	}

	public MyGraph( double[] values1, int[] values2) {
		this.values1 = values1;
		this.vals2 = values2;     	
      	
      	height = maxRange;
      	width = 2*values1.length;
	}

	public MyGraph( int[] values1, double[] values2) {
		this.vals1 = values1;
		this.values2 = values2;     	
      	
      	height = maxRange;
      	width = 2*values1.length;
	}

	public BufferedImage getGraph() {
		System.out.println("create graph " + width + " x " + height);
		image = new BufferedImage ( width, height, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g2 = image.createGraphics();
		g2.setColor( Color.black) ;
		g2.fillRect(0, 0, width, height);
		Shape l;
        g2.setColor(Color.blue);
		for ( int i=0 ; i<width ; i=i+50) {
			l = new Line2D.Double(i, 0, i, maxRange);
		}   
        g2.setColor(Color.red);
        g2.setStroke(new BasicStroke(1));
        if (x1 != null) {
			double maxY = 0;
			double maxX = 0;
			double minY = y1[0];
			double minX = x1[0];
			for ( int i=0 ; i<y1.length-1 ; i++ ) {
				maxY = maxY > y1[i] ? maxY : y1[i];
				maxX = maxX > x1[i] ? maxX : x1[i];
				minY = minY < y1[i] ? minY : y1[i];
				minX = minX < x1[i] ? minX : x1[i];
			}
			System.out.println("x min = "+minX+" max = "+maxX+" y min = "+minY+" max = "+maxY);
			double rnge = maxY - minY;
			double xr = maxX - minX;
			for ( int i=0 ; i<x1.length-1 ; i++ ) {
				int next = i==x1.length-1 ? 0 : i+1;
				l = new Line2D.Double((x1[i]-minX)*maxWidth/xr, (maxY-y1[i])*maxRange/rnge, (x1[next]-minX)*maxWidth/xr, (maxY-y1[next])*maxRange/rnge);
        		g2.draw(l);
			}
			g2.setColor(Color.blue);
			l = new Line2D.Double(0, maxY*maxRange/rnge, maxWidth, maxY*maxRange/rnge);
        	g2.draw(l);   
		}

		if (values1 != null) {
			double max = 0;
			double min = values1[0];
			for ( int i=0 ; i<values1.length-1 ; i++ ) {
				max = max > values1[i] ? max : values1[i];
				min = min < values1[i] ? min : values1[i];
			}
			double rnge = max - min;
			for ( int i=0 ; i<values1.length-1 ; i++ ) {
				l = new Line2D.Double(2*i, (max-values1[i])*maxRange/rnge, 2*i+2, (max-values1[i+1])*maxRange/rnge);
        		g2.draw(l);
			}
			g2.setColor(Color.blue);
			l = new Line2D.Double(0, max*maxRange/rnge, 2*(values1.length-1), max*maxRange/rnge);
        	g2.draw(l);   
		}
		if (vals1 != null) {
			for ( int i=0 ; i<vals1.length-1 ; i++ ) {
				l = new Line2D.Double(2*i, maxRange/2-vals1[i], 2*i+2, maxRange/2-vals1[i+1]);
        		g2.draw(l);
			}
			g2.setColor(Color.blue);
			l = new Line2D.Double(0,  maxRange/2, 2*vals1.length, maxRange/2);
        	g2.draw(l);
		}
		g2.setColor(Color.green);
		if (values2 != null) {
			double max = 0;
			double min = values1[0];
			for ( int i=0 ; i<values1.length-1 ; i++ ) {
				max = max > values1[i] ? max : values1[i];
				min = min < values1[i] ? min : values1[i];
			}
			double rnge = max - min;
			for ( int i=0 ; i<values2.length-1 ; i++ ) {
				l = new Line2D.Double(2*i, (max-values2[i])*maxRange/rnge, 2*i+2, (max-values2[i+1])*maxRange/rnge);
        		g2.draw(l);
			}
			g2.setColor(Color.blue);
			l = new Line2D.Double(0, max*maxRange/rnge, 2*(values2.length-1), max*maxRange/rnge);
        	g2.draw(l); 
		}
		if (vals2 != null) {
			for ( int i=0 ; i<vals2.length-1 ; i++ ) {
				l = new Line2D.Double(2*i, maxRange/2-vals2[i], 2*i+2, maxRange/2-vals2[i+1]);
        		g2.draw(l);
			}
		}
		
		g2.dispose();
		return image;
	}
}