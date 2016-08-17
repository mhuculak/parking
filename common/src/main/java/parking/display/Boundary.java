package parking.display;

import parking.opencv.Pos;

import org.opencv.core.Point;

import java.util.List;
import java.util.ArrayList;

public class Boundary {
	private List<Point> points;
	
	public void add(Point p) {
		if (points == null) {
			points = new ArrayList<Point>();
		}
		points.add(p);
	}

	public void add(Pos p) {
		if (points == null) {
			points = new ArrayList<Point>();
		}
		points.add(new Point( p.x, p.y));
	}

	public int size() {
		if (points == null) {
			return 0;
		}
		return points.size();
	}

	public int[] getX() {
		int[] xpoints = new int[size()];
		for (int i=0 ; i<size() ; i++) {
			xpoints[i] = (int)(points.get(i).x + 0.5);
		}
		return xpoints;
	}

	public int[] getY() {
		int[] ypoints = new int[size()];
		for (int i=0 ; i<size() ; i++) {
			ypoints[i] = (int)(points.get(i).y + 0.5);
		}
		return ypoints;
	}
}