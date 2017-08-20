package parking.display;



//
//  used for placing cell on the display
//    x is longitude, y is latitude
//

public class CellPosition {
	public int x;
	public int y;

	public CellPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static CellPosition delta(CellPosition p2, CellPosition p1) {
		return new CellPosition( p2.x - p1.x, p2.y - p1.y);
	}

	public static double distance(CellPosition p2, CellPosition p1) {
		CellPosition delta = delta(p2, p1);
		return Math.sqrt( delta.x*delta.x + delta.y*delta.y);
	}

	public String toString() {
		return x+" "+y;
	}
}