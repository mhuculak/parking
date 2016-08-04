package parking.map;

//
// Used to display sign marker on google map
//
public class SignMarker {
	
	private Position position;
	private String id;

	public SignMarker(String id, Position position) {
		this.id = id;
		this.position = position;
	}

	public Position getPosition() {
		return position;
	}

	public String getID() {
		return id;
	}

	public int getIDasInt() {
		return Integer.parseInt(id);
	}
}