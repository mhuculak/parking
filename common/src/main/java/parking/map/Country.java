package parking.map;

import java.util.Map;
import java.util.HashMap;

//
//  FOLLOW: https://en.wikipedia.org/wiki/ISO_3166-1
//
public enum Country {
	CAN,
	USA;	

	public static final Map<String, Country> countryMap = new HashMap<String, Country>();

	static {
		countryMap.put("Canada", CAN);
		countryMap.put("United States", USA);
	}
}

