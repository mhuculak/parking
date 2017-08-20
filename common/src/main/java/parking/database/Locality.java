package parking.database;

public class Locality {
	private String name;
	private MapEntityType type;

	public Locality(String name, MapEntityType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public MapEntityType getType() {
		return type;
	}
}