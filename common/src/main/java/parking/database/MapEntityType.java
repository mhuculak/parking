package parking.database;

import java.util.Map;
import java.util.HashMap;

public enum MapEntityType {
	Country,
	StateProvince,
	AdminRegion,
	City,
	Town,
	Street,
	StreetSegment;

	public static final Map<MapEntityType, MapEntityType> subType = new HashMap<MapEntityType, MapEntityType>();
	public static final Map<MapEntityType, MapEntityType> superType = new HashMap<MapEntityType, MapEntityType>();
	static{

		subType.put(Country, StateProvince);		
		subType.put(StateProvince, City);		
		subType.put(City, Town);
		subType.put(Town, Street);
		subType.put(Street, StreetSegment);

		superType.put(StateProvince, Country);		
		superType.put(City, StateProvince);		
		superType.put(Town, City);
		superType.put(Street, Town);
		superType.put(StreetSegment, Street);
	}
}