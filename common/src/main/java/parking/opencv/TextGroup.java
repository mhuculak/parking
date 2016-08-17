package parking.opencv;

import parking.util.Logger;
import parking.util.LoggingTag;

import org.opencv.core.Point;

import java.util.Collections;
import java.util.Comparator;

import java.util.List;
import java.util.ArrayList;

class TextShapeComparator implements Comparator<TextShape> {
	@Override
	public int compare(TextShape s1, TextShape s2) {
		if (s1.getMinX() < s2.getMinX()) {
			return -1;
		}
		else if (s1.getMinX() > s2.getMinX()) {
			return 1;
		}
		else {
			return 0;
		}
	}
}

public class TextGroup {
	
	private List<TextShape> shapes;
	private Line baseline;
	private List<Point> baselinePoints;
	private String rawText;
	private Logger m_logger;

//	private static final double maxDist = 5.0;
	private static final double maxDist = 10.0;	
	private static final double minValidRatio = 0.5;

	public TextGroup(TextShape shape, Logger logger) {
		m_logger = new Logger(logger, this, LoggingTag.Shape);
		shapes = new ArrayList<TextShape>();			
		shapes.add(shape);
		updateBaseline(shape.getBaseline());
		m_logger.log("New group with "+shape);
	}

	public boolean add(TextShape shape) {		
		double dist = getBaselineDist(shape);
		if (dist < maxDist) { 
			shapes.add(shape);				
			updateBaseline(shape.getBaseline());
			m_logger.log("add "+shape+" to group size = "+size());
			return true;
		}
		return false;
	}

	public int size() {
		if (shapes != null) {
			return shapes.size();
		}
		return 0;
	}

	public Line getBaseline() {
		return baseline;
	}

	public String getRawText() {
		return rawText;
	}

	public List<TextShape> getShapes() {
		return shapes;
	}

	public void sort() {
		TextShapeComparator comparator = new TextShapeComparator();
		Collections.sort(shapes, comparator);
		StringBuilder sb = new StringBuilder(10);
		for (TextShape shape : shapes) {
			if (shape.getTextMatches().size() > 0) {
				sb.append(shape.getTextMatches().get(0).getText());
			}
			else {
				sb.append("?");
			}
		}
		rawText = sb.toString();
	}

	public String toString() {
		return "size = "+size()+" baseline = "+baseline;
	}

	private void updateBaseline(Line l) {
		if (baselinePoints == null) {
			baselinePoints = new ArrayList<Point>();			
		}
		baselinePoints.add(l.p1);
		baselinePoints.add(l.p2);
		int numPoints = baselinePoints.size();
		if (numPoints > 2) {
			double[][] bpts = new double[numPoints][2];
			for ( int i=0 ; i<numPoints ; i++ ) {
				bpts[i][0] = baselinePoints.get(i).x;
				bpts[i][1] = baselinePoints.get(i).y;
			}	
			baseline = new Line(bpts, true, null);
		}
		else {
			baseline = l;
		}
	}

	private double getBaselineDist(TextShape shape) {
		Line l = shape.getBaseline();				
		double d1 = Line.getDistance(baseline, l.p1);
		double d2 = Line.getDistance(baseline, l.p2);
		return Math.sqrt(d1*d1 + d2*d2);
	}

	public static List<TextGroup> getTextGroups(List<TextShape> shapes, Logger logger) {
		List<TextGroup> textGroups = new ArrayList<TextGroup>();
		for ( TextShape shape : shapes) {
			if (shape.getTextMatches() != null  && shape.getTextMatches().size() > 0) {
				boolean added = false;
				int i=0;
				for ( i=0 ; i<textGroups.size() && added == false ; i++) {
					TextGroup group = textGroups.get(i);
					if (group.add(shape)) {
						added = true;
					}
				}
				if (added == false) {
					textGroups.add(new TextGroup(shape, logger));
				}
			}
			else if (shape.getTopologyX() != null && shape.getTopologyY() != null) {
				logger.log("Dropping "+shape.getTopologyX()+" "+shape.getTopologyY()+" bounds = "+shape.getBound());
			}
		}
		
		for ( TextGroup group : textGroups) {
			group.sort();
		}
		return textGroups;
	}
}