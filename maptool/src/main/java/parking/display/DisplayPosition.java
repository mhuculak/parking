package parking.display;

public class DisplayPosition {
	public double x;
	public double y;

	public DisplayPosition(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void update(DisplayPosition dp) {
		this.x = dp.x;
		this.y = dp.y;
	}	

	public static DisplayPosition delta(DisplayPosition p2, DisplayPosition p1) {
		return new DisplayPosition( p2.x - p1.x, p2.y - p1.y);
	}

	public static double distance(DisplayPosition p2, DisplayPosition p1) {
		DisplayPosition delta = delta(p2, p1);
		return Math.sqrt( delta.x*delta.x + delta.y*delta.y);
	}

	public String toString() {
		return x+" "+y;
	}
}