package parking.opencv;

public class Pos {
	public int x;
	public int y;

	public Pos( int x, int y) {
		this.x = x;
		this.y = y;
	}

	public boolean equals(Pos p) {
		if (p.x == x && p.y == y) {
			return true;
		}
		return false;
	}
	public String toString() {
		return x+","+y;
	}

	public static double getDist(Pos p1, Pos p2) {
		int dx = p1.x - p2.x;
		int dy = p1.y - p2.y;
		return Math.sqrt(dx*dx + dy*dy);
	}
}