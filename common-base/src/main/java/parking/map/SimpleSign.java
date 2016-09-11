package parking.map;

public class SimpleSign {
	public String id;
	public Position position;
	public String schedule;

	public SimpleSign(String id, Position p, String s) {
		this.id = id;
		position = p;
		schedule = s;
	}
}