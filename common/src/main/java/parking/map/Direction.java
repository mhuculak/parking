package parking.map;

public class Direction {
	private Position p1;
	private Position p2;

	public Direction(Position p1, Position p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public Position getP1() {
		return p1;
	}

	public Position getP2() {
		return p2;
	}
}